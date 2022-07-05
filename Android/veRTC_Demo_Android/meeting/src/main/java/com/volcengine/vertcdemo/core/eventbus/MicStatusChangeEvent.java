package com.volcengine.vertcdemo.core.eventbus;

import com.google.gson.annotations.SerializedName;
import com.volcengine.vertcdemo.core.net.rtm.RTMBizInform;

public class MicStatusChangeEvent implements RTMBizInform {

    @SerializedName("user_id")
    public String userId;
    @SerializedName("status")
    public boolean status;

    public MicStatusChangeEvent(String userId, boolean status) {
        this.userId = userId;
        this.status = status;
    }
}
