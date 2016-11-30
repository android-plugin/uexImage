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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ace.universalimageloader.core.DisplayImageOptions;
import com.ace.universalimageloader.core.display.SimpleBitmapDisplayer;

import org.zywx.wbpalmstar.base.ACEImageLoader;
import org.zywx.wbpalmstar.base.ResoureFinder;
import org.zywx.wbpalmstar.plugin.ueximage.model.PictureFolder;
import org.zywx.wbpalmstar.plugin.ueximage.util.Constants;
import org.zywx.wbpalmstar.plugin.ueximage.util.EUEXImageConfig;
import org.zywx.wbpalmstar.plugin.ueximage.util.UEXImageUtil;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AlbumListActivity extends Activity implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final String TAG = "AlbumListActivity";
    private ListView lvAlbumList;

    private UEXImageUtil uexImageUtil;
    private List<PictureFolder> pictureFolders;
    private ImageView ivProgressBar;
    private FolderAdapter adapter;
    private ImageView ivLeftTitle;
    private Button btnRightTitle;
    private ResoureFinder finder;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        finder = ResoureFinder.getInstance(this);

        setContentView(finder.getLayoutId("plugin_uex_image_activity_album_list"));
        uexImageUtil = UEXImageUtil.getInstance();

        ivLeftTitle = (ImageView)findViewById(finder.getId("iv_left_on_title"));
        btnRightTitle = (Button) findViewById(finder.getId("btn_finish_title"));

        ivProgressBar = (ImageView) findViewById(finder.getId("iv_progress_bar"));
        lvAlbumList = (ListView) findViewById(finder.getId("local_album_list"));

        Animation animation = AnimationUtils.loadAnimation(this, finder.getAnimId("plugin_uex_image_rotate_loading"));
        ivProgressBar.startAnimation(animation);
        initData();
        ivLeftTitle.setOnClickListener(commonClickListener);
        btnRightTitle.setOnClickListener(commonClickListener);
        lvAlbumList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(AlbumListActivity.this, PictureGridActivity.class);
                intent.putExtra(Constants.EXTRA_FOLDER_PATH, pictureFolders.get(i).getFolderPath());
                startActivityForResult(intent, EUExImage.REQUEST_IMAGE_PICKER);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == EUExImage.REQUEST_IMAGE_PICKER && resultCode == RESULT_OK) {
            setResult(resultCode, null);
            finish();
        }
    }

    private View.OnClickListener commonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v.getId() == finder.getId("iv_left_on_title")) {
                setResult(Constants.OPERATION_CANCELLED, null);
                finish();
            } else if (v.getId() == finder.getId("btn_finish_title")) {
                //如果选择的图片小于最小数目，给一个提示
                if(uexImageUtil.getCheckedItems().size() <  EUEXImageConfig.getInstance().getMinImageCount()) {
                    Toast.makeText(AlbumListActivity.this, String.format(finder.getString("plugin_uex_image_at_least_choose"),  EUEXImageConfig.getInstance().getMinImageCount()), Toast.LENGTH_SHORT).show();
                } else {
                    setResult(RESULT_OK, null);
                    finish();
                }
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        if (uexImageUtil.getCheckedItems().size() > 0) {
            btnRightTitle.setText("完成(" + uexImageUtil.getCheckedItems().size() + "/" +  EUEXImageConfig.getInstance().getMaxImageCount()+")" );
        }

    }


    private void initData() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                uexImageUtil.initAlbumList(AlbumListActivity.this);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //初始化完毕后，显示文件夹列表
                        if (!isFinishing()) {
                            ivProgressBar.clearAnimation();
                            ((View) ivProgressBar.getParent()).setVisibility(View.GONE);
                            adapter = new FolderAdapter(AlbumListActivity.this, uexImageUtil.getPictureFolderList());
                            lvAlbumList.setAdapter(adapter);
                            lvAlbumList.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }
        }).start();
    }



    public class FolderAdapter extends BaseAdapter {
        List<PictureFolder> folders;
        Context context;
        DisplayImageOptions options;
        FolderAdapter(Context context, List<PictureFolder> folders) {
            pictureFolders = folders;
            this.folders = folders;
            this.context = context;
            options = new DisplayImageOptions.Builder()
                    .cacheInMemory(true)
                    .cacheOnDisk(false)
                    .showImageForEmptyUri(finder.getDrawable("plugin_uex_image_loading"))
                    .showImageOnFail(finder.getDrawable("plugin_uex_image_loading"))
                    .showImageOnLoading(finder.getDrawable("plugin_uex_image_loading"))
                    .bitmapConfig(Bitmap.Config.RGB_565)
                    .displayer(new SimpleBitmapDisplayer())
                    .considerExifParams(true)//考虑Exif旋转
                    .build();
            //根据文件夹内的图片数量降序显示
            Collections.sort(folders, new Comparator<PictureFolder>() {
                @Override
                public int compare(PictureFolder first, PictureFolder second) {
                    return second.getCount().compareTo(first.getCount());
                }
            });
        }

        @Override
        public int getCount() {
            return folders.size();
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View convertView, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            if (convertView == null || convertView.getTag() == null) {
                viewHolder = new ViewHolder();
                convertView = LayoutInflater.from(context).inflate(finder.getLayoutId("plugin_uex_image_item_album_list"), null);
                viewHolder.imageView = (ImageView) convertView.findViewById(finder.getId("imageView"));
                viewHolder.textView = (TextView) convertView.findViewById(finder.getId("textview"));
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            PictureFolder pictureFolder = folders.get(i);
            viewHolder.textView.setText(pictureFolder.getFolderName() + "(" + pictureFolder.getCount() + ")");
            if (pictureFolder.getCount() > 0) {
                ACEImageLoader.getInstance().displayImageWithOptions(pictureFolder.getFirstImagePath(), viewHolder.imageView, options,null,null);
            }
            return convertView;
        }
        private class ViewHolder {
            ImageView imageView;
            TextView textView;
        }
    }

    @Override
    public void onBackPressed() {
        setResult(Constants.OPERATION_CANCELLED, null);
        finish();
    }
}
