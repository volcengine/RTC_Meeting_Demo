package com.volcengine.vertcdemo.meeting.event;

/**
 * 被主持人静音事件
 */
public class MuteEvent {

    public String uid;

    public MuteEvent(String uid) {
        this.uid = uid;
    }
}
