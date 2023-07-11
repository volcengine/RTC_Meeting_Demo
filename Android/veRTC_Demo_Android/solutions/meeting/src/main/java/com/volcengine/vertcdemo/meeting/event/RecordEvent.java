// Copyright (c) 2023 Beijing Volcano Engine Technology Ltd.
// SPDX-License-Identifier: MIT

package com.volcengine.vertcdemo.meeting.event;

/**
 * 录制开始、结束事件
 */
public class RecordEvent {

    public boolean isStart;

    public RecordEvent(boolean isStart) {
        this.isStart = isStart;
    }
}
