package com.volcengine.vertcdemo.meeting.bean;

import com.google.gson.annotations.SerializedName;
import com.volcengine.vertcdemo.core.net.rts.RTSBizInform;

import java.util.Objects;

public class MeetingUserInfo implements RTSBizInform {
    @SerializedName("user_id")
    public String userId; // user's id
    @SerializedName("user_name")
    public String userName; // user's name
    @SerializedName("user_uniform_name")
    public String userUniformName; // user's uniform name for sort
    @SerializedName("room_id")
    public String roomId; // meeting's id
    @SerializedName("created_at")
    public long createdAt; // timestamp when meeting created
    @SerializedName("is_host")
    public boolean isHost; // whether user is host
    @SerializedName("is_sharing")
    public boolean isSharing; // whether somebody is sharing screen
    @SerializedName("is_mic_on")
    public boolean isMicOn;  // whether local microphone is on
    @SerializedName("is_camera_on")
    public boolean isCameraOn; // whether local camera is on
    @SerializedName("volume")
    public int volume; //volume value
    @SerializedName("user_status")
    public int userStatus;

    @Override
    public String toString() {
        return "MeetingUserInfo{" +
                "userId='" + userId + '\'' +
                ", userName='" + userName + '\'' +
                ", userUniformName='" + userUniformName + '\'' +
                ", roomId='" + roomId + '\'' +
                ", createdAt=" + createdAt +
                ", isHost=" + isHost +
                ", isSharing=" + isSharing +
                ", isMicOn=" + isMicOn +
                ", isCameraOn=" + isCameraOn +
                ", volume=" + volume +
                ", userStatus=" + userStatus +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MeetingUserInfo info = (MeetingUserInfo) o;
        return Objects.equals(userId, info.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    public MeetingUserInfo deepCopy(MeetingUserInfo info) {
        info.userId = userId;
        info.userName = userName;
        info.userUniformName = userUniformName;
        info.roomId = roomId;
        info.createdAt = createdAt;
        info.isHost = isHost;
        info.isSharing = isSharing;
        info.isMicOn = isMicOn;
        info.isCameraOn = isCameraOn;
        info.volume = volume;
        info.userStatus = userStatus;
        return info;
    }
}
