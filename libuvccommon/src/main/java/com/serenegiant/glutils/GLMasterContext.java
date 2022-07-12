package com.serenegiant.glutils;

/**
 * Created by saki on 2018/02/10.
 * 共有コンテキストのマスターをを保持するためだけのクラス
 * Applicationクラス等でシングルトンとして使う
 */
public class GLMasterContext {
	private static final String TAG = GLMasterContext.class.getSimpleName();

	private MasterTask mMasterTask;
	
	public GLMasterContext(final int maxClientVersion, final int flags) {
		mMasterTask = new MasterTask(maxClientVersion, flags);
		new Thread(mMasterTask, TAG).start();
		mMasterTask.waitReady();
	}
	
	@Override
	protected void finalize() throws Throwable {
		try {
			release();
		} finally {
			super.finalize();
		}
	}

	public synchronized void release() {
		if (mMasterTask != null) {
			mMasterTask.release();
			mMasterTask = null;
		}
	}
	
	public synchronized EGLBase.IContext getContext()
		throws IllegalStateException {
		if (mMasterTask != null) {
			return mMasterTask.getContext();
		} else {
			throw new IllegalStateException("already released");
		}
	}
	
	private static class MasterTask extends EglTask {
		public MasterTask(final int maxClientVersion, final int flags) {
			super(maxClientVersion, null, flags);
		}
		
		@Override
		protected void onStart() {
			// do nothing
		}
		
		@Override
		protected void onStop() {
			// do nothing
		}
		
		@Override
		protected Object processRequest(final int request,
			final int arg1, final int arg2, final Object obj) throws TaskBreak {
			// do nothing
			return null;
		}
	}
}
