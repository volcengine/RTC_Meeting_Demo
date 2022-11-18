package com.volcengine.vertcdemo.meeting.event;

/**
 * 麦克风状态发生变化事件
 */
public class SDKSpeakerStatusChangedEvent {

    public boolean isSpeaker;

    public SDKSpeakerStatusChangedEvent(boolean isSpeaker) {
        this.isSpeaker = isSpeaker;
    }
}
