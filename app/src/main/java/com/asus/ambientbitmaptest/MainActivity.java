package com.asus.ambientbitmaptest;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.LruCache;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Manifest;

/**
 * Created by sophia2 on 2015/10/13.
 */
public class MainActivity extends Activity implements AdapterView.OnItemClickListener{

    private static String PIC_PATH = "/sdcard/AmbientBitmapTest/pictures";
    private ArrayList<Bitmap> mData;
    private SparseArray<String> mFileNames;

    private static final String Tag= "MainActivity";
    private DiskImgWorkerTask diskImgWorkerTask;

    private ListView mlistview;
    private ImageAdapter imageAdapter;
    private static final int REQUESTCODE_STORAGE = 1;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //for Android M permissions and just for Api23
//        List<String> permissions = new ArrayList<String>();
//        int hasStorageReadPermission = checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE);
//        int hasStorageWritePermisson = checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
//        if(hasStorageReadPermission != PackageManager.PERMISSION_GRANTED )
//            permissions.add(android.Manifest.permission.READ_EXTERNAL_STORAGE);
//        if(hasStorageWritePermisson != PackageManager.PERMISSION_GRANTED)
//            permissions.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
//        requestPermissions(permissions.toArray(new String[permissions.size()]),REQUESTCODE_STORAGE);

        mlistview = (ListView) findViewById(R.id.id_listview);
//        mData = new ArrayList<Bitmap>();
//        mCacheData = new LruCache<String,Bitmap>();
        mFileNames = new SparseArray<String>();
        diskImgWorkerTask = new DiskImgWorkerTask();
        diskImgWorkerTask.execute();




    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        Intent i = new Intent(MainActivity.this,DataSendService.class);
        i.setAction(ConstValue.sendOnePic);
        Log.d(Tag,"position is " + i + "view is " + view.getId() + "long id --row id? " + id);
        String filePath = mFileNames.get(position);
        Log.d(Tag,"filepath is " + filePath );
        i.putExtra("filePath", filePath);
        startService(i);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        diskImgWorkerTask.cancel(true);
        diskImgWorkerTask = null;
        if(mFileNames != null){
            mFileNames.clear();
            mFileNames = null;
        }
        if(mlistview != null){
            mlistview.setAdapter(null);
            mlistview = null;
        }

        if(imageAdapter!=null){
            imageAdapter.clear();
            imageAdapter = null;
        }



    }

    class DiskImgWorkerTask extends AsyncTask<Void,Void,String>{

        @Override
        protected String  doInBackground(Void... params) {
            Log.d(Tag,"diskImgWorkerTasker doInbackground");
            try {
                File f= new File(PIC_PATH);
                    if(f.exists()){
                    File flist[] = f.listFiles();
                    if (flist == null || flist.length == 0) {
                        return null;
                    }
                    for (int i=0;i<flist.length;i++) {
                        File file = flist[i];
                        if(file !=null) {
                            Log.d("getFilePath",file.getAbsolutePath());
                            mFileNames.append(i, file.getAbsolutePath());
//                            mData.add(BitmapFactory.decodeFile(file.getAbsolutePath()));
                        }
                    }
                }
            } catch (Exception e){
                e.printStackTrace();
            }



                return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            imageAdapter = new ImageAdapter(MainActivity.this, mFileNames,mlistview);
            mlistview.setAdapter(imageAdapter);
            mlistview.setDivider(null);
            mlistview.setOnItemClickListener(MainActivity.this);




        }
    }

//    private final class  ImageAdapter extends BaseAdapter {
//
//        Context mContext;
//        public ImageAdapter(Context context,final List<Bitmap> items) {
//
//            mContext=context;
//        }
//
//        @Override
//        public int getCount() {
//            return 0;
//        }
//
//        @Override
//        public Object getItem(int position) {
//            return null;
//        }
//
//        @Override
//        public long getItemId(int position) {
//            return 0;
//        }
//
//        public View getView(int position, View view, ViewGroup viewGroup) {
//            RelativeLayout listItem;
//            Bitmap word = this.getItem(position);
//            if(view == null) {
//                listItem = (RelativeLayout) View.inflate(mContext,R.layout.listview_item,null);
//            } else {
//                if(view instanceof RelativeLayout) listItem = (RelativeLayout) view;
//                else {
//                    return view;
//                }
//            }
//            ImageView textview = (ImageView) listItem.findViewById(R.id.id_img_item);
//            textview.setImageBitmap(word);
//            return listItem;
//        }
//
//       class ViewHolder{
//            ImageView imageView;
//        }
//    }
}
