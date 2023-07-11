// Copyright (c) 2023 Beijing Volcano Engine Technology Ltd.
// SPDX-License-Identifier: MIT

package com.volcengine.vertcdemo.meeting.bean;

import com.volcengine.vertcdemo.common.GsonUtils;
import com.volcengine.vertcdemo.core.net.rts.RTSBizResponse;

import java.util.ArrayList;
import java.util.List;

public class MeetingUsersInfo extends ArrayList implements RTSBizResponse {

    private List<MeetingUserInfo> users;

    public List<MeetingUserInfo> getUsers() {
        if (users == null || users.size() != size()) {
            users = new ArrayList<>();
            for (int i = 0; i < size(); i++) {
                Object obj = get(i);
                if (obj != null) {
                    MeetingUserInfo userInfo = GsonUtils.gson().fromJson(GsonUtils.gson().toJson(obj), MeetingUserInfo.class);
                    if (userInfo != null) {
                        users.add(userInfo);
                    }
                }
            }
        }
        return users;
    }
}
