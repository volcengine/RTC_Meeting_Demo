package com.volcengine.vertcdemo.meeting.bean;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class MeetingRoomInfo {
    @SerializedName("room_id")
    public String roomId; // meeting's id
    @SerializedName("room_name")
    public String roomName; // meeting's name
    @SerializedName("host_id")
    public String hostId; // host's id
    @SerializedName("host_name")
    public String hostName; // host's name
    @SerializedName("screen_shared_uid")
    public String screenSharedUid; // user's id who is sharing screen
    @SerializedName("record")
    public boolean record; // whether meeting is recording
    @SerializedName("created_at")
    public long createdAt; // timestamp when meeting created
    @SerializedName("now")
    public long now;
    @SerializedName("user_counts")
    public int userCounts;
    @SerializedName("micon_counts")
    public int micOnCounts;
    @SerializedName("room_type")
    public int roomType;
    @SerializedName("app_id")
    public String appId;
    @SerializedName("status")
    public int status; //class status 0:not started 1:in class 2:class end
    @SerializedName("create_time")
    public long createTime;
    @SerializedName("update_time")
    public long updateTime;
    @SerializedName("begin_class_time")
    public long beginClassTime;
    @SerializedName("end_class_time")
    public long endClassTime;
    @SerializedName("audio_mute_all")
    public boolean audioMuteAll;
    @SerializedName("video_mute_all")
    public boolean videoMuteAll;
    @SerializedName("enable_group_speech")
    public boolean enableGroupSpeech;
    @SerializedName("enable_interactive")
    public boolean enableInteractive;
    @SerializedName("is_recording")
    public boolean isRecording;
    @SerializedName("room_child_info")
    public List<MeetingRoomInfo> roomChildInfo;
    // ns
    @SerializedName("begin_class_time_real")
    public long beginClassTimeReal;
    @SerializedName("teacher_name")
    public String teacherName;
    @SerializedName("create_user_id")
    public String createUserId;

    @Override
    public String toString() {
        return "MeetingRoomInfo{" +
                "roomId='" + roomId + '\'' +
                ", roomName='" + roomName + '\'' +
                ", hostId='" + hostId + '\'' +
                ", hostName='" + hostName + '\'' +
                ", screenSharedUid='" + screenSharedUid + '\'' +
                ", record=" + record +
                ", createdAt=" + createdAt +
                ", now=" + now +
                ", userCounts=" + userCounts +
                ", micOnCounts=" + micOnCounts +
                ", roomType=" + roomType +
                ", appId='" + appId + '\'' +
                ", status=" + status +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                ", beginClassTime=" + beginClassTime +
                ", endClassTime=" + endClassTime +
                ", audioMuteAll=" + audioMuteAll +
                ", videoMuteAll=" + videoMuteAll +
                ", enableGroupSpeech=" + enableGroupSpeech +
                ", enableInteractive=" + enableInteractive +
                ", isRecording=" + isRecording +
                ", roomChildInfo=" + roomChildInfo +
                ", beginClassTimeReal=" + beginClassTimeReal +
                ", teacherName='" + teacherName + '\'' +
                ", createUserId='" + createUserId + '\'' +
                '}';
    }
}