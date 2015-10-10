/*
 * Copyright (c) 2015.  The AppCan Open Source Project.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

package org.zywx.wbpalmstar.plugin.ueximage;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.os.Environment;
import android.os.Process;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.zywx.wbpalmstar.base.BDebug;
import org.zywx.wbpalmstar.base.ResoureFinder;
import org.zywx.wbpalmstar.base.cache.ImageLoadTask;
import org.zywx.wbpalmstar.base.cache.ImageLoadTask$ImageLoadTaskCallback;
import org.zywx.wbpalmstar.base.cache.ImageLoaderManager;
import org.zywx.wbpalmstar.base.cache.MyAsyncTask;
import org.zywx.wbpalmstar.plugin.ueximage.vo.ImageInfo;
import org.zywx.wbpalmstar.plugin.ueximage.vo.ImageItem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

public class PictureActivity extends Activity implements OnClickListener,
		ViewPager.OnPageChangeListener {

	public static final String TAG = "PictureActivity";
	private static final int TAG_NULL_IMAGE = 0;
	private static final int TAG_TINY_IMAGE = 1;
	private static final int TAG_SRC_IMAGE = 2;
	private ViewPager viewPager;
	private Button controllerBack;
	private Button btnSave;
	private TextView controllerTitle;
	private RelativeLayout controllerLayer;
	private ImageView btnZoomOut;
	private ImageView btnZoomIn;
	private AlphaAnimation fadeInAnim;
	private AlphaAnimation fadeOutAnim;
	private ArrayList<ImageItem> arrayList;
	private LayoutInflater inflater;
	private ImageLoaderManager loaderManager;
	private int showIndex;
	private int maxSize;
	private ProgressBar progressBar;
	private MultiTouchImageView currentImageView;
	private ResoureFinder finder;
	private PagerAdapter pagerAdapter = new PagerAdapter() {

		@Override
		public void startUpdate(View container) {

		}

		public boolean isViewFromObject(View view, Object object) {
			return view == ((ImageView) object);
		}

		@Override
		public Object instantiateItem(View container, int position) {
			final ImageItem item = arrayList.get(position);
			final ImageView imageView = item.getImageView();
			imageView.setTag(TAG_NULL_IMAGE);
			final Bitmap tinyBitmap = loaderManager.getCacheBitmap(item
					.getImageInfo().srcUrl);
			imageView.setImageBitmap(tinyBitmap);
			if (tinyBitmap != null) {
				imageView.setTag(ImageItem.TAG_TINY_IMAGE);
			}
			((ViewPager) container).addView(imageView);
			return imageView;
		}

		@Override
		public int getCount() {
			return arrayList == null ? 0 : arrayList.size();
		}

		@Override
		public void finishUpdate(View container) {

		}

		@Override
		public void destroyItem(View container, int position, Object item) {
			MultiTouchImageView imageView = (MultiTouchImageView) item;
			((ViewPager) container).removeView(imageView);
			imageView.setImageDrawable(null);
			imageView.setTag(ImageItem.TAG_NULL_IMAGE);
		}

	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		finder = ResoureFinder.getInstance(this);
		loaderManager = ImageLoaderManager.initImageLoaderManager(this);
		setContentView(finder.getLayoutId("plugin_imagebrowser_picture_layout"));
		inflater = LayoutInflater.from(this);
		setupView();
		Intent intent = getIntent();
		showIndex = intent.getIntExtra(
				ImageWatcherActivity.INTENT_KEY_SHOW_IMAGE_INDEX, 0);
		ArrayList<ImageInfo> list = ImageGridAdapter.getImageInfoList();
		arrayList = new ArrayList<ImageItem>(list.size());
		for (ImageInfo info : list) {
			final ImageItem item = new ImageItem();
			final MultiTouchImageView imageView = (MultiTouchImageView) inflater
					.inflate(finder
							.getLayoutId("plugin_imagebrowser_pager_item"),
							null);
			imageView.setTag(PictureActivity.TAG_NULL_IMAGE);
			item.setImageView(imageView);
			item.setImageInfo(info);
			arrayList.add(item);
		}
		Configuration config = getResources().getConfiguration();
		viewPager.setAdapter(pagerAdapter);
		viewPager.setOnPageChangeListener(this);
		viewPager.setCurrentItem(showIndex);
	}

	private void setupView() {
		LinearInterpolator interpolator = new LinearInterpolator();
		fadeInAnim = new AlphaAnimation(0.0f, 1.0f);
		fadeInAnim.setFillAfter(true);
		fadeInAnim.setDuration(300);
		fadeInAnim.setInterpolator(interpolator);
		fadeOutAnim = new AlphaAnimation(1.0f, 0.0f);
		fadeOutAnim.setFillAfter(true);
		fadeOutAnim.setDuration(300);
		fadeOutAnim.setInterpolator(interpolator);

		controllerLayer = (RelativeLayout) findViewById(finder
				.getId("plugin_image_watcher_controller_layer"));
		controllerBack = (Button) findViewById(finder
				.getId("plugin_image_watcher_controller_top_back"));
		controllerBack.setOnClickListener(this);
		btnSave = (Button) findViewById(finder
				.getId("plugin_image_watcher_controller_top_save"));
		btnSave.setOnClickListener(this);
		controllerTitle = (TextView) findViewById(finder
				.getId("plugin_image_watcher_controller_top_title"));
		btnZoomIn = (ImageView) findViewById(finder
				.getId("plugin_image_watcher_btn_zoom_in"));
		btnZoomIn.setOnClickListener(this);
		btnZoomOut = (ImageView) findViewById(finder
				.getId("plugin_image_watcher_btn_zoom_out"));
		btnZoomOut.setOnClickListener(this);
		progressBar = (ProgressBar) findViewById(finder
				.getId("plugin_image_watcher_progressbar"));
		// 初始化所有要用到的动画
		viewPager = (ViewPager) findViewById(finder
				.getId("plugin_image_watcher_pager"));
		maxSize = ImageUtility.getPictrueSourceMaxSize(this);
	}

	private void showControllerLayer() {
		controllerLayer.setVisibility(View.VISIBLE);
		controllerLayer.startAnimation(fadeInAnim);
	}

	private void hideControllerLayer() {
		controllerLayer.setVisibility(View.GONE);
		controllerLayer.startAnimation(fadeOutAnim);
	}

	private void backForResult() {
		int index = viewPager.getCurrentItem();
		Intent intent = new Intent(getIntent().getAction());
		intent.putExtra("index", index);
		setResult(ImageWatcherActivity.REQUEST_CODE_SHOW_PICTURE, intent);
		this.finish();
	}

	@Override
	public void onClick(View v) {
		if (controllerBack == v) {
			backForResult();
		} else if (btnSave == v) {
			saveImage();
		} else if (btnZoomIn == v) {
			currentImageView.doubleZoomIn();
		} else if (btnZoomOut == v) {
			currentImageView.ZoomToBase();
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			backForResult();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onPageSelected(int lastIndex, int position) {
		progressBar.setVisibility(View.GONE);
		if (lastIndex != position) {// 发生过页面切换,同步GridView与ViewPager的页面索引值
			final ImageItem lastItem = arrayList.get(lastIndex);
			MultiTouchImageView imageView = lastItem.getImageView();
			imageView.setImageMatrix(imageView.getBaseMatrix());
		}
		controllerTitle.setText((position + 1) + "/" + arrayList.size());
		final ImageItem imageItem = arrayList.get(position);
		currentImageView = imageItem.getImageView();
		currentImageView.setOnSingleTapListener(new MultiTouchImageView.OnSingleTapListener() {
			@Override
			public void onSingleTap(MultiTouchImageView view) {
				switch (controllerLayer.getVisibility()) {
				case View.VISIBLE:
					hideControllerLayer();
					break;
				case View.GONE:
				case View.INVISIBLE:
					showControllerLayer();
					break;
				}
			}
		});
		final ImageInfo imageInfo = imageItem.getImageInfo();
		Log.i(TAG, "imageUrl== " + imageInfo.srcUrl);
		if (!TextUtils.isEmpty(imageInfo.srcUrl)
				&& imageInfo.srcUrl.startsWith("http://")) {
			imageInfo.srcUrl = imageInfo.srcUrl.replace("[", "%5B");
			imageInfo.srcUrl = imageInfo.srcUrl.replace("]", "%5D");
		}
		switch ((Integer) currentImageView.getTag()) {
		case TAG_NULL_IMAGE:
			Bitmap tinyBitmap = loaderManager.getCacheBitmap(imageInfo.srcUrl);
			if (tinyBitmap == null) {
				progressBar.setVisibility(View.VISIBLE);
				loaderManager.asyncLoad(new GridImageLoadTask(imageInfo,
						imageInfo.srcUrl, this)
						.addCallback(new ImageLoadTask$ImageLoadTaskCallback() {

							@Override
							public void onImageLoaded(ImageLoadTask task,
									Bitmap bitmap) {
								progressBar.setVisibility(View.GONE);
								if (bitmap != null) {
									currentImageView.setTag(TAG_TINY_IMAGE);
									currentImageView.setImageBitmap(bitmap);
								} else {
									Toast.makeText(
											PictureActivity.this,
											finder.getString("plugin_image_browser_load_image_fail"),
											Toast.LENGTH_SHORT).show();
								}
							}
						}));
			} else {
				currentImageView.setTag(TAG_TINY_IMAGE);
				currentImageView.setImageBitmap(tinyBitmap);
			}
			break;
		case TAG_TINY_IMAGE:
			startSrcBitmapAsncLoad(imageItem);
			break;
		}
	}

	private MyAsyncTask myAsyncTask;

	private void startSrcBitmapAsncLoad(final ImageItem imageItem) {
		final String savePath = imageItem.getImageInfo().savePath;
		if (savePath != null) {
			if (myAsyncTask != null
					&& myAsyncTask.getStatus() != Status.FINISHED) {
				myAsyncTask.cancel(false);
			}
			myAsyncTask = new MyAsyncTask() {
				private BitmapFactory.Options options = new BitmapFactory.Options();

				protected Object doInBackground(Object... params) {
					Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
					Bitmap bitmap = null;
					try {
						bitmap = ImageUtility.decodeSourceBitmapByPath(
								savePath, options, maxSize);
					} catch (OutOfMemoryError e) {
						BDebug.e(TAG, "OutOfMemoryError: " + e.getMessage());
					}
					return bitmap;
				};

				public void handleOnCanceled(MyAsyncTask task) {
					this.options.requestCancelDecode();
				};

				public void handleOnCompleted(MyAsyncTask task, Object result) {
					BDebug.d(TAG, "ImageAsyncTask-->onCompleted: " + result);
					if (isCancelled()) {
						return;
					}
					if (result != null) {
						Bitmap bitmap = (Bitmap) result;
						final ImageView imageView = imageItem.getImageView();
						imageView.setImageBitmap(bitmap);
						imageView.setTag(TAG_SRC_IMAGE);
					} else {
						Toast.makeText(
								PictureActivity.this,
								finder.getString("plugin_image_browser_load_image_fail"),
								Toast.LENGTH_SHORT).show();
					}
				};
			};
			myAsyncTask.execute(new Object[] {});
		}

	}

	/**
	 * 保存图片
	 */
	@SuppressWarnings("unchecked")
	private void saveImage() {
		final Bitmap bitmap = currentImageView.getBitmap();
		if (bitmap != null) {
			new MyAsyncTask() {
				private AlertDialog alertDialog;
				private File targetFile;

				public void handleOnPreLoad(MyAsyncTask task) {
					if (Environment.getExternalStorageState().equals(
							Environment.MEDIA_MOUNTED)) {
						alertDialog = new AlertDialog.Builder(
								PictureActivity.this)
								.setMessage(
										finder.getString("plugin_image_borwser_now_saving_image_please_wait"))
								.setCancelable(false).create();
						alertDialog.show();
					} else {
						cancel(true);
						Toast.makeText(
								PictureActivity.this,
								finder.getString("plugin_image_browser_sd_have_not_mount_so_can_not_save"),
								Toast.LENGTH_SHORT).show();
					}
				};

				protected Object doInBackground(Object... params) {
					boolean isSuc = false;
					File targetFolder = Environment
							.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
					FileOutputStream fos = null;
					try {
						targetFile = new File(targetFolder, UUID.randomUUID()
								+ ".jpg");
						fos = new FileOutputStream(targetFile);
						if (bitmap.compress(
								bitmap.hasAlpha() ? CompressFormat.PNG
										: CompressFormat.JPEG, 100, fos)) {
							isSuc = true;
							// 更新媒体库数据
							UpdateMediaData.getInstance(PictureActivity.this)
									.updateFile(targetFile.getAbsolutePath());
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
					return isSuc;
				};

				public void handleOnCanceled(MyAsyncTask task) {
					alertDialog.dismiss();
				};

				public void handleOnCompleted(MyAsyncTask task, Object result) {
					alertDialog.dismiss();
					if ((Boolean) result) {
						Toast.makeText(
								PictureActivity.this,
								finder.getString("plugin_image_browser_save_folder")
										+ ": " + targetFile.getAbsolutePath(),
								Toast.LENGTH_SHORT).show();
					} else {
						Toast.makeText(
								PictureActivity.this,
								finder.getString("plugin_image_browser_save_fail"),
								Toast.LENGTH_SHORT).show();
					}
				};
			}.execute(new Object[] {});
		} else {
			Toast.makeText(
					PictureActivity.this,
					finder.getString("plugin_image_browser_image_have_not_load_can_not_save"),
					Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}
}