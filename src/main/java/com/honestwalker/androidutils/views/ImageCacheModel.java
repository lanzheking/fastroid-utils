package com.honestwalker.androidutils.views;

import com.honestwalker.androidutils.StringUtil;
import com.orm.androrm.Model;
import com.orm.androrm.field.BlobField;
import com.orm.androrm.field.CharField;

public class ImageCacheModel extends Model {
	
	/** 图片url */
	protected CharField url;
	/** 缓存图片资源 */
	protected BlobField image;
	/** 最后更新时间 */
	protected CharField createTime;
	
	public ImageCacheModel() {
		super();
		url   = new CharField(30);
		image   = new BlobField();
		createTime = new CharField(13);
	}

	public String getUrl() {
		return url.get();
	}

	public void setUrl(String url) {
		this.url.set(url);
	}

	public byte[] getImage() {
		return this.image.get();
	}

	public void setImage(byte[] image) {
		this.image.set(image);
	}
	
	public Long getCteateTime() {
		String createTimeStr = this.createTime.get(); 
		if(StringUtil.isEmptyOrNull(createTimeStr)) {
			return 0l;
		} else {
			return Long.parseLong(createTimeStr + "");
		}
	}

	public void setCteateTime(long createTime) {
		this.createTime.set(createTime + "");
	}
	
	
}
