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

import android.text.TextUtils;

public class ObjectHelper {

	public static boolean asBoolean(final Object val, final boolean defaultValue) {
		if (val instanceof Boolean) {
			return (Boolean)val;
		} else if (val instanceof Byte) {
			return ((Byte)val) != 0;
		} else if (val instanceof Short) {
			return ((Short)val) != 0;
		} else if (val instanceof Integer) {
			return ((Integer)val) != 0;
		} else if (val instanceof Long) {
			return ((Long)val) != 0;
		} else if (val instanceof Float) {
			return ((Float)val) != 0;
		} else if (val instanceof Double) {
			return ((Double)val) != 0;
		} else if (val instanceof Number) {
			return ((Number)val).doubleValue() != 0;
		} else if (val instanceof String) {
			if (!TextUtils.isEmpty((String)val)) {
				final String v = (String)val;
				// 16進文字列かも
				if (v.startsWith("0x") || v.startsWith("0X")) {
					try {
						return Integer.parseInt(v.substring(2), 16) != 0;
					} catch (final Exception e1) {
						return Long.parseLong(v.substring(2), 16) != 0;
					}
				}
				try {
					// 数字の文字列
					return Double.parseDouble(v) != 0;
				} catch (final Exception e) {
					//
				}
				// 16進文字列かも
				try {
					return Integer.parseInt(v, 16) != 0;
				} catch (final Exception e) {
					//
				}
				// 16進文字列かも
				try {
					return Long.parseLong(v, 16) != 0;
				} catch (final Exception e) {
					//
				}
				return  Boolean.parseBoolean((String)val);
			}
		}
		return defaultValue;
	}

	public static byte asByte(final Object val, final byte defaultValue) {
		if (val instanceof Boolean) {
			return (Boolean)val ? (byte)1 : (byte)0;
		} else if (val instanceof Byte) {
			return ((Byte)val);
		} else if (val instanceof Short) {
			return ((Short)val).byteValue();
		} else if (val instanceof Integer) {
			return ((Integer)val).byteValue();
		} else if (val instanceof Long) {
			return ((Long)val).byteValue();
		} else if (val instanceof Float) {
			return ((Float)val).byteValue();
		} else if (val instanceof Double) {
			return ((Double)val).byteValue();
		} else if (val instanceof Number) {
			return ((Number)val).byteValue();
		} else if (val instanceof String) {
			if (!TextUtils.isEmpty((String)val)) {
				final String v = (String)val;
				// 16進文字列かも
				if (v.startsWith("0x") || v.startsWith("0X")) {
					try {
						return (byte)Integer.parseInt(v.substring(2), 16);
					} catch (final Exception e1) {
						return (byte)Long.parseLong(v.substring(2), 16);
					}
				}
				try {
					// 数字の文字列
					return ((Double)Double.parseDouble(v)).byteValue();
				} catch (final Exception e) {
					//
				}
				// 16進文字列かも
				try {
					return ((Integer)Integer.parseInt(v, 16)).byteValue();
				} catch (final Exception e) {
					//
				}
				// 16進文字列かも
				try {
					return ((Long)Long.parseLong(v, 16)).byteValue();
				} catch (final Exception e) {
					//
				}
				return  Boolean.parseBoolean((String)val) ? (byte)1 : (byte)0;
			}
		}
		return defaultValue;
	}

	public static short asShort(final Object val, final short defaultValue) {
		if (val instanceof Boolean) {
			return (Boolean)val ? (short)1 : (short)0;
		} else if (val instanceof Byte) {
			return ((Byte)val);
		} else if (val instanceof Short) {
			return ((Short)val);
		} else if (val instanceof Integer) {
			return ((Integer)val).shortValue();
		} else if (val instanceof Long) {
			return ((Long)val).shortValue();
		} else if (val instanceof Float) {
			return ((Float)val).shortValue();
		} else if (val instanceof Double) {
			return ((Double)val).shortValue();
		} else if (val instanceof Number) {
			return ((Number)val).shortValue();
		} else if (val instanceof String) {
			if (!TextUtils.isEmpty((String)val)) {
				final String v = (String)val;
				// 16進文字列かも
				if (v.startsWith("0x") || v.startsWith("0X")) {
					try {
						return (short)Integer.parseInt(v.substring(2), 16);
					} catch (final Exception e1) {
						return (short)Long.parseLong(v.substring(2), 16);
					}
				}
				try {
					// 数字の文字列
					return ((Double)Double.parseDouble(v)).shortValue();
				} catch (final Exception e) {
					//
				}
				// 16進文字列かも
				try {
					return ((Integer)Integer.parseInt(v, 16)).shortValue();
				} catch (final Exception e) {
					//
				}
				// 16進文字列かも
				try {
					return ((Long)Long.parseLong(v, 16)).shortValue();
				} catch (final Exception e) {
					//
				}
				return  Boolean.parseBoolean((String)val) ? (short)1 : (short)0;
			}
		}
		return defaultValue;
	}

	public static int asInt(final Object val, final int defaultValue) {
		if (val instanceof Boolean) {
			return ((Boolean)val) ? 1 : 0;
		} else if (val instanceof Byte) {
			return ((Byte)val);
		} else if (val instanceof Short) {
			return ((Short)val);
		} else if (val instanceof Integer) {
			return (Integer)val;
		} else if (val instanceof Long) {
			return ((Long)val).intValue();
		} else if (val instanceof Float) {
			return ((Float)val).intValue();
		} else if (val instanceof Double) {
			return ((Double)val).intValue();
		} else if (val instanceof Number) {
			return ((Number)val).intValue();
		} else if (val instanceof String) {
			if (!TextUtils.isEmpty((String)val)) {
				final String v = (String)val;
				// 16進文字列かも
				if (v.startsWith("0x") || v.startsWith("0X")) {
					try {
						return Integer.parseInt(v.substring(2), 16);
					} catch (final Exception e1) {
						return ((Long)Long.parseLong(v.substring(2), 16)).intValue();
					}
				}
				try {
					// 数字の文字列
					return ((Double)Double.parseDouble(v)).intValue();
				} catch (final Exception e) {
					//
				}
				// 16進文字列かも
				try {
					return Integer.parseInt(v, 16);
				} catch (final Exception e2) {
					//
				}
				// 16進文字列かも
				try {
					return ((Long)Long.parseLong(v, 16)).intValue();
				} catch (final Exception e) {
					//
				}
				return Boolean.parseBoolean((String)val) ? 1 : 0;
			}
		}
		return defaultValue;
	}
	
	public static long asLong(final Object val, final long defaultValue) {
		if (val instanceof Boolean) {
			return ((Boolean)val) ? 1 : 0;
		} else if (val instanceof Byte) {
			return ((Byte)val);
		} else if (val instanceof Short) {
			return ((Short)val);
		} else if (val instanceof Integer) {
			return (Integer)val;
		} else if (val instanceof Long) {
			return (Long)val;
		} else if (val instanceof Float) {
			return ((Float)val).longValue();
		} else if (val instanceof Double) {
			return ((Double)val).longValue();
		} else if (val instanceof Number) {
			return ((Number)val).longValue();
		} else if (val instanceof String) {
			if (!TextUtils.isEmpty((String)val)) {
				final String v = (String)val;
				// 16進文字列かも
				if (v.startsWith("0x") || v.startsWith("0X")) {
					try {
						return Long.parseLong(v.substring(2), 16);
					} catch (final Exception e1) {
						//
					}
				}
				try {
					// 数字の文字列
					return ((Double)Double.parseDouble(v)).longValue();
				} catch (final Exception e) {
					//
				}
				// 16進文字列かも
				try {
					return Long.parseLong(v, 16);
				} catch (final Exception e2) {
					//
				}
				return Boolean.parseBoolean((String)val) ? 1 : 0;
			}
		}
		return defaultValue;
	}

	public static float asFloat(final Object val, final float defaultValue) {
		if (val instanceof Boolean) {
			return ((Boolean)val) ? 1 : 0;
		} else if (val instanceof Byte) {
			return ((Byte)val);
		} else if (val instanceof Short) {
			return ((Short)val);
		} else if (val instanceof Integer) {
			return (Integer)val;
		} else if (val instanceof Long) {
			return ((Long)val).floatValue();
		} else if (val instanceof Float) {
			return (Float)val;
		} else if (val instanceof Double) {
			return ((Double)val).floatValue();
		} else if (val instanceof Number) {
			return ((Number)val).floatValue();
		} else if (val instanceof String) {
			if (!TextUtils.isEmpty((String)val)) {
				final String v = (String)val;
				// 16進文字列かも
				if (v.startsWith("0x") || v.startsWith("0X")) {
					try {
						return ((Long)Long.parseLong(v.substring(2), 16)).floatValue();
					} catch (final Exception e1) {
						//
					}
				}
				try {
					// 数字の文字列
					return ((Double)Double.parseDouble(v)).floatValue();
				} catch (final Exception e) {
					//
				}
				// 16進文字列かも
				try {
					return ((Long)Long.parseLong(v, 16)).floatValue();
				} catch (final Exception e2) {
					//
				}
				return Boolean.parseBoolean((String)val) ? 1 : 0;
			}
		}
		return defaultValue;
	}

	public static double asDouble(final Object val, final double defaultValue) {
		if (val instanceof Boolean) {
			return ((Boolean)val) ? 1 : 0;
		} else if (val instanceof Byte) {
			return ((Byte)val);
		} else if (val instanceof Short) {
			return ((Short)val);
		} else if (val instanceof Integer) {
			return (Integer)val;
		} else if (val instanceof Long) {
			return ((Long)val).doubleValue();
		} else if (val instanceof Float) {
			return (Float)val;
		} else if (val instanceof Double) {
			return (Double)val;
		} else if (val instanceof Number) {
			return ((Number)val).doubleValue();
		} else if (val instanceof String) {
			if (!TextUtils.isEmpty((String)val)) {
				final String v = (String)val;
				// 16進文字列かも
				if (v.startsWith("0x") || v.startsWith("0X")) {
					try {
						return ((Long)Long.parseLong(v.substring(2), 16)).doubleValue();
					} catch (final Exception e1) {
						//
					}
				}
				try {
					// 数字の文字列
					return Double.parseDouble(v);
				} catch (final Exception e) {
					//
				}
				// 16進文字列かも
				try {
					return ((Long)Long.parseLong(v, 16)).doubleValue();
				} catch (final Exception e2) {
					//
				}
				return Boolean.parseBoolean((String)val) ? 1 : 0;
			}
		}
		return defaultValue;
	}
}
