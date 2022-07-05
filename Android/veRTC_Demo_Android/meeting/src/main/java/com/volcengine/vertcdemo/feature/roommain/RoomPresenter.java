package com.volcengine.vertcdemo.feature.roommain;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Lifecycle;

import com.ss.bytertc.engine.VideoStreamDescription;
import com.ss.video.rtc.demo.basic_module.ui.CommonDialog;
import com.ss.video.rtc.demo.basic_module.utils.SafeToast;
import com.volcengine.vertcdemo.core.MeetingDataManager;
import com.volcengine.vertcdemo.core.MeetingRTCManager;
import com.volcengine.vertcdemo.core.net.IRequestCallback;
import com.volcengine.vertcdemo.core.net.rtm.RTMBizResponse;
import com.volcengine.vertcdemo.feature.participant.ParticipantActivity;
import com.volcengine.vertcdemo.feature.settings.SettingsActivity;
import com.volcengine.vertcdemo.meeting.R;

import java.util.LinkedList;
import java.util.List;

public class RoomPresenter {

    private final RoomActivity mRoomActivity;

    private final Runnable mDurationCounting = new Runnable() {
        @Override
        public void run() {
            if (mRoomActivity != null && !mRoomActivity.isFinishing()) {
                mRoomActivity.updateDuration();
                mRoomActivity.getCountingView().postDelayed(mDurationCounting, 500);
            }
        }
    };

    public RoomPresenter(RoomActivity activity) {
        mRoomActivity = activity;
    }

    public void joinChannel(String token, String channelId, String uid) {
        MeetingRTCManager.enableAutoSubscribe(true, false);

        VideoStreamDescription description = new VideoStreamDescription();
        description.videoSize = MeetingDataManager.sSettingsConfigEntity.getResolution();
        description.frameRate = MeetingDataManager.sSettingsConfigEntity.getFrameRate();
        description.maxKbps = MeetingDataManager.sSettingsConfigEntity.getBitRate();
        List<VideoStreamDescription> descriptions = new LinkedList<>();
        descriptions.add(description);
        MeetingRTCManager.setVideoProfiles(descriptions);

        MeetingRTCManager.joinChannel(token, channelId, null, uid);
    }

    public void startCountDuration() {
        mRoomActivity.getCountingView().removeCallbacks(mDurationCounting);
        mRoomActivity.getCountingView().post(mDurationCounting);
    }

    public void onScreenShareClick() {
        if (mRoomActivity == null || mRoomActivity.isFinishing()) {
            return;
        }
        String uid = MeetingDataManager.getScreenShareUid();
        if (MeetingDataManager.hasSomeoneScreenShare()) {
            if (MeetingDataManager.isSelf(uid)) {
                MeetingDataManager.stopScreenSharing();
            } else {
                SafeToast.show("屏幕共享发起失败，请提示前一位参会者结束共享");
            }
        } else {
            MeetingDataManager.startScreenSharing(mRoomActivity);
        }
    }

    public void openParticipant() {
        if (mRoomActivity != null && !mRoomActivity.isFinishing()) {
            Intent intent = new Intent(mRoomActivity, ParticipantActivity.class);
            mRoomActivity.startActivity(intent);
        }
    }

    public void onRecordClick() {
        if (!MeetingDataManager.isRecording()) {
            if (MeetingDataManager.isSelfHost()) {
                MeetingDataManager.startMeetingRecord();
            } else {
                SafeToast.show("如需录制会议，请提醒主持人开启录制。");
            }
        }
    }

    public void openSettings() {
        if (mRoomActivity != null && !mRoomActivity.isFinishing()) {
            Intent intent = new Intent(mRoomActivity, SettingsActivity.class);
            intent.putExtra(Intent.EXTRA_REFERRER, SettingsActivity.REFER_ROOM);
            mRoomActivity.startActivity(intent);
        }
    }

    public void attemptLeaveMeeting() {
        Activity activity = mRoomActivity;
        if (activity == null || activity.isFinishing()) {
            return;
        }
        final boolean isHost = MeetingDataManager.isSelfHost();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.transparentDialog);
        View view = activity.getLayoutInflater().inflate(R.layout.layout_leave_meeting, null);
        builder.setView(view);
        TextView titleTv = view.findViewById(R.id.leave_meeting_title);
        TextView finishTv = view.findViewById(R.id.leave_meeting_finish);
        TextView confirmTv = view.findViewById(R.id.leave_meeting_confirm);
        TextView cancelTv = view.findViewById(R.id.leave_meeting_cancel);
        builder.setCancelable(true);
        final AlertDialog dialog = builder.create();
        if (isHost) {
            titleTv.setText("请移交主持人给指定参会者，方能离开会议");
            finishTv.setVisibility(View.VISIBLE);

            confirmTv.setTextColor(Color.parseColor("#4CFFFFFF"));
            confirmTv.setBackgroundResource(R.drawable.meeting_leave_disable_bg);
        } else {
            finishTv.setVisibility(View.GONE);
            titleTv.setText("请再次确认是否要离开会议？");

            confirmTv.setBackgroundResource(R.drawable.meeting_leave_alert_bg);
            confirmTv.setTextColor(Color.parseColor("#FFFFFF"));

            confirmTv.setOnClickListener((v) -> {
                dialog.dismiss();
                MeetingRTCManager.ins().getRTMClient().requestLeaveMeeting(MeetingDataManager.getMeetingId(),
                        new IRequestCallback<RTMBizResponse>() {
                            @Override
                            public void onSuccess(RTMBizResponse data) {

                            }

                            @Override
                            public void onError(int errorCode, String message) {

                            }
                        });
                leaveRoom();
                activity.finish();
            });
        }
        finishTv.setOnClickListener((v) -> {
            dialog.dismiss();
            MeetingRTCManager.ins().getRTMClient().requestFinishMeeting(MeetingDataManager.getMeetingId(),
                    new IRequestCallback<RTMBizResponse>() {
                        @Override
                        public void onSuccess(RTMBizResponse data) {

                        }

                        @Override
                        public void onError(int errorCode, String message) {

                        }
                    });
            leaveRoom();
            activity.finish();
        });

        cancelTv.setOnClickListener((v) -> dialog.dismiss());
        dialog.show();
    }

    public void onMeetingEnd() {
        MeetingRTCManager.ins().getRTMClient().requestLeaveMeeting(MeetingDataManager.getMeetingId(),
                new IRequestCallback<RTMBizResponse>() {
                    @Override
                    public void onSuccess(RTMBizResponse data) {

                    }

                    @Override
                    public void onError(int errorCode, String message) {

                    }
                });
        leaveRoom();
    }

    public void onAskOpenMic() {
        AppCompatActivity activity = mRoomActivity;
        if (activity == null || activity.isFinishing() || activity.getLifecycle().getCurrentState() != Lifecycle.State.RESUMED) {
            return;
        }
        if (MeetingDataManager.isSelfHost()) {
            return;
        }
        if (MeetingDataManager.getMicStatus()) {
            return;
        }
        CommonDialog dialog = new CommonDialog(activity);
        dialog.setMessage(activity.getString(R.string.on_ask_open_mic));
        dialog.setPositiveListener(v -> {
            if (!MeetingDataManager.getMicStatus()) {
                MeetingDataManager.switchMic(true);
            }
            dialog.dismiss();
        });
        dialog.setNegativeListener(v -> dialog.dismiss());
        dialog.show();
    }

    public void leaveRoom() {
        MeetingRTCManager.leaveChannel();
    }

    public void finish() {
        mRoomActivity.getCountingView().removeCallbacks(mDurationCounting);
    }
}
