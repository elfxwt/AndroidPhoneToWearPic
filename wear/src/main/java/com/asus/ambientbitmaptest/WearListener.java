package com.asus.ambientbitmaptest;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

/**
 * Created by sophia2 on 2015/10/13.
 */
public class WearListener implements DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static String TAG = "WearListener";
    private GoogleApiClient mGoogleApiClient;
    private Context mContext;
    public static String FILENAME="filename";
    public static final String RECEIVE_FILE = "receivefile";
    private static  String PACKAGE_FILE ;

    public void init(Context context){
        mContext = context;
        mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
        PACKAGE_FILE =  mContext.getFilesDir().getAbsolutePath();
    }





    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected ");
        Wearable.DataApi.addListener(mGoogleApiClient, this);

        File file = new File(PACKAGE_FILE);
        if (!file.exists()) {  // this file should already exist when launch the activity
            Log.d(TAG, "Target folder isn't exists.Try to build.");
            file.mkdir();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        Log.d(TAG, "onDataChanged(): " + dataEventBuffer);
        for (DataEvent event : dataEventBuffer) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                // DataItem changed
                Log.d(TAG, "Receive data from phone.");
                DataItem item = event.getDataItem();
                if (item.getUri().getPath().compareTo("/watchface/data/AmbientBitmapTest") == 0) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    final String filename = dataMap.getString("filename");
                    Log.d(TAG, "Get send file request form Phone.File Name=" + filename);
                    Asset asset = dataMap.getAsset("FileAsset");
                    if (asset != null) {
                        String tmppath = saveFile(mContext, filename, asset);

                        sendRecieveBmpBroadCast(tmppath);
                    }
                    return;
                }
                //molly ++++ for test
            }
        }

    }

    private String saveFile(Context context, String filename, Asset asset) {
        Log.d(TAG,"saveFile begin");
        String fullpath = null;
        GoogleApiClient mClient = new GoogleApiClient.Builder(context).addApi(Wearable.API).build();
        ConnectionResult result = mClient.blockingConnect(100, TimeUnit.SECONDS);
        if(!result.isSuccess()){
            Log.d(TAG, "loadBitmapFromAsset !"+result.isSuccess());
            return "";
        }

        InputStream assetInputStream = Wearable.DataApi.getFdForAsset(mClient, asset).await().getInputStream();
        mClient.disconnect();
        if(assetInputStream == null){
            Log.d(TAG, "loadBitmapFromAsset assetInputStream");
            return "";
        }
        else{
            fullpath = PACKAGE_FILE+"/"+filename;
            Log.d(TAG,"save file path is" + fullpath);
            File temp = new File(fullpath);
            if(temp.exists()){
                Log.d(TAG, "the file is already exist,delete it");
                boolean res = temp.delete();
                Log.d(TAG, "delete file result is " + res);
           }

            OutputStream stream = null;
            try{
                stream = new FileOutputStream(fullpath);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            if(stream != null){
                try {
                    byte[] buffer = new byte[assetInputStream.available()];
                    int len = -1;
//                    while((len = assetInputStream.read(buffer)) != -1){
//                        stream.write(buffer,0,len);
//                    }
                    assetInputStream.read(buffer);
                    stream.write(buffer);
                    stream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }finally {

                    try {
                        stream.close();
                        assetInputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }

        }

        Log.v(TAG,"saveFile end");

        return fullpath;
    }


    private void sendRecieveBmpBroadCast(String tmppath) {
        Intent i = new Intent();
        i.setAction(RECEIVE_FILE);
        i.putExtra(FILENAME,tmppath);
        mContext.sendBroadcast(i);

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
}
