package com.volcengine.vertcdemo.feature.history;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ss.video.rtc.demo.basic_module.acivities.BaseActivity;
import com.ss.video.rtc.demo.basic_module.ui.CommonDialog;
import com.volcengine.vertcdemo.core.MeetingRTCManager;
import com.volcengine.vertcdemo.core.eventbus.MeetingEndEvent;
import com.volcengine.vertcdemo.core.eventbus.SolutionDemoEventManager;
import com.volcengine.vertcdemo.core.net.IRequestCallback;
import com.volcengine.vertcdemo.core.net.rtm.RTMBizResponse;
import com.volcengine.vertcdemo.bean.MeetingRecordInfo;
import com.volcengine.vertcdemo.meeting.R;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import static androidx.recyclerview.widget.RecyclerView.VERTICAL;
import static com.volcengine.vertcdemo.feature.settings.SettingsActivity.EXTRA_VIEW_TYPE;
import static com.volcengine.vertcdemo.feature.settings.SettingsActivity.REFER_ROOM;
import static com.volcengine.vertcdemo.feature.settings.SettingsActivity.VIEW_TYPE_MINE;

public class HistoryActivity extends BaseActivity {

    private HistoryPresenter mHistoryPresenter;
    private HistoryAdapter mHistoryAdapter;

    private String mRefer = "";
    private String mViewType;

    private final HistoryAdapter.OnClickRecord mOnClickRecord = new HistoryAdapter.OnClickRecord() {
        @Override
        public void onClick(String url) {
            mHistoryPresenter.downloadMeetingFile(url);
        }

        @Override
        public void onLongClick(MeetingRecordInfo info) {
            if (TextUtils.equals(mViewType, VIEW_TYPE_MINE) && info.video_holder) {
                deleteMineRecord(info);
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        Intent intent = getIntent();
        mRefer = intent.getStringExtra(Intent.EXTRA_REFERRER);
        mViewType = intent.getStringExtra(EXTRA_VIEW_TYPE);
    }

    @Override
    protected void onGlobalLayoutCompleted() {
        super.onGlobalLayoutCompleted();

        ImageView backArrow = findViewById(R.id.title_bar_left_iv);
        backArrow.setImageResource(R.drawable.back_arrow);
        backArrow.setOnClickListener(v -> finish());
        TextView title = findViewById(R.id.title_bar_title_tv);
        if (TextUtils.equals(mViewType, VIEW_TYPE_MINE)) {
            title.setText(R.string.mine_history_title);
        } else {
            title.setText(R.string.all_history_title);
        }

        RecyclerView recyclerView = findViewById(R.id.meeting_record_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, VERTICAL, false));
        mHistoryAdapter = new HistoryAdapter(mOnClickRecord);
        recyclerView.setAdapter(mHistoryAdapter);

        mHistoryPresenter = new HistoryPresenter(this);
        mHistoryPresenter.loadData(TextUtils.equals(mViewType, VIEW_TYPE_MINE));
        SolutionDemoEventManager.register(this);
    }

    @Override
    public void finish() {
        super.finish();
        SolutionDemoEventManager.unregister(this);
    }

    public void showData(List<MeetingRecordInfo> infoList) {
        mHistoryAdapter.setRecordList(infoList);
    }

    private void deleteMineRecord(MeetingRecordInfo info) {
        CommonDialog dialog = new CommonDialog(this);
        dialog.setMessage("是否删除该会议记录?");
        dialog.setPositiveListener(v -> {
            MeetingRTCManager.ins().getRTMClient().requestDeleteRecord(info.vid, new IRequestCallback<RTMBizResponse>() {
                @Override
                public void onSuccess(RTMBizResponse data) {

                }

                @Override
                public void onError(int errorCode, String message) {

                }
            });
            dialog.dismiss();
        });
        dialog.setNegativeListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMeetingEndEvent(MeetingEndEvent endEvent) {
        if (REFER_ROOM.equals(mRefer)) {
            finish();
        }
    }
}