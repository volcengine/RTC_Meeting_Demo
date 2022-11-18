package com.volcengine.vertcdemo.meeting.bean;

import com.google.gson.annotations.SerializedName;
import com.volcengine.vertcdemo.core.net.rts.RTSBizResponse;

import java.util.List;

public class MeetingTokenInfo implements RTSBizResponse {

    @SerializedName("token")
    public String token;
    @SerializedName("info")
    public MeetingRoomInfo info;
    @SerializedName("users")
    public List<MeetingUserInfo> users;

    @Override
    public String toString() {
        return "MeetingTokenInfo{" +
                "token='" + token + '\'' +
                ", info=" + info +
                ", users=" + users +
                '}';
    }
}
