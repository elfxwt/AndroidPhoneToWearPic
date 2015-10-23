package com.asus.ambientbitmaptest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.service.wallpaper.WallpaperService;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.widget.Toast;

import java.io.File;

/**
 * Created by sophia2 on 2015/10/13.
 */
public class AmbientWatchface extends CanvasWatchFaceService {
    private static final String TAG = "AmbientWatchface";

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    public class Engine extends CanvasWatchFaceService.Engine {

        SparseArray<String> data;

        protected Time mTime;
        private Bitmap mAmBg=null;
        protected final Context mContext = getApplicationContext();
        protected BitmapFactory.Options mOp;
        private WearListener wearListener;
        private boolean mAmbient;
        private boolean mLowBitAmbient;

        private float mLastUpInScreenX , mLastUpInScreenY ;
        private long mLastClickTime ;
        private static final long DOUBLE_CLICK_TIME = 500;

    public BroadcastReceiver notifyReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.v(TAG, "action=" + intent.getAction());
                if(intent.getAction().equalsIgnoreCase(wearListener.RECEIVE_FILE)){
                    String filename = intent.getStringExtra(WearListener.FILENAME);
                    Log.d(TAG,"Recieve filename is " + filename);
                    if(filename!=null && !filename.equals("")){
                        Toast toast = Toast.makeText(mContext, "Receive the image", Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
                        toast.show();
                        mAmBg = getBmpFromFile(filename);
                        invalidate();
                    }

                }
            }
        };

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            Log.v(TAG, "onCreate");

            mLastClickTime = 0;
            mLastUpInScreenX = 0;
            mLastUpInScreenX = 0;
            mTime = new Time();
            wearListener = new WearListener();
            wearListener.init(mContext);
            WatchFaceStyle.Builder mWatchfaceBuilder = new WatchFaceStyle.Builder(AmbientWatchface.this);
            mWatchfaceBuilder
//                    .setViewProtection(WatchFaceStyle.PROTECT_STATUS_BAR | WatchFaceStyle.PROTECT_HOTWORD_INDICATOR)
//                    .setPeekOpacityMode(WatchFaceStyle.PEEK_OPACITY_MODE_TRANSLUCENT)
//                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT).setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
//                    .setShowSystemUiTime(false)
//                    .setAmbientPeekMode(WatchFaceStyle.AMBIENT_PEEK_MODE_HIDDEN)
                    .setAcceptsTapEvents(true); // build() !!!

//            mWatchfaceBuilder.setAmbientPeekMode(WatchFaceStyle.AMBIENT_PEEK_MODE_HIDDEN);


            setWatchFaceStyle(mWatchfaceBuilder.build()); // pay attention to set the style
            int density=getResources().getDisplayMetrics().densityDpi;
            mOp=new  BitmapFactory.Options();
            mOp.inDensity = density;
//            mAmBg = getBmpFromFile("");
            mContext.registerReceiver(notifyReceiver,new IntentFilter(WearListener.RECEIVE_FILE));


        }

        private Bitmap getBmpFromFile(String path){
            Bitmap ret= null;
            try {
                File f= new File(path);
                if(f.exists()){
                    Log.v(TAG,"test file exists path="+path);
                    ret = BitmapFactory.decodeFile(path, mOp);
                }
            } catch (Exception e){
                e.printStackTrace();
            }
            if(ret ==null) {
                int bg = mContext.getResources().getIdentifier("p" + mOp.inDensity, "drawable", getPackageName());
                ret = BitmapFactory.decodeResource(mContext.getResources(), bg, mOp);
            }
            return ret;
        }

        @Override
        public void onTapCommand(@TapType int tapType, int x, int y, long eventTime) {
            Log.d(TAG,"onTapCommend tapType "+ tapType + "x is "+x + " y is " + y );
            Log.d(TAG,"onTapCommend mLastClickTime "+ mLastClickTime + " mLastUpInScreenX "+
                    mLastUpInScreenX + " mLastUpInScreenY " + mLastUpInScreenY + " eventTime " + eventTime);
            super.onTapCommand(tapType, x, y, eventTime);
//            if(eventTime >= DOUBLE_CLICK_TIME){
//                 return;
//            }
            switch (tapType){
                case WatchFaceService.TAP_TYPE_TAP:
                    Log.d(TAG,"go to Tap_type_tap");
                    if(Math.abs(mLastUpInScreenX - x) <15 && Math.abs(mLastUpInScreenY - y) < 15 ) {
                        // make sure for the same point
                        Log.d(TAG,"location is ok");
                        if (System.currentTimeMillis() - mLastClickTime <= DOUBLE_CLICK_TIME) {
                            Log.d(TAG,"time is ok");
                            Toast toast = Toast.makeText(mContext, "Double Click", Toast.LENGTH_SHORT);
                            toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
                            toast.show();
                        }
                    }
                    mLastClickTime = System.currentTimeMillis();
                    mLastUpInScreenX = x;
                    mLastUpInScreenY = y;
                    break;
            }


        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            super.onDraw(canvas, bounds);
            if(mAmBg != null)
                canvas.drawColor(Color.WHITE);
            if(mAmbient){
                drawAmbientView(canvas);
            }else{
                drawInterView(canvas);
            }

        }




        private void drawInterView(Canvas canvas) {
            canvas.drawColor(Color.WHITE);
            if (mAmBg != null && mAmBg.getWidth() < canvas.getWidth() && mAmBg.getHeight() < canvas.getHeight()) {
                canvas.drawBitmap(mAmBg, canvas.getWidth() / 2 - mAmBg.getWidth() / 2, canvas.getHeight() / 2 - mAmBg.getHeight() / 2, null);
            } else if (mAmBg != null) {
                canvas.drawBitmap(mAmBg, 0, 0, null);
            }
        }

        private void drawAmbientView(Canvas canvas) {
            canvas.drawColor(Color.BLACK);
            if(mAmBg !=null && mAmBg.getWidth()< canvas.getWidth() && mAmBg.getHeight()< canvas.getHeight()) {
                canvas.drawBitmap(mAmBg, canvas.getWidth() / 2 - mAmBg.getWidth() / 2, canvas.getHeight() / 2 - mAmBg.getHeight() / 2, null);
            }else if(mAmBg!=null){
                canvas.drawBitmap(mAmBg,0,0,null);
            }
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            mAmbient = inAmbientMode;
            invalidate();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(WatchFaceService.PROPERTY_LOW_BIT_AMBIENT, false);
            Log.d(TAG, "onPropertiesChanged mLowBitAmbient=" + mLowBitAmbient);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            mContext.unregisterReceiver(notifyReceiver);
            if(mAmBg != null){
                mAmBg.recycle();
                mAmBg = null;

            }
        }
    }
}
