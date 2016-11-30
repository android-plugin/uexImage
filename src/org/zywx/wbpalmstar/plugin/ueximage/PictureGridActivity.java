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
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.ace.universalimageloader.core.DisplayImageOptions;
import com.ace.universalimageloader.core.assist.ImageScaleType;
import com.ace.universalimageloader.core.display.SimpleBitmapDisplayer;
import com.ace.universalimageloader.core.listener.SimpleImageLoadingListener;

import org.json.JSONArray;
import org.zywx.wbpalmstar.base.ACEImageLoader;
import org.zywx.wbpalmstar.base.ResoureFinder;
import org.zywx.wbpalmstar.plugin.ueximage.model.PictureInfo;
import org.zywx.wbpalmstar.plugin.ueximage.util.CommonUtil;
import org.zywx.wbpalmstar.plugin.ueximage.util.Constants;
import org.zywx.wbpalmstar.plugin.ueximage.util.EUEXImageConfig;
import org.zywx.wbpalmstar.plugin.ueximage.util.UEXImageUtil;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

//以九宫格的形式显示某个文件夹下的图片列表
public class PictureGridActivity extends Activity {
    private final String TAG = "PictureGridActivity";
    /*当打开系统图库时folderName才会有值，如果是打开图片选择器，此处图片信息将完全从系统中读。并且用户可以做选择图片的操作。
    如果是执行打开浏览器操作，则不会有值
     */
    private String folderPath;
    private String folderName;
    private GridView gvPictures;

    private ImageView ivGoBack;
    private TextView tvTitle;
    private UEXImageUtil uexImageUtil;
    private Button btnFinishInTitle;
    private List <PictureInfo> picList;
    private List<String> checkedItems;
    private MyAdapter adapter;

    private boolean isOpenBrowser = false;
    private ResoureFinder finder;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        finder = ResoureFinder.getInstance(this);
        setContentView(finder.getLayoutId("plugin_uex_image_activity_picture_grid"));

        uexImageUtil = UEXImageUtil.getInstance();
        folderPath = getIntent().getStringExtra(Constants.EXTRA_FOLDER_PATH);
        //执行浏览操作
        if(TextUtils.isEmpty(folderPath)) {
            isOpenBrowser = true;
            JSONArray imageDataArray  = EUEXImageConfig.getInstance().getDataArray();
            picList = uexImageUtil.transformData(imageDataArray);
        } else { //执行选择操作
            folderName = new File(folderPath).getName();
            picList = CommonUtil.getPictureInfo(this, folderPath);
            uexImageUtil.setCurrentPicList(picList);
        }
        uexImageUtil.setCurrentPicList(picList);

        ivGoBack = (ImageView) findViewById(finder.getId("iv_left_on_title"));
        tvTitle = (TextView) findViewById(finder.getId("tv_title"));
        btnFinishInTitle = (Button) findViewById(finder.getId("btn_finish_title"));
        gvPictures = (GridView) findViewById(finder.getId("gv_pictures"));
        //拖动下拉条和滑动过程中暂停加载
//        gvPictures.setOnScrollListener(new PauseOnScrollListener((ImageLoader)(ACEImageLoader.getInstance()), true, true));
        if(isOpenBrowser) {
            initViewForBrowser();
        } else {
            initViewForPicker();
        }
    }



    private void initViewForPicker() {
        tvTitle.setText(folderName);
        adapter = new MyAdapter(this, picList);
        gvPictures.setAdapter(adapter);
        checkedItems = uexImageUtil.getCheckedItems();
        if (checkedItems.size() > 0) {
            btnFinishInTitle.setText("完成(" + checkedItems.size() + "/" +  EUEXImageConfig.getInstance().getMaxImageCount() + ")");
            btnFinishInTitle.setEnabled(true);
        }
        ivGoBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        btnFinishInTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkedItems.size() >=  EUEXImageConfig.getInstance().getMinImageCount()) {
                    setResult(RESULT_OK, new Intent());
                    finish();
                } else {
                    String str = String.format(finder.getString("plugin_uex_image_at_least_choose"),  EUEXImageConfig.getInstance().getMinImageCount());
                    Toast.makeText(PictureGridActivity.this, str, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    private void initViewForBrowser()  {
        ivGoBack.setVisibility(View.INVISIBLE);
        adapter = new MyAdapter(this, picList);
        gvPictures.setAdapter(adapter);
        btnFinishInTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setResult(RESULT_OK, null);
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        adapter.notifyDataSetChanged();
    }

    public class MyAdapter extends BaseAdapter {
        DisplayImageOptions options;
        List<PictureInfo> paths;

        public MyAdapter(Context context, List<PictureInfo> paths) {
            this.paths = paths;
            options = new DisplayImageOptions.Builder()
                    .cacheInMemory(true)
                    .cacheOnDisk(false)
                    .showImageForEmptyUri(finder.getDrawableId("plugin_uex_image_loading"))
                    .showImageOnFail(finder.getDrawableId("plugin_uex_image_loading"))
                    .showImageOnLoading(finder.getDrawableId("plugin_uex_image_loading"))
                    .bitmapConfig(Bitmap.Config.RGB_565)
                    .imageScaleType(ImageScaleType.EXACTLY)
                    .displayer(new SimpleBitmapDisplayer())
                    .considerExifParams(true)//考虑Exif旋转
                    .build();
            Collections.sort(paths, new Comparator<PictureInfo>() {
				@Override
				public int compare(PictureInfo lhs, PictureInfo rhs) {
					if (lhs.getLastModified() > rhs.getLastModified()) {
						return 1;
					} else if (lhs.getLastModified() == rhs.getLastModified()) {
						return 0;
					} else if (lhs.getLastModified() < rhs.getLastModified()) {
						return -1;
					}
					return 0;
				}
			});
        }

        @Override
        public int getCount() {
            return paths.size();
        }

        @Override
        public PictureInfo getItem(int i) {
            return paths.get(i);
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(final int i, View convertView, ViewGroup viewGroup) {
            ViewHolder viewHolder;

            if (convertView == null || convertView.getTag() == null) {
                viewHolder = new ViewHolder();
                LayoutInflater inflater = getLayoutInflater();
                convertView = inflater.inflate(finder.getLayoutId("plugin_uex_image_item_grid_picture"), null);
                viewHolder.imageView = (ImageView) convertView.findViewById(finder.getId("iv_item"));
                viewHolder.checkBox = (CheckBox) convertView.findViewById(finder.getId("checkbox"));
                //如果是浏览图片，则没有选择的checkbox
                if(isOpenBrowser) {
                    viewHolder.checkBox.setVisibility(View.INVISIBLE);
                } else {
                    viewHolder.checkBox.setOnCheckedChangeListener(onCheckedChangeListener);
                }

                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            final ImageView imageView = viewHolder.imageView;
            PictureInfo pictureInfo = paths.get(i);

            final ViewHolder tempViewHolder = viewHolder;
            if (!isOpenBrowser) {
                ACEImageLoader.getInstance().displayImageWithOptions(pictureInfo.getSrc(), viewHolder.imageView, options,
                        loadingListener);
                viewHolder.checkBox.setTag(pictureInfo.getSrc());
                viewHolder.checkBox.setChecked(checkedItems.contains(pictureInfo.getSrc()));
            } else {//浏览图片：对于传入的图片的加载
                String url = pictureInfo.getSrc();
                if (pictureInfo.getThumb() != null) {
                    url = pictureInfo.getThumb();
                }
                if (url.substring(0,4).equalsIgnoreCase(Constants.HTTP)) {
                    ACEImageLoader.getInstance().displayImageWithOptions(url, viewHolder.imageView, options);
                } else {
                    Bitmap bitmap= CommonUtil.getLocalImage(PictureGridActivity.this, url);
                    imageView.setImageBitmap(bitmap);
                }
            }
            imageView.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    picPreview(i);
                }
            });

            return convertView;
        }

        private class ViewHolder {
            ImageView imageView;
            CheckBox checkBox;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == EUExImage.REQUEST_IMAGE_PICKER && resultCode == RESULT_OK) {
            setResult(resultCode, null);
            finish();
        }
    }

    private void picPreview (int position) {
        Intent intent = new Intent(PictureGridActivity.this, ImagePreviewActivity.class);
        intent.putExtra(Constants.EXTRA_FOLDER_NAME, folderName);
        if(isOpenBrowser) {
            EUEXImageConfig.getInstance().setStartIndex(position);
            startActivity(intent);
            finish();
        } else {
            intent.putExtra(Constants.EXTRA_PIC_INDEX, position);
            startActivityForResult(intent, EUExImage.REQUEST_IMAGE_PICKER);
        }
    }
    private CheckBox.OnCheckedChangeListener onCheckedChangeListener = new CheckBox.OnCheckedChangeListener(){

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (!isChecked) {
                if (checkedItems.contains(buttonView.getTag())) {
                    checkedItems.remove(buttonView.getTag());
                }
            } else {
                if (!checkedItems.contains(buttonView.getTag())) {
                    if(checkedItems.size() >=  EUEXImageConfig.getInstance().getMaxImageCount()){
                        Toast.makeText(PictureGridActivity.this, "最多选择" +  EUEXImageConfig.getInstance().getMaxImageCount() + "张图片", Toast.LENGTH_SHORT).show();
                        buttonView.setChecked(false);
                        return;
                    }
                    checkedItems.add((String) buttonView.getTag());
                }
            }
            if (checkedItems.size() > 0) {
                btnFinishInTitle.setText("完成(" +checkedItems.size()+ "/"+  EUEXImageConfig.getInstance().getMaxImageCount() + ")");
                btnFinishInTitle.setEnabled(true);
            } else {
                btnFinishInTitle.setText("完成");
                btnFinishInTitle.setEnabled(false);
            }
        }
    };
    SimpleImageLoadingListener loadingListener = new SimpleImageLoadingListener() {
        @Override
        public void onLoadingComplete(String imageUri, View view, final Bitmap bm) {
            if (TextUtils.isEmpty(imageUri)) {
                return;
            }
            //此处加一个#eeeeee的滤镜，防止checkbox看不清
            try {
                ((ImageView) view).getDrawable().setColorFilter(Color.argb(0xff, 0xee, 0xee, 0xee), PorterDuff.Mode.MULTIPLY);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

}
