package com.volcengine.vertcdemo.core.eventbus;

public class SpeakerStatusChangedEvent {

    public boolean isSpeaker;

    public SpeakerStatusChangedEvent(boolean isSpeaker) {
        this.isSpeaker = isSpeaker;
    }
}
