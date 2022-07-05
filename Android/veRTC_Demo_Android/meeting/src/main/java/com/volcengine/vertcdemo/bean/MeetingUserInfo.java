package com.volcengine.vertcdemo.bean;

import com.google.gson.annotations.SerializedName;
import com.volcengine.vertcdemo.core.net.rtm.RTMBizInform;

import java.util.Objects;

public class MeetingUserInfo implements RTMBizInform {
    @SerializedName("user_id")
    public String user_id; // user's id
    @SerializedName("user_name")
    public String user_name; // user's name
    @SerializedName("user_uniform_name")
    public String user_uniform_name; // user's uniform name for sort
    @SerializedName("room_id")
    public String room_id; // meeting's id
    @SerializedName("created_at")
    public long created_at; // timestamp when meeting created
    @SerializedName("is_host")
    public boolean is_host; // whether user is host
    @SerializedName("is_sharing")
    public boolean is_sharing; // whether somebody is sharing screen
    @SerializedName("is_mic_on")
    public boolean is_mic_on;  // whether local microphone is on
    @SerializedName("is_camera_on")
    public boolean is_camera_on; // whether local camera is on
    @SerializedName("volume")
    public int volume; //volume value
    @SerializedName("user_status")
    public int user_status;

    @Override
    public String toString() {
        return "MeetingUserInfo{" +
                "user_id='" + user_id + '\'' +
                ", user_name='" + user_name + '\'' +
                ", user_uniform_name='" + user_uniform_name + '\'' +
                ", room_id='" + room_id + '\'' +
                ", created_at=" + created_at +
                ", is_host=" + is_host +
                ", is_sharing=" + is_sharing +
                ", is_mic_on=" + is_mic_on +
                ", is_camera_on=" + is_camera_on +
                ", volume=" + volume +
                ", user_status=" + user_status +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MeetingUserInfo info = (MeetingUserInfo) o;
        return Objects.equals(user_id, info.user_id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user_id);
    }

    public MeetingUserInfo deepCopy(MeetingUserInfo info) {
        info.user_id = user_id;
        info.user_name = user_name;
        info.user_uniform_name = user_uniform_name;
        info.room_id = room_id;
        info.created_at = created_at;
        info.is_host = is_host;
        info.is_sharing = is_sharing;
        info.is_mic_on = is_mic_on;
        info.is_camera_on = is_camera_on;
        info.volume = volume;
        info.user_status = user_status;
        return info;
    }
}
