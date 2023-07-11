// Copyright (c) 2023 Beijing Volcano Engine Technology Ltd.
// SPDX-License-Identifier: MIT

package com.volcengine.vertcdemo.meeting.feature.participant;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.volcengine.vertcdemo.common.SolutionBaseActivity;
import com.volcengine.vertcdemo.common.SolutionCommonDialog;
import com.volcengine.vertcdemo.common.SolutionToast;
import com.volcengine.vertcdemo.core.eventbus.SolutionDemoEventManager;
import com.volcengine.vertcdemo.core.eventbus.VolumeEvent;
import com.volcengine.vertcdemo.core.net.IRequestCallback;
import com.volcengine.vertcdemo.core.net.rts.RTSBizResponse;
import com.volcengine.vertcdemo.meeting.R;
import com.volcengine.vertcdemo.meeting.bean.MeetingUserInfo;
import com.volcengine.vertcdemo.meeting.bean.MeetingUsersInfo;
import com.volcengine.vertcdemo.meeting.core.MeetingDataManager;
import com.volcengine.vertcdemo.meeting.core.MeetingRTCManager;
import com.volcengine.vertcdemo.meeting.databinding.ActivityParticipantBinding;
import com.volcengine.vertcdemo.meeting.event.AskOpenMicEvent;
import com.volcengine.vertcdemo.meeting.event.CameraStatusChangedEvent;
import com.volcengine.vertcdemo.meeting.event.HostChangedEvent;
import com.volcengine.vertcdemo.meeting.event.KickedOffEvent;
import com.volcengine.vertcdemo.meeting.event.MeetingEndEvent;
import com.volcengine.vertcdemo.meeting.event.MicStatusChangeEvent;
import com.volcengine.vertcdemo.meeting.event.MuteAllEvent;
import com.volcengine.vertcdemo.meeting.event.RoomCloseEvent;
import com.volcengine.vertcdemo.meeting.event.ShareScreenEvent;
import com.volcengine.vertcdemo.meeting.event.UserJoinEvent;
import com.volcengine.vertcdemo.meeting.event.UserLeaveEvent;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 参会者人员页面
 */
public class ParticipantActivity extends SolutionBaseActivity implements View.OnClickListener {

    private TextView mTitleView;
    private ActivityParticipantBinding mViewBinding;

    private ParticipantAdapter mParticipantAdapter = null;
    private final ParticipantAdapter.UserOptionCallback mUserOptionCallback = userInfo -> {
        if (MeetingDataManager.isSelfHost()) {
            showUserOptionDialog(ParticipantActivity.this,
                    userInfo.userName,
                    userInfo.userId,
                    MeetingDataManager.isUserMute(userInfo.userId));
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewBinding = ActivityParticipantBinding.inflate(getLayoutInflater());
        setContentView(mViewBinding.getRoot());

        mViewBinding.titleBarLayout.setLeftBack(v -> onBackPressed());

        mViewBinding.usersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mParticipantAdapter = new ParticipantAdapter(new ArrayList<>(), mUserOptionCallback);
        mViewBinding.usersRecyclerView.setAdapter(mParticipantAdapter);

        mTitleView = mViewBinding.getRoot().findViewById(R.id.title_bar_title_tv);
        mViewBinding.usersMuteAllUsers.setVisibility(MeetingDataManager.isSelfHost() ? View.VISIBLE : View.GONE);
        mViewBinding.usersMuteAllUsers.setOnClickListener(this);

        updateTitle();
        SolutionDemoEventManager.register(this);
        loadParticipant();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SolutionDemoEventManager.unregister(this);
    }

    @Override
    public void onClick(View v) {
        if (v == mViewBinding.usersMuteAllUsers) {
            MeetingRTCManager.ins().getRTSClient().requestMuteUsers("", new IRequestCallback<RTSBizResponse>() {
                @Override
                public void onSuccess(RTSBizResponse data) {

                }

                @Override
                public void onError(int errorCode, String message) {
                    SolutionToast.show(R.string.mute_all_failed);
                }
            });
        }
    }

    private void updateTitle() {
        mTitleView.setText(getString(R.string.users_title, mParticipantAdapter.getItemCount()));
    }

    private void loadParticipant() {
        MeetingRTCManager.ins().getRTSClient().requestGetMeetingUserInfo(new IRequestCallback<MeetingUsersInfo>() {
            @Override
            public void onSuccess(MeetingUsersInfo data) {
                if (data == null) {
                    return;
                }
                List<MeetingUserInfo> userInfoList = data.getUsers();
                if (userInfoList != null && !userInfoList.isEmpty()) {
                    showData(userInfoList);
                } else {
                    showLocalData();
                }
            }

            @Override
            public void onError(int errorCode, String message) {

            }
        });
    }

    private void showLocalData() {
        final List<MeetingUserInfo> infoList = MeetingDataManager.getAllMeetingUserInfoList();
        if (infoList.isEmpty()) {
            return;
        }
        String hostUid = MeetingDataManager.getHostUid();
        MeetingUserInfo info;
        MeetingUserInfo hostInfo = null;
        for (int i = infoList.size() - 1; i >= 0; i--) {
            info = infoList.get(i);
            if (hostUid != null && hostUid.equals(info.userId)) {
                hostInfo = info;
                infoList.remove(i);
                break;
            }
        }
        if (hostInfo != null) {
            infoList.add(0, hostInfo);
        }
        showData(infoList);
    }

    private void showData(List<MeetingUserInfo> infoList) {
        if (isFinishing()) {
            return;
        }
        runOnUiThread(() -> {
            if (!ParticipantActivity.this.isFinishing()) {
                mParticipantAdapter.setData(infoList);
                updateTitle();
            }
        });
    }

    /**
     * 打开用户操作对话框
     * @param activity Android页面
     * @param userName 用户名称
     * @param uid 用户id
     * @param isMute 是否处于静音状态
     */
    private void showUserOptionDialog(Activity activity, String userName, String uid, boolean isMute) {
        if (MeetingDataManager.isSelf(uid)) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.transparentDialog);
        View view = activity.getLayoutInflater().inflate(R.layout.layout_users_options, null);
        builder.setView(view);
        TextView micOption = view.findViewById(R.id.user_option_mic);
        TextView hostOption = view.findViewById(R.id.user_option_host);
        builder.setCancelable(true);
        AlertDialog dialog = builder.create();
        if (isMute) {
            micOption.setText(R.string.option_ask_open_mic);
            micOption.setOnClickListener(v -> {
                MeetingRTCManager.ins().getRTSClient().requestUserMicOn(uid, new IRequestCallback<RTSBizResponse>() {
                    @Override
                    public void onSuccess(RTSBizResponse data) {

                    }

                    @Override
                    public void onError(int errorCode, String message) {
                        SolutionToast.show(R.string.option_failed);
                    }
                });
                dialog.dismiss();
            });
        } else {
            micOption.setText(R.string.option_mute_user);
            micOption.setOnClickListener(v -> {
                MeetingRTCManager.ins().getRTSClient().requestMuteUsers(uid, new IRequestCallback<RTSBizResponse>() {
                    @Override
                    public void onSuccess(RTSBizResponse data) {

                    }

                    @Override
                    public void onError(int errorCode, String message) {
                        SolutionToast.show(R.string.option_failed);
                    }
                });
                dialog.dismiss();
            });
        }
        hostOption.setText(R.string.option_change_host);
        hostOption.setOnClickListener(v -> {
            confirmChangeHost(userName, uid);
            dialog.dismiss();
        });
        dialog.show();
    }

    /**
     * 弹窗确认是否要移交主持人
     *
     * @param userName 要移交的用户名称
     * @param uid 要移交的用户id
     */
    private void confirmChangeHost(String userName, String uid) {
        SolutionCommonDialog changeHostDialog = new SolutionCommonDialog(ParticipantActivity.this);
        changeHostDialog.setMessage(String.format("是否将主持人移交给：%s", userName));
        changeHostDialog.setPositiveListener((v) -> {
            MeetingRTCManager.ins().getRTSClient().requestChangedHost(uid, new IRequestCallback<RTSBizResponse>() {
                @Override
                public void onSuccess(RTSBizResponse data) {

                }

                @Override
                public void onError(int errorCode, String message) {
                    SolutionToast.show(R.string.option_failed);
                }
            });
            changeHostDialog.dismiss();
        });
        changeHostDialog.setNegativeListener((v) -> changeHostDialog.dismiss());
        changeHostDialog.show();
    }

    public void onAskOpenMic() {
        if (isFinishing() || getLifecycle().getCurrentState() != Lifecycle.State.RESUMED) {
            return;
        }
        if (MeetingDataManager.isSelfHost()) {
            return;
        }
        if (MeetingDataManager.getMicStatus()) {
            return;
        }
        SolutionCommonDialog dialog = new SolutionCommonDialog(this);
        dialog.setMessage(this.getString(R.string.on_ask_open_mic));
        dialog.setPositiveListener(v -> {
            if (!MeetingDataManager.getMicStatus()) {
                MeetingDataManager.switchMic(true);
            }
            dialog.dismiss();
        });
        dialog.setNegativeListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void OnUserJoinEvent(UserJoinEvent event) {
        mParticipantAdapter.onUserJoin(event.meetingUserInfo);
        updateTitle();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void OnUserJoinEvent(UserLeaveEvent event) {
        mParticipantAdapter.onUserLeave(event.meetingUserInfo);
        updateTitle();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void OnShareScreenEvent(ShareScreenEvent event) {
        mParticipantAdapter.onScreenShareChanged(event.userId, event.status);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void OnMicStatusChangeEvent(MicStatusChangeEvent event) {
        mParticipantAdapter.onMicStatusChanged(event.userId, event.status);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void OnCameraStatusChangedEvent(CameraStatusChangedEvent event) {
        mParticipantAdapter.onCameraStatusChanged(event.userId, event.status);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onHostChangedEvent(HostChangedEvent event) {
        mViewBinding.usersMuteAllUsers.setVisibility(MeetingDataManager.isSelf(event.currentUid) ? View.VISIBLE : View.GONE);
        mParticipantAdapter.onHostChanged(event.formerUid, event.currentUid);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMeetingEndEvent(MeetingEndEvent event) {
        finish();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRoomCloseEvent(RoomCloseEvent event) {
        finish();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAskOpenMicEvent(AskOpenMicEvent event) {
        onAskOpenMic();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onVolumeEvent(VolumeEvent event) {
        Map<String, Integer> uidVolumeMap = event.uidVolumeMap;
        for (Map.Entry<String, Integer> entry : uidVolumeMap.entrySet()) {
            mParticipantAdapter.onVolumeEvent(entry.getKey(), entry.getValue());
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMuteAllEvent(MuteAllEvent event) {
        mParticipantAdapter.onMuteAllEvent();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onKickedOffEvent(KickedOffEvent event) {
        finish();
    }
}
