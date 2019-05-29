package org.zywx.wbpalmstar.plugin.ueximage.deletebrowser;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.zywx.wbpalmstar.engine.universalex.EUExUtil;
import org.zywx.wbpalmstar.plugin.ueximage.EUExImage;
import org.zywx.wbpalmstar.plugin.ueximage.model.PictureInfo;
import org.zywx.wbpalmstar.plugin.ueximage.util.EUEXImageConfig;
import org.zywx.wbpalmstar.plugin.ueximage.util.UEXImageUtil;

import java.util.ArrayList;
import java.util.List;

public class PreviewDetailActivity extends FragmentActivity {
//    private ArrayList<String> imageArrayList;
//    private String current;
    private ViewPager viewPager;
    private ArrayList<View> viewArrayList;
    View view;
    private CheckBox superCheckBox;
    private int currentPosition;
    private ArrayList<String> imageItemArrayList=new ArrayList<>();

    private UEXImageUtil uexImageUtil;
    private List<PictureInfo> picList ;
    private int picIndex;
    private TextView tvTitle;
    private ArrayList<String> mIdenticalList=new ArrayList<>();
    private ArrayList<String> mTemporaryList=new ArrayList<>();
    private TextView finish_title;
    private ImageView ivGoBack;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(EUExUtil.getResLayoutID("plugin_uex_image_delete_preview"));

        superCheckBox= (CheckBox) findViewById(EUExUtil.getResIdID("checkbox"));
        uexImageUtil = UEXImageUtil.getInstance();
        JSONArray imageDataArray = EUEXImageConfig.getInstance()
                .getDataArray();
        picList = uexImageUtil.transformData(imageDataArray);
        picIndex = EUEXImageConfig.getInstance().getStartIndex();
        //        Intent intent = getIntent();
//        imageArrayList = intent.getStringArrayListExtra("iamgeList");
//        current = intent.getStringExtra("current");



        viewArrayList = new ArrayList<>();
        for (int i = 0; i < picList.size(); i++) {
            view = LayoutInflater.from(PreviewDetailActivity.this).inflate(EUExUtil.getResLayoutID("plugin_delete_item_image"), null);
            viewArrayList.add(view);
            ImageView imageView = (ImageView) view.findViewById(EUExUtil.getResIdID("image"));
            Glide.with(PreviewDetailActivity.this).load(picList.get(i).getSrc()).into(imageView);
            mTemporaryList.add(picList.get(i).getSrc());
        }
        initView();

    }

    private void initView() {
        viewPager = (ViewPager) findViewById(EUExUtil.getResIdID("viewpager"));
        tvTitle = (TextView) findViewById(EUExUtil.getResIdID("tv_title"));
        finish_title = (TextView) findViewById(EUExUtil.getResIdID("btn_finish_title"));
        ivGoBack= (ImageView) findViewById(EUExUtil.getResIdID("iv_left_on_title"));

        viewPager.setAdapter(new PreViewAdapter(viewArrayList));
        if (picIndex < picList.size()){
                viewPager.setCurrentItem(picIndex);

        }
        ivGoBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        finish_title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int i = 0; i < mTemporaryList.size(); i++) {
                    for (int j = 0; j < imageItemArrayList.size(); j++) {
                        if (mTemporaryList.get(i).equals(imageItemArrayList.get(j))){
                            mIdenticalList.add(mTemporaryList.get(i));
                        }
                    }
                }
                mTemporaryList.removeAll(mIdenticalList);
                Gson gson=new Gson();
                String s = gson.toJson(mTemporaryList);
                EUExImage.previewdelete.cbDelete(s);
                finish();
            }
        });



        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                currentPosition = position;
                tvTitle.setText((position + 1) + "/" + picList.size());
                Toast.makeText(PreviewDetailActivity.this, position + "", Toast.LENGTH_SHORT).show();
                View view = viewArrayList.get(position);
                ImageView imageView = (ImageView) view.findViewById(EUExUtil.getResIdID("image"));
                Glide.with(PreviewDetailActivity.this).load(picList.get(position).getSrc()).into(imageView);
                String path=picList.get(position).getSrc();
                if (imageItemArrayList.contains(path)){
                    superCheckBox.setChecked(true);
                }else {
                    superCheckBox.setChecked(false);
                }

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        superCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (superCheckBox.isChecked()){
                    imageItemArrayList.add(picList.get(currentPosition).getSrc());
                }else {
                    imageItemArrayList.remove(picList.get(currentPosition).getSrc());

                }
            }
        });

    }

    class PreViewAdapter extends PagerAdapter {

        ArrayList<View> viewArrayList = null;

        PreViewAdapter(ArrayList<View> viewArrayList) {
            this.viewArrayList = viewArrayList;
        }

        @Override
        public int getCount() {
            return viewArrayList.size();
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView(viewArrayList.get(position));//删除页卡
        }


        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            container.addView(viewArrayList.get(position));//添加页卡
            return viewArrayList.get(position);
        }

    }
}
