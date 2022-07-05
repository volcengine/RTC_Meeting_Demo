package com.volcengine.vertcdemo.bean;

import com.ss.video.rtc.demo.basic_module.utils.GsonUtils;
import com.volcengine.vertcdemo.core.net.rtm.RTMBizResponse;

import java.util.ArrayList;
import java.util.List;

public class MeetingRecordInfoList extends ArrayList implements RTMBizResponse {

    private List<MeetingRecordInfo> meetingRecordInfoList;

    public List<MeetingRecordInfo> getMeetingRecordInfoList() {
        if (meetingRecordInfoList == null || meetingRecordInfoList.size() != size()) {
            meetingRecordInfoList = new ArrayList<>();
            for (int i = 0; i < size(); i++) {
                Object obj = get(i);
                if (obj != null) {
                    try {
                        MeetingRecordInfo recordInfo = GsonUtils.gson().fromJson(GsonUtils.gson().toJson(obj), MeetingRecordInfo.class);
                        if (recordInfo != null) {
                            meetingRecordInfoList.add(recordInfo);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return meetingRecordInfoList;
    }
}
