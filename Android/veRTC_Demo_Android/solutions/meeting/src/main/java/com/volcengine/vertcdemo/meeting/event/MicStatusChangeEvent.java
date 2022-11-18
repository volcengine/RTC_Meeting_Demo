package com.volcengine.vertcdemo.meeting.event;

import com.google.gson.annotations.SerializedName;
import com.volcengine.vertcdemo.core.net.rts.RTSBizInform;

/**
 * 用户麦克风开关变化事件
 */
public class MicStatusChangeEvent implements RTSBizInform {

    @SerializedName("user_id")
    public String userId;
    @SerializedName("status")
    public boolean status;

    public MicStatusChangeEvent(String userId, boolean status) {
        this.userId = userId;
        this.status = status;
    }
}
