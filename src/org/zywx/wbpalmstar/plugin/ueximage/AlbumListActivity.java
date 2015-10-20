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
import com.ace.universalimageloader.core.ImageLoader;
import com.ace.universalimageloader.core.display.SimpleBitmapDisplayer;
import com.ace.universalimageloader.core.imageaware.ImageViewAware;

import org.zywx.wbpalmstar.plugin.ueximage.util.CommonUtil;
import org.zywx.wbpalmstar.plugin.ueximage.util.Constants;
import org.zywx.wbpalmstar.plugin.ueximage.util.EUEXImageConfig;
import org.zywx.wbpalmstar.plugin.ueximage.model.PictureInfo;
import org.zywx.wbpalmstar.plugin.ueximage.util.UEXImageUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class AlbumListActivity extends Activity implements Serializable {
    public static final String TAG = "AlbumListActivity";
    private ListView lvAlbumList;

    private UEXImageUtil uexImageUtil;

    private List<String> folderNameList;
    private ImageView ivProgressBar;
    private FolderAdapter adapter;
    private ImageView ivLeftTitle;
    private Button btnRightTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album_list);
        uexImageUtil = UEXImageUtil.getInstance();

        ivLeftTitle = (ImageView)findViewById(R.id.iv_left_on_title);
        btnRightTitle = (Button) findViewById(R.id.btn_finish_title);

        ivProgressBar = (ImageView) findViewById(R.id.iv_progress_bar);
        lvAlbumList = (ListView) findViewById(R.id.local_album_list);


        Animation animation = AnimationUtils.loadAnimation(this, R.anim.rotate_loading);
        ivProgressBar.startAnimation(animation);
        initData();
        ivLeftTitle.setOnClickListener(commonClickListener);
        btnRightTitle.setOnClickListener(commonClickListener);
        lvAlbumList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(AlbumListActivity.this, PictureGridActivity.class);
                intent.putExtra(Constants.EXTRA_FOLDER_NAME, folderNameList.get(i));
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
            switch (v.getId()) {
                case R.id.iv_left_on_title:
                    setResult(Constants.OPERATION_CANCELLED, null);
                    finish();
                    break;
                case R.id.btn_finish_title:
                    //如果选择的图片小于最小数目，给一个提示
                    if(uexImageUtil.getCheckedItems().size() <  EUEXImageConfig.getInstance().getMinImageCount()) {
                        Toast.makeText(AlbumListActivity.this, String.format(getString(R.string.at_least_choose),  EUEXImageConfig.getInstance().getMinImageCount()), Toast.LENGTH_SHORT).show();
                    } else {
                        setResult(RESULT_OK, null);
                        finish();
                    }
                    break;
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
                            adapter = new FolderAdapter(AlbumListActivity.this, uexImageUtil.getFolderList());
                            lvAlbumList.setAdapter(adapter);
                            lvAlbumList.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }
        }).start();
    }



    public class FolderAdapter extends BaseAdapter {
        Map<String, List<PictureInfo>> folders;
        Context context;
        DisplayImageOptions options;
        FolderAdapter(Context context, Map<String, List<PictureInfo>> folders) {
            this.folders = folders;
            this.context = context;
            folderNameList = new ArrayList<String>();

            options = new DisplayImageOptions.Builder()
                    .cacheInMemory(true)
                    .cacheOnDisk(false)
                    .showImageForEmptyUri(R.drawable.loading)
                    .showImageOnFail(R.drawable.loading)
                    .showImageOnLoading(R.drawable.loading)
                    .bitmapConfig(Bitmap.Config.RGB_565)
                    .displayer(new SimpleBitmapDisplayer()).build();

            Iterator iter = folders.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                String key = (String) entry.getKey();
                folderNameList.add(key);
            }
            final Map<String, List<PictureInfo>> tempFolders = folders;
            //根据文件夹内的图片数量降序显示
            Collections.sort(folderNameList, new Comparator<String>() {
                public int compare(String arg0, String arg1) {
                    Integer num1 = tempFolders.get(arg0).size();
                    Integer num2 = tempFolders.get(arg1).size();
                    return num2.compareTo(num1);
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
                convertView = LayoutInflater.from(context).inflate(R.layout.item_album_list, null);
                viewHolder.imageView = (ImageView) convertView.findViewById(R.id.imageView);
                viewHolder.textView = (TextView) convertView.findViewById(R.id.textview);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            String name = folderNameList.get(i);
            List<PictureInfo> files = folders.get(name);
            viewHolder.textView.setText(name + "(" + files.size() + ")");
            if (files.size() > 0) {
                ImageLoader.getInstance().displayImage(files.get(0).getThumb(),new ImageViewAware(viewHolder.imageView),options,null,null);
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
