package com.volcengine.vertcdemo.meeting.feature.history;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.ss.video.rtc.demo.basic_module.acivities.BaseActivity;
import com.ss.video.rtc.demo.basic_module.ui.CommonDialog;
import com.ss.video.rtc.demo.basic_module.utils.SafeToast;
import com.volcengine.vertcdemo.core.net.ErrorTool;
import com.volcengine.vertcdemo.meeting.core.MeetingRTCManager;
import com.volcengine.vertcdemo.meeting.databinding.ActivityHistoryBinding;
import com.volcengine.vertcdemo.meeting.event.MeetingEndEvent;
import com.volcengine.vertcdemo.core.eventbus.SolutionDemoEventManager;
import com.volcengine.vertcdemo.core.net.IRequestCallback;
import com.volcengine.vertcdemo.core.net.rts.RTSBizResponse;
import com.volcengine.vertcdemo.meeting.bean.MeetingRecordInfo;
import com.volcengine.vertcdemo.meeting.R;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import static androidx.recyclerview.widget.RecyclerView.VERTICAL;
import static com.volcengine.vertcdemo.meeting.feature.settings.SettingsActivity.EXTRA_VIEW_TYPE;
import static com.volcengine.vertcdemo.meeting.feature.settings.SettingsActivity.REFER_ROOM;
import static com.volcengine.vertcdemo.meeting.feature.settings.SettingsActivity.VIEW_TYPE_MINE;

public class HistoryActivity extends BaseActivity {

    private HistoryPresenter mHistoryPresenter;
    private HistoryAdapter mHistoryAdapter;

    private String mRefer = "";
    private String mViewType;

    private ActivityHistoryBinding mViewBinding;

    private final HistoryAdapter.OnClickRecord mOnClickRecord = new HistoryAdapter.OnClickRecord() {
        @Override
        public void onClick(String url) {
            mHistoryPresenter.downloadMeetingFile(url);
        }

        @Override
        public void onLongClick(MeetingRecordInfo info) {
            if (TextUtils.equals(mViewType, VIEW_TYPE_MINE) && info.videoHolder) {
                deleteMineRecord(info);
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewBinding = ActivityHistoryBinding.inflate(getLayoutInflater());
        setContentView(mViewBinding.getRoot());

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

        mViewBinding.meetingRecordRecyclerView.setLayoutManager(new LinearLayoutManager(this, VERTICAL, false));
        mHistoryAdapter = new HistoryAdapter(mOnClickRecord);
        mViewBinding.meetingRecordRecyclerView.setAdapter(mHistoryAdapter);

        mHistoryPresenter = new HistoryPresenter(this);
        mHistoryPresenter.loadData(TextUtils.equals(mViewType, VIEW_TYPE_MINE));
        SolutionDemoEventManager.register(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SolutionDemoEventManager.unregister(this);
    }

    public void showData(List<MeetingRecordInfo> infoList) {
        mHistoryAdapter.setRecordList(infoList);
    }

    private void deleteMineRecord(MeetingRecordInfo info) {
        CommonDialog dialog = new CommonDialog(this);
        dialog.setMessage(getString(R.string.mine_history_delete_tip));
        dialog.setPositiveListener(v -> {
            MeetingRTCManager.ins().getRTSClient().requestDeleteRecord(info.vid, new IRequestCallback<RTSBizResponse>() {
                @Override
                public void onSuccess(RTSBizResponse data) {

                }

                @Override
                public void onError(int errorCode, String message) {
                    SafeToast.show(ErrorTool.getErrorMessageByErrorCode(errorCode, message));
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