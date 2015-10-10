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
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

public class UpdateMediaData {
	private static UpdateMediaData sUpdateMediaData = null;
	private Context mContext;

	public UpdateMediaData(Context context) {
		mContext = context;
	}

	public static UpdateMediaData getInstance(Context context) {
		if (sUpdateMediaData == null) {
			sUpdateMediaData = new UpdateMediaData(context);
		}
		sUpdateMediaData.mContext = context;
		return sUpdateMediaData;
	}

	/**
	 * 更新媒体库
	 * 
	 * @param path
	 *            要更新的文件或者目录
	 */
	public void updateFile(String path) {
		Log.i("uexImageBrowser", "插入媒体库");
		mContext.sendBroadcast(new Intent(
				Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://"
						+ path)));
	}
}
