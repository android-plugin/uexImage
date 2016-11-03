package org.zywx.wbpalmstar.plugin.ueximage.widget;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.zywx.wbpalmstar.engine.universalex.EUExUtil;

/**
 * Created by fred on 16/10/13.
 */
public class RowView extends LinearLayout {
    private Context context;
    public RowView(Context context, String label, String detail) {
        super(context);
        this.context = context;
        initView(label, detail);
    }

    private void initView(String label, String detail) {
        LayoutInflater.from(context).inflate(EUExUtil.getResLayoutID("plugin_uex_image_normal_rowview"), this);
        TextView tvTitle = (TextView) findViewById(EUExUtil.getResIdID("tv_label"));
        TextView tvCcontent = (TextView) findViewById(EUExUtil.getResIdID("tv_detail"));
        tvTitle.setText(label);
        tvCcontent.setText(detail);
    }
}
