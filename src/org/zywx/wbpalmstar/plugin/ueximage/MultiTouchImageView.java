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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.widget.ImageView;

public class MultiTouchImageView extends ImageView {

	public static final String TAG = "MultiTouch";
	private static final int NONE = 0;
	private static final int DRAG = 1;
	private static final int ZOOM = 2;
	private static final float MAX_SCALE_RATE = 1000F;
	private static final float SCALE_SLOP = 0.01f;
	private static final float MOVE_SLOP = 16;

	private int mode = NONE;
	private float startDistance;
	private Bitmap currentBitmap = null;

	// 表示当前图片显示的Matrix,通过用户行为作出改变
	private Matrix currentMatrix = new Matrix();

	private Matrix savedMatrix = new Matrix();// 保存在手指接触屏幕瞬间时的Matrix
	// 表示初始值Matrix
	protected Matrix baseMatrix = new Matrix();

	// Temporary buffer used for getting the values out of a matrix.
	private float[] mMatrixValues = new float[9];
	private PointF startPoint = new PointF();
	private PointF midPoint = new PointF();
	public Bitmap displayBitmap;
	private boolean enableMultiTouch = true;
	public boolean needMoveEvent = false;
	private int bitmapWidth;
	private int bitmapHeight;
	private OnSingleTapListener singleTapListener = null;
	private GestureDetector gestureDetector = new GestureDetector(
			new SimpleOnGestureListener() {

				public boolean onSingleTapConfirmed(MotionEvent e) {
					if (singleTapListener != null) {
						singleTapListener.onSingleTap(MultiTouchImageView.this);
					}
					return true;
				};

				public boolean onDoubleTap(MotionEvent event) {
					final float currentScale = getCurrentScale();
					final float baseScale = getScale(baseMatrix);
					if (roughCompareScale(baseScale, currentScale)) {// 缩放率差距降噪，认为相等(处在初始状态)
						float targetScale = Math.min(currentScale * 2,
								MAX_SCALE_RATE);// 放大为当前缩放率的2倍但是不能超过最大缩放率
						float deltaScale = targetScale / currentScale;
						final Matrix tmpMatrix = new Matrix(currentMatrix);
						tmpMatrix.postScale(deltaScale, deltaScale,
								event.getX(), event.getY());
						float destX = (getWidth() - getScaleWidth(tmpMatrix)) / 2;
						float destY = (getHeight() - getScaleHeight(tmpMatrix)) / 2;
						AnimZoomTo(targetScale, event.getX(), event.getY(),
								destX, destY, 300);
						return true;
					}
					if (currentScale > getScale(baseMatrix)) {// 放大状态,双击变成初始状态
						AnimZoomTo(getScale(baseMatrix), event.getX(),
								event.getY(), getTransX(baseMatrix),
								getTransY(baseMatrix), 300);
						return true;
					}
					return true;
				};

			});

	public void setOnSingleTapListener(OnSingleTapListener listener) {
		this.singleTapListener = listener;
	}

	public MultiTouchImageView(Context context) {
		super(context);
		init();
	}

	public MultiTouchImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public MultiTouchImageView(Context context, AttributeSet attrs, int style) {
		super(context, attrs, style);
		init();
	}

	private void init() {
		if (Build.VERSION.SDK_INT >= 7) {
			enableMultiTouch = true;
		} else {
			enableMultiTouch = false;
		}
		setScaleType(ScaleType.MATRIX);
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		final Drawable drawable = getDrawable();
		if (drawable != null) {
			bitmapWidth = drawable.getIntrinsicWidth();
			bitmapHeight = drawable.getIntrinsicHeight();
			setFitScreenMatrix(bitmapWidth, bitmapHeight, baseMatrix);
			setImageMatrix(baseMatrix);
			currentMatrix.set(baseMatrix);
		}
	}

	@Override
	public void setImageBitmap(Bitmap bm) {
		super.setImageBitmap(bm);
		this.currentBitmap = bm;
	}

	public Bitmap getBitmap() {
		return this.currentBitmap;
	}

	public boolean onTouchEvent(MotionEvent event) {
		if (!enableMultiTouch) {
			return false;
		}
		if (gestureDetector.onTouchEvent(event)) {
			return true;
		}
		final int action = event.getAction() & MotionEvent.ACTION_MASK;
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			currentMatrix.set(getImageMatrix());
			savedMatrix.set(currentMatrix);
			// 如果当前缩放率等于标准缩放率，允许拦截touch事件,但是仍然返回true表示处理了事件，防止拦截初始状态下的多点触摸事件
			if (roughCompareScale(getCurrentScale(), getScale(baseMatrix))) {
				getParent().requestDisallowInterceptTouchEvent(false);
			} else {// 放大状态，图片可以拖拉移动
				// 不允许拦截ViewGroup touch事件
				getParent().requestDisallowInterceptTouchEvent(true);
			}
			startPoint.set(event.getX(), event.getY());

			return true;
		case MotionEvent.ACTION_POINTER_DOWN:
			// 多点触摸模式，不允许拦截touch事件
			getParent().requestDisallowInterceptTouchEvent(true);
			startDistance = spacing(event);
			savedMatrix.set(currentMatrix);
			midPoint(midPoint, event);
			mode = ZOOM;
			return true;
		case MotionEvent.ACTION_MOVE:
			currentMatrix.set(savedMatrix);
			if (mode == NONE) {
				float offsetX = Math.abs(event.getX() - startPoint.x);
				float offsetY = Math.abs(event.getY() - startPoint.y);
				if (offsetX > MOVE_SLOP || offsetY > MOVE_SLOP) {
					mode = DRAG;
				}
				return false;
			}
			switch (mode) {
			case DRAG:
				if (roughCompareScale(getScale(currentMatrix),
						getScale(baseMatrix))) {
					return false;
				}

				float moveX = event.getX() - startPoint.x;
				float moveY = event.getY() - startPoint.y;
				float currentTransX = getTransX(currentMatrix);
				float currentTransY = getTransY(currentMatrix);
				boolean hasMovedX = false;
				if (canCurrentCoverX()) {// 在X方向超过屏幕才能移动在X方向移動

					if (moveX > 0) {// 向右移动
						final float currentMaxTransX = getCurrentMaxTransX();
						if (currentTransX != currentMaxTransX) {
							if (currentTransX + moveX < currentMaxTransX) {// 不能超过最大值
								currentMatrix.postTranslate(moveX, 0);
								hasMovedX = true;
							} else {// 超过最大值设置为最大值
								currentMatrix.postTranslate(currentMaxTransX
										- currentTransX, 0);
							}
						}
					} else if (moveX < 0) {
						final float currentMinTransX = getCurrentMinTransX();
						if (currentMinTransX != currentTransX) {
							if (currentTransX + moveX > currentMinTransX) {
								currentMatrix.postTranslate(moveX, 0);
								hasMovedX = true;
							} else {
								currentMatrix.postTranslate(currentMinTransX
										- currentTransX, 0);
							}
						}
					}
				}

				if (canCurrentCoverY()) {// 能移动
					if (moveY > 0) {// 向下移动
						final float currentMaxTransY = getCurrentMaxTransY();

						if (currentMaxTransY != currentTransY) {
							if (currentTransY + moveY < currentMaxTransY) {// 不能超过最大值
								currentMatrix.postTranslate(0, moveY);
							} else {
								currentMatrix.postTranslate(0, currentMaxTransY
										- currentTransY);
							}
						}
					} else if (moveY < 0) {// 向上移动
						final float currentMinTransY = getCurrentMinTransY();
						if (currentMinTransY != currentTransY) {// 没有到达边缘
							if (currentTransY + moveY > currentMinTransY) {// 没有超过Y方向最小边缘
								currentMatrix.postTranslate(0, moveY);
							} else {// 超过Y方向最小边缘，直接设置为最小边缘偏移量
								currentMatrix.postTranslate(0, currentMinTransY
										- currentTransY);
							}
						}
					}
				}
				setImageMatrix(currentMatrix);
				getParent().requestDisallowInterceptTouchEvent(hasMovedX);
				return true;
			case ZOOM:
				float newDistance = spacing(event);
				currentMatrix.set(savedMatrix);
				float scale = newDistance / startDistance;
				currentMatrix.postScale(scale, scale, midPoint.x, midPoint.y);
				this.setImageMatrix(currentMatrix);
				return true;
			}
			break;
		case MotionEvent.ACTION_UP:// 第一个手指离开
			mode = NONE;
			return true;
		case MotionEvent.ACTION_POINTER_UP:// 第2个手指离开
			float currentScale = getScale(currentMatrix);
			float baseScale = getScale(baseMatrix);
			if (currentScale <= baseScale) {// 缩小状态，需要弹回原始大小
				AnimZoomTo(baseScale, midPoint.x, midPoint.y,
						getTransX(baseMatrix), getTransY(baseMatrix), 300);
			} else {
				if (currentScale > MAX_SCALE_RATE) {// 大于最大缩放率，弹回最大缩放率
					float deltaScale = MAX_SCALE_RATE / currentScale;
					final Matrix tmpMatrix = new Matrix(currentMatrix);
					tmpMatrix.postScale(deltaScale, deltaScale, midPoint.x,
							midPoint.y);
					final float targetTransX = getTransX(tmpMatrix);
					final float targetTransY = getTransY(tmpMatrix);
					AnimZoomTo(MAX_SCALE_RATE, midPoint.x, midPoint.y,
							targetTransX, targetTransY, 300);
				}
			}
			mode = NONE;
			return true;
		case MotionEvent.ACTION_CANCEL:
			mode = NONE;
			break;
		}
		return true;
	}

	/**
	 * 计算2点之间的距离
	 * 
	 * @param event
	 * @return
	 */
	private float spacing(MotionEvent event) {
		float x = event.getX(0) - event.getX(1);
		float y = event.getY(0) - event.getY(1);
		return FloatMath.sqrt(x * x + y * y);
	}

	/**
	 * 计算2点之间的中点坐标
	 * 
	 * @param point
	 * @param event
	 */
	private void midPoint(PointF point, MotionEvent event) {
		float x = event.getX(0) + event.getX(1);
		float y = event.getY(0) + event.getY(1);
		point.set(x / 2, y / 2);
	}

	/**
	 * 根据View和图片的宽高获得适应屏幕的Matrix
	 * 
	 * @param bitmapW
	 * @param bitmapH
	 * @param matrix
	 */
	private void setFitScreenMatrix(int bitmapW, int bitmapH, Matrix matrix) {
		if (bitmapH > 0 && bitmapW > 0) {
			matrix.reset();
			final float viewWidth = getWidth();
			final float viewHeight = getHeight();
			float viewRate = viewWidth / viewHeight;
			float bitmapRate = bitmapW / bitmapH;
			if (viewRate > bitmapRate) {
				float heightScale = viewHeight / bitmapH;
				matrix.postScale(heightScale, heightScale);
				matrix.postTranslate((viewWidth - bitmapW * heightScale) / 2, 0);
			} else {
				float widthScale = viewWidth / bitmapW;
				matrix.postScale(widthScale, widthScale);
				matrix.postTranslate(0, (viewHeight - bitmapH * widthScale) / 2);
			}
		}
	}

	public Matrix getBaseMatrix() {
		return baseMatrix;
	}

	protected float getValue(Matrix matrix, int whichValue) {
		matrix.getValues(mMatrixValues);
		return mMatrixValues[whichValue];
	}

	// Get the scale factor out of the matrix.
	private float getScale(Matrix matrix) {
		return getValue(matrix, Matrix.MSCALE_X);
	}

	private float getTransX(Matrix matrix) {
		return getValue(matrix, Matrix.MTRANS_X);
	}

	private float getTransY(Matrix matrix) {
		return getValue(matrix, Matrix.MTRANS_Y);
	}

	protected float getCurrentScale() {
		return getScale(currentMatrix);
	}

	private void zoomTo(float scale, float centerX, float centerY) {
		float oldScale = getCurrentScale();
		float deltaScale = scale / oldScale;
		currentMatrix.postScale(deltaScale, deltaScale, centerX, centerY);
		setImageMatrix(currentMatrix);
	}

	private void zoomTo(float targetScale, float centerX, float centerY,
			float targetTransX, float targetTransY) {
		final float currentScale = getCurrentScale();
		final float deltaScale = targetScale / currentScale;// 目标缩放率与当前缩放率的差量

		final float currentTransX = getTransX(currentMatrix);
		final float deltaTransX = targetTransX - currentTransX;// 目标X轴偏移量与当前X轴偏移量的差量

		final float currentTransY = getTransY(currentMatrix);
		final float deltaTransY = targetTransY - currentTransY;// 目标Y轴偏移量与当前Y轴偏移量的差量

		currentMatrix.postScale(deltaScale, deltaScale, centerX, centerY);// 会造成transX和transY;
		currentMatrix.postTranslate(deltaTransX
				- (getTransX(currentMatrix) - currentTransX), deltaTransY
				- (getTransY(currentMatrix) - currentTransY));
		setImageMatrix(currentMatrix);
	}

	private void animTranslateTo(final float destX, final float destY,
			final float duration) {
		final float oldTransX = getTransX(currentMatrix);
		final float oldTransY = getTransY(currentMatrix);
		final float perTransX = (destX - oldTransX) / duration;
		final float perTransY = (destY - oldTransY) / duration;
		final long startTime = System.currentTimeMillis();
		mHandler.post(new Runnable() {
			public void run() {
				final long now = System.currentTimeMillis();
				final float currentMs = Math.min(duration, now - startTime);
				float targetTransX = oldTransX + (perTransX * currentMs);// 此刻目标transX
				float targetTransY = oldTransY + (perTransY * currentMs);// 此刻目标transY
				final float deltaTransX = targetTransX
						- getTransX(currentMatrix);// 目标X轴偏移量与当前X轴偏移量的差量
				final float deltaTransY = targetTransY
						- getTransY(currentMatrix);// 目标Y轴偏移量与当前Y轴偏移量的差量
				currentMatrix.postTranslate(deltaTransX, deltaTransY);
				setImageMatrix(currentMatrix);
				if (currentMs < duration) {
					mHandler.post(this);
				}
			}
		});
	}

	/**
	 * 动画缩放图片到指定比例和位置
	 * 
	 * @param destScale
	 *            目标缩放率
	 * @param centerX
	 *            缩放中心X
	 * @param centerY
	 *            缩放中心Y
	 * @param destX
	 *            目标transX
	 * @param destY
	 *            目标transY
	 * @param duration
	 *            动画时间
	 */

	private void AnimZoomTo(final float destScale, final float centerX,
			final float centerY, final float destX, final float destY,
			final float duration) {
		final float perScale = (destScale - getCurrentScale()) / duration;
		final float oldScale = getCurrentScale();

		final float oldTransX = getTransX(currentMatrix);
		final float perTransX = (destX - oldTransX) / duration;

		final float oldTransY = getTransY(currentMatrix);
		final float perTransY = (destY - oldTransY) / duration;
		final long startTime = System.currentTimeMillis();
		mHandler.post(new Runnable() {
			public void run() {
				final long now = System.currentTimeMillis();
				final float currentMs = Math.min(duration, now - startTime);
				float targetScale = oldScale + (perScale * currentMs);// 此刻目标scale
				float targetTransX = oldTransX + (perTransX * currentMs);// 此刻目标transX
				float targetTransY = oldTransY + (perTransY * currentMs);// 此刻目标transY
				zoomTo(targetScale, centerX, centerY, targetTransX,
						targetTransY);
				if (currentMs < duration) {
					mHandler.post(this);
				}
			}
		});

	}

	protected Handler mHandler = new Handler();

	protected void zoomTo(final float scale, final float centerX,
			final float centerY, final float durationMs) {
		final float perScale = (scale - getCurrentScale()) / durationMs;
		final float oldScale = getCurrentScale();
		final long startTime = System.currentTimeMillis();
		mHandler.post(new Runnable() {
			public void run() {
				long now = System.currentTimeMillis();
				float currentMs = Math.min(durationMs, now - startTime);
				float targetScale = oldScale + (perScale * currentMs);
				zoomTo(targetScale, centerX, centerY);
				if (currentMs < durationMs) {
					mHandler.post(this);
				}
			}
		});
	}

	public void translateToCenter() {
		float destX = getTransX(currentMatrix);
		float destY = getTransY(currentMatrix);
		if (!canCurrentCoverX()) {
			destX = (getWidth() - getCurrentScaleWidth()) / 2;
		}
		if (!canCurrentCoverY()) {
			destY = (getHeight() - getCurrentScaleHeight()) / 2;
		}
		animTranslateTo(destX, destY, 300);
	}

	public String getMatrixInfo(Matrix matrix) {
		return "--> Scale:" + getValue(matrix, Matrix.MSCALE_X) + " TransX:"
				+ getValue(matrix, Matrix.MTRANS_X) + "  TransY:"
				+ getValue(matrix, Matrix.MTRANS_Y);
	}

	public boolean matrixCompare(Matrix m1, Matrix m2) {
		if (m1 == null || m2 == null) {
			return false;
		}
		if (getValue(m1, Matrix.MSCALE_X) != getValue(m2, Matrix.MSCALE_X)) {
			return false;
		}
		if (getValue(m1, Matrix.MTRANS_X) != getValue(m2, Matrix.MTRANS_X)) {
			return false;
		}
		if (getValue(m1, Matrix.MTRANS_Y) != getValue(m2, Matrix.MTRANS_Y)) {
			return false;
		}
		return true;
	}

	public int getDurationByScale(float currentScale, float destScale) {
		float absRate = Math.abs(destScale - currentScale);
		float duration = absRate * 1000;
		duration = Math.max(duration, 200);
		duration = Math.min(duration, 300);
		return (int) duration;
	}

	private float getCurrentScaleWidth() {
		return getScale(currentMatrix) * bitmapWidth;
	}

	private float getScaleWidth(Matrix matrix) {
		return getScale(matrix) * bitmapWidth;
	}

	private float getScaleHeight(Matrix matrix) {
		return getScale(matrix) * bitmapHeight;
	}

	private float getCurrentScaleHeight() {
		return getScale(currentMatrix) * bitmapHeight;
	}

	private float getCurrentMinTransX() {
		return getWidth() - getCurrentScaleWidth();
	}

	private float getCurrentMaxTransX() {
		return 0;
	}

	private float getCurrentMinTransY() {
		return getHeight() - getCurrentScaleHeight();
	}

	private float getCurrentMaxTransY() {
		return 0;
	}

	/**
	 * 当前图片显示在X坐标方向是否已经覆盖满屏幕
	 * 
	 * @return
	 */
	private boolean canCurrentCoverX() {
		if (getCurrentScaleWidth() >= getWidth()) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 当前图片显示在Y坐标方向是否已经覆盖满屏幕
	 * 
	 * @return
	 */
	private boolean canCurrentCoverY() {
		if (getCurrentScaleHeight() >= getHeight()) {
			return true;
		} else {
			return false;
		}
	}

	public static String getStringActionByEvent(int action) {
		switch (action) {
		case MotionEvent.ACTION_POINTER_DOWN:
			return "POINTER_DOWN";
		case MotionEvent.ACTION_POINTER_UP:
			return "POINTER_UP";
		case MotionEvent.ACTION_CANCEL:
			return "CANCEL";
		case MotionEvent.ACTION_DOWN:
			return "DOWN";
		case MotionEvent.ACTION_MOVE:
			return "MOVE";
		case MotionEvent.ACTION_UP:
			return "UP";
		}
		return null;
	}

	/**
	 * 粗略比较2个Scale的XScale，差距在0.01认为是一样
	 * 
	 * @param src
	 * @param dest
	 * @return
	 */
	public boolean roughCompareScale(float src, float dest) {
		if (Math.abs(src - dest) < SCALE_SLOP) {
			return true;
		} else {
			return false;
		}
	}

	public void doubleZoomIn() {
		final float currentScale = getCurrentScale();
		final float baseScale = getScale(baseMatrix);
		if (roughCompareScale(baseScale, currentScale)) {// 缩放率差距降噪，认为相等(处在初始状态)
			float targetScale = Math.min(currentScale * 2, MAX_SCALE_RATE);// 放大为当前缩放率的2倍但是不能超过最大缩放率
			float deltaScale = targetScale / currentScale;
			final Matrix tmpMatrix = new Matrix(currentMatrix);
			tmpMatrix.postScale(deltaScale, deltaScale, getWidth() / 2,
					getHeight() / 2);
			float destX = (getWidth() - getScaleWidth(tmpMatrix)) / 2;
			float destY = (getHeight() - getScaleHeight(tmpMatrix)) / 2;
			AnimZoomTo(targetScale, getWidth() / 2, getHeight() / 2, destX,
					destY, 300);
		}

	}

	public void ZoomToBase() {
		if (getCurrentScale() > getScale(baseMatrix)) {// 放大状态,双击变成初始状态
			AnimZoomTo(getScale(baseMatrix), getWidth() / 2, getHeight() / 2,
					getTransX(baseMatrix), getTransY(baseMatrix), 300);
		}
	}

	public static interface OnSingleTapListener {
		void onSingleTap(MultiTouchImageView view);
	}
}