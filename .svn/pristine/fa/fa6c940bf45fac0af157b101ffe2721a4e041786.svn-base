package com.honestwalker.androidutils.ImageSelector;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.os.Process;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;

import com.honestwalker.androidutils.IO.LogCat;
import com.honestwalker.androidutils.ImageSelector.ImageScan.MultiImageSelectorActivity;
import com.honestwalker.androidutils.ImageSelector.utils.BroadcaseManager;
import com.honestwalker.androidutils.ImageSelector.utils.PictureUtil;
import com.honestwalker.androidutils.ImageUtil;
import com.honestwalker.androidutils.R;
import com.honestwalker.androidutils.ViewUtils.ViewSizeHelper;
import com.honestwalker.androidutils.equipment.DisplayUtil;
import com.honestwalker.androidutils.exception.ExceptionUtil;
import com.honestwalker.androidutils.system.ProcessUtil;

import java.io.File;
import java.util.ArrayList;

public class ImageSelectorAgentActivity extends Activity {

	private final String TAG = "ImageSelector";

	private ImageView previewIV;

	private Uri cameraOutputPath;
	private String outputPath = "";

	private ImageSelectType type;

	private boolean signleSelect = false;
	private int maxSelect = 9;

	private Uri imgCutUri;

	private int maxWidthPx = 0;

	private int aspectX;
	private int aspectY;

	private boolean needCut;

	private static String processName;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		processName = ProcessUtil.getCurProcessName(this);

		setContentView(R.layout.activity_imageselector_preview);

		initView();

		LogCat.d(TAG, "ImageSelectorAgentActivity 创建 图片选择进程 @ " + Process.myPid());

		Intent intent = getIntent();
		needCut    = intent.getBooleanExtra("needCut", false);
		maxWidthPx = intent.getIntExtra("maxWidth", 0);

		aspectX = intent.getIntExtra("aspectX" , 1);
		aspectY = intent.getIntExtra("aspectY" , 1);

		LogCat.d(TAG , "图片比例 " + aspectX + " : " + aspectY);

		LogCat.d(TAG, "intent maxWidth=" + maxWidthPx);
		type = (ImageSelectType) intent.getSerializableExtra(ImageSelectType.class.getSimpleName());

		signleSelect = intent.getBooleanExtra("signleSelect", false);
		outputPath = intent.getStringExtra("outputPath");
		LogCat.d(TAG , "图片输出到:" + outputPath);
		maxSelect  = intent.getIntExtra("maxSelect", 9);

		imgCutUri  = (Uri) intent.getParcelableExtra("imgCutUri");

		if(ImageSelectType.TYPE_CAMERA.equals(type)) {
			openCamera();
		} else if(ImageSelectType.TYPE_SINGLE_IMAGE_SELECTOR.equals(type)) {
			singleImageSelect();
		} else if(ImageSelectType.TYPE_MULTI_IMAGE_SELECTOR.equals(type)) {
			multiImageSelect();
		} else if(ImageSelectType.TYPE_IMAGE_SELECTOR.equals(type)) {
			sysImageSelect();
		} else if(ImageSelectType.TYPE_IMAGE_CUT.equals(type)) {
			toImageCut(imgCutUri);
		} else {
			setResult(RESULT_CANCELED);
		}

	}

	private void initView() {
		previewIV = (ImageView) findViewById(R.id.imageview1);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	private void keepIntent(Intent intent) {
		intent.putExtra("maxWidth" , maxWidthPx);
		intent.putExtra("aspectX", aspectX);
		intent.putExtra("aspectY", aspectY);
	}

	/**
	 * 打开摄像头开始拍照
	 */
	public void openCamera() {

		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		intent.putExtra(MediaStore.EXTRA_SCREEN_ORIENTATION, Configuration.ORIENTATION_PORTRAIT);

		try {
//        	cameraOutputPath = Uri.fromFile(new File(outputPath + System.currentTimeMillis() + ".jpg"));
			cameraOutputPath = Uri.fromFile(new File(outputPath));
		} catch (Exception e) {}

		intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraOutputPath);
//		intent.putExtra("android.intent.extra.screenOrientation", false);
		keepIntent(intent);

		this.startActivityForResult(intent, ImageSelector.REQUEST_CAMERA);

	}

	/** 打开图片剪切工具 */
	private void toImageCut(Uri resultUri) {
		Intent intentCut = new Intent("com.android.camera.action.CROP");
		//需要裁减的图片格式

		intentCut.setDataAndType(resultUri, "image/*");

		//允许裁减
		intentCut.putExtra("crop", "true");
		if(maxWidthPx > 0) {
			//剪裁后ImageView显时图片的宽
			intentCut.putExtra("outputX", maxWidthPx);
			int maxHeightPx = maxWidthPx * aspectY / aspectX;
			//剪裁后ImageView显时图片的高
			intentCut.putExtra("outputY", maxHeightPx);
			LogCat.d(TAG , "剪切最大像素 " + maxWidthPx);
		}
		//设置剪裁框的宽高比例
		intentCut.putExtra("aspectX", aspectX);
		intentCut.putExtra("aspectY", aspectY);
		intentCut.putExtra("scale", true);
//		intentCut.putExtra(MediaStore.EXTRA_OUTPUT, outputPath);                // 剪切工具保存到这个uri
		intentCut.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(outputPath)));                // 剪切工具保存到这个uri
//		intentCut.putExtra("return-data", true);                    // 加这个 小米2 会报错
		intentCut.putExtra("outputFormat", Bitmap.CompressFormat.PNG.toString());//返回格式 
		Log.d(TAG, "剪切图片将要输出到:" + outputPath);

		keepIntent(intentCut);

		this.startActivityForResult(intentCut, ImageSelector.REQUEST_IMAGE_CUT);
	}

	/** 系统图片选择工具 */
	public void sysImageSelect() {
		Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		this.startActivityForResult(
				Intent.createChooser(intent, "Select Picture"), ImageSelector.REQUEST_IMAGE_SELECT);
	}

	/** 单图片选择工具 */
	public void singleImageSelect() {
		Intent intent = new Intent(ImageSelectorAgentActivity.this,
				MultiImageSelectorActivity.class);
		intent.putExtra("signleSelect", signleSelect);
		intent.putExtra("maxSelect", maxSelect);
		this.startActivityForResult(intent, ImageSelector.REQUEST_SINGLE_IMAGE_SELECT);
	}
	/** 多图片选择工具 */
	public void multiImageSelect() {
		Intent intent = new Intent(ImageSelectorAgentActivity.this,
				MultiImageSelectorActivity.class);
		intent.putExtra("signleSelect", signleSelect);
		intent.putExtra("maxSelect", maxSelect);
		this.startActivityForResult(intent, ImageSelector.REQUEST_MULTI_IMAGE_SELECT);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		Log.d(TAG, "ImageSelectorAgentActivity OnActivityResult requestCode=" + requestCode + " resultCode=" + resultCode);

		if(resultCode == RESULT_CANCELED) {
			LogCat.d(TAG, "取消操作");
			Intent intent = new Intent();
			intent.putExtra("requestCode", requestCode);
			intent.putExtra("resultCode", resultCode);
			sendCallbackBroadcast(intent);
			finish();
			return;
		}

		Intent intent = new Intent();

		if(data == null) data = new Intent();

		intent.putExtra("requestCode", requestCode);
		intent.putExtra("resultCode", resultCode);
		intent.putExtra(ImageSelectType.class.getSimpleName(), type);

		maxWidthPx = intent.getIntExtra("maxWidth" , maxWidthPx);

		if(intent.hasExtra("aspectX")) {
			aspectX = intent.getIntExtra("aspectX" , 1);
		}
		if(intent.hasExtra("aspectY")) {
			aspectY = intent.getIntExtra("aspectY" , 1);
		}

		LogCat.d(TAG , "本地 比例 " + aspectX + " : " + aspectY);

		if(ImageSelector.REQUEST_CAMERA == requestCode) {

			intent.putExtra("imgPath", cameraOutputPath.getPath());

			if(maxWidthPx != 0) {
				ImageUtil imageUtl = new ImageUtil();

				imageUtl.imageZip(cameraOutputPath.getPath(), maxWidthPx);
				LogCat.d(TAG, "拍照完毕 图片压缩到 " + maxWidthPx);
			}

			int degree = PictureUtil.readPictureDegree(cameraOutputPath.getPath());
			LogCat.d(TAG , "拍照后图片方向: " + degree);

			if(degree != 90) {
				boolean rotateResult = PictureUtil.rotate(cameraOutputPath.getPath() , 90);
				if(rotateResult) {
					LogCat.d(TAG , "照片转正成功");
				} else {
					LogCat.d(TAG , "照片转正失败");
				}
			}

			showPreview(cameraOutputPath.getPath());

			ifNeedCut(intent);

		} else if (ImageSelector.REQUEST_IMAGE_SELECT == requestCode){

			Uri selectedImageUri = data.getData();

			String picturePath = "";
			if(selectedImageUri.toString().startsWith("content://")) {
				String[] filePathColumn = { MediaStore.Images.Media.DATA };
				Cursor cursor = getContentResolver().query(selectedImageUri, filePathColumn, null, null, null);
				cursor.moveToFirst();

				int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
				picturePath = cursor.getString(columnIndex);
				cursor.close();
			} else {
				picturePath = selectedImageUri.toString().replace("file://" , "");
			}

			Log.d(TAG, "图片选择完成 picturePath=" + picturePath);

			intent.putExtra("imgPath", picturePath);

			showPreview(picturePath);

			ifNeedCut(intent);

		} else if(ImageSelector.REQUEST_SINGLE_IMAGE_SELECT == requestCode) {
			ArrayList<String> imgPaths = data.getStringArrayListExtra("imgPaths");
			Log.d(TAG, "单图选择 " + imgPaths);
			if(imgPaths != null && imgPaths.size() >= 1) {
				intent.putExtra("imgPath", imgPaths.get(0));
			}
			if(data != null) {
				intent.setData(data.getData());
			}
			ifNeedCut(intent);
		} else if(ImageSelector.REQUEST_MULTI_IMAGE_SELECT == requestCode) {

			ArrayList<String> imgPaths = data.getStringArrayListExtra("imgPaths");
			Log.d(TAG, "多图选择 " + imgPaths);
			intent.putExtra("imgPaths", imgPaths);

		} else if(ImageSelector.REQUEST_IMAGE_CUT == requestCode) {

			try {
				if(data != null && data.getExtras() != null) {
					Bitmap cropedBitmap = data.getExtras().getParcelable("data");
					if(cropedBitmap == null) {
						Log.d(TAG, "返回数据没有图片，从" + outputPath + "中取。");
						cropedBitmap = BitmapFactory.decodeFile(outputPath);
						Log.d(TAG, "取得到的图片:" + cropedBitmap);
					} else {
						Log.d(TAG, "返回数据有图片");
					}
					if(cropedBitmap != null) {
						Log.d(TAG, "cropedBitmap = " + cropedBitmap.getWidth() + "   " + cropedBitmap.getHeight());
						// 清晰度设置
						File saveFile = new ImageUtil().bitmapToFile(outputPath, cropedBitmap, 100, cropedBitmap.getWidth());
//							Bitmap bitmap = new ImageUtil().bitmapZip(cropedBitmap, 100);
						if(saveFile != null) {
							Log.d(TAG, "保存图片到:" + saveFile.getPath() + "  " + saveFile.exists());
						} else {
							Log.d(TAG, "cropedBitmap 转图片失败 ；");
							saveFile = new File(outputPath);
						}
						intent.putExtra("imgPath", saveFile.getPath());
					} else {
						Log.d(TAG, outputPath + " bitmap 转图片失败 ");
					}

					try {
						if(!cropedBitmap.isRecycled()) {
							cropedBitmap.recycle();
						}
					} catch (Exception e) {}

				} else {
					Log.d(TAG, "data = null || data.getExtras() = null");
				}

			} catch (Exception e) {
				ExceptionUtil.showException(TAG , e);
			}

			sendCallbackBroadcast(intent);
			finish();

		}

//		if(needCut) {			// 需要图片剪切时，只有剪切完毕才finish
////			sendCallbackBroadcast(intent);
//
//			if(ImageSelector.REQUEST_IMAGE_CUT == requestCode) {
//				finish();
//			}
//		} else {
//			sendCallbackBroadcast(intent);
//			finish();
//		}

	}

	private void ifNeedCut(Intent intent) {
		if(needCut) {
			Uri uri = Uri.fromFile(new File(intent.getStringExtra("imgPath")));
			toImageCut(uri);
		} else {
			sendCallbackBroadcast(intent);
			finish();
		}
	}

	private void sendCallbackBroadcast(Intent intent) {
		LogCat.d(TAG , "发送广播 ");
		BroadcaseManager.sendBroadcast(ImageSelectorAgentActivity.this, ImageSelector.ACTION, intent);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
//		LogCat.d(TAG, "杀死图片选择进程 @ " + Process.myPid());
//		Process.killProcess(Process.myPid());
		try {
			previewIV.setImageBitmap(null);
			if(!previewBMP.isRecycled()) {
				previewBMP.recycle();
			}
		} catch (Exception e) {}
	}

	private Bitmap previewBMP;

	private void showPreview(String imgPath) {
		try {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			Bitmap bitmap = BitmapFactory.decodeFile(imgPath, options);
			int bitmapWidth = options.outWidth;
			int bitmapHeight = options.outHeight;
			options.inJustDecodeBounds = false;
			options.inSampleSize = 1;
			previewBMP = BitmapFactory.decodeFile(imgPath , options);
			previewIV.setImageBitmap(previewBMP);
			ViewSizeHelper.getInstance(this).setWidth(previewIV , DisplayUtil.getWidth(this) , bitmapWidth , bitmapHeight);
		} catch (Exception e) {
			ExceptionUtil.showException(e);
		}

	}

	public static String getProcessName() {
		return processName;
	}

}