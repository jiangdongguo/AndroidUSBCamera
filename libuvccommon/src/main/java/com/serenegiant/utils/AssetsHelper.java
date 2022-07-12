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

import android.content.res.AssetManager;
import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class AssetsHelper {

	public static String loadString(@NonNull final AssetManager assets, @NonNull final String name) throws IOException {
		final StringBuffer sb = new StringBuffer();
		final char[] buf = new char[1024];
		final BufferedReader reader = new BufferedReader(new InputStreamReader(assets.open(name)));
		int r = reader.read(buf);
		while (r > 0) {
			sb.append(buf, 0, r);
			r = reader.read(buf);
		}
		return sb.toString();
	}
}
