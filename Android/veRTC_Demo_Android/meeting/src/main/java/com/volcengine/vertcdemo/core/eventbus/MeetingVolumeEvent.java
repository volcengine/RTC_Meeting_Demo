package com.volcengine.vertcdemo.core.eventbus;

import com.ss.bytertc.engine.handler.IRTCEngineEventHandler;

public class MeetingVolumeEvent {
    public IRTCEngineEventHandler.AudioVolumeInfo[] speakers;
    public int totalVolume;

    public MeetingVolumeEvent(IRTCEngineEventHandler.AudioVolumeInfo[] speakers, int totalVolume) {
        this.speakers = speakers;
        this.totalVolume = totalVolume;
    }
}
