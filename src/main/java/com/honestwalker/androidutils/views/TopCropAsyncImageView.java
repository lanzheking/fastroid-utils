package com.honestwalker.androidutils.views;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView.ScaleType;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.honestwalker.androidutils.UIHandler;
import com.honestwalker.androidutils.IO.LogCat;
import com.honestwalker.androidutils.ViewUtils.ViewSizeHelper;
import com.honestwalker.androidutils.exception.ExceptionUtil;
import com.honestwalker.androidutils.pool.ThreadPool;
import com.orm.androrm.DatabaseAdapter;
import com.orm.androrm.Filter;
import com.orm.androrm.Model;

/**
 * 支持异步的ImageView 最好是用于需要异步的列表，否则static的imageCache无法及时释放 <br />
 * 初始化时设置高宽度才能时progressbar居中 <br />
 * 图片默认顶部对齐，高度超出部分将被剪切
 * @author honestwalker
 *
 */
public class TopCropAsyncImageView extends RelativeLayout {
	
	private String TAG = "AsyncImageView";
	
	private static ExecutorService threadPool = Executors.newFixedThreadPool(20);
	
	public static Map<String,SoftReference<Bitmap>> imageCache = new HashMap<String, SoftReference<Bitmap>>();
	
	private String imageUrl = null;
	
	private AsyncLoadListener asyncLoadListener = null;
	
	private boolean isLoaded = false;
	
	/** 是否启用 sqlite 图片缓存 */
	private boolean useDBCache = true;
	
	/** 是否根据指定的width 计算相应的inSampleSize, 开启比较耗时，但可一定程度防止OOM错误 ， 一般图片都较大时才开启 */
	private boolean caleInSampleSize = false;
	
	private Context context;
	
	private TopCropImageView   imageView;
	private ProgressBar progressBar;
	
	private LayoutInflater inflater;
	
	public TopCropAsyncImageView(Context context) {
		super(context);
		this.context = context;
//		initDB();
		createView();
	}
	
	public TopCropAsyncImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
//		initDB();
		createView();
	}
	
	public TopCropAsyncImageView(Context context, AttributeSet attrs , int defStyle) {
		super(context, attrs);
		this.context = context;
//		initDB();
		createView();
	}

	/** 初始化数据库 在application调用 */
	public static void initDB(Context context , int databaseVersion) {
	     List<Class<? extends Model>> models = new ArrayList<Class<? extends Model>>();
	     models.add(ImageCacheModel.class);         // 要建立的表

	    // 建立数据库
	     DatabaseAdapter.setDatabaseName("KANCART_DB");

	     DatabaseAdapter adapter = DatabaseAdapter.getInstance(context , databaseVersion);
	     adapter.setModels(models);   // 开始创建
	}
	
	/** 建立图片和loading */
	private void createView() {
		
		inflater = ((Activity)context).getLayoutInflater();
		
		progressBar = new ProgressBar(context);
		LayoutParams lp = new LayoutParams(20,20);
		lp.addRule(RelativeLayout.CENTER_IN_PARENT);
		progressBar.setLayoutParams(lp);
		this.addView(progressBar);
		
		imageView   = new TopCropImageView(context);
		
		LayoutParams ivlp = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		imageView.setLayoutParams(ivlp);
		this.addView(imageView);
		
	}
	
	public void setScaleType(ScaleType scaleType) {
		imageView.setScaleType(scaleType);
	}
	 
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
//		LogCat.d(TAG,  "(getHeight() / 2)=" + (getHeight() / 2) + "   (progressBar.getHeight() / 2)=" + (progressBar.getHeight() / 2));
//		ViewSizeHelper.getInstance(context).setWidth(progressBar, 15);
//		ViewSizeHelper.getInstance(context).setHeight(progressBar, 15);
//		ViewSizeHelper.getInstance(context).marginTop(progressBar, (getHeight() / 2) - (progressBar.getHeight() / 2));
	}
	
	/**
	 * 异步加载图片
	 * @param imageUrl
	 * @param useDBCache       是否使用数据库缓存
	 * @param useMemCache      是否使用内存缓存
	 */
	public void loadUrl(String imageUrl,final int width , boolean useDBCache , boolean useMemCache) {
		
		this.useDBCache = useDBCache;
		if(!useMemCache) {
			imageCache.remove(imageUrl);
		}
		
		loadUrl(imageUrl, width);
		
	}
	
	public void loadUrl(String imageUrl) {
		this.imageUrl = imageUrl;
		loadImageFromUrl(imageUrl, new AsyncLoadListener() {
			@Override
			public void onComplete(Bitmap bitmap) {
				if(bitmap != null) {
					TopCropAsyncImageView.this.imageView.setImageBitmap(bitmap);
					int sw = bitmap.getWidth();
					int sh = bitmap.getHeight();
//					int height = width * sh / sw;
//					ViewSizeHelper.getInstance(getContext()).setWidth(AsyncImageView.this, (int)(DisplayUtil.getWidth(context)  * 0.4));
//					ViewSizeHelper.getInstance(getContext()).setHeight(AsyncImageView.this, (int)(DisplayUtil.getWidth(context)  * 0.4 * 0.5) );
					if(asyncLoadListener != null) {
						asyncLoadListener.onComplete(bitmap);
					}
					isLoaded = true;
				} else {
					if(asyncLoadListener != null) {
						asyncLoadListener.onFail(new NullPointerException());
					}
				}
			}
			@Override
			public void onFail(Exception e) {
				if(asyncLoadListener != null) {
					asyncLoadListener.onFail(e);
				}
			}
			@Override
			public void onStart() {
				if(asyncLoadListener != null) {
					asyncLoadListener.onStart();
				}
			}
		});
	
	}
	
	/**
	 * 异步加载图片
	 * @param imageUrl
	 */
	public void loadUrl(String imageUrl,final int width) {
		this.imageUrl = imageUrl;
		loadImageFromUrl(imageUrl, width, new AsyncLoadListener() {
			@Override
			public void onComplete(Bitmap bitmap) {
				if(bitmap != null) {
					TopCropAsyncImageView.this.imageView.setImageBitmap(bitmap);
					int sw = bitmap.getWidth();
					int sh = bitmap.getHeight();
					int height = width * sh / sw;
					ViewSizeHelper.getInstance(getContext()).setWidth(TopCropAsyncImageView.this, width);
					ViewSizeHelper.getInstance(getContext()).setHeight(TopCropAsyncImageView.this, height);
					if(asyncLoadListener != null) {
						asyncLoadListener.onComplete(bitmap);
					}
					isLoaded = true;
				} else {
					if(asyncLoadListener != null) {
						asyncLoadListener.onFail(new NullPointerException());
					}
				}
			}
			@Override
			public void onFail(Exception e) {
				if(asyncLoadListener != null) {
					asyncLoadListener.onFail(e);
				}
			}
			@Override
			public void onStart() {
				if(asyncLoadListener != null) {
					asyncLoadListener.onStart();
				}
			}
		});
	}
	
	public void loadUrl(String imageUrl,final int width, final AsyncLoadListener asyncLoadListener) {

		this.imageUrl = imageUrl;
		loadImageFromUrl(imageUrl, width, new AsyncLoadListener() {
			@Override
			public void onComplete(Bitmap bitmap) {
				if(bitmap != null) {
					TopCropAsyncImageView.this.imageView.setImageBitmap(bitmap);
					int sw = bitmap.getWidth();
					int sh = bitmap.getHeight();
					int height = width * sh / sw;
					ViewSizeHelper.getInstance(getContext()).setWidth(TopCropAsyncImageView.this, width);
					ViewSizeHelper.getInstance(getContext()).setHeight(TopCropAsyncImageView.this, height);
					if(asyncLoadListener != null) {
						asyncLoadListener.onComplete(bitmap);
					}
					isLoaded = true;
				} else { 
					if(asyncLoadListener != null) {
						asyncLoadListener.onFail(new NullPointerException());
					}
				}
			}
			@Override
			public void onFail(Exception e) {
				if(asyncLoadListener != null) {
					asyncLoadListener.onFail(e);
				}
			}
			@Override
			public void onStart() {
				if(asyncLoadListener != null) {
					asyncLoadListener.onStart();
				}
			}
		});
	}
	
	public interface AsyncLoadListener {
		public void onStart();
		public void onComplete(Bitmap bitmap);
		public void onFail(Exception e);
	}
	
	/**
	 * 异步加载图片
	 * @param url 图片url
	 * @param width 需要的图片宽度
	 * @param imageUrlLoadListener 加载监听
	 */
	private synchronized void loadImageFromUrl(final String url ,final AsyncLoadListener imageUrlLoadListener ) {
		if(imageUrlLoadListener != null) {
			imageUrlLoadListener.onStart();
		}
		if(imageCache.containsKey(url)) {
			final SoftReference<Bitmap> sbitmap = imageCache.get(url);
			if(sbitmap != null && sbitmap.get() != null) {
				if(imageUrlLoadListener != null) {
					UIHandler.post(new Runnable() {
						@Override
						public void run() {
							imageUrlLoadListener.onComplete(sbitmap.get());
							return;
						}
					});
				}
			}
		} else if(useDBCache) {
	        UIHandler.post(new Runnable() {
				@Override
				public void run() {
					Filter filter = new Filter();
					filter.is("url", url);   // 查找条件， 查找指定uname的用户
					List<ImageCacheModel> icList = null;
					try {
						icList = ImageCacheModel.objects(context, ImageCacheModel.class).filter(filter).all().toList();
					} catch (Exception e) {}
					if(icList != null && icList.size() > 0) {
						ImageCacheModel ic = icList.get(0);
						final byte[] data = ic.getImage();
						ThreadPool.threadPool(new Runnable() {
							@Override
							public void run() {
								if(data != null) {
									final Bitmap bmp = BitmapFactory.decodeByteArray(data,0,data.length);
									if(bmp != null) {
										UIHandler.post(new Runnable() {
											@Override
											public void run() {
												imageUrlLoadListener.onComplete(bmp);
												imageCache.put(url, new SoftReference<Bitmap>(bmp));
											}
										});
									} else {
										loadFromNet(url, imageUrlLoadListener);
									}
								} else {
									loadFromNet(url, imageUrlLoadListener);
								}
							}
						});
					} else {
						loadFromNet(url, imageUrlLoadListener);
					}
				}
			});
		} else {
			loadFromNet(url , imageUrlLoadListener);
		}
		
	}
	
	/**
	 * 异步加载图片
	 * @param url 图片url
	 * @param width 需要的图片宽度
	 * @param imageUrlLoadListener 加载监听
	 */
	private synchronized void loadImageFromUrl(final String url , final int width ,final AsyncLoadListener imageUrlLoadListener ) {
		if(imageUrlLoadListener != null) {
			imageUrlLoadListener.onStart();
		}
		if(imageCache.containsKey(url)) {
			final SoftReference<Bitmap> sbitmap = imageCache.get(url);
			if(sbitmap != null && sbitmap.get() != null) {
				if(imageUrlLoadListener != null) {
					UIHandler.post(new Runnable() {
						@Override
						public void run() {
							imageUrlLoadListener.onComplete(sbitmap.get());
							return;
						}
					});
				}
			}
		} else if(useDBCache) {
	        UIHandler.post(new Runnable() {
				@Override
				public void run() {
					Filter filter = new Filter();
					filter.is("url", url);   // 查找条件， 查找指定uname的用户
					List<ImageCacheModel> icList = null;
					try {
						icList = ImageCacheModel.objects(context, ImageCacheModel.class).filter(filter).all().toList();
					} catch (Exception e) {}
					if(icList != null && icList.size() > 0) {
						ImageCacheModel ic = icList.get(0);
						final byte[] data = ic.getImage();
						ThreadPool.threadPool(new Runnable() {
							@Override
							public void run() {
								if(data != null) {
									final Bitmap bmp = BitmapFactory.decodeByteArray(data,0,data.length);
									if(bmp != null) {
										UIHandler.post(new Runnable() {
											@Override
											public void run() {
												imageUrlLoadListener.onComplete(bmp);
												imageCache.put(url, new SoftReference<Bitmap>(bmp));
											}
										});
									} else {
										loadFromNet(url, width, imageUrlLoadListener);
									}
								} else {
									loadFromNet(url, width, imageUrlLoadListener);
								}
							}
						});
					} else {
						loadFromNet(url, width, imageUrlLoadListener);
					}
				}
			});
		} else {
			loadFromNet(url, width, imageUrlLoadListener);
		}
		
	}
	
	private void loadFromNet(final String url ,final AsyncLoadListener imageUrlLoadListener ) {
		threadPool.execute(new Runnable() {
			@Override
			public void run() {
				try {
//					String fixUrl = url;
//					if(fixUrl.indexOf("屏幕快照 2013-10-28 下午3_43_44") > 0) {
//						fixUrl = fixUrl.replace("屏幕快照 2013-10-28 下午3_43_44", java.net.URLEncoder.encode("屏幕快照 2013-10-28 下午3_43_44"));
//					
//					}
//					org.apache.commons.httpclient.URI uri = new org.apache.commons.httpclient.URI(fixUrl, false	, "UTF-8");
//					fixUrl = uri.toString();  
					
					org.apache.commons.httpclient.URI uri = new org.apache.commons.httpclient.URI(url, false, "UTF-8");
					
					URL aURL = new URL( uri.toString() );
					URLConnection con = aURL.openConnection();
					con.connect();
					InputStream is = con.getInputStream();
					
					BitmapFactory.Options opts = new BitmapFactory.Options();
					
					int inSampleSize = 1;
					
					// 重新取得连接取得bitmap
					con = aURL.openConnection();
					con.connect();
					is = con.getInputStream();
					
					BufferedInputStream bis = new BufferedInputStream(is);
					
					opts.inSampleSize = inSampleSize;
					final Bitmap bm = BitmapFactory.decodeStream(bis,null,opts);
					bis.close();
					is.close();
					
					if(imageUrlLoadListener != null) {
						if(bm == null) {
							imageUrlLoadListener.onFail(new Exception("读取不到图片."));
						} else {
							imageCache.put(url, new SoftReference<Bitmap>(bm));
							ImageCacheModel imageCache = new ImageCacheModel();
							imageCache.setCteateTime(System.currentTimeMillis());
							
							ByteArrayOutputStream os = new ByteArrayOutputStream();
							if(url.toLowerCase().endsWith(".png")) {
								bm.compress(Bitmap.CompressFormat.PNG, 100, os);  // 100是清晰度 100最清晰
							} else {
								bm.compress(Bitmap.CompressFormat.JPEG, 100, os);
							}
							imageCache.setImage(os.toByteArray());
							os.close();
							
							imageCache.setUrl(url);
							
							try {
								imageCache.save(context);
							} catch (Exception e) {
							}
							
							UIHandler.post(new Runnable() {
								@Override
								public void run() {
									imageUrlLoadListener.onComplete(bm);
								}
							});
						}
					}
					
				} catch (final Exception e) {
					ExceptionUtil.showException("ImageCache",e);
					if(imageUrlLoadListener != null) {
						UIHandler.post(new Runnable() {
							@Override
							public void run() {
								imageUrlLoadListener.onFail(e);
							}
						});
					}
				}
			}
		});
	}
	
	private void loadFromNet(final String url , final int width ,final AsyncLoadListener imageUrlLoadListener ) {
		threadPool.execute(new Runnable() {
			@Override
			public void run() {
				try {
//					String fixUrl = url;
//					if(fixUrl.indexOf("屏幕快照 2013-10-28 下午3_43_44") > 0) {
//						fixUrl = fixUrl.replace("屏幕快照 2013-10-28 下午3_43_44", java.net.URLEncoder.encode("屏幕快照 2013-10-28 下午3_43_44"));
//					
//					}
//					org.apache.commons.httpclient.URI uri = new org.apache.commons.httpclient.URI(fixUrl, false	, "UTF-8");
//					fixUrl = uri.toString();  
					
					org.apache.commons.httpclient.URI uri = new org.apache.commons.httpclient.URI(url, false, "UTF-8");
					
					URL aURL = new URL( uri.toString() );
					URLConnection con = aURL.openConnection();
					con.connect();
					InputStream is = con.getInputStream();
					
					BitmapFactory.Options opts = new BitmapFactory.Options();
					
					int inSampleSize = 1;
					if(caleInSampleSize) {
						inSampleSize = getOptimalInSampleSize(is,width);
					}
					
					// 重新取得连接取得bitmap
					con = aURL.openConnection();
					con.connect();
					is = con.getInputStream();
					
					BufferedInputStream bis = new BufferedInputStream(is);
					
					opts.inSampleSize = inSampleSize;
					final Bitmap bm = BitmapFactory.decodeStream(bis,null,opts);
					bis.close();
					is.close();
					
					if(imageUrlLoadListener != null) {
						if(bm == null) {
							imageUrlLoadListener.onFail(new Exception("读取不到图片."));
						} else {
							imageCache.put(url, new SoftReference<Bitmap>(bm));
							ImageCacheModel imageCache = new ImageCacheModel();
							imageCache.setCteateTime(System.currentTimeMillis());
							
							ByteArrayOutputStream os = new ByteArrayOutputStream();
							if(url.toLowerCase().endsWith(".png")) {
								bm.compress(Bitmap.CompressFormat.PNG, 100, os);  // 100是清晰度 100最清晰
							} else {
								bm.compress(Bitmap.CompressFormat.JPEG, 100, os);
							}
							imageCache.setImage(os.toByteArray());
							os.close();
							
							imageCache.setUrl(url);
							
							try {
								imageCache.save(context);
							} catch (Exception e) {
							}
							
							UIHandler.post(new Runnable() {
								@Override
								public void run() {
									imageUrlLoadListener.onComplete(bm);
								}
							});
						}
					}
					
				} catch (final Exception e) {
					ExceptionUtil.showException("ImageCache",e);
					if(imageUrlLoadListener != null) {
						UIHandler.post(new Runnable() {
							@Override
							public void run() {
								imageUrlLoadListener.onFail(e);
							}
						});
					}
				}
			}
		});
	}
	
	/**
	 * 获取bitmap的大小，不会内存溢出，只取得大小,
	 * @param context
	 * @param is imageUrl建立连接后的is
	 * @return
	 */
	private int getOptimalInSampleSize(InputStream is,int width){
		long startTime = System.currentTimeMillis();
		BitmapFactory.Options opt=new BitmapFactory.Options(); 
		opt.inPreferredConfig=Bitmap.Config.RGB_565; 
		opt.inPurgeable=true; 
		opt.inInputShareable=true; 
		Bitmap bitmap = BitmapFactory.decodeStream(is,null,opt);
		try {
			LogCat.d(TAG, "bitmap size =" + bitmap.getWidth() + "   " + bitmap.getHeight());
		} catch (Exception e) {
		}
		if(bitmap.getWidth() <= width) {
			LogCat.d(TAG, "getOptimalInSampleSize耗时：" + (System.currentTimeMillis() - startTime));
			return 1;
		} else {
			for(int i=2;i<=50;i++) {
				if(bitmap.getWidth() / i < width) {
					LogCat.d(TAG, "getOptimalInSampleSize耗时：" + (System.currentTimeMillis() - startTime) + " inSampleSize=" + i);
					return i;
				}
			}
		}
		return 1;
	}
	
	/**
	 * 回收，同时会清掉设置imageview无图片
	 */
	public void recyle() {
		if(imageUrl != null && imageCache.get(imageUrl) != null) {
			Bitmap bitmap = imageCache.get(imageUrl).get();
			if(bitmap != null) {
				TopCropAsyncImageView.this.imageView.setImageDrawable(null);
				bitmap.recycle();
				imageCache.remove(imageUrl);
			}
		}
	}

	public AsyncLoadListener getAsyncLoadListener() {
		return asyncLoadListener;
	}

	public void setAsyncLoadListener(AsyncLoadListener asyncLoadListener) {
		this.asyncLoadListener = asyncLoadListener;
	}
	
	public boolean isLoaded() {
		return isLoaded;
	}
	
	/**
	 * 手动重新设置图片
	 */
	public void setImageResource(int resId) {
		TopCropAsyncImageView.this.imageView.setImageResource(resId);
	}
	public void setImageBitmap (Bitmap bm) {
		TopCropAsyncImageView.this.imageView.setImageBitmap(bm);
		ViewSizeHelper.getInstance(getContext()).setWidth(TopCropAsyncImageView.this, bm.getWidth());
		ViewSizeHelper.getInstance(getContext()).setHeight(TopCropAsyncImageView.this, bm.getHeight());
	}
	public void setImageDrawable (Drawable drawable)  {
		TopCropAsyncImageView.this.imageView.setImageDrawable(drawable);
	}
}
