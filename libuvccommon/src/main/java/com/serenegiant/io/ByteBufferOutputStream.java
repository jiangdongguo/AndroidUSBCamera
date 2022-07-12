package com.serenegiant.io;

/*
 * Originally came from
 * https://gist.github.com/hoijui/7fe8a6d31b20ae7af945
 * hoijui/ByteBufferOutputStream.java
 */

/*
This is free and unencumbered software released into the public domain.
Anyone is free to copy, modify, publish, use, compile, sell, or
distribute this software, either in source code form or as a compiled
binary, for any purpose, commercial or non-commercial, and by any
means.
In jurisdictions that recognize copyright laws, the author or authors
of this software dedicate any and all copyright interest in the
software to the public domain. We make this dedication for the benefit
of the public at large and to the detriment of our heirs and
successors. We intend this dedication to be an overt act of
relinquishment in perpetuity of all present and future rights to this
software under copyright law.
THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.
For more information, please refer to <http://unlicense.org/>
 */

import androidx.annotation.NonNull;

import java.io.OutputStream;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

/**
 * Wraps a {@link ByteBuffer} so it can be used like an {@link OutputStream}. This is similar to a
 * {@link java.io.ByteArrayOutputStream}, just that this uses a {@code ByteBuffer} instead of a
 * {@code byte[]} as internal storage.
 */
public class ByteBufferOutputStream extends OutputStream {
	
	private ByteBuffer wrappedBuffer;
	private final boolean autoEnlarge;
	
	public ByteBufferOutputStream(final ByteBuffer wrappedBuffer, final boolean autoEnlarge) {
		
		this.wrappedBuffer = wrappedBuffer;
		this.autoEnlarge = autoEnlarge;
	}
	
	/**
	 * create and return a new byte buffer that shares this buffer's content as read only ByteBuffer
	 * @return
	 */
	public ByteBuffer toByteBuffer() {
		
		final ByteBuffer byteBuffer = wrappedBuffer.duplicate();
		byteBuffer.flip();
		return byteBuffer.asReadOnlyBuffer();
	}
	
	/**
	 * Resets the <code>count</code> field of this byte array output stream to zero, so that all
	 * currently accumulated output in the output stream is discarded. The output stream can be used
	 * again, reusing the already allocated buffer space.
	 *
	 * @see java.io.ByteArrayInputStream#count
	 */
	public void reset() {
		wrappedBuffer.rewind();
	}
	
	/**
	 * return current size of the buffer,
	 * this value is a position of backed ByteBuffer (not a limit, capacity)
	 * added saki
	 * @return
	 */
	public int size() {
		return wrappedBuffer.position();
	}

	/**
	 * Increases the capacity to ensure that it can hold at least the number of elements specified
	 * by the minimum capacity argument.
	 *
	 * @param minCapacity the desired minimum capacity
	 */
	private void growTo(final int minCapacity) {
		
		// overflow-conscious code
		final int oldCapacity = wrappedBuffer.capacity();
		int newCapacity = oldCapacity << 1;
		if (newCapacity - minCapacity < 0) {
			newCapacity = minCapacity;
		}
		if (newCapacity < 0) {
			if (minCapacity < 0) { // overflow
				throw new OutOfMemoryError();
			}
			newCapacity = Integer.MAX_VALUE;
		}
		final ByteBuffer oldWrappedBuffer = wrappedBuffer;
		// create the new buffer
		if (wrappedBuffer.isDirect()) {
			wrappedBuffer = ByteBuffer.allocateDirect(newCapacity);
		} else {
			wrappedBuffer = ByteBuffer.allocate(newCapacity);
		}
		// copy over the old content into the new buffer
		oldWrappedBuffer.flip();
		wrappedBuffer.put(oldWrappedBuffer);
	}
	
	@Override
	public void write(final int bty) {
		
		try {
			wrappedBuffer.put((byte) bty);
		} catch (final BufferOverflowException ex) {
			if (autoEnlarge) {
				final int newBufferSize = wrappedBuffer.capacity() * 2;
				growTo(newBufferSize);
				write(bty);
			} else {
				throw ex;
			}
		}
	}
	
	@Override
	public void write(@NonNull final byte[] bytes) {
		
		int oldPosition = 0;
		try {
			oldPosition = wrappedBuffer.position();
			wrappedBuffer.put(bytes);
		} catch (final BufferOverflowException ex) {
			if (autoEnlarge) {
				final int newBufferSize
					= Math.max(wrappedBuffer.capacity() * 2, oldPosition + bytes.length);
				growTo(newBufferSize);
				write(bytes);
			} else {
				throw ex;
			}
		}
	}
	
	@Override
	public void write(@NonNull final byte[] bytes, final int off, final int len) {
		
		int oldPosition = 0;
		try {
			oldPosition = wrappedBuffer.position();
			wrappedBuffer.put(bytes, off, len);
		} catch (final BufferOverflowException ex) {
			if (autoEnlarge) {
				final int newBufferSize
					= Math.max(wrappedBuffer.capacity() * 2, oldPosition + len);
				growTo(newBufferSize);
				write(bytes, off, len);
			} else {
				throw ex;
			}
		}
	}
}