package com.honestwalker.androidutils.ImageSelector;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import com.honestwalker.androidutils.IO.LogCat;
import com.honestwalker.androidutils.ImageSelector.utils.BroadcaseManager;
import com.honestwalker.androidutils.system.ProcessUtil;

import java.io.File;
import java.util.ArrayList;

public class ImageSelector {
	
	private Activity mContext;
	
	private final String TAG = "ImageSelector";
	
	public final static int REQUEST_CAMERA = 15479401;
	public final static int REQUEST_IMAGE_SELECT = 15479402;
	public final static int REQUEST_IMAGE_CUT = 15479403;
	public final static int REQUEST_SINGLE_IMAGE_SELECT = 15479406;
	public final static int REQUEST_MULTI_IMAGE_SELECT = 15479404;
	
	private ImageSelectListener imageSelectListener;
	
	public static final String ACTION = "com.honestwalker.models.ImageSelector";
	
	/** 是否需要切割图片 */
	private boolean needCut = false;
	private String  outputPath = "";

	// 裁剪比例
	private int aspectX = 1;
	private int aspectY = 1;

	private int maxWidth = 1024;

	public ImageSelector(Activity context) {
		this.mContext = context;
	}
	
	public void openCamera(boolean needCut , String outputPath , int maxWidth , int aspectX , int aspectY) {
		this.needCut = needCut;
		this.outputPath = outputPath;
		this.aspectX = aspectX;
		this.aspectY = aspectY;
		this.maxWidth = maxWidth;
		BroadcaseManager.registerReceiver(mContext, ACTION, imageSelectorReceiver);
		
		Intent intent = new Intent(mContext , ImageSelectorAgentActivity.class);
		intent.putExtra(ImageSelectType.class.getSimpleName(), ImageSelectType.TYPE_CAMERA);
		intent.putExtra("outputPath", outputPath);
		intent.putExtra("aspectX", aspectX);
		intent.putExtra("aspectY" , aspectY);
		intent.putExtra("maxWidth" , maxWidth);
		mContext.startActivity(intent);
	}
	
	public void openCamera(String outputPath) {
		
		BroadcaseManager.registerReceiver(mContext, ACTION, imageSelectorReceiver);
		
		Intent intent = new Intent(mContext , ImageSelectorAgentActivity.class);
		intent.putExtra(ImageSelectType.class.getSimpleName(), ImageSelectType.TYPE_CAMERA);
		intent.putExtra("outputPath", outputPath);
		mContext.startActivity(intent);
		
	}
	
	private void toImageCut(Uri imgCutUri , int maxWidth) {
		Intent intent = new Intent(mContext , ImageSelectorAgentActivity.class);
		intent.putExtra(ImageSelectType.class.getSimpleName(), ImageSelectType.TYPE_IMAGE_CUT);
		intent.putExtra("outputPath", outputPath);
		intent.putExtra("imgCutUri", imgCutUri);
		intent.putExtra("aspectX" , aspectX);
		intent.putExtra("aspectY" , aspectY);
		intent.putExtra("maxWidth" , maxWidth);
		mContext.startActivity(intent);
	}
	
	public void selectImage(boolean needCut , String outputPath , int aspectX , int aspectY) {
		this.needCut = needCut;
		this.outputPath = outputPath;
		this.aspectX = aspectX;
		this.aspectY = aspectY;
		BroadcaseManager.registerReceiver(mContext, ACTION, imageSelectorReceiver);
		Intent intent = new Intent(mContext , ImageSelectorAgentActivity.class);
		intent.putExtra(ImageSelectType.class.getSimpleName(), ImageSelectType.TYPE_IMAGE_SELECTOR);
		intent.putExtra("needCut", needCut);
		intent.putExtra("outputPath" , outputPath);
		intent.putExtra("aspectX" , aspectX);
		intent.putExtra("aspectY", aspectY);
		intent.putExtra("maxWidth" , maxWidth);
		mContext.startActivity(intent);
	}
	
	public void selectImage() {
		BroadcaseManager.registerReceiver(mContext, ACTION , imageSelectorReceiver);
		Intent intent = new Intent(mContext , ImageSelectorAgentActivity.class);
		intent.putExtra(ImageSelectType.class.getSimpleName(), ImageSelectType.TYPE_IMAGE_SELECTOR);
		mContext.startActivity(intent);
	}
	
	/**
	 * 
	 * @param needCut
	 * @param outputPath  输出文件路径，包括文件名和后缀
	 */
	public void singleSelectImage(boolean needCut , String outputPath , int aspectX , int aspectY) {
		this.needCut = needCut;
		this.outputPath = outputPath;
		this.aspectX = aspectX;
		this.aspectY = aspectY;
		singleSelectImage();
	}
	
	public void singleSelectImage() {
		
		BroadcaseManager.registerReceiver(mContext, ACTION, imageSelectorReceiver);
		
		Intent intent = new Intent(mContext , ImageSelectorAgentActivity.class);
		intent.putExtra("signleSelect", true);
		intent.putExtra(ImageSelectType.class.getSimpleName(), ImageSelectType.TYPE_SINGLE_IMAGE_SELECTOR);
		intent.putExtra("aspectX" , aspectX);
		intent.putExtra("aspectY" , aspectY);
		mContext.startActivity(intent);
	}
	
	public void multiSelectImage(int maxSelect) {
		
		BroadcaseManager.registerReceiver(mContext, ACTION , imageSelectorReceiver);
		
		Intent intent = new Intent(mContext , ImageSelectorAgentActivity.class);
		intent.putExtra(ImageSelectType.class.getSimpleName(), ImageSelectType.TYPE_MULTI_IMAGE_SELECTOR);
		intent.putExtra("maxSelect", maxSelect);
		mContext.startActivity(intent);
	}
	
	public void setImageSelectListener(ImageSelectListener imageSelectListener) {
		this.imageSelectListener = imageSelectListener;
	}

	/** 图片选择广播接收者 */
	private BroadcastReceiver imageSelectorReceiver = new BroadcastReceiver(){ 
		
        @Override
        public void onReceive(Context context, Intent intent) {
			LogCat.d(TAG, "接收到广播");
        	if(intent != null) {
        		int resultCode = intent.getIntExtra("resultCode" , mContext.RESULT_CANCELED);
        		int requestCode = intent.getIntExtra("requestCode", -1);
        		
        		ArrayList<String> selectedImages = new ArrayList<String>();

				if(resultCode == Activity.RESULT_CANCELED) {
					unregisterReceiver();
					if(imageSelectListener != null) {
						imageSelectListener.onCancel();
						imageSelectListener.onComplete();
					}
				} else if((requestCode == REQUEST_IMAGE_SELECT || requestCode == REQUEST_CAMERA || requestCode == REQUEST_SINGLE_IMAGE_SELECT)) {
					String selectedImagePath = intent.getStringExtra("imgPath");
					if(selectedImagePath == null) {
						return;
					}
					selectedImages.add(selectedImagePath);

					Log.d(TAG, "需要图片剪切 " + ImageSelector.this.needCut);
					if(ImageSelector.this.needCut) {  // 如果需要剪切

						Bitmap bitmap = BitmapFactory.decodeFile(selectedImagePath);

						// 去图片剪切
						Uri resultUri = Uri.fromFile(new File(selectedImagePath));

						Uri mUri = Uri.parse("content://media/external/images/media");

						Cursor cursor = mContext.managedQuery(
								MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, null,
								null, MediaStore.Images.Media.DEFAULT_SORT_ORDER);
						cursor.moveToFirst();

						while (!cursor.isAfterLast()) {
							String data = cursor.getString(cursor
									.getColumnIndex(MediaStore.MediaColumns.DATA));
							if (selectedImagePath.equals(data)) {
								int ringtoneID = cursor.getInt(cursor
										.getColumnIndex(MediaStore.MediaColumns._ID));
								resultUri = Uri.withAppendedPath(mUri, ""
										+ ringtoneID);
								break;
							}
							cursor.moveToNext();
						}

//        				Uri resultUri = ( (intent == null) || resultCode != ((Activity)context).RESULT_OK ) ? null : intent.getData();

						Log.d(TAG, "将要剪切的图片:" + resultUri);

						if(resultUri != null) {
							toImageCut(resultUri , maxWidth);
							return;
						}
					}

					if(!needCut) {
						if(imageSelectListener != null) {
							ImageSelectType type = (ImageSelectType) intent.getSerializableExtra(ImageSelectType.class.getSimpleName());
							imageSelectListener.onSelected(type, selectedImages);
							unregisterReceiver();
							imageSelectListener.onComplete();
						}
					}

				} else if(requestCode == REQUEST_MULTI_IMAGE_SELECT) {
					ArrayList<String> imgPaths = intent.getStringArrayListExtra("imgPaths");
					selectedImages.addAll(imgPaths);
				} else if(requestCode == REQUEST_IMAGE_CUT) {
					String selectedImagePath = intent.getStringExtra("imgPath");
					selectedImages.add(selectedImagePath);

					if(imageSelectListener != null) {
						ImageSelectType type = (ImageSelectType) intent.getSerializableExtra(ImageSelectType.class.getSimpleName());
						imageSelectListener.onSelected(type, selectedImages);
						unregisterReceiver();
						imageSelectListener.onComplete();
					}

				}

        	}
        }
    };

	/**
	 * 反注册广播
	 */
	private void unregisterReceiver() {
		try {
			BroadcaseManager.unregisterReceiver(mContext, imageSelectorReceiver);
		} catch (Exception e) {
			LogCat.d(TAG , "广播已经回收");
		}
	}

	/**
	 * 回收资源
	 * @param context
	 */
	public void destroy(Context context) {
		LogCat.d(TAG , "图片选择流程完毕 ， getProcessName=" + ImageSelectorAgentActivity.getProcessName());
		if(ImageSelectorAgentActivity.getProcessName() != null) {
			ProcessUtil.killProcessByName(ImageSelectorAgentActivity.getProcessName());
		}
		unregisterReceiver();
	}

}
