// Copyright (c) 2023 Beijing Volcano Engine Technology Ltd.
// SPDX-License-Identifier: MIT

package com.volcengine.vertcdemo.meeting.bean;

import com.google.gson.annotations.SerializedName;
import com.volcengine.vertcdemo.core.net.rts.RTSBizResponse;

public class MeetingRecordInfo implements RTSBizResponse {
    @SerializedName("uid")
    public String uid;
    @SerializedName("download_url")
    public String downloadUrl; // meeting's download url
    @SerializedName("created_at")
    public long createdAt; // timestamp when meeting started
    @SerializedName("record_status")
    public int recordStatus;
    @SerializedName("room_id")
    public String roomId;
    @SerializedName("room_name")
    public String roomName;
    @SerializedName("vid")
    public String vid;
    @SerializedName("video_holder")
    public boolean videoHolder;
    @SerializedName("create_time")
    public long createTime;
    @SerializedName("update_time")
    public long updateTime;
    @SerializedName("record_begin_time")
    public long recordBeginTime;
    @SerializedName("record_end_time")
    public long recordEndTime;
    @SerializedName("video_url")
    public String videoUrl;

    @Override
    public String toString() {
        return "MeetingRecordInfo{" +
                "uid='" + uid + '\'' +
                ", downloadUrl='" + downloadUrl + '\'' +
                ", createdAt=" + createdAt +
                ", recordStatus=" + recordStatus +
                ", roomId='" + roomId + '\'' +
                ", roomName='" + roomName + '\'' +
                ", vid='" + vid + '\'' +
                ", videoHolder=" + videoHolder +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                ", recordBeginTime=" + recordBeginTime +
                ", recordEndTime=" + recordEndTime +
                ", videoUrl='" + videoUrl + '\'' +
                '}';
    }
}
