package org.zywx.wbpalmstar.plugin.ueximage.model;

/**
 * Created by Fred on 2015/8/13.
 */
public class PictureInfo {
    private String src;//原图系统URI或路径
    private String thumb;//缩略图URI或路径
    private String desc;

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    public String getThumb() {
        return thumb;
    }

    public void setThumb(String thumb) {
        this.thumb = thumb;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
