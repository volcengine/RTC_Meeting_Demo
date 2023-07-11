// Copyright (c) 2023 Beijing Volcano Engine Technology Ltd.
// SPDX-License-Identifier: MIT

package com.volcengine.vertcdemo.meeting.feature.createroom;

import com.google.gson.annotations.SerializedName;
import com.vertcdemo.joinrtsparams.bean.JoinRTSRequest;
import com.volcengine.vertcdemo.core.SolutionDataManager;
import com.volcengine.vertcdemo.meeting.core.Constants;

public class MeetingJoinRTSRequest extends JoinRTSRequest {

    @SerializedName("account_id")
    public final String volcAccountId;
    @SerializedName("vod_space")
    public final String vodSpace;

    public MeetingJoinRTSRequest() {
        super(Constants.SOLUTION_NAME_ABBR, SolutionDataManager.ins().getToken());

        this.volcAccountId = Constants.ACCOUNT_ID;
        this.vodSpace = Constants.VOD_SPACE;
    }

    @Override
    public String toString() {
        return "MeetingJoinRTSRequest{" +
                "scenesName='" + scenesName + '\'' +
                ", loginToken='" + loginToken + '\'' +
                ", volcAccountId='" + volcAccountId + '\'' +
                ", vodSpace='" + vodSpace + '\'' +
                '}';
    }
}
