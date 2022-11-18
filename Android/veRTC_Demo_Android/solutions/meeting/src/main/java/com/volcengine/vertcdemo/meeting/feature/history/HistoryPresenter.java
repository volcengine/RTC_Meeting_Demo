package com.volcengine.vertcdemo.meeting.feature.history;

import android.content.Intent;
import android.net.Uri;

import com.volcengine.vertcdemo.meeting.bean.MeetingRecordInfoList;
import com.volcengine.vertcdemo.meeting.core.MeetingRTCManager;
import com.volcengine.vertcdemo.core.net.IRequestCallback;
import com.volcengine.vertcdemo.meeting.bean.MeetingRecordInfo;

import java.util.ArrayList;
import java.util.List;

public class HistoryPresenter {

    private final HistoryActivity mHistoryActivity;

    public HistoryPresenter(HistoryActivity historyActivity) {
        mHistoryActivity = historyActivity;
    }

    public void loadData(boolean isMine) {
        MeetingRTCManager.ins().getRTSClient().requestGetHistoryVideoRecord(new IRequestCallback<MeetingRecordInfoList>() {
            @Override
            public void onSuccess(MeetingRecordInfoList data) {
                if (data == null) {
                    return;
                }
                List<MeetingRecordInfo> infoList = data.getMeetingRecordInfoList();
                if (infoList == null) {
                    return;
                }
                if (mHistoryActivity != null && !mHistoryActivity.isFinishing()) {
                    List<MeetingRecordInfo> filterList = new ArrayList<>();
                    for (MeetingRecordInfo info : infoList) {
                        if (info.videoHolder == isMine) {
                            filterList.add(info);
                        }
                    }
                    mHistoryActivity.runOnUiThread(() -> mHistoryActivity.showData(filterList));
                }
            }

            @Override
            public void onError(int errorCode, String message) {

            }
        });
    }

    public void downloadMeetingFile(String url) {
        if (mHistoryActivity != null && !mHistoryActivity.isFinishing()) {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            mHistoryActivity.startActivity(i);
        }
    }
}
