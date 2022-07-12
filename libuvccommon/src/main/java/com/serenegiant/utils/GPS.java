package com.serenegiant.utils;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2018 saki t_saki@serenegiant.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
*/

public class GPS {
	public static class Datum {
		public final double a;	// 長半径(赤道半径)[m]
		public final double b;	// 短半径(極半径)[m]
		public final double inv_ellipticity;
		public final double e2;
		public final double ae2;

		public Datum(final double a, final double b, final double ellipticity, final boolean inverse) {
			this.a = a;
			this.b = b;
			this.inv_ellipticity = inverse ? ellipticity : 1 / ellipticity;
			final double a2 = a * a;
			e2 = (a2 - b * b) / a2;
			ae2 = a * (1-e2);
		}
	}

	/** ベッセル(旧日本測地系) */
	public static final Datum BESSEL;
	/** WGS84(GPS)測地系の定数 */
	public static final Datum WGS84;
	/** 世界測地系の定数 */
	public static final Datum GRS80;
	static {
		BESSEL = new Datum(6377397.155, 6356079.000000, 299.152813000, true);	// 旧日本測地系
		WGS84 = new Datum(6378137.000, 6356752.314245, 298.257223563, true);	// GPS
		GRS80 = new Datum(6378137.000, 6356752.314140, 298.257222101, true);	// 世界測地系
	}

	/**
	 * 緯度経度で指定した2点間の距離[m]を計算, ヒュベニの公式
	 * @param datum 測地系定数
	 * @param latitude_degree1 地点1緯度[度]
	 * @param longitude_degree1 地点1軽度[度]
	 * @param altitude1 地点1高度[m](未使用)
	 * @param latitude_degree2 地点2緯度[度]
	 * @param longitude_degree2 地点2軽度[度]
	 * @param altitude2 地点2高度[m](未使用)
	 * @return
	 */
	public static double distance(final Datum datum,
		final double latitude_degree1, final double longitude_degree1, final double altitude1,
		final double latitude_degree2, final double longitude_degree2, final double altitude2) {

		final double latitude1 = Math.toRadians(latitude_degree1);
		final double latitude2 = Math.toRadians(latitude_degree2);
		final double longitude1 = Math.toRadians(longitude_degree1);
		final double longitude2 = Math.toRadians(longitude_degree2);

		final double dy = latitude1 - latitude2;
		final double dx = longitude1 - longitude2;

		final double u = (latitude1 + latitude2) / 2.0;
		final double sin_u = Math.sin(u);
		final double w = Math.sqrt(1 - datum.e2 * sin_u * sin_u);
		final double m = datum.ae2 / (w * w * w);
		final double n = datum.a / w;

		final double dm = dy * m;
		final double dn = dx * n * Math.cos(u);
		return Math.sqrt(dm * dm + dn * dn);
	}
}
