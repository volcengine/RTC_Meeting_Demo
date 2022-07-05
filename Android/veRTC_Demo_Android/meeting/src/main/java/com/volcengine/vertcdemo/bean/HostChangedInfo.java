package com.volcengine.vertcdemo.bean;

import com.google.gson.annotations.SerializedName;
import com.volcengine.vertcdemo.core.net.rtm.RTMBizInform;

public class HostChangedInfo implements RTMBizInform {

    @SerializedName("former_host_id")
    public String formerUid;
    @SerializedName("host_id")
    public String currentUid;

    public HostChangedInfo(String formerUid, String currentUid) {
        this.formerUid = formerUid;
        this.currentUid = currentUid;
    }
}
