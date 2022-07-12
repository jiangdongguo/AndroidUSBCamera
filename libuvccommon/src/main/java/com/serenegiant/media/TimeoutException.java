package com.serenegiant.media;
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

public class TimeoutException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7207769104864850593L;

	public TimeoutException() {
	}

	public TimeoutException(String detailMessage) {
		super(detailMessage);
	}

	public TimeoutException(Throwable throwable) {
		super(throwable);
	}

	public TimeoutException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

}
