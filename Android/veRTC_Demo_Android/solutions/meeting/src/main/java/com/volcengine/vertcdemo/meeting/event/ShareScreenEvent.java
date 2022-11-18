package com.volcengine.vertcdemo.meeting.event;

import com.google.gson.annotations.SerializedName;
import com.volcengine.vertcdemo.core.net.rts.RTSBizInform;

/**
 * 屏幕分享事件
 */
public class ShareScreenEvent implements RTSBizInform {

    @SerializedName("status")
    public boolean status;
    @SerializedName("user_id")
    public String userId;
    @SerializedName("user_name")
    public String userName;

    @Override
    public String toString() {
        return "ShareScreenEvent{" +
                "status=" + status +
                ", userId='" + userId + '\'' +
                ", userName='" + userName + '\'' +
                '}';
    }

    public ShareScreenEvent(boolean status, String userId, String userName) {
        this.status = status;
        this.userId = userId;
        this.userName = userName;
    }
}
