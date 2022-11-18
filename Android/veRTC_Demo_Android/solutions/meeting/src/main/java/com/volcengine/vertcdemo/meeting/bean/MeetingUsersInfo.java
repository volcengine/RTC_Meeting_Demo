package com.volcengine.vertcdemo.meeting.bean;

import com.ss.video.rtc.demo.basic_module.utils.GsonUtils;
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
