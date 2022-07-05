package com.volcengine.vertcdemo.feature.history;

import android.content.Intent;
import android.net.Uri;

import com.volcengine.vertcdemo.bean.MeetingRecordInfoList;
import com.volcengine.vertcdemo.core.MeetingRTCManager;
import com.volcengine.vertcdemo.core.net.IRequestCallback;
import com.volcengine.vertcdemo.bean.MeetingRecordInfo;

import java.util.LinkedList;
import java.util.List;

public class HistoryPresenter {

    private final HistoryActivity mHistoryActivity;

    public HistoryPresenter(HistoryActivity historyActivity) {
        mHistoryActivity = historyActivity;
    }

    public void loadData(boolean isMine) {
        MeetingRTCManager.ins().getRTMClient().requestGetHistoryVideoRecord(new IRequestCallback<MeetingRecordInfoList>() {
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
                    List<MeetingRecordInfo> filterList = new LinkedList<>();
                    for (MeetingRecordInfo info : infoList) {
                        if (info.video_holder == isMine) {
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
