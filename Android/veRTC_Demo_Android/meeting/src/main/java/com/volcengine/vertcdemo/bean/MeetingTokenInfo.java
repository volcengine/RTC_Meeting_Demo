package com.volcengine.vertcdemo.bean;

import com.google.gson.annotations.SerializedName;
import com.volcengine.vertcdemo.core.net.rtm.RTMBizInform;
import com.volcengine.vertcdemo.core.net.rtm.RTMBizResponse;

import java.util.List;

public class MeetingTokenInfo implements RTMBizResponse {

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
