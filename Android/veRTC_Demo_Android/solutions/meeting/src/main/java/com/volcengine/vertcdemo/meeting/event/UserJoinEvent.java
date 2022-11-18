package com.volcengine.vertcdemo.meeting.event;


import com.volcengine.vertcdemo.meeting.bean.MeetingUserInfo;

/**
 * 用户加入房间事件
 */
public class UserJoinEvent {

    public MeetingUserInfo meetingUserInfo;

    public UserJoinEvent(MeetingUserInfo meetingUserInfo) {
        this.meetingUserInfo = meetingUserInfo;
    }
}
