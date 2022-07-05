package com.volcengine.vertcdemo.bean;

import com.google.gson.annotations.SerializedName;
import com.volcengine.vertcdemo.core.net.rtm.RTMBizResponse;

public class MeetingRecordInfo implements RTMBizResponse {
    @SerializedName("uid")
    public String uid;
    @SerializedName("download_url")
    public String download_url; // meeting's download url
    @SerializedName("created_at")
    public long created_at; // timestamp when meeting started
    @SerializedName("record_status")
    public int record_status;
    @SerializedName("room_id")
    public String room_id;
    @SerializedName("room_name")
    public String room_name;
    @SerializedName("vid")
    public String vid;
    @SerializedName("video_holder")
    public boolean video_holder;
    @SerializedName("create_time")
    public long create_time;
    @SerializedName("update_time")
    public long update_time;
    @SerializedName("record_begin_time")
    public long record_begin_time;
    @SerializedName("record_end_time")
    public long record_end_time;
    @SerializedName("video_url")
    public String video_url;

    @Override
    public String toString() {
        return "MeetingRecordInfo{" +
                "uid='" + uid + '\'' +
                ", download_url='" + download_url + '\'' +
                ", created_at=" + created_at +
                ", record_status=" + record_status +
                ", room_id='" + room_id + '\'' +
                ", room_name='" + room_name + '\'' +
                ", vid='" + vid + '\'' +
                ", video_holder=" + video_holder +
                ", create_time=" + create_time +
                ", update_time=" + update_time +
                ", record_begin_time=" + record_begin_time +
                ", record_end_time=" + record_end_time +
                ", video_url=" + video_url +
                '}';
    }
}
