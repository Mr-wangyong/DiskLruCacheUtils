package com.mrwang.disklrucachedemo;

import com.mrwang.disklrucache.DiskLruCacheUtils;
import com.mrwang.disklrucache.logutils.LogUtils;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.ImageView;

public class MainActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		ImageView iv=(ImageView)findViewById(R.id.iv);
		LoadImage(iv);
	}

	private void LoadImage(ImageView iv) {
		//图片地址
		String imageUrl = "http://img.my.csdn.net/uploads/201309/01/1378037235_7476.jpg";
		//获取utils对象
		DiskLruCacheUtils cacheUtils = new DiskLruCacheUtils(this);
		//写入到本地缓存中
		cacheUtils.writeDiskLruCache(imageUrl);
		//由于从网络获取图片需要时间,请在此sleep一下
		SystemClock.sleep(1000);
		//从本地缓存中读取
		Bitmap bitmap = cacheUtils.readDiskLruCache(imageUrl);
		//设置到界面上
		if (bitmap==null) {
			LogUtils.i("读取失败");
		}
		iv.setImageBitmap(bitmap);
	}
}
