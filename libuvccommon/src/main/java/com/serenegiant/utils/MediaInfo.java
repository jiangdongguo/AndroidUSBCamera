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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Build;
import androidx.annotation.RequiresApi;
import android.text.TextUtils;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
public final class MediaInfo {

	public static JSONObject get() throws JSONException {
		final JSONObject result = new JSONObject();
		try {
			result.put("VIDEO", getVideo());
		} catch (final Exception e) {
			result.put("VIDEO", e.getMessage());
		}
		try {
			result.put("AUDIO", getAudio());
		} catch (final Exception e) {
			result.put("AUDIO", e.getMessage());
		}
		return result;
	}

	private static final JSONObject getVideo() throws JSONException{
		final JSONObject result = new JSONObject();
    	// コーデックの一覧を取得
        final int numCodecs = getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
        	final MediaCodecInfo codecInfo = getCodecInfoAt(i);
        	final JSONObject codec = new JSONObject();
            final String[] types = codecInfo.getSupportedTypes();
            final int n = types.length;
            boolean isvideo = false;
            for (int j = 0; j < n; j++) {
                if (types[j].startsWith("video/")) {
                	isvideo = true;
					codec.put(Integer.toString(j), types[j]);
		    		final MediaCodecInfo.CodecCapabilities capabilities;
		    		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
		        	try {
		    			capabilities = getCodecCapabilities(codecInfo, types[j]);
		        	} finally {
		        		// 元の優先度に戻す
		        		Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
		        	}
		        	try {
						final int[] colorFormats = capabilities.colorFormats;
						final int m = colorFormats != null ? colorFormats.length : 0;
						if (m > 0) {
							final JSONObject caps = new JSONObject();
							for (int k = 0; k < m; k++) {
								caps.put(String.format(Locale.US, "COLOR_FORMAT(%d)", k), getColorFormatName(colorFormats[k]));
							}
							codec.put("COLOR_FORMATS", caps);
						}
		        	} catch (final Exception e) {
		            	codec.put("COLOR_FORMATS", e.getMessage());
		        	}
		            try {
				        final MediaCodecInfo.CodecProfileLevel[] profileLevel = capabilities.profileLevels;
			        	final int m = profileLevel != null ? profileLevel.length : 0;
			        	if (m > 0) {
				        	final JSONObject profiles = new JSONObject();
					        for (int k = 0; k < m; k++) {
					        	profiles.put(Integer.toString(k), getProfileLevelString(types[j], profileLevel[k]));
					        }
				        	codec.put("PROFILES", profiles);
			        	}
		            } catch (final Exception e) {
			        	codec.put("PROFILES", e.getMessage());
		            }
                }
            }
            if (isvideo)
            	result.put(codecInfo.getName(), codec);
        }
		return result;
	}

	private static final JSONObject getAudio() throws JSONException {
		final JSONObject result = new JSONObject();
    	// コーデックの一覧を取得
        final int numCodecs = getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
        	final MediaCodecInfo codecInfo = getCodecInfoAt(i);
        	final JSONObject codec = new JSONObject();
            final String[] types = codecInfo.getSupportedTypes();
            final int n = types.length;
            boolean isaudio = false;
            for (int j = 0; j < n; j++) {
                if (types[j].startsWith("audio/")) {
                	isaudio = true;
                	codec.put(Integer.toString(j), types[j]);
                }
            }
            if (isaudio)
            	result.put(codecInfo.getName(), codec);
        }
		return result;
	}

    /**
     * 指定したcolorFormatを表す文字列を取得する
     * @param colorFormat
     * @return
     */
    public static final String getColorFormatName(final int colorFormat) {
    	switch (colorFormat) {
    	case MediaCodecInfo.CodecCapabilities.COLOR_Format12bitRGB444:
    		return "COLOR_Format12bitRGB444";
    	case MediaCodecInfo.CodecCapabilities.COLOR_Format16bitARGB1555:
    		return "COLOR_Format16bitARGB1555";
    	case MediaCodecInfo.CodecCapabilities.COLOR_Format16bitARGB4444:
    		return "COLOR_Format16bitARGB4444";
    	case MediaCodecInfo.CodecCapabilities.COLOR_Format16bitBGR565:
    		return "COLOR_Format16bitBGR565";
    	case MediaCodecInfo.CodecCapabilities.COLOR_Format16bitRGB565:
    		return "COLOR_Format16bitRGB565";
    	case MediaCodecInfo.CodecCapabilities.COLOR_Format18BitBGR666:
    		return "COLOR_Format18BitBGR666";
    	case MediaCodecInfo.CodecCapabilities.COLOR_Format18bitARGB1665:
    		return "COLOR_Format18bitARGB1665";
    	case MediaCodecInfo.CodecCapabilities.COLOR_Format18bitRGB666:
    		return "COLOR_Format18bitRGB666";
    	case MediaCodecInfo.CodecCapabilities.COLOR_Format19bitARGB1666:
    		return "COLOR_Format19bitARGB1666";
    	case MediaCodecInfo.CodecCapabilities.COLOR_Format24BitABGR6666:
    		return "COLOR_Format24BitABGR6666";
    	case MediaCodecInfo.CodecCapabilities.COLOR_Format24BitARGB6666:
    		return "COLOR_Format24BitARGB6666";
    	case MediaCodecInfo.CodecCapabilities.COLOR_Format24bitARGB1887:
    		return "COLOR_Format24bitARGB1887";
    	case MediaCodecInfo.CodecCapabilities.COLOR_Format24bitBGR888:
    		return "COLOR_Format24bitBGR888";
    	case MediaCodecInfo.CodecCapabilities.COLOR_Format24bitRGB888:
    		return "COLOR_Format24bitRGB888";
    	case MediaCodecInfo.CodecCapabilities.COLOR_Format25bitARGB1888:
    		return "COLOR_Format25bitARGB1888";
    	case MediaCodecInfo.CodecCapabilities.COLOR_Format32bitARGB8888:
    		return "COLOR_Format32bitARGB8888";
    	case MediaCodecInfo.CodecCapabilities.COLOR_Format32bitBGRA8888:
    		return "COLOR_Format32bitBGRA8888";
    	case MediaCodecInfo.CodecCapabilities.COLOR_Format8bitRGB332:
    		return "COLOR_Format8bitRGB332";
    	case MediaCodecInfo.CodecCapabilities.COLOR_FormatCbYCrY:
    		return "COLOR_FormatCbYCrY";
    	case MediaCodecInfo.CodecCapabilities.COLOR_FormatCrYCbY:
    		return "COLOR_FormatCrYCbY";
    	case MediaCodecInfo.CodecCapabilities.COLOR_FormatL16:
    		return "COLOR_FormatL16";
    	case MediaCodecInfo.CodecCapabilities.COLOR_FormatL2:
    		return "COLOR_FormatL2";
    	case MediaCodecInfo.CodecCapabilities.COLOR_FormatL24:
    		return "COLOR_FormatL24";
    	case MediaCodecInfo.CodecCapabilities.COLOR_FormatL32:
    		return "COLOR_FormatL32";
    	case MediaCodecInfo.CodecCapabilities.COLOR_FormatL4:
    		return "COLOR_FormatL4";
    	case MediaCodecInfo.CodecCapabilities.COLOR_FormatL8:
    		return "COLOR_FormatL8";
    	case MediaCodecInfo.CodecCapabilities.COLOR_FormatMonochrome:
    		return "COLOR_FormatMonochrome";
    	case MediaCodecInfo.CodecCapabilities.COLOR_FormatRawBayer10bit:
    		return "COLOR_FormatRawBayer10bit";
    	case MediaCodecInfo.CodecCapabilities.COLOR_FormatRawBayer8bit:
    		return "COLOR_FormatRawBayer8bit";
    	case MediaCodecInfo.CodecCapabilities.COLOR_FormatRawBayer8bitcompressed:
    		return "COLOR_FormatRawBayer8bitcompressed";
    	case MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface:	// = OMX_COLOR_FormatAndroidOpaque(0x7F000789)
    		return "COLOR_FormatSurface_COLOR_FormatAndroidOpaque";
    	case MediaCodecInfo.CodecCapabilities.COLOR_FormatYCbYCr:
    		return "COLOR_FormatYCbYCr";
    	case MediaCodecInfo.CodecCapabilities.COLOR_FormatYCrYCb:
    		return "COLOR_FormatYCrYCb";
    	case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV411PackedPlanar:
    		return "COLOR_FormatYUV411PackedPlanar";
    	case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV411Planar:
    		return "COLOR_FormatYUV411Planar";
    	case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
    		return "COLOR_FormatYUV420PackedPlanar";
    	case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
    		return "COLOR_FormatYUV420PackedSemiPlanar";
    	case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
    		return "COLOR_FormatYUV420Planar";
    	case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
    		return "COLOR_FormatYUV420SemiPlanar";
    	case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV422PackedPlanar:
    		return "COLOR_FormatYUV422PackedPlanar";
    	case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV422PackedSemiPlanar:
    		return "COLOR_FormatYUV422PackedSemiPlanar";
    	case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV422Planar:
    		return "COLOR_FormatYUV422Planar";
    	case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV422SemiPlanar:
    		return "COLOR_FormatYUV422SemiPlanar";
    	case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV444Interleaved:
    		return "COLOR_FormatYUV444Interleaved";
    	case MediaCodecInfo.CodecCapabilities.COLOR_QCOM_FormatYUV420SemiPlanar:	// = OMX_QCOM_COLOR_FormatYVU420SemiPlanar(0x7FA30C00)
    		return "COLOR_QCOM_FormatYUV420SemiPlanar";
    	case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar: // = OMX_TI_COLOR_FormatYUV420PackedSemiPlanar(0x7F000100)
    		return "COLOR_TI_FormatYUV420PackedSemiPlanar";
    	case 0x6F000000:
    		return "OMX_COLOR_FormatKhronosExtensions";
    	case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible: // = 0x7F420888;
    		return "COLOR_FormatYUV420Flexible";
    	case 0x7FA30C03:
    		return "OMX_QCOM_COLOR_FormatYUV420PackedSemiPlanar64x32Tile2m8ka";
    	case 0x7FC00002:
    		return "OMX_SEC_COLOR_FormatNV12Tiled";
    	case 0x7FA30C04:
    		return "OMX_QCOM_COLOR_FormatYUV420PackedSemiPlanar32m";
    	default:
    		return String.format(Locale.getDefault(), "COLOR_Format_Unknown(%d)", colorFormat);
    	}
    }

    public static String getProfileLevelString(final String mimeType, final MediaCodecInfo.CodecProfileLevel profileLevel) {
    	String result;
    	if (!TextUtils.isEmpty(mimeType)) {
	    	if (mimeType.equalsIgnoreCase("video/avc")) {
		    	switch (profileLevel.profile) {
		        // from OMX_VIDEO_AVCPROFILETYPE
		    	case MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline:	// 0x01;
		    		result = "AVCProfileBaseline"; break;
		    	case MediaCodecInfo.CodecProfileLevel.AVCProfileMain:		// 0x02;
		    		result = "AVCProfileMain"; break;
		    	case MediaCodecInfo.CodecProfileLevel.AVCProfileExtended:	// 0x04;
		    		result = "AVCProfileExtended"; break;
		    	case MediaCodecInfo.CodecProfileLevel.AVCProfileHigh:		// 0x08;
		    		result = "AVCProfileHigh"; break;
		    	case MediaCodecInfo.CodecProfileLevel.AVCProfileHigh10:		// 0x10;
		    		result = "AVCProfileHigh10"; break;
		    	case MediaCodecInfo.CodecProfileLevel.AVCProfileHigh422:	// 0x20;
		    		result = "AVCProfileHigh422"; break;
		    	case MediaCodecInfo.CodecProfileLevel.AVCProfileHigh444:	// 0x40;
		    		result = "AVCProfileHigh444"; break;
		    	default:
		    		result = "unknown profile " + profileLevel.profile; break;
		    	}
	    		switch (profileLevel.level) {
	            // from OMX_VIDEO_AVCLEVELTYPE
		    	case MediaCodecInfo.CodecProfileLevel.AVCLevel1:		// 0x01;
		    		result = result + ".AVCLevel1"; break;
		    	case MediaCodecInfo.CodecProfileLevel.AVCLevel1b:		// 0x02;
		    		result = result + ".AVCLevel1b"; break;
		    	case MediaCodecInfo.CodecProfileLevel.AVCLevel11:		// 0x04;
		    		result = result + ".AVCLevel11"; break;
		    	case MediaCodecInfo.CodecProfileLevel.AVCLevel12:		// 0x08;
		    		result = result + ".AVCLevel12"; break;
		    	case MediaCodecInfo.CodecProfileLevel.AVCLevel13:		// 0x10;
		    		result = result + ".AVCLevel13"; break;
		    	case MediaCodecInfo.CodecProfileLevel.AVCLevel2:		// 0x20;
		    		result = result + ".AVCLevel2"; break;
		    	case MediaCodecInfo.CodecProfileLevel.AVCLevel21:		// 0x40;
		    		result = result + ".AVCLevel21"; break;
		    	case MediaCodecInfo.CodecProfileLevel.AVCLevel22:		// 0x80;
		    		result = result + ".AVCLevel22"; break;
		    	case MediaCodecInfo.CodecProfileLevel.AVCLevel3:		// 0x100;
		    		result = result + ".AVCLevel3"; break;
		    	case MediaCodecInfo.CodecProfileLevel.AVCLevel31:		// 0x200;
		    		result = result + ".AVCLevel31"; break;
		    	case MediaCodecInfo.CodecProfileLevel.AVCLevel32:		// 0x400;
		    		result = result + ".AVCLevel32"; break;
		    	case MediaCodecInfo.CodecProfileLevel.AVCLevel4:		// 0x800;
		    		result = result + ".AVCLevel4"; break;
		    	case MediaCodecInfo.CodecProfileLevel.AVCLevel41:		// 0x1000;
		    		result = result + ".AVCLevel41"; break;
		    	case MediaCodecInfo.CodecProfileLevel.AVCLevel42:		// 0x2000;
		    		result = result + ".AVCLevel42"; break;
		    	case MediaCodecInfo.CodecProfileLevel.AVCLevel5:		// 0x4000;
		    		result = result + ".AVCLevel5"; break;
		    	case MediaCodecInfo.CodecProfileLevel.AVCLevel51:		// 0x8000;
		    		result = result + ".AVCLevel51"; break;
		    	default:
		    		result = result + ".unknown level " + profileLevel.level; break;
	    		}
	    	} else if (mimeType.equalsIgnoreCase("video/h263")) {
		    	switch (profileLevel.profile) {
		    	// from OMX_VIDEO_H263PROFILETYPE
		    	case MediaCodecInfo.CodecProfileLevel.H263ProfileBaseline:				// 0x01;
		    		result = "H263ProfileBaseline"; break;
		    	case MediaCodecInfo.CodecProfileLevel.H263ProfileH320Coding:			// 0x02;
		    		result = "H263ProfileH320Coding"; break;
		    	case MediaCodecInfo.CodecProfileLevel.H263ProfileBackwardCompatible:	// 0x04;
		    		result = "H263ProfileBackwardCompatible"; break;
		    	case MediaCodecInfo.CodecProfileLevel.H263ProfileISWV2:					// 0x08;
		    		result = "H263ProfileISWV2"; break;
		    	case MediaCodecInfo.CodecProfileLevel.H263ProfileISWV3:					// 0x10;
		    		result = "H263ProfileISWV3"; break;
		    	case MediaCodecInfo.CodecProfileLevel.H263ProfileHighCompression:		// 0x20;
		    		result = "H263ProfileHighCompression"; break;
		    	case MediaCodecInfo.CodecProfileLevel.H263ProfileInternet:				// 0x40;
		    		result = "H263ProfileInternet"; break;
		    	case MediaCodecInfo.CodecProfileLevel.H263ProfileInterlace:				// 0x80;
		    		result = "H263ProfileInterlace"; break;
		    	case MediaCodecInfo.CodecProfileLevel.H263ProfileHighLatency:			// 0x100;
		    		result = "H263ProfileHighLatency"; break;
		    	default:
		    		result = "unknown profile " + profileLevel.profile; break;
		    	}
	    		switch (profileLevel.level) {
	            // from OMX_VIDEO_H263LEVELTYPE
		    	case MediaCodecInfo.CodecProfileLevel.H263Level10:					// 0x01;
		    		result = result + ".H263Level10"; break;
		    	case MediaCodecInfo.CodecProfileLevel.H263Level20:					// 0x02;
		    		result = result + ".H263Level20"; break;
		    	case MediaCodecInfo.CodecProfileLevel.H263Level30:					// 0x04;
		    		result = result + ".H263Level30"; break;
		    	case MediaCodecInfo.CodecProfileLevel.H263Level40:					// 0x08;
		    		result = result + ".H263Level40"; break;
		    	case MediaCodecInfo.CodecProfileLevel.H263Level45:					// 0x10;
		    		result = result + ".H263Level45"; break;
		    	case MediaCodecInfo.CodecProfileLevel.H263Level50:					// 0x20;
		    		result = result + ".H263Level50"; break;
		    	case MediaCodecInfo.CodecProfileLevel.H263Level60:					// 0x40;
		    		result = result + ".H263Level60"; break;
		    	case MediaCodecInfo.CodecProfileLevel.H263Level70:					// 0x80;
		    		result = result + ".H263Level70"; break;
		    	default:
		    		result = result + ".unknown level " + profileLevel.level; break;
	    		}
	    	} else if (mimeType.equalsIgnoreCase("video/mpeg4")) {
		    	switch (profileLevel.profile) {
	            // from OMX_VIDEO_MPEG4PROFILETYPE
		    	case MediaCodecInfo.CodecProfileLevel.MPEG4ProfileSimple:			// 0x01;
		    		result = "MPEG4ProfileSimple"; break;
		    	case MediaCodecInfo.CodecProfileLevel.MPEG4ProfileSimpleScalable:	// 0x02;
		    		result = "MPEG4ProfileSimpleScalable"; break;
		    	case MediaCodecInfo.CodecProfileLevel.MPEG4ProfileCore:				// 0x04;
		    		result = "MPEG4ProfileCore"; break;
		    	case MediaCodecInfo.CodecProfileLevel.MPEG4ProfileMain:				// 0x08;
		    		result = "MPEG4ProfileMain"; break;
		    	case MediaCodecInfo.CodecProfileLevel.MPEG4ProfileNbit:				// 0x10;
		    		result = "MPEG4ProfileNbit"; break;
		    	case MediaCodecInfo.CodecProfileLevel.MPEG4ProfileScalableTexture:	// 0x20;
		    		result = "MPEG4ProfileScalableTexture"; break;
		    	case MediaCodecInfo.CodecProfileLevel.MPEG4ProfileSimpleFace:		// 0x40;
		    		result = "MPEG4ProfileSimpleFace"; break;
		    	case MediaCodecInfo.CodecProfileLevel.MPEG4ProfileSimpleFBA:		// 0x80;
		    		result = "MPEG4ProfileSimpleFBA"; break;
		    	case MediaCodecInfo.CodecProfileLevel.MPEG4ProfileBasicAnimated:	// 0x100;
		    		result = "MPEG4ProfileBasicAnimated"; break;
		    	case MediaCodecInfo.CodecProfileLevel.MPEG4ProfileHybrid:			// 0x200;
		    		result = "MPEG4ProfileHybrid"; break;
		    	case MediaCodecInfo.CodecProfileLevel.MPEG4ProfileAdvancedRealTime:	// 0x400;
		    		result = "MPEG4ProfileAdvancedRealTime"; break;
		    	case MediaCodecInfo.CodecProfileLevel.MPEG4ProfileCoreScalable:		// 0x800;
		    		result = "MPEG4ProfileCoreScalable"; break;
		    	case MediaCodecInfo.CodecProfileLevel.MPEG4ProfileAdvancedCoding:	// 0x1000;
		    		result = "MPEG4ProfileAdvancedCoding"; break;
		    	case MediaCodecInfo.CodecProfileLevel.MPEG4ProfileAdvancedCore:		// 0x2000;
		    		result = "MPEG4ProfileAdvancedCore"; break;
		    	case MediaCodecInfo.CodecProfileLevel.MPEG4ProfileAdvancedScalable:	// 0x4000;
		    		result = "MPEG4ProfileAdvancedScalable"; break;
		    	case MediaCodecInfo.CodecProfileLevel.MPEG4ProfileAdvancedSimple:	// 0x8000;
		    		result = "MPEG4ProfileAdvancedSimple"; break;
		    	default:
		    		result = "unknown profile " + profileLevel.profile; break;
		    	}
	    		switch (profileLevel.level) {
	            // from OMX_VIDEO_MPEG4LEVELTYPE
	        	case MediaCodecInfo.CodecProfileLevel.MPEG4Level0:			// 0x01;
	        		result = result + ".MPEG4Level0"; break;
	        	case MediaCodecInfo.CodecProfileLevel.MPEG4Level0b:			// 0x02;
	        		result = result + ".MPEG4Level0b"; break;
	        	case MediaCodecInfo.CodecProfileLevel.MPEG4Level1:			// 0x04;
	        		result = result + ".MPEG4Level1"; break;
	        	case MediaCodecInfo.CodecProfileLevel.MPEG4Level2:			// 0x08;
	        		result = result + ".MPEG4Level2"; break;
	        	case MediaCodecInfo.CodecProfileLevel.MPEG4Level3:			// 0x10;
	        		result = result + ".MPEG4Level3"; break;
	        	case MediaCodecInfo.CodecProfileLevel.MPEG4Level4:			// 0x20;
	        		result = result + ".MPEG4Level4"; break;
	        	case MediaCodecInfo.CodecProfileLevel.MPEG4Level4a:			// 0x40;
	        		result = result + ".MPEG4Level4a"; break;
	        	case MediaCodecInfo.CodecProfileLevel.MPEG4Level5:			// 0x80;
	        		result = result + ".MPEG4Level5"; break;
		    	default:
		    		result = result + ".unknown level " + profileLevel.level; break;
	    		}
	    	} else if (mimeType.equalsIgnoreCase("ausio/aac")) {
	            // from OMX_AUDIO_AACPROFILETYPE
		    	switch (profileLevel.level) {
		    	case MediaCodecInfo.CodecProfileLevel.AACObjectMain:		// 1;
		    		result = "AACObjectMain"; break;
		    	case MediaCodecInfo.CodecProfileLevel.AACObjectLC:			// 2;
		    		result = "AACObjectLC"; break;
		    	case MediaCodecInfo.CodecProfileLevel.AACObjectSSR:			// 3;
		    		result = "AACObjectSSR"; break;
		    	case MediaCodecInfo.CodecProfileLevel.AACObjectLTP:			// 4;
		    		result = "AACObjectLTP"; break;
		    	case MediaCodecInfo.CodecProfileLevel.AACObjectHE:			// 5;
		    		result = "AACObjectHE"; break;
		    	case MediaCodecInfo.CodecProfileLevel.AACObjectScalable:	// 6;
		    		result = "AACObjectScalable"; break;
		    	case MediaCodecInfo.CodecProfileLevel.AACObjectERLC:		// 17;
		    		result = "AACObjectERLC"; break;
		    	case MediaCodecInfo.CodecProfileLevel.AACObjectLD:			// 23;
		    		result = "AACObjectLD"; break;
		    	case MediaCodecInfo.CodecProfileLevel.AACObjectHE_PS:		// 29;
		    		result = "AACObjectHE_PS"; break;
		    	case MediaCodecInfo.CodecProfileLevel.AACObjectELD:			// 39;
		    		result = "AACObjectELD"; break;
		    	default:
		    		result = "profile:unknown " + profileLevel.profile; break;
		    	}
	    	} else if (mimeType.equalsIgnoreCase("video/vp8")) {
		    	switch (profileLevel.profile) {
	            // from OMX_VIDEO_VP8PROFILETYPE
		    	case MediaCodecInfo.CodecProfileLevel.VP8ProfileMain:		// 0x01;
		    		result = "VP8ProfileMain"; break;
		    	default:
		    		result = "unknown profile " + profileLevel.profile; break;
		    	}
				switch (profileLevel.level) {
	            // from OMX_VIDEO_VP8LEVELTYPE
		    	case MediaCodecInfo.CodecProfileLevel.VP8Level_Version0:	// 0x01;
		    		result = result + ".VP8Level_Version0"; break;
		    	case MediaCodecInfo.CodecProfileLevel.VP8Level_Version1:	// 0x02;
		    		result = result + ".VP8Level_Version1"; break;
		    	case MediaCodecInfo.CodecProfileLevel.VP8Level_Version2:	// 0x04;
		    		result = result + ".VP8Level_Version2"; break;
		    	case MediaCodecInfo.CodecProfileLevel.VP8Level_Version3:	// 0x08;
		    		result = result + ".VP8Level_Version3"; break;
		    	default:
		    		result = result + ".unknown level" + profileLevel.level; break;
		    	}
	    	} else {
	    		result = "unknown profile " + profileLevel.profile;
	    	}
    	} else {
    		result = "mime type is null";
    	}

    	return result;
    }

    // 静的にキャッシュするようにした
    private static final List<MediaCodecInfo> sCodecList = new ArrayList<MediaCodecInfo>();

    @SuppressWarnings("deprecation")
	private static final void updateCodecs() {
    	if (sCodecList.size() == 0) {
	    	// コーデックの一覧を取得
	        final int n = MediaCodecList.getCodecCount();
	        for (int i = 0; i < n; i++) {
	        	sCodecList.add(MediaCodecList.getCodecInfoAt(i));
	        }
    	}
    }

    public static final int getCodecCount() {
    	updateCodecs();
    	return sCodecList.size();
    }

    public static final List<MediaCodecInfo> getCodecs() {
    	updateCodecs();
    	return sCodecList;
    }

    public static final MediaCodecInfo getCodecInfoAt(final int ix) {
    	updateCodecs();
    	return sCodecList.get(ix);
    }

    // getCapabilitiesForTypeがすごく遅い機種があるので静的にキャッシュする
    private static final HashMap<String, HashMap<MediaCodecInfo, MediaCodecInfo.CodecCapabilities>>
		sCapabilities = new HashMap<String, HashMap<MediaCodecInfo, MediaCodecInfo.CodecCapabilities>>();

    public static MediaCodecInfo.CodecCapabilities getCodecCapabilities(final MediaCodecInfo codecInfo, final String mimeType) {
		HashMap<MediaCodecInfo, MediaCodecInfo.CodecCapabilities> caps = sCapabilities.get(mimeType);
		if (caps == null) {
			caps = new HashMap<MediaCodecInfo, MediaCodecInfo.CodecCapabilities>();
			sCapabilities.put(mimeType, caps);
		}
		MediaCodecInfo.CodecCapabilities capabilities = caps.get(codecInfo);
		if (capabilities == null) {
	    	// XXX 通常の優先度ではSC-06DでMediaCodecInfo#getCapabilitiesForTypeが返ってこないので一時的に昇格
			Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
			try {
				capabilities = codecInfo.getCapabilitiesForType(mimeType);
				caps.put(codecInfo, capabilities);
			} finally {
				// 元の優先度に戻す
				Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
			}
		}
		return capabilities;
	}

}
