package com.asus.ambientbitmaptest;

import android.app.IntentService;
import android.content.Intent;
import android.content.IntentSender;
import android.media.browse.MediaBrowser;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * Created by sophia2 on 2015/10/13.
 */
public class DataSendService extends IntentService implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private GoogleApiClient mClient;
    private boolean mResolvingError = false;
    private static final String TAG = "DataSendService";

    private long magic;


    public DataSendService(){
        super("sendService");

    }

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public DataSendService(String name) {
        super(name);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG,"DataSendService is start.");
        mClient = new GoogleApiClient.Builder(this.getApplicationContext()).addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        magic = System.currentTimeMillis(); // not no its use;
        mClient.blockingConnect(100, TimeUnit.MILLISECONDS); // blockingConnect VC Connect
        if(ConstValue.sendOnePic.equals(intent.getAction())){
           String mFilePath = intent.getStringExtra("filePath");
            if(mFilePath != null && mFilePath.length()!=0){
                sendFileToWear(mFilePath);
            }
        }

    }

    private void sendFileToWear(String filePath) {
        Log.d(TAG,"in sendFileToWear.");
        File file = new File(filePath);
        if(file.exists()){
            Log.d(TAG, "file  exist");
            if(file != null){
                String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
                Asset asset = getFileFromPath(filePath);
                if(asset !=null) {
                    PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(ConstValue.wearPath);
                    putDataMapRequest.getDataMap().putLong("magic",magic);
                    putDataMapRequest.getDataMap().putString("filename",fileName);
                    putDataMapRequest.getDataMap().putAsset("FileAsset", asset);
                    PutDataRequest request = putDataMapRequest.asPutDataRequest();
                    DataApi.DataItemResult result = Wearable.DataApi.putDataItem(mClient, request).await();
                    Log.d(TAG, "send file result=" + result.getStatus());
                }
            }
        }


    }

    private Asset getFileFromPath(String path) {
        Log.d(TAG, "in getFileFromPath.");
        Asset ret = null;
        //this is test translate apk file
        InputStream is = null;
        try {
            is = new BufferedInputStream(new FileInputStream(path));
            byte buffer[] = new byte[is.available()];
            is.read(buffer);
            ret = Asset.createFromBytes(buffer);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return ret;
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected");
        mResolvingError = false;

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed");
        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (connectionResult.hasResolution()) {
            try {
                mResolvingError = true;
                connectionResult.startResolutionForResult(null, 1000);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                mClient.connect();
            }
        } else {
            mResolvingError = false;
        }

    }
}
