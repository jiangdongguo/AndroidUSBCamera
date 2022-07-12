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

import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.Locale;

import javax.microedition.khronos.egl.EGL10;

import org.json.JSONException;
import org.json.JSONObject;

import android.opengl.GLES20;

import com.serenegiant.glutils.EGLBase;

public class OpenGLInfo {
	private static final int EGL_CLIENT_APIS                    = 0x308D;

	public static JSONObject get() throws JSONException {
		JSONObject result = new JSONObject();
		try {
	    	final EGLBase egl = EGLBase.createFrom(3, null, false, 0, false);
	    	final EGLBase.IEglSurface dummy = egl.createOffscreen(1, 1);
	    	dummy.makeCurrent();
	    	try {
		    	final IntBuffer val = IntBuffer.allocate(2);
		    	JSONObject glinfo = new JSONObject();
		    	try {
		    		glinfo.put("GL_VENDOR", GLES20.glGetString(GLES20.GL_VENDOR));
		    	} catch (Exception e) {
		    		glinfo.put("GL_VENDOR", e.getMessage());
		    	}
		    	try {
		    		glinfo.put("GL_VERSION", GLES20.glGetString(GLES20.GL_VERSION));
		    	} catch (Exception e) {
		    		glinfo.put("GL_VERSION", e.getMessage());
		    	}
		    	try {
			    	glinfo.put("GL_RENDERER", GLES20.glGetString(GLES20.GL_RENDERER));
		    	} catch (Exception e) {
		    		glinfo.put("GL_RENDERER", e.getMessage());
		    	}
		    	try {
			        GLES20.glGetIntegerv(GLES20.GL_MAX_VERTEX_ATTRIBS, val);
			        glinfo.put("GL_MAX_VERTEX_ATTRIBS", val.get(0));
		    	} catch (Exception e) {
		    		glinfo.put("GL_MAX_VERTEX_ATTRIBS", e.getMessage());
		    	}
		    	try {
			        GLES20.glGetIntegerv(GLES20.GL_MAX_VERTEX_UNIFORM_VECTORS, val);
			        glinfo.put("GL_MAX_VERTEX_UNIFORM_VECTORS", val.get(0));
		    	} catch (Exception e) {
		    		glinfo.put("GL_MAX_VERTEX_UNIFORM_VECTORS", e.getMessage());
		    	}
		    	try {
			        GLES20.glGetIntegerv(GLES20.GL_MAX_VARYING_VECTORS, val);
			        glinfo.put("GL_MAX_VARYING_VECTORS", val.get(0));
		    	} catch (Exception e) {
		    		glinfo.put("GL_MAX_VARYING_VECTORS", e.getMessage());
		    	}
		    	try {
			        GLES20.glGetIntegerv(GLES20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS, val);
			        glinfo.put("GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS", val.get(0));
		    	} catch (Exception e) {
		    		glinfo.put("GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS", e.getMessage());
		    	}
		    	try {
			        GLES20.glGetIntegerv(GLES20.GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS, val);
			        glinfo.put("GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS", val.get(0));
		    	} catch (Exception e) {
		    		glinfo.put("GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS", e.getMessage());
		    	}
		    	try {
			        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_IMAGE_UNITS, val);
			        glinfo.put("GL_MAX_TEXTURE_IMAGE_UNITS", val.get(0));
		    	} catch (Exception e) {
		    		glinfo.put("GL_MAX_TEXTURE_IMAGE_UNITS", e.getMessage());
		    	}
		    	try {
			        GLES20.glGetIntegerv(GLES20.GL_MAX_FRAGMENT_UNIFORM_VECTORS, val);
			        glinfo.put("GL_MAX_FRAGMENT_UNIFORM_VECTORS", val.get(0));
		    	} catch (Exception e) {
		    		glinfo.put("GL_MAX_FRAGMENT_UNIFORM_VECTORS", e.getMessage());
		    	}
		    	try {
			        GLES20.glGetIntegerv(GLES20.GL_MAX_CUBE_MAP_TEXTURE_SIZE, val);
			        glinfo.put("GL_MAX_CUBE_MAP_TEXTURE_SIZE", val.get(0));
		    	} catch (Exception e) {
		    		glinfo.put("GL_MAX_CUBE_MAP_TEXTURE_SIZE", e.getMessage());
		    	}
		    	try {
			        GLES20.glGetIntegerv(GLES20.GL_MAX_RENDERBUFFER_SIZE, val);
			        glinfo.put("GL_MAX_RENDERBUFFER_SIZE", val.get(0));
		    	} catch (Exception e) {
		    		glinfo.put("GL_MAX_RENDERBUFFER_SIZE", e.getMessage());
		    	}
		    	try {
			        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, val);
			        glinfo.put("GL_MAX_TEXTURE_SIZE", val.get(0));
		    	} catch (Exception e) {
		    		glinfo.put("GL_MAX_TEXTURE_SIZE", e.getMessage());
		    	}
		    	try {
			        GLES20.glGetIntegerv(GLES20.GL_MAX_VIEWPORT_DIMS, val);
			        glinfo.put("GL_MAX_VIEWPORT_DIMS", String.format(Locale.US, "%d x %d", val.get(0), val.get(1)));
		    	} catch (Exception e) {
		    		glinfo.put("GL_MAX_VIEWPORT_DIMS", e.getMessage());
		    	}
		    	try {
			        glinfo.put("GL_EXTENSIONS", formatExtensions(GLES20.glGetString(GLES20.GL_EXTENSIONS)));
		    	} catch (Exception e) {
		    		glinfo.put("GL_EXTENSIONS", e.getMessage());
		    	}
		        result.put("GL_INFO", glinfo);
		    	JSONObject eglinfo = new JSONObject();
		    	try {
		    		eglinfo.put("EGL_VENDOR", egl.queryString(EGL10.EGL_VENDOR));
		    	} catch (Exception e) {
		    		glinfo.put("EGL_VENDOR", e.getMessage());
		    	}
		    	try {
		    		eglinfo.put("EGL_VERSION", egl.queryString(EGL10.EGL_VERSION));
		    	} catch (Exception e) {
		    		glinfo.put("EGL_VERSION", e.getMessage());
		    	}
		    	try {
		    		eglinfo.put("EGL_CLIENT_APIS", egl.queryString(EGL_CLIENT_APIS));
		    	} catch (Exception e) {
		    		glinfo.put("EGL_CLIENT_APIS", e.getMessage());
		    	}
		    	try {
		    		eglinfo.put("EGL_EXTENSIONS:", formatExtensions(egl.queryString(EGL10.EGL_EXTENSIONS)));
		    	} catch (Exception e) {
		    		glinfo.put("EGL_EXTENSIONS", e.getMessage());
		    	}
		        result.put("EGL_INFO", eglinfo);
	    	} finally {
		        dummy.release();
		        egl.release();
	    	}
		} catch (Exception e) {
			result.put("EXCEPTION", e.getMessage());
		}
		return result;
	}


    /**
     * Formats the extensions string, which is a space-separated list, into a series of indented
     * values followed by newlines.  The list is sorted.
     */
    private static final JSONObject formatExtensions(String ext) throws JSONException {
    	JSONObject result = new JSONObject();
        final String[] values = ext.split(" ");
        Arrays.sort(values);
        for (int i = 0; i < values.length; i++) {
			result.put(Integer.toString(i), values[i]);
        }
        return result;
    }

}
