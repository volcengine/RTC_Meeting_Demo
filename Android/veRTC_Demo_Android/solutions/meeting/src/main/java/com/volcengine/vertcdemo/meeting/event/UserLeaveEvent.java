// Copyright (c) 2023 Beijing Volcano Engine Technology Ltd.
// SPDX-License-Identifier: MIT

package com.volcengine.vertcdemo.meeting.event;

import com.volcengine.vertcdemo.meeting.bean.MeetingUserInfo;

/**
 * 用户离开房间事件
 */
public class UserLeaveEvent {

    public MeetingUserInfo meetingUserInfo;

    public UserLeaveEvent(MeetingUserInfo meetingUserInfo) {
        this.meetingUserInfo = meetingUserInfo;
    }
}
