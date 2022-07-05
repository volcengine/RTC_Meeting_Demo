package com.volcengine.vertcdemo.bean;

import com.ss.video.rtc.demo.basic_module.utils.GsonUtils;
import com.volcengine.vertcdemo.core.net.rtm.RTMBizResponse;

import java.util.ArrayList;
import java.util.List;

public class MeetingUsersInfo extends ArrayList implements RTMBizResponse {

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
