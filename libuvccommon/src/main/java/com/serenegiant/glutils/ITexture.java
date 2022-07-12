package com.serenegiant.glutils;
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

import android.graphics.Bitmap;

import java.io.IOException;

public interface ITexture {
	void release();

	void bind();
	void unbind();

	int getTexTarget();
	int getTexture();

	float[] getTexMatrix();
	void getTexMatrix(float[] matrix, int offset);

	int getTexWidth();
	int getTexHeight();

	void loadTexture(String filePath) throws NullPointerException, IOException;
	void loadTexture(Bitmap bitmap) throws NullPointerException;
}
