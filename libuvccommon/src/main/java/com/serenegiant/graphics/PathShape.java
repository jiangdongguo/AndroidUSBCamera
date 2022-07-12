package com.serenegiant.graphics;
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

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

/**
 * 指定したPathを描画するシェイプ
 */
public class PathShape extends BaseShape {

    private Path mPath = new Path();

    /**
     * PathShape constructor.
     *
     * @param path       Shape表示内容を定義するPath。Pathの座標値はShape内描画座標系で
     * Shapeの描画内容の位置関係を示すだけなので実際の表示サイズとは異なる。
     * @param stdWidth   Shape内座標系の最大幅
     * @param stdHeight  Shape内座標系の最大高さ
     */
    public PathShape(final Path path, final float stdWidth, final float stdHeight) {
    	super(stdWidth, stdHeight);
        setPath(path);
    }

    @Override
    protected void doDraw(final Canvas canvas, final Paint paint) {
        canvas.drawPath(mPath, paint);
    }

    @Override
    public PathShape clone() throws CloneNotSupportedException {
        final PathShape shape = (PathShape) super.clone();
        shape.mPath = new Path(mPath);
        return shape;
    }

    /**
     * Shape表示内容を定義するPathを設定する
     * @param path
     */
    public void setPath(final Path path) {
    	mPath.reset();
    	if (path != null && !path.isEmpty()) {
    		mPath.addPath(path);
    	}
    }

    /**
     * 設定されているPathを返す
     * @return
     */
    public Path getPath() {
    	return mPath;
    }
}
