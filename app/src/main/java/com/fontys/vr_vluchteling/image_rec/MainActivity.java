package com.fontys.vr_vluchteling.image_rec;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.fontys.vr_vluchteling.R;
import com.google.vrtoolkit.cardboard.CardboardActivity;
import com.google.vrtoolkit.cardboard.CardboardView;
import com.google.vrtoolkit.cardboard.Eye;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.Viewport;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import pl.itraff.TestApi.ItraffApi.ItraffApi;
import pl.itraff.TestApi.ItraffApi.model.APIObject;
import pl.itraff.TestApi.ItraffApi.model.APIResponse;

/**
 * Created by Wouter on 8-3-2016!
 */
public class MainActivity extends CardboardActivity implements CardboardView.StereoRenderer, SurfaceTexture.OnFrameAvailableListener {
    private static final String TAG = "MainActivity";
    private Handler iTraffApiHandler;
    private File mFile;
    private Camera camera;

    //Google SDK fields
    private static final int GL_TEXTURE_EXTERNAL_OES = 0x8D65;
    private FloatBuffer vertexBuffer, textureVerticesBuffer;
    private ShortBuffer drawListBuffer;
    private int mProgram;
    // number of coordinates per vertex in this array
    static final int COORDS_PER_VERTEX = 2;
    static float squareVertices[] = { // in counterclockwise order:
            -1.0f, -1.0f, // 0.left - mid
            1.0f, -1.0f, // 1. right - mid
            -1.0f, 1.0f, // 2. left - top
            1.0f, 1.0f,  // 3. right - top

    };
    private short drawOrder[] =  {0, 2, 1, 1, 2, 3 }; // order to draw vertices
    static float textureVertices[] = {
            0.0f, 1.0f,  // A. left-bottom
            1.0f, 1.0f,  // B. right-bottom
            0.0f, 0.0f,  // C. left-top
            1.0f, 0.0f   // D. right-top
    };
    private int texture;
    private CardboardOverlayView mOverlayView;
    private CardboardView cardboardView;
    private SurfaceTexture surface;

    //Cardboard Lookthrough
    private float[] mView;
    private float[] mCamera;
    boolean introFinished = false;

    public void startCamera(int texture){
        surface = new SurfaceTexture(texture);
        surface.setOnFrameAvailableListener(this);

        try
        {
            if(camera==null) {
                camera = Camera.open();
            }
            camera.setPreviewTexture(surface);
            camera.startPreview();
        }
        catch (IOException ioe)
        {
            Log.w("MainActivity","CAM LAUNCH FAILED");
        }
    }

    static private int createTexture(){
        int[] texture = new int[1];

        GLES20.glGenTextures(1,texture, 0);
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, texture[0]);
        GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MIN_FILTER,GL10.GL_LINEAR);
        GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

        return texture[0];
    }

    /**
     * Converts a raw text file, saved as a resource, into an OpenGL ES shader
     * @param type The type of shader we will be creating.
    // * @param resId The resource ID of the raw text file about to be turned into a shader.
     * @return
     */
    private int loadGLShader(int type, String code) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, code);
        GLES20.glCompileShader(shader);

        // Get the compilation status.
        final int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

        // If the compilation failed, delete the shader.
        if (compileStatus[0] == 0) {
            Log.e(TAG, "Error compiling shader: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            shader = 0;
        }

        if (shader == 0) {
            throw new RuntimeException("Error creating shader.");
        }

        return shader;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.common_ui);

        cardboardView = (CardboardView) findViewById(R.id.cardboard_view);
        cardboardView.setRenderer(this);
        cardboardView.setOnCardboardBackButtonListener(new Runnable() {
            @Override
            public void run() {
                onBackPressed();
            }
        });
        setCardboardView(cardboardView);

        mCamera = new float[16];
        mView = new float[16];

        final Vibrator mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        if(mOverlayView == null) {
            mOverlayView = (CardboardOverlayView) findViewById(R.id.overlay);
        }


        YoYo.with(Techniques.FadeInUp).duration(1000).playOn(findViewById(R.id.logoLeft));
        YoYo.with(Techniques.FadeInUp).duration(1000).playOn(findViewById(R.id.logoRight));
        final Handler h = new Handler();
        h.postDelayed(new Runnable(){
            public void run(){
                //do something
                YoYo.with(Techniques.FadeOutUp).duration(1000).playOn(findViewById(R.id.logoLeft));
                YoYo.with(Techniques.FadeOutUp).duration(1000).playOn(findViewById(R.id.logoRight));

                YoYo.with(Techniques.FadeInUp).duration(1000).playOn(findViewById(R.id.gifLeft));
                YoYo.with(Techniques.FadeInUp).duration(1000).playOn(findViewById(R.id.gifRight));

                final Handler h = new Handler();
                h.postDelayed(new Runnable(){
                    public void run(){

                    }
                }, 1800);
            }
        }, 1800);

        // Callback from API
        iTraffApiHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Bundle data = msg.getData();
                if (data != null) {
                    APIResponse response = (APIResponse) data.getSerializable(ItraffApi.RESPONSE);

                    String message = "";
                    try {
                        JSONObject JSONResponse = new JSONObject();
                        JSONResponse.put("status", response.getStatus()); // set status , 0 = success , 0 != error

                        if (response.getStatus() == 0) {
                            //List<APIObject> mijnlijst = response.getObjects();

                            JSONResponse.put("id", response.getObjects().get(0).getId());
                            JSONResponse.put("name", response.getObjects().get(0).getName());
                            mOverlayView.show3DToast("Succes");

                            Intent ToolBoxActivity = new Intent(MainActivity.this, com.google.unity.GoogleUnityActivity.class);
                            startActivity(ToolBoxActivity);
                        } else {
                            JSONResponse.put("error", response.getMessage());
                            mVibrator.vibrate(1000);
                            mOverlayView.show3DToast("Where is the Poster?");
                            camera.startPreview();
                        }
                        // message is for debugging purposes.
                        message = JSONResponse.toString();

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    // startDetailsActivity(mFile, message);
                }
            }
        };
    }

    /**
     * Is called when a picture has been taken and saved Wraps the take picture in a byteArray,
     * to be send with the api call.
     */
    private void makeApiCall() {
        byte[] pictureData;
        Bitmap image = null;
        InputStream fis = null;
        try {
            if(mFile!= null) {
                fis = new FileInputStream(mFile);
            }else{
                Log.e(TAG, "File is null.");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = false;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        options.inDither = true;
        options.inSampleSize = 4;
        image = BitmapFactory.decodeStream(fis, null, options);

        if (image == null) {
            return;
        }
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        pictureData = stream.toByteArray();
        image.recycle();
        image = null;

        if (pictureData != null) {
            // check internet connection
            if (ItraffApi.isOnline(getActivity().getApplicationContext())) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);

                // send photo
                String CLIENT_API_KEY = "70952f8b88";
                Integer CLIENT_API_ID = 45590;
                ItraffApi api = new ItraffApi(CLIENT_API_ID, CLIENT_API_KEY, TAG, true);

                if (prefs.getString("mode", "single").equals("multi")) {
                    api.setMode(ItraffApi.MODE_MULTI);
                } else {
                    api.setMode(ItraffApi.MODE_SINGLE);
                }

                api.sendPhoto(pictureData, iTraffApiHandler, prefs.getBoolean("allResults", true));
            } else {
                mOverlayView.show3DToast("No Internet connection.");
                camera.startPreview();
            }
        }
    }

    /**
     * Increment the score, hide the object, and give feedback if the user pulls the magnet while
     * looking at the object. Otherwise, remind the user what to do.
     */
    @Override
    public void onCardboardTrigger() {
        Log.e(TAG, "onCardboardTrigger4");
        //TODO remove this and create a intro dude.
        introFinished = true;
        camera.takePicture(null, null, mPicture);
        mOverlayView.showGifImage(R.drawable.cardboard_help);

    }

    /**
     * Helper method to access the camera returns null if it cannot get the
     * camera or does not exist
     *
     * @return
     */
    Camera.PictureCallback mPicture = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            mFile = getOutputMediaFile();
            if (mFile == null) {
                return;
            }
            try {
                FileOutputStream fos = new FileOutputStream(mFile);
                fos.write(data);
                fos.close();
                makeApiCall();

            } catch (FileNotFoundException e) {

            } catch (IOException e) {

            }
        }
    };

    private static File getOutputMediaFile() {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "VrFontys");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.e("VrFontys", "failed to create directory");
                return null;
            }
        }
        // Create a media file name
        return new File(mediaStorageDir.getPath() + File.separator + "IMG_VrScan.jpg");
    }


    @Override
    public void onRendererShutdown() {
        Log.i(TAG, "onRendererShutdown");
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        Log.i(TAG, "onSurfaceChanged");
    }

    /**
     * Creates the buffers we use to store information about the 3D world. OpenGL doesn't use Java
     * arrays, but rather needs data in a format it can understand. Hence we use ByteBuffers.
     * @param config The EGL configuration used when creating the surface.
     */
    @Override
    public void onSurfaceCreated(EGLConfig config) {
        Log.i(TAG, "onSurfaceCreated");
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 0.5f); // Dark background so text shows up well

        ByteBuffer bb = ByteBuffer.allocateDirect(squareVertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(squareVertices);
        vertexBuffer.position(0);

        ByteBuffer dlb = ByteBuffer.allocateDirect(drawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);


        ByteBuffer bb2 = ByteBuffer.allocateDirect(textureVertices.length * 4);
        bb2.order(ByteOrder.nativeOrder());
        textureVerticesBuffer = bb2.asFloatBuffer();
        textureVerticesBuffer.put(textureVertices);
        textureVerticesBuffer.position(0);

        String vertexShaderCode = "attribute vec4 position;" +
                "attribute vec2 inputTextureCoordinate;" +
                "varying vec2 textureCoordinate;" +
                "void main()" +
                "{" +
                "gl_Position = position;" +
                "textureCoordinate = inputTextureCoordinate;" +
                "}";
        int vertexShader = loadGLShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        String fragmentShaderCode = "#extension GL_OES_EGL_image_external : require\n" +
                "precision mediump float;" +
                "varying vec2 textureCoordinate;                            \n" +
                "uniform samplerExternalOES s_texture;               \n" +
                "void main(void) {" +
                "  gl_FragColor = texture2D( s_texture, textureCoordinate );\n" +
                //"  gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0);\n" +
                "}";
        int fragmentShader = loadGLShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        mProgram = GLES20.glCreateProgram();             // create empty OpenGL ES Program
        GLES20.glAttachShader(mProgram, vertexShader);   // add the vertex shader to program
        GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
        GLES20.glLinkProgram(mProgram);

        texture = createTexture();
        startCamera(texture);

    }

    /**
     * Prepares OpenGL ES before we draw a frame.
     * @param headTransform The head transformation in the new frame.
     */
    @Override
    public void onNewFrame(HeadTransform headTransform) {
        float[] mtx = new float[16];
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        surface.updateTexImage();
        surface.getTransformMatrix(mtx);

    }
    /**
     * Draws a frame for an eye. The transformation for that eye (from the camera) is passed in as
     * a parameter.
     * @param eye The transformations to apply to render this eye.
     */
    @Override
    public void onDrawEye(Eye eye) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        GLES20.glUseProgram(mProgram);

        GLES20.glActiveTexture(GL_TEXTURE_EXTERNAL_OES);
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, texture);

        int mPositionHandle = GLES20.glGetAttribLocation(mProgram, "position");
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        int vertexStride = COORDS_PER_VERTEX * 4;
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);

        int mTextureCoordHandle = GLES20.glGetAttribLocation(mProgram, "inputTextureCoordinate");
        GLES20.glEnableVertexAttribArray(mTextureCoordHandle);
        GLES20.glVertexAttribPointer(mTextureCoordHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, textureVerticesBuffer);
        int mColorHandle = GLES20.glGetAttribLocation(mProgram, "s_texture");

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mTextureCoordHandle);

        Matrix.multiplyMM(mView, 0, eye.getEyeView(), 0, mCamera, 0);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture arg0) {
        this.cardboardView.requestRender();

    }

    @Override
    public void onFinishFrame(Viewport viewport) {

    }

    public Context getActivity() {
        return this;
    }
}
