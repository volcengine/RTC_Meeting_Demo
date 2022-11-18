package com.volcengine.vertcdemo.meeting.feature.createroom;

import android.content.Intent;
import android.view.View;

import com.ss.video.rtc.demo.basic_module.ui.CommonDialog;
import com.ss.video.rtc.demo.basic_module.ui.CommonExperienceDialog;
import com.ss.video.rtc.demo.basic_module.utils.IMEUtils;
import com.ss.video.rtc.demo.basic_module.utils.SafeToast;
import com.volcengine.vertcdemo.core.net.ErrorTool;
import com.volcengine.vertcdemo.meeting.bean.MeetingTokenInfo;
import com.volcengine.vertcdemo.meeting.core.MeetingDataManager;
import com.volcengine.vertcdemo.meeting.core.MeetingRTCManager;
import com.volcengine.vertcdemo.core.SolutionDataManager;
import com.volcengine.vertcdemo.core.eventbus.RefreshUserNameEvent;
import com.volcengine.vertcdemo.core.eventbus.SolutionDemoEventManager;
import com.volcengine.vertcdemo.core.net.IRequestCallback;
import com.volcengine.vertcdemo.meeting.feature.roommain.RoomActivity;
import com.volcengine.vertcdemo.meeting.feature.settings.SettingsActivity;
import com.volcengine.vertcdemo.meeting.R;

public class CreateMeetingPresenter {

    private CreateMeetingActivity mLoginActivity;
    private boolean isJoining = false;

    public CreateMeetingPresenter(CreateMeetingActivity activity) {
        mLoginActivity = activity;
    }

    public void release() {
        mLoginActivity = null;
    }

    public void onClickMeeting(View view) {
        if (isJoining) {
            return;
        }
        isJoining = true;
        final String meetingId = mLoginActivity.getMeetingId();
        final String userName = mLoginActivity.getUserName();

        SolutionDataManager.ins().setUserName(userName);

        MeetingRTCManager.ins().getRTSClient().requestJoinMeeting(meetingId,
                MeetingDataManager.getMicStatus(),
                MeetingDataManager.getCameraStatus(), new IRequestCallback<MeetingTokenInfo>() {
                    @Override
                    public void onSuccess(MeetingTokenInfo data) {
                        isJoining = false;

                        openRoom(view, userName, meetingId, data);
                    }

                    @Override
                    public void onError(int errorCode, String message) {
                        isJoining = false;
                        SafeToast.show(ErrorTool.getErrorMessageByErrorCode(errorCode, message));
                    }
                });
    }

    private void openRoom(View view, String userName, String meetingId, MeetingTokenInfo info) {
        if (mLoginActivity != null && !mLoginActivity.isFinishing()) {
            mLoginActivity.onJoinRoom();
            IMEUtils.closeIME(view);

            SolutionDataManager.ins().setUserName(userName);
            SolutionDemoEventManager.post(new RefreshUserNameEvent(userName, true));
            Intent intent = new Intent(mLoginActivity, RoomActivity.class);
            intent.putExtra(RoomActivity.EXTRA_KEY_MEETING_ID, meetingId);
            intent.putExtra(RoomActivity.EXTRA_KEY_USER_ID, SolutionDataManager.ins().getUserId());
            intent.putExtra(RoomActivity.EXTRA_KEY_USER_NAME, userName);
            intent.putExtra(RoomActivity.EXTRA_KEY_TOKEN, info.token);
            intent.putExtra(RoomActivity.EXTRA_KEY_MEETING_LAST, getMeetingLastMs(info));

            mLoginActivity.startActivityForResult(intent, RoomActivity.ROOM_REQUEST_CODE);
        }
    }

    private long getMeetingLastMs(MeetingTokenInfo info) {
        if (info == null || info.info == null) {
            return 0;
        }
        long delta = info.info.now - info.info.createdAt;
        return Math.max(0, delta / 1000000);
    }

    public void onClickMic() {
        MeetingDataManager.switchMic(true);
    }

    public void onClickCamera() {
        MeetingDataManager.switchCamera(true);
    }

    public void onClickSetting() {
        if (mLoginActivity != null && !mLoginActivity.isFinishing()) {
            Intent intent = new Intent(mLoginActivity, SettingsActivity.class);
            intent.putExtra(Intent.EXTRA_REFERRER, SettingsActivity.REFER_LOGIN);
            mLoginActivity.startActivity(intent);
        }
    }

    public void dealResult(int requestCode, int resultCode) {
        if (mLoginActivity != null && !mLoginActivity.isFinishing()) {
            if (requestCode == RoomActivity.ROOM_REQUEST_CODE) {
                CommonExperienceDialog dialog = new CommonExperienceDialog(
                        mLoginActivity, (i, i1, s) -> MeetingRTCManager.ins().feedback(i, i1, s));
                dialog.show();

                if (resultCode == RoomActivity.REQUEST_CODE_DUPLICATE_LOGIN) {
                    CommonDialog kickOutDialog = new CommonDialog(mLoginActivity);
                    kickOutDialog.setMessage(mLoginActivity.getString(R.string.login_duplicate_login));
                    kickOutDialog.setPositiveListener(v -> kickOutDialog.dismiss());
                    kickOutDialog.show();
                }
            }
        }
    }
}
