package com.volcengine.vertcdemo.core.eventbus;


import com.volcengine.vertcdemo.bean.MeetingUserInfo;

public class UserJoinEvent {

    public MeetingUserInfo meetingUserInfo;

    public UserJoinEvent(MeetingUserInfo meetingUserInfo) {
        this.meetingUserInfo = meetingUserInfo;
    }
}
