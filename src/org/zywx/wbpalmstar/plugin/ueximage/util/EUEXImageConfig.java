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
package org.zywx.wbpalmstar.plugin.ueximage.util;

import android.graphics.Color;

import org.json.JSONArray;
import org.zywx.wbpalmstar.base.BDebug;
import org.zywx.wbpalmstar.engine.universalex.EUExUtil;
import org.zywx.wbpalmstar.plugin.ueximage.vo.ViewFrameVO;

/**
 * Created by Fred on 2015/10/17.
 */
public class EUEXImageConfig {
    private static final String TAG = "EUEXImageConfig";
    
    private volatile static EUEXImageConfig instance = null;
    public static EUEXImageConfig getInstance() {
        if (instance == null) {
            synchronized(CommonUtil.class) {
                if (instance == null) {
                    instance = new EUEXImageConfig();
                }
            }
        }
        return instance;
    }
    //以下是关于openPicker相关的参数设置
    //设置最小选择的图片数目, 0表示无限制
    private int minImageCount = 1;
    //设置可以选择的图片的个数，0表示无限制
    private int maxImageCount = Integer.MAX_VALUE;
    //图片质量
    private double quality = 0.5;
    //是否用png格式导出图片
    private boolean isUsePng = false;
    //是否显示detailedInfo;
    private boolean isShowDetailedInfo =false;

    //以下是关于openBrowser相关的设置
    //显示分享按钮
    private boolean isDisplayActionButton = false;
    //显示切换箭头
    private boolean isDisplayNavArrows = false;
    //允许九宫格视图
    private boolean enableGrid = true;

    private boolean isStartOnGrid = false;

    //非负整数 起始图片位置
    private int startIndex = 0;
    //图片信息集合
    private JSONArray dataArray;

    //是否是浏览图片
    private boolean isOpenBrowser;

    // UI样式
    private int style = Constants.UI_STYLE_OLD;

    /** *单张图片预览位置、大小 */
    private ViewFrameVO picPreviewFrame = new ViewFrameVO();
    /** *图片grid位置、大小 */
    private ViewFrameVO picGridFrame = new ViewFrameVO();
    private int viewGridBg = Color.parseColor(Constants.DEF_GRID_VIEW_BG_COLOR);
    private String gridBrowserTitle = EUExUtil
            .getString("plugin_uex_image_default_grid_browser_title");

    public int getMaxImageCount() {
        return maxImageCount;
    }

    public void setMaxImageCount(int maxImageCount) {
        this.maxImageCount = maxImageCount;
    }

    public int getMinImageCount() {
        return minImageCount;
    }

    public void setMinImageCount(int minImageCount) {
        this.minImageCount = minImageCount;
    }

    public double getQuality() {
        return quality;
    }

    public void setQuality(double quality) {
        this.quality = quality;
    }

    public boolean getIsUsePng() {
        return isUsePng;
    }

    public void setIsUsePng(boolean isUsePng) {
        this.isUsePng = isUsePng;
    }

    public boolean isShowDetailedInfo() {
        return isShowDetailedInfo;
    }

    public void setIsShowDetailedInfo(boolean isShowDetailedInfo) {
        this.isShowDetailedInfo = isShowDetailedInfo;
    }

    public boolean isDisplayActionButton() {
        return isDisplayActionButton;
    }

    public void setIsDisplayActionButton(boolean isDisplayActionButton) {
        this.isDisplayActionButton = isDisplayActionButton;
    }

    public boolean isDisplayNavArrows() {
        return isDisplayNavArrows;
    }

    public void setIsDisplayNavArrows(boolean isDisplayNavArrows) {
        this.isDisplayNavArrows = isDisplayNavArrows;
    }

    public boolean isEnableGrid() {
        return enableGrid;
    }

    public void setEnableGrid(boolean enableGrid) {
        this.enableGrid = enableGrid;
    }

    public boolean isStartOnGrid() {
        return isStartOnGrid;
    }

    public void setIsStartOnGrid(boolean isStartOnGrid) {
        this.isStartOnGrid = isStartOnGrid;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public void setStartIndex(int startIndex) {
        this.startIndex = startIndex;
    }

    public JSONArray getDataArray() {
        return dataArray;
    }

    public void setDataArray(JSONArray dataArray) {
        this.dataArray = dataArray;
    }

    public boolean getIsOpenBrowser() {
        return isOpenBrowser;
    }

    public void setIsOpenBrowser(boolean isOpenBrowser) {
        this.isOpenBrowser = isOpenBrowser;
    }

    public int getUIStyle() {
        return style;
    }

    public void setUIStyle(int style) {
        this.style = style;
    }

    public ViewFrameVO getPicPreviewFrame() {
        return picPreviewFrame;
    }

    public void setPicPreviewFrame(ViewFrameVO viewFrameVO) {
        this.picPreviewFrame = viewFrameVO;
    }

    public ViewFrameVO getPicGridFrame() {
        return picGridFrame;
    }

    public void setPicGridFrame(ViewFrameVO viewFrameVO) {
        this.picGridFrame = viewFrameVO;
        BDebug.d(TAG, viewFrameVO);
    }

    public int getViewGridBackground() {
        return viewGridBg;
    }

    public void setViewGridBackground(int viewGridBg) {
        this.viewGridBg = viewGridBg;
    }

    public String getGridBrowserTitle() {
        return gridBrowserTitle;
    }

    public void setGridBrowserTitle(String gridBrowserTitle) {
        this.gridBrowserTitle = gridBrowserTitle;
    }
}
