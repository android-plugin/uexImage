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


public class Constants {
    public static final String EXTRA_FOLDER_NAME = "extra_folder_name";
    public static final String EXTRA_FOLDER_PATH = "extra_folder_path";
    public static final String EXTRA_PIC_INDEX = "image_position";

    public static final String HTTP = "http";
    /**
     * 选择取消
     */
    public static final int OPERATION_CANCELLED = 1000;
    /**
     * 选择完成
     */
    public static final int OPERATION_CONFIRMED = 1001;

    public static final String UI_STYLE = "style";
    /** 插件旧风格UI */
    public static final int UI_STYLE_OLD = 0;
    /** 插件新风格UI，仿微信优化 */
    public static final int UI_STYLE_NEW = 1;
    public static final int WHAT_HIDE_IV_TO_GRID = 0;
    public static final int WHAT_SHOW_IV_TO_GRID = 1;
    public static final int HIDE_IV_TO_GRID_TIMEOUT = 3000;
    public static final int SHOW_IV_TO_GRID_TIMEOUT = 500;
    /** 位置、大小 */
    public static final String VIEW_FRAME_PIC_PREVIEW = "viewFramePicPreview";
    public static final String VIEW_FRAME_PIC_GRID = "viewFramePicGrid";
    public static final String VIEW_FRAME_VO_X = "x";
    public static final String VIEW_FRAME_VO_Y = "y";
    public static final String VIEW_FRAME_VO_W = "w";
    public static final String VIEW_FRAME_VO_H = "h";

    /** 原EUExImage类中的常量 */
    /** 裁剪图片 */
    public static final int REQUEST_CROP_IMAGE = 100;
    /** 选择图片 */
    public static final int REQUEST_IMAGE_PICKER = 101;
    /** 浏览图片 */
    public static final int REQUEST_IMAGE_BROWSER = 102;
    public static final int REQUEST_IMAGE_BROWSER_FROM_GRID = 103;

    /** 原Crop类中的常量 */
    public static final int REQUEST_CROP = 6709;
    public static final int REQUEST_PICK = 9162;
    public static final int RESULT_ERROR = 404;

    public static final String GRID_VIEW_BACKGROUND = "gridBackgroundColor";
    public static final String GRID_BROWSER_TITLE = "gridBrowserTitle";
    public static final String DEF_GRID_VIEW_BG_COLOR = "#000000";
    public static final String LONG_CLICK_CB_IMAGE_PATH = "imagePath";

    /** 压缩图片使用 */
    public static final int DES_FILE_LENGTH_10K = 10 * 1024;
    public static final int HEIGHT_10K = 240;
    public static final int WIDTH_10K = 320;
    public static final int DES_FILE_LENGTH_30K = 30 * 1024;
    public static final int HEIGHT_30K = 480;
    public static final int WIDTH_30K = 800;
    public static final int DES_FILE_LENGTH_100K = 100 * 1024;
    public static final int HEIGHT_100K = 720;
    public static final int WIDTH_100K = 1080;
    public static final String SRC_PATH = "srcPath";
    public static final String DES_PATH = "desPath";
    public static final String DES_LENGTH = "desLength";
    public static final String JK_STATUSE = "status";
    public static final String JK_FILE_PATH = "filePath";
    public static final String JK_OK = "ok";
    public static final String JK_FAIL = "fail";
    public static final String COMPRESS_TEMP_FILE_PREFIX = "compress_temp_";
    public static final String COMPRESS_TEMP_FILE_SUFFIX = "jpg";
    // 图片临时保存的位置
    public static final String TEMP_PATH = "uex_image_temp";
    public static final String NO_MEDIA = ".nomedia";
    public static final int ERROR_CODE_SUCCESS = 1;
    public static final int ERROR_CODE_FAIL = 0;
}
