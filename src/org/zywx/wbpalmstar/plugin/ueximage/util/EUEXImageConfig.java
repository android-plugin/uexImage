package org.zywx.wbpalmstar.plugin.ueximage.util;

import org.json.JSONArray;


/**
 * Created by Fred on 2015/10/17.
 */
public class EUEXImageConfig {
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
    private int maxImageCount;
    //图片质量
    private double quality;
    //是否用png格式导出图片
    private boolean isUsePng;
    //是否显示detailedInfo;
    private boolean isShowDetailedInfo;

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
}
