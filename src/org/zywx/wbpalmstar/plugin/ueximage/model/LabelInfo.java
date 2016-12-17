package org.zywx.wbpalmstar.plugin.ueximage.model;

/**
 * Created by fred on 16/12/1.
 */
public class LabelInfo {
    private String id;
    private String content;
    private float left;
    private float right;
    private float top;
    private float bottom;
    private int targetPointMode;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public float getLeft() {
        return left;
    }

    public void setLeft(float left) {
        this.left = left;
    }

    public float getRight() {
        return right;
    }

    public void setRight(float right) {
        this.right = right;
    }

    public float getTop() {
        return top;
    }

    public void setTop(float top) {
        this.top = top;
    }

    public float getBottom() {
        return bottom;
    }

    public void setBottom(float bottom) {
        this.bottom = bottom;
    }

    public int getTargetPointMode() {
        return targetPointMode;
    }

    public void setTargetPointMode(int targetPointMode) {
        this.targetPointMode = targetPointMode;
    }
}
