package com.honestwalker.androidutils.ViewUtils;

import android.content.Context;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

public class RelactiveLayoutParamsGetter {
	public static RelativeLayout.LayoutParams getLayoutParams(Context context,View view,int width,int height) {
		RelativeLayout.LayoutParams lp = null;
		try {
			lp = (LayoutParams) view.getLayoutParams();
			if(lp == null) {
				lp = new RelativeLayout.LayoutParams(width,height);
			}
		} catch (Exception e) {
			if(lp == null) {
				lp = new RelativeLayout.LayoutParams(width,height);
			} else {
				lp.width  = width;
				lp.height = height;
			}
		}
		return lp;
	}
}
