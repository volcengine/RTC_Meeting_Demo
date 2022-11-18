package com.volcengine.vertcdemo.meeting.event;

import com.google.gson.annotations.SerializedName;
import com.volcengine.vertcdemo.core.net.rts.RTSBizInform;

/**
 * 房主发生变化事件
 */
public class HostChangedEvent implements RTSBizInform {

    @SerializedName("former_host_id")
    public String formerUid;
    @SerializedName("host_id")
    public String currentUid;

    public HostChangedEvent(String formerUid, String currentUid) {
        this.formerUid = formerUid;
        this.currentUid = currentUid;
    }
}
