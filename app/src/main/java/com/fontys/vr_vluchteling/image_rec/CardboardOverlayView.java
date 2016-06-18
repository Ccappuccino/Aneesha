/*
 * Copyright 2014 Google Inc. All Rights Reserved.

 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fontys.vr_vluchteling.image_rec;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.opengl.Visibility;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.fontys.vr_vluchteling.R;

import pl.droidsonroids.gif.GifImageView;

/**
 * Contains two sub-views to provide a simple stereo HUD.
 */
public class CardboardOverlayView extends LinearLayout {
    private static final String TAG = CardboardOverlayView.class.getSimpleName();
    private final CardboardOverlayEyeView mLeftView;
    private final CardboardOverlayEyeView mRightView;
    private AlphaAnimation mTextFadeAnimation;

    public CardboardOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(HORIZONTAL);

        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1.0f);
        params.setMargins(0, 0, 0, 0);

        mLeftView = new CardboardOverlayEyeView(context, attrs);
        mLeftView.setLayoutParams(params);
        addView(mLeftView);

        mRightView = new CardboardOverlayEyeView(context, attrs);
        mRightView.setLayoutParams(params);
        addView(mRightView);

        // Set some reasonable defaults.
        setDepthOffset(0.08f);
        setIds();
        setVisibility(View.VISIBLE);

    }

    public void show3DToast(String message) {
        setText(message);
        setTextAlpha(1f);
        YoYo.with(Techniques.FadeIn).duration(1000).playOn(findViewById(R.id.TextViewLeft));
        YoYo.with(Techniques.FadeIn).duration(1000).playOn(findViewById(R.id.TextViewRight));

        final Handler h2 = new Handler();
        h2.postDelayed(new Runnable(){
            public void run(){
                YoYo.with(Techniques.FadeOut).duration(1000).playOn(findViewById(R.id.TextViewLeft));
                YoYo.with(Techniques.FadeOut).duration(1000).playOn(findViewById(R.id.TextViewRight));
            }
        }, 3000);
    }


    private void setIds(){
        mLeftView.setLogoId(R.id.logoLeft);
        mRightView.setLogoId(R.id.logoRight);

        mLeftView.setAneeshaGifId(R.id.gifAneeshaLeft);
        mRightView.setAneeshaGifId(R.id.gifAneeshaRight);

        mLeftView.setHelpGifId(R.id.gifHelpLeft);
        mRightView.setHelpGifId(R.id.gifHelpRight);

        mLeftView.setTextViewId(R.id.TextViewLeft);
        mRightView.setTextViewId(R.id.TextViewRight);

    }
    private void setDepthOffset(float offset) {
        mLeftView.setOffset(offset);
        mRightView.setOffset(-offset);
    }

    private void setText(String text) {
        mLeftView.setText(text);
        mRightView.setText(text);
    }

    private void setTextAlpha(float alpha) {
        mLeftView.setTextViewAlpha(alpha);
        mRightView.setTextViewAlpha(alpha);
    }

    public void setAneeshaVisible(int v){
        mLeftView.setAneeshaGifVisible(v);
        mRightView.setAneeshaGifVisible(v);
    }
    /**
     * A simple view group containing some horizontally centered text underneath a horizontally
     * centered image.
     *
     * This is a helper class for CardboardOverlayView.
     */
    private class CardboardOverlayEyeView extends ViewGroup {

        private final ImageView imageView;
        private final TextView textView;
        private final GifImageView gib;
        private final GifImageView gifAneesha;
        private float offset;

        public CardboardOverlayEyeView(Context context, AttributeSet attrs) {
            super(context, attrs);
            imageView = new ImageView(context, attrs);
            imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            imageView.setAdjustViewBounds(true);  // Preserve aspect ratio.
            imageView.setImageResource(R.drawable.le_logo);
            imageView.setAlpha(0f);
            addView(imageView);



            gib = new GifImageView(context, attrs);
            gib.setScaleType(GifImageView.ScaleType.CENTER_INSIDE);
            gib.setAdjustViewBounds(true);  // Preserve aspect ratio.
            gib.setImageResource(R.drawable.cardboard_help);
            gib.setAlpha(0f);
            addView(gib);

            gifAneesha = new GifImageView(context, attrs);
            gifAneesha.setScaleType(GifImageView.ScaleType.CENTER_INSIDE);
            gifAneesha.setAdjustViewBounds(true);  // Preserve aspect ratio.
            gifAneesha.setImageResource(R.drawable.aneesha_gif2);
            gifAneesha.setVisibility(View.GONE);
            addView(gifAneesha);

            textView = new TextView(context, attrs);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.0f);
            Typeface tf = Typeface.createFromAsset(context.getAssets(), "fonts/Avenir-Book.ttf");
            textView.setTypeface(tf);
            textView.setGravity(Gravity.CENTER);
            textView.setShadowLayer(3.0f, 0.0f, 0.0f, Color.DKGRAY);
            textView.setTextColor(Color.BLACK);
            addView(textView);
        }

        public void setLogoId(int id){
            imageView.setId(id);
        }
        public void setAneeshaGifId(int id){
            gifAneesha.setId(id);
        }
        public void setHelpGifId(int id){
            gib.setId(id);
        }
        public void setTextViewId(int id){
            textView.setId(id);
        }
        public void setAneeshaGifVisible(int v){
            gifAneesha.setVisibility(v);
        }
        public void setHelpGifVisible(int v){
            gib.setVisibility(v);
        }

        public void setText(String text) {
            textView.setText(text);
        }
        public void setGifImage(int drawable) {
            gib.setImageResource(drawable);
        }
        public void setTextViewAlpha(float alpha) {
            textView.setAlpha(alpha);
        }

        public void setOffset(float offset) {
            this.offset = offset;
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            // Width and height of this ViewGroup.
            final int width = right - left;
            final int height = bottom - top;

            // The size of the image, given as a fraction of the dimension as a ViewGroup. We multiply
            // both width and heading with this number to compute the image's bounding box. Inside the
            // box, the image is the horizontally and vertically centered.
            final float imageSize = 0.72f;
            final float gibSize = 0.72f;

            // The fraction of this ViewGroup's height by which we shift the image off the ViewGroup's
            // center. Positive values shift downwards, negative values shift upwards.
            final float verticalImageOffset = -0.07f;
            final float verticalGibSizeOffset = -0.07f;

            // Vertical position of the text, specified in fractions of this ViewGroup's height.
            final float verticalTextPos = 0.52f;

            // Layout ImageView
            float imageMargin = (1.0f - imageSize) / 2.0f;
            float leftMargin = (int) (width * (imageMargin + offset));
            float topMargin = (int) (height * (imageMargin + verticalImageOffset));
            imageView.layout(
                (int) leftMargin, (int) topMargin,
                (int) (leftMargin + width * imageSize), (int) (topMargin + height * imageSize));
            gib.layout(
                    (int) leftMargin, (int) topMargin,
                    (int) (leftMargin + width * imageSize), (int) (topMargin + height * imageSize));

            // Layout GifView
            float gifMargin = (1.0f - gibSize) / 2.0f;
            float gifLeftMargin = (int) (width * (gifMargin + offset));
            float gifTopMargin = (int) (height * (gifMargin*3 + verticalGibSizeOffset));
            gifAneesha.layout(
                    (int) gifLeftMargin, (int) gifTopMargin,
                    (int) (gifLeftMargin + width * gibSize), (int) (gifTopMargin + height * gibSize));


            // Layout TextView
            leftMargin = offset * width;
            topMargin = height * verticalTextPos;
            textView.layout(
                (int) leftMargin, (int) topMargin,
                (int) (leftMargin + width), (int) (topMargin + height * (1.0f - verticalTextPos)));



        }
    }
}
