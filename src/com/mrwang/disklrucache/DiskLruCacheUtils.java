package com.mrwang.disklrucache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.provider.MediaStore.Files;
import android.text.format.Formatter;

import com.mrwang.disklrucache.DiskLruCache.Editor;
import com.mrwang.disklrucache.DiskLruCache.Snapshot;
import com.mrwang.disklrucache.logutils.LogUtils;

/**
 * 硬盘缓存图片的工具类 默认图片缓存上限为10M
 * 
 * @author Administrator
 * 
 */
public class DiskLruCacheUtils {
	private Context context;
	private DiskLruCache mDiskLruCache;

	/**
	 * 创建对象,指定文件夹名称
	 * 
	 * @param context
	 *            当前上下文
	 * @param fileName
	 *            存放文件夹的名称 默认为bitmap
	 */
	public DiskLruCacheUtils(Context context, String fileName) {
		this.context = context;
		createDiskLruCache(fileName);
	}

	/**
	 * 创建对象 ,默认文件夹为bitmap
	 * 
	 * @param context
	 *            当前上下文
	 */
	public DiskLruCacheUtils(Context context) {
		this.context = context;
		createDiskLruCache("bitmap");
	}

	private DiskLruCache createDiskLruCache(String fileName) {
		File file = getDiskCacheDir(context, fileName);
		if (!file.exists()) {
			file.mkdirs();
		}
		try {
			// 创建一个10M的硬盘缓存
			mDiskLruCache = DiskLruCache.open(file, getAppVersion(context), 1,
					10 * 1024 * 1024);
		} catch (IOException e) {
			e.printStackTrace();
			LogUtils.d("创建缓存失败");
		}
		return mDiskLruCache;
	}

	/**
	 * 从网络上获取图片并写入本地缓存
	 * 
	 * @param imageUrl
	 *            图片URL地址
	 */
	public void writeDiskLruCache(final String imageUrl) {
		new Thread(){
			public void run() {
				try {
					// 将URl地址转化为MD5编码
					String key = hashKeyFroDisk(imageUrl);
					Editor edit = mDiskLruCache.edit(key);
					if (edit != null) {
						// 从编辑器获取输出流 (也可以获取输入流) 因为我们设置的是一个key对应一个文件,所以这里传入0
						OutputStream os = edit.newOutputStream(0);
						if (downLoadUrlSteam(imageUrl, os)) {
							// 提交编辑器
							edit.commit();
							LogUtils.i("1.存入成功");
						} else {
							// 退出编辑器
							edit.abort();
							LogUtils.i("1.存入失败");
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
					LogUtils.d("图片写入失败");
				}
			};
		}.start();
		
	}

	/**
	 * 读取缓存图片
	 * 
	 * @param imageUrl
	 *            图片的URL地址
	 * @return bitmap 读取到的图片<br/>
	 *         null 读取失败
	 */
	public Bitmap readDiskLruCache(String imageUrl) {
		try {
			String key = hashKeyFroDisk(imageUrl);
			Snapshot snapshot = mDiskLruCache.get(key);
			if (snapshot != null) {
				InputStream is = snapshot.getInputStream(0);
				Bitmap bitmap = BitmapFactory.decodeStream(is);
				return bitmap;
			}
		} catch (IOException e) {
			e.printStackTrace();
			LogUtils.d("图片读取失败");
		}
		return null;

	}

	/**
	 * 获取图片缓存路径
	 * 
	 * @param context
	 *            context
	 * @param uniqueName
	 *            需要传入的文件夹名称 如bitmap等
	 * @return
	 */
	private File getDiskCacheDir(Context context, String uniqueName) {
		String cachePath;
		if (Environment.MEDIA_MOUNTED.equals(Environment
				.getExternalStorageState())
				|| Environment.isExternalStorageRemovable()) {
			// 当SD卡存在或者SD卡不可移除
			// 直接在SD卡上创建目录
			cachePath = context.getExternalCacheDir().getPath();
		} else {
			// 没有SD卡或SD卡被移除的状态 获取内置存储的位置
			cachePath = context.getCacheDir().getPath();
		}
		// 中间一个参数File.separator代表"/"分隔符
		return new File(cachePath + File.separator + uniqueName);
	}

	/**
	 * 获取应用程序的版本号, 每次版本变化 均会导致DiskLruCache删除文件
	 * 
	 * @param context
	 * @return
	 */
	private int getAppVersion(Context context) {
		try {
			PackageInfo info = context.getPackageManager().getPackageInfo(
					context.getPackageName(), 0);
			return info.versionCode;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return 0;
	}

	private Boolean flag = false;

	/**
	 * 从网络获取图片写入到指定的输出流中
	 * 
	 * @param StringUrl
	 *            图片的Url地址
	 * @param os
	 *            输出流
	 * @return
	 */
	private boolean downLoadUrlSteam(final String StringUrl,
			final OutputStream os) {

		HttpURLConnection conn = null;
		BufferedOutputStream bos = null;
		BufferedInputStream bis = null;
		try {
			URL url = new URL(StringUrl);
			conn = (HttpURLConnection) url.openConnection();
			conn.setReadTimeout(5000);
			// 这里构造参数可以传一个缓存大小进去
			bis = new BufferedInputStream(conn.getInputStream(), 8 * 1024);
			bos = new BufferedOutputStream(os, 8 * 1024);
			int b;
			while ((b = bis.read()) != -1) {
				bos.write(b);
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();

		} finally {
			if (conn != null) {
				conn.disconnect();
			}
			try {
				if (bos != null) {
					bos.close();
				}
				if (bis != null) {
					bis.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	private String hashKeyFroDisk(String key) {
		String cacheKey;
		try {
			MessageDigest digest = MessageDigest.getInstance("MD5");
			digest.update(key.getBytes());
			cacheKey = bytesToHexString(digest.digest());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			cacheKey = String.valueOf(key.hashCode());
		}
		return cacheKey;
	}

	/**
	 * 将byte数组转化为16进制字母
	 * 
	 * @param bytes
	 * @return
	 */
	private String bytesToHexString(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < bytes.length; i++) {
			String hex = Integer.toHexString(0xFF & bytes[i]);
			if (hex.length() == 1) {
				sb.append('0');
			}
			sb.append(hex);
		}
		return sb.toString();
	}
	/**
	 * 从本地缓存中移除一张图片
	 * @param imageUrl
	 * @return
	 */
	public boolean remove(String imageUrl){
		try {
			String key = hashKeyFroDisk(imageUrl);
			mDiskLruCache.remove(key);
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			LogUtils.d("图片移除失败");
		}
		return false;
	}
	/**
	 * 获取当前本地缓存的大小
	 * @return
	 */
	public String getCacheSize(){
		return Formatter.formatFileSize(context, mDiskLruCache.size());
	}
	/**
	 * 将内存中的操作记录同步到日志文件（也就是journal文件）当中
	 * 建议Activity的onPause()方法中去调用一次
	 */
	public void flush(){
		try {
			mDiskLruCache.flush();
		} catch (IOException e) {
			e.printStackTrace();
			LogUtils.d("日志文件写入失败");
		}
	}
	/**
	 * 将DiskLruCache关闭掉(open()方法对应)<br/>
	 * 关闭掉了之后就不能再调用DiskLruCache中任何操作缓存数据的方法<br/>
	 * 建议在Activity的onDestroy()方法中去调用
	 */
	public void close(){
		try {
			mDiskLruCache.close();
		} catch (IOException e) {
			e.printStackTrace();
			LogUtils.d("DiskLruCache关闭失败");
		}
	}
	/**
	 * 将所有的缓存数据全部删除<br/>
	 */
	public void delete(){
		try {
			mDiskLruCache.delete();
		} catch (IOException e) {
			e.printStackTrace();
			LogUtils.d("本地缓存删除失败");
		}
	}
}
