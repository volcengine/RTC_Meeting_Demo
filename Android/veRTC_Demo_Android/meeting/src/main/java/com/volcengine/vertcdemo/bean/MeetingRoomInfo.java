package com.volcengine.vertcdemo.bean;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class MeetingRoomInfo {
    @SerializedName("room_id")
    public String room_id; // meeting's id
    @SerializedName("room_name")
    public String room_name; // meeting's name
    @SerializedName("host_id")
    public String host_id; // host's id
    @SerializedName("host_name")
    public String host_name; // host's name
    @SerializedName("screen_shared_uid")
    public String screen_shared_uid; // user's id who is sharing screen
    @SerializedName("record")
    public boolean record; // whether meeting is recording
    @SerializedName("created_at")
    public long created_at; // timestamp when meeting created
    @SerializedName("now")
    public long now;
    @SerializedName("user_counts")
    public int user_counts;
    @SerializedName("micon_counts")
    public int micon_counts;
    @SerializedName("room_type")
    public int room_type;
    @SerializedName("app_id")
    public String app_id;
    @SerializedName("status")
    public int status; //class status 0:not started 1:in class 2:class end
    @SerializedName("create_time")
    public long create_time;
    @SerializedName("update_time")
    public long update_time;
    @SerializedName("begin_class_time")
    public long begin_class_time;
    @SerializedName("end_class_time")
    public long end_class_time;
    @SerializedName("audio_mute_all")
    public boolean audio_mute_all;
    @SerializedName("video_mute_all")
    public boolean video_mute_all;
    @SerializedName("enable_group_speech")
    public boolean enable_group_speech;
    @SerializedName("enable_interactive")
    public boolean enable_interactive;
    @SerializedName("is_recording")
    public boolean is_recording;
    @SerializedName("room_child_info")
    public List<MeetingRoomInfo> room_child_info;
    // ns
    @SerializedName("begin_class_time_real")
    public long begin_class_time_real;
    @SerializedName("teacher_name")
    public String teacher_name;
    @SerializedName("create_user_id")
    public String create_user_id;

    @Override
    public String toString() {
        return "MeetingRoomInfo{" +
                "room_id='" + room_id + '\'' +
                ", room_name='" + room_name + '\'' +
                ", host_id='" + host_id + '\'' +
                ", host_name='" + host_name + '\'' +
                ", screen_shared_uid='" + screen_shared_uid + '\'' +
                ", record=" + record +
                ", created_at=" + created_at +
                ", now=" + now +
                ", user_counts=" + user_counts +
                ", micon_counts=" + micon_counts +
                ", room_type=" + room_type +
                ", app_id='" + app_id + '\'' +
                ", status=" + status +
                ", create_time=" + create_time +
                ", update_time=" + update_time +
                ", begin_class_time=" + begin_class_time +
                ", end_class_time=" + end_class_time +
                ", audio_mute_all=" + audio_mute_all +
                ", video_mute_all=" + video_mute_all +
                ", enable_group_speech=" + enable_group_speech +
                ", enable_interactive=" + enable_interactive +
                ", is_recording=" + is_recording +
                ", room_child_info=" + room_child_info +
                ", begin_class_time_real=" + begin_class_time_real +
                ", teacher_name='" + teacher_name + '\'' +
                ", create_user_id='" + create_user_id + '\'' +
                '}';
    }
}