package com.honestwalker.androidutils.views;

import android.graphics.Bitmap;

public interface AsyncCircleImageLoadListener {
	public void onStart(AsyncCircleImageView view );
	public void onComplete(AsyncCircleImageView view , Bitmap bitmap);
	public void onFail(AsyncCircleImageView view , Exception e);
}
