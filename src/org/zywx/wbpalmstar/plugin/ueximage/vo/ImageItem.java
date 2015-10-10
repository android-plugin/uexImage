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

package org.zywx.wbpalmstar.plugin.ueximage.vo;

import org.zywx.wbpalmstar.plugin.ueximage.MultiTouchImageView;

import java.io.Serializable;

public class ImageItem implements Serializable {
	/**
	 * @Fields serialVersionUID : TODO
	 */
	private static final long serialVersionUID = -8015462855272325673L;
	public static final int TAG_NULL_IMAGE = 0;
	public static final int TAG_TINY_IMAGE = 1;
	public static final int TAG_SRC_IMAGE = 2;
	private MultiTouchImageView imageView;
	private ImageInfo imageInfo;

	public ImageItem() {
		imageInfo = new ImageInfo();
	}

	public MultiTouchImageView getImageView() {
		return imageView;
	}

	public void setImageView(MultiTouchImageView imageView) {
		this.imageView = imageView;
	}

	public ImageInfo getImageInfo() {
		return imageInfo;
	}

	public void setImageInfo(ImageInfo imageInfo) {
		this.imageInfo = imageInfo;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}
		if (o instanceof ImageItem) {
			final ImageItem imageItem = (ImageItem) o;
			if (imageInfo.srcUrl.equals(imageItem.getImageInfo().srcUrl)) {
				return true;
			}
		}
		return super.equals(o);
	}

}