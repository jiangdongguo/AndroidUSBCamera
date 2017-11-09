package com.serenegiant.usb.encoder;

/** 录制参数
 *
 * Created by jiangdongguo on 2017/10/19.
 */

public class RecordParams {
    private String recordPath;
    private int recordDuration;
    private boolean voiceClose;
    private boolean isAutoSave;

    public boolean isVoiceClose() {
        return voiceClose;
    }

    public void setVoiceClose(boolean voiceClose) {
        this.voiceClose = voiceClose;
    }

    public String getRecordPath() {
        return recordPath;
    }

    public void setRecordPath(String recordPath) {
        this.recordPath = recordPath;
    }

    public int getRecordDuration() {
        return recordDuration;
    }

    public void setRecordDuration(int recordDuration) {
        this.recordDuration = recordDuration;
    }

    public boolean isAutoSave() {
        return isAutoSave;
    }

    public void setAutoSave(boolean autoSave) {
        isAutoSave = autoSave;
    }
}
