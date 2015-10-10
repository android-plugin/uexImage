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

import java.io.Serializable;

/**
 * Created by ylt on 15/10/10.
 */
public class OpenPickerInputVO implements Serializable {

    private int min;
    private int max;
    private double quality;
    private boolean detailedInfo;

    public int getMin() {
        return min;
    }

    public void setMin(int min) {
        this.min = min;
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public double getQuality() {
        return quality;
    }

    public void setQuality(double quality) {
        this.quality = quality;
    }

    public boolean isDetailedInfo() {
        return detailedInfo;
    }

    public void setDetailedInfo(boolean detailedInfo) {
        this.detailedInfo = detailedInfo;
    }
}
