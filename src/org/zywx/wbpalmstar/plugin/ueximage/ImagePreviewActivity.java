package org.zywx.wbpalmstar.plugin.ueximage;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.ace.universalimageloader.core.DisplayImageOptions;
import com.ace.universalimageloader.core.ImageLoader;

import org.json.JSONArray;
import org.zywx.wbpalmstar.base.ResoureFinder;
import org.zywx.wbpalmstar.plugin.ueximage.model.PictureInfo;
import org.zywx.wbpalmstar.plugin.ueximage.util.CommonUtil;
import org.zywx.wbpalmstar.plugin.ueximage.util.Constants;
import org.zywx.wbpalmstar.plugin.ueximage.util.EUEXImageConfig;
import org.zywx.wbpalmstar.plugin.ueximage.util.UEXImageUtil;

import java.io.File;
import java.util.List;

public class ImagePreviewActivity extends Activity {
    private final String TAG = "ImagePreviewActivity";

    private ViewPager viewPager;
    private String folderName;

    private ImageView ivGoBack;
    private TextView tvTitle;
    private Button btnFinishInTitle;
    private CheckBox cbChoose;
    private TextView tvCheckbox;
    private List<PictureInfo> checkedItems;
    private UEXImageUtil uexImageUtil;

    private ImageView imageView;

    private List<PictureInfo> picList;
    private int picIndex;
    private boolean isOpenBrowser;
    //仅在浏览图片时有用。
    private TextView tvShare;
    private TextView tvToGrid;
    private ResoureFinder finder;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        finder = ResoureFinder.getInstance(this);
        setContentView(finder.getLayoutId("activity_image_preview"));
        uexImageUtil = UEXImageUtil.getInstance();
        isOpenBrowser = EUEXImageConfig.getInstance().getIsOpenBrowser();
        initData();
        if (isOpenBrowser) {
            initViewForBrowser();
        } else {
            initViewForPicker();
        }

    }
    private void initData() {
        if (isOpenBrowser) {
            JSONArray imageDataArray  = EUEXImageConfig.getInstance().getDataArray();
            picList = uexImageUtil.transformData(imageDataArray);
            picIndex = EUEXImageConfig.getInstance().getStartIndex();
        } else {
            folderName = getIntent().getExtras().getString(Constants.EXTRA_FOLDER_NAME);
            picIndex = getIntent().getExtras().getInt(Constants.EXTRA_PIC_INDEX);
            checkedItems = uexImageUtil.getCheckedItems();
            picList = uexImageUtil.getFolderList().get(folderName);
        }
    }





    private void initViewForPicker() {
        ivGoBack = (ImageView) findViewById(finder.getId("iv_left_on_title"));
        tvTitle = (TextView) findViewById(finder.getId("tv_title"));
        tvTitle.setText(folderName);
        btnFinishInTitle = (Button) findViewById(finder.getId("btn_finish_title"));
        viewPager = (ViewPager) findViewById(finder.getId("vp_picture"));
        cbChoose = (CheckBox) findViewById(finder.getId("checkbox"));
        ivGoBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(picIndex);
        viewPager.setOnPageChangeListener(onPageChangeListener);
        if (checkedItems.size() > 0) {
            btnFinishInTitle.setText("完成(" + checkedItems.size() + "/" +  EUEXImageConfig.getInstance().getMaxImageCount() + ")");
            btnFinishInTitle.setEnabled(true);
        }
        cbChoose.setTag(picList.get(picIndex));
        cbChoose.setOnCheckedChangeListener(onCheckedChangeListener);
        btnFinishInTitle.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if (checkedItems.size() >=  EUEXImageConfig.getInstance().getMinImageCount()) {
                    setResult(RESULT_OK, null);
                    finish();
                } else {
                    String str = String.format(finder.getString("at_least_choose"),  EUEXImageConfig.getInstance().getMinImageCount());
                    Toast.makeText(ImagePreviewActivity.this, str, Toast.LENGTH_SHORT).show();
                }

            }
        });

    }
    private void initViewForBrowser() {
        ivGoBack = (ImageView) findViewById(finder.getId("iv_left_on_title"));
        tvTitle = (TextView) findViewById(finder.getId("tv_title"));

        btnFinishInTitle = (Button) findViewById(finder.getId("btn_finish_title"));

        viewPager = (ViewPager) findViewById(finder.getId("vp_picture"));
        cbChoose = (CheckBox) findViewById(finder.getId("checkbox"));
        tvShare = (TextView) findViewById(finder.getId("tv_share"));
        tvToGrid = (TextView) findViewById(finder.getId("tv_to_grid"));

        ivGoBack.setVisibility(View.INVISIBLE);
        tvCheckbox = (TextView) findViewById(finder.getId("tv_checkbox"));
        cbChoose.setVisibility(View.INVISIBLE);
        tvCheckbox.setVisibility(View.INVISIBLE);
        tvToGrid.setVisibility(View.VISIBLE);
        tvToGrid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ImagePreviewActivity.this, PictureGridActivity.class);
                startActivity(intent);
                finish();
            }
        });

        btnFinishInTitle.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                setResult(RESULT_OK, null);
                finish();
            }
        });

        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(picIndex);
        viewPager.setOnPageChangeListener(onPageChangeListener);
        if (EUEXImageConfig.getInstance().isDisplayActionButton()) {
            tvShare.setVisibility(View.VISIBLE);
            tvShare.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    final String src = picList.get(picIndex).getSrc();
                    Bitmap bitmap;
                    if (src.substring(0, 4).equalsIgnoreCase(Constants.HTTP)) {
                        bitmap = ImageLoader.getInstance().loadImageSync(src);
                    } else {
                        bitmap = CommonUtil.getLocalImage(ImagePreviewActivity.this, src);
                    }
                    File file = new File(Environment.getExternalStorageDirectory(),
                            File.separator + UEXImageUtil.TEMP_PATH + File.separator + "uex_image_to_share.jpg");

                    if (CommonUtil.saveBitmap2File(bitmap, file)) {
                        Intent shareIntent = new Intent();
                        shareIntent.setAction(Intent.ACTION_SEND);
                        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
                        shareIntent.setType("image/*");
                        startActivity(Intent.createChooser(shareIntent, "分享到"));
                    } else {
                        Toast.makeText(ImagePreviewActivity.this, "图片操作失败，请重试", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            if (!EUEXImageConfig.getInstance().isEnableGrid()) {
                ((View)tvShare.getParent()).setVisibility(View.INVISIBLE);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!isOpenBrowser) {
            cbChoose.setChecked(checkedItems.contains(picList.get(picIndex)));
        }
        tvTitle.setText((picIndex + 1) + "/" + picList.size());
    }

    private PagerAdapter adapter = new PagerAdapter() {

        @Override
        public int getCount() {
            return picList.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object o) {
            return view == o;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            LayoutInflater inflater = (LayoutInflater) container.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = inflater.inflate(finder.getLayoutId("view_pager_item"), null);
            imageView = (ImageView) view.findViewById(finder.getId("image"));

            //显示图片的配置
            DisplayImageOptions options = new DisplayImageOptions.Builder()
                    .cacheInMemory(true)
                    .cacheOnDisk(true)
                    .bitmapConfig(Bitmap.Config.RGB_565)
                    .showImageOnLoading(finder.getDrawableId("loading"))
                    .build();

            final String src = picList.get(position).getSrc();
            if (!isOpenBrowser) {
                ImageLoader.getInstance().displayImage(src, imageView, options);
            } else {//浏览图片：对于传入的图片的加载
                if (src.substring(0,4).equalsIgnoreCase(Constants.HTTP)) {
                    //如果是从网上下载图片，需要将下载后的图片存到缓存中
                    ImageLoader.getInstance().displayImage(src,imageView, options);
                } else {
                    Bitmap bitmap= CommonUtil.getLocalImage(ImagePreviewActivity.this, src);
                    imageView.setImageBitmap(bitmap);
                }
            }

            container.addView(view);
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }
    };



    private ViewPager.OnPageChangeListener onPageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int i, float v, int i1) {
        }

        @Override
        public void onPageSelected(int i) {
            picIndex = i;
            if (!isOpenBrowser) {
                cbChoose.setChecked(checkedItems.contains(picList.get(i)));
            }
            tvTitle.setText((i + 1) + "/" + picList.size());

        }

        @Override
        public void onPageScrollStateChanged(int i) {
            if (!isOpenBrowser) {
                if (i == 1) {//开始滑动
                    cbChoose.setOnCheckedChangeListener(null);
                } else if ( i == 0) {//静止
                    cbChoose.setOnCheckedChangeListener(onCheckedChangeListener);
                    cbChoose.setTag(picList.get(picIndex));
                }
            }
        }
    };
    //仅当在选择图片时才会用得上
    private CheckBox.OnCheckedChangeListener onCheckedChangeListener = new CheckBox.OnCheckedChangeListener(){

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                if (!checkedItems.contains(buttonView.getTag())) {
                    if(checkedItems.size() >=  EUEXImageConfig.getInstance().getMaxImageCount()){
                        Toast.makeText(ImagePreviewActivity.this, "最多选择" +  EUEXImageConfig.getInstance().getMaxImageCount() + "张图片", Toast.LENGTH_SHORT).show();
                        buttonView.setChecked(false);
                        return;
                    }
                    checkedItems.add((PictureInfo) buttonView.getTag());
                }
            } else {
                if (checkedItems.contains(buttonView.getTag())) {
                    checkedItems.remove(buttonView.getTag());
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
}
