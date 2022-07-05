package com.volcengine.vertcdemo.core.eventbus;

import com.volcengine.vertcdemo.bean.MeetingUserInfo;

public class UserLeaveEvent {

    public MeetingUserInfo meetingUserInfo;

    public UserLeaveEvent(MeetingUserInfo meetingUserInfo) {
        this.meetingUserInfo = meetingUserInfo;
    }
}
