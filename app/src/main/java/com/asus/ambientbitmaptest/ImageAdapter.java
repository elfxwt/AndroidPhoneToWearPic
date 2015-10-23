package com.asus.ambientbitmaptest;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.util.Log;
import android.util.LruCache;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Created by sophia2 on 2015/10/15.
 */
public class ImageAdapter extends BaseAdapter implements AbsListView.OnScrollListener{
    private Context mContext;
    private LruCache<String,Bitmap> mCacheData;
    Set<DecodeBmpFromDiskWorker> taskCollections;
    private SparseArray<String> mFileNames;
    private ListView mImgListView;

    private int mFirstVisibleItem;
    private int mVisibleItemCount;
    private boolean isFirstEnter = true;

    private String Tag = "ImageAdapter";

    public ImageAdapter(Context context,SparseArray<String> fileNames,ListView imgListView){
        this.mContext = context;
        this.mFileNames = fileNames;
        // this two way is the same number
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int memClass =((ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();

        Log.d(Tag,"maxMemory is " +maxMemory + " memClass is " + memClass);
        taskCollections = new HashSet<DecodeBmpFromDiskWorker>();
        int cacheSize = maxMemory/16; // there is something limited
        Log.d(Tag,"cacheSize is " + cacheSize);
        mCacheData = new LruCache<String,Bitmap>(cacheSize){
            @Override
            protected int sizeOf(String key, Bitmap value) {
                Log.d(Tag,"lruCahe sizeof is " + value.getByteCount()/1024);
                return value.getByteCount();
            }
        };
        this.mImgListView = imgListView;
        mImgListView.setOnScrollListener(this);

        }
    @Override
    public int getCount() {
        return mFileNames.size();
    }

    @Override
    public String getItem(int position) {
        return mFileNames.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }



    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Log.d(Tag,"getView is called");
        String filePath = getItem(position);
        ViewHolder viewHolder ;
        if(convertView == null){
            convertView = View.inflate(mContext,R.layout.listview_item,null);
            viewHolder = new ViewHolder();
            viewHolder.imageView = (ImageView) convertView.findViewById(R.id.id_img_item);
            convertView.setTag(viewHolder);
        }else
        viewHolder = (ViewHolder) convertView.getTag();
        viewHolder.imageView.setTag(filePath);
//        viewHolder.imageView.setImageBitmap(mFileNames.get(position));
        setImagView(filePath,viewHolder.imageView);
        return convertView;
    }
    static class ViewHolder{
        ImageView imageView;
    }

    private void setImagView(String filePath,ImageView imageView){
        Log.d(Tag,"setImagView");
        Bitmap bitmap = getBitmapFromCache(filePath);
        if(bitmap != null)
            imageView.setImageBitmap(bitmap);
        else
            imageView.setImageResource(R.drawable.p280);
    }


   private Bitmap getBitmapFromCache(String key){
      return mCacheData.get(key);

    }

    public void addBitmapToMemoryCache(String key,Bitmap bitmap){
        if(getBitmapFromCache(key) == null)
            mCacheData.put(key,bitmap);
    }


    private void loadBitmapFromDisk(int firstVisibleItem,int visibleItemCount){
        Log.d(Tag,"loadBitmapFromDisk");
        String path;
        try{
            for(int i = firstVisibleItem;i < visibleItemCount + firstVisibleItem;i++){
                path = mFileNames.get(i);
                Bitmap tmp = getBitmapFromCache(path);
                if(tmp == null){
                    Log.d(Tag,"loadbitmap " + i + " path is " +path);
                    DecodeBmpFromDiskWorker worker = new DecodeBmpFromDiskWorker();
                    taskCollections.add(worker);
                    worker.execute(path);
                }else{
                    ImageView imageView = (ImageView) mImgListView.findViewWithTag(path);
                    if(imageView != null)
                        imageView.setImageBitmap(tmp);
                }

            }

        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public void clear(){
        mContext = null;
        if(mCacheData != null){
            mCacheData.evictAll();
            mCacheData = null;
        }
        if(taskCollections != null){
            cancleAllTasks();
            taskCollections.clear();
            taskCollections = null;
        }

        if(mFileNames != null){
            mFileNames.clear();
            mFileNames = null;
        }

        mImgListView = null;


    }


    public void cancleAllTasks(){
        if(taskCollections != null){
            for(DecodeBmpFromDiskWorker task:taskCollections){
                task.cancel(false);
            }

        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        Log.d(Tag,"onScrollStateChanged");
        if (scrollState == SCROLL_STATE_IDLE) {
            loadBitmapFromDisk(mFirstVisibleItem,mVisibleItemCount);
        }else
            cancleAllTasks();


    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        Log.d(Tag,"onScroll");
        mFirstVisibleItem = firstVisibleItem;
        mVisibleItemCount = visibleItemCount;

        Log.d("sophia", "onscroll" + "firstVisibleItem = " + mFirstVisibleItem + " mvisibleitemcount=" + mVisibleItemCount);
        if(visibleItemCount == 0)  // at first,this number will be 0!
            mVisibleItemCount = 7;

        //首次进入当前 页面的时候要进行下载
        if(isFirstEnter && visibleItemCount >= 0){
            loadBitmapFromDisk(mFirstVisibleItem,mVisibleItemCount);
            isFirstEnter = false;
        }
    }





    class DecodeBmpFromDiskWorker extends AsyncTask<String,Void,Bitmap>{
        private String filePath = null;

        @Override
        protected Bitmap doInBackground(String... params) {
            filePath = params[0];
            Log.d(Tag," do inbackground filepath is " + filePath);
            Bitmap bitmap ;
            bitmap = BitmapFactory.decodeFile(filePath);
            if(bitmap == null)
                bitmap = BitmapFactory.decodeResource(mContext.getResources(),R.drawable.p280);  // 防范措施
            else
                addBitmapToMemoryCache(filePath,bitmap);
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            ImageView imageView = (ImageView) mImgListView.findViewWithTag(filePath);
            if(imageView != null)
                imageView.setImageBitmap(bitmap);
            taskCollections.remove(this);
        }
    }
}
