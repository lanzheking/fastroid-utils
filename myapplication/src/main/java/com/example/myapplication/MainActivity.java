package com.example.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.antfortune.freeline.FreelineCore;
import com.honestwalker.androidutils.IO.LogCat;
import com.honestwalker.androidutils.ImageSelector.ImageSelectListener;
import com.honestwalker.androidutils.ImageSelector.ImageSelectType;
import com.honestwalker.androidutils.ImageSelector.ImageSelector;
import com.honestwalker.androidutils.ImageSelector.ImageSelectorDialogPage;
import com.honestwalker.androidutils.ViewUtils.ViewSizeHelper;
import com.honestwalker.androidutils.equipment.DisplayUtil;
import com.honestwalker.androidutils.equipment.SDCardUtil;
import com.honestwalker.androidutils.ui.DesignUtil;
import com.honestwalker.androidutils.ui.Size;
import com.lidroid.xutils.ViewUtils;
import com.lidroid.xutils.view.annotation.ContentView;
import com.lidroid.xutils.view.annotation.ViewInject;
import com.lidroid.xutils.view.annotation.event.OnClick;

import java.io.File;
import java.util.ArrayList;

@ContentView(R.layout.activity_main)
public class MainActivity extends Activity {

    private ImageSelector imageSelector;

    @ViewInject(R.id.btn1)
    private Button btn;

    @ViewInject(R.id.imageview1)
    private ImageView iv;

    @ViewInject(R.id.layout1)
    private LinearLayout multiImgLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FreelineCore.init(getApplication());

        ViewUtils.inject(this);

        imageSelector = new ImageSelector(this);

        imageSelector.setImageSelectListener(imageSelectListener);

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void addImage(String path) {
        ImageView iv = new ImageView(this);
        iv.setLayoutParams(new ViewGroup.LayoutParams(300, 300));
        iv.setImageDrawable(BitmapDrawable.createFromPath(path));
        ViewSizeHelper.getInstance(this).marginLeft(iv, 50);
        multiImgLayout.addView(iv);
    }

    private ImageSelectListener imageSelectListener = new ImageSelectListener() {
        @Override
        public void onSelect() {

        }

        @Override
        public void onSelected(ImageSelectType type, ArrayList<String> imagePath) {

            LogCat.d("ImageSelector", "onSelected " + "  type=" + type);

            if(ImageSelectType.TYPE_MULTI_IMAGE_SELECTOR.equals(type)) {
                multiImgLayout.removeAllViews();
                for (String path : imagePath) {
                    addImage(path);
                }
            } else {
                LogCat.d("ImageSelector", "img=" + imagePath.get(0));
                iv.setImageDrawable(BitmapDrawable.createFromPath(imagePath.get(0)));
                Log.d("ImageSelector", "size: " + imagePath.size());
            }

        }

        @Override
        public void onCancel() {

        }

        @Override
        public void onComplete() {

        }
    };

    @OnClick(R.id.btn1)
    public void cameraBTNOnClick(View view1) {
        String outputPath = SDCardUtil.getSDRootPath() + "/test.jpg";
        imageSelector.openCamera(outputPath, 1024, 1080 , 1920);
    }

    @OnClick(R.id.btn2)
    public void cameraCropBTNOnClick(View view) {
        String outputPath = SDCardUtil.getSDRootPath() + "/test.jpg";
        imageSelector.openCameraAndCrop(outputPath, 1024, 1080 , 1920);
    }

    @OnClick(R.id.btn3)
    public void imgSelectBTNOnClick(View view) {
        imageSelector.selectImage();
    }

    @OnClick(R.id.btn4)
    public void imgSelectCropBTNOnClick(View view) {
        String outputPath = SDCardUtil.getSDRootPath() + "/test.jpg";
        imageSelector.selectImageAndCrop(outputPath , 1 , 1);
    }

    @OnClick(R.id.btn5)
    public void singleImgSelectOnClick(View view) {
        String outputPath = SDCardUtil.getSDRootPath() + "/test.jpg";
        imageSelector.singleSelectImageAndCrop(outputPath, 1 , 1);
    }

    @OnClick(R.id.btn6)
    public void multiImgSelectBTNOnClick(View view) {
        imageSelector.multiSelectImage(6);
    }

    @OnClick(R.id.btn7)
    public void dialogBTNOnClick(View views) {
        String outputPath = SDCardUtil.getSDRootPath() + "/test.jpg";
        ImageSelectorDialogPage dialog = new ImageSelectorDialogPage(this, outputPath);
        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        LogCat.d("ImageSelector", "requestCode=" + requestCode + "   resultCode=" + resultCode);
        if(imageSelector.isImageSelect(requestCode)) {
            imageSelector.onActivityResult(requestCode, resultCode, data);
        }
    }

}
