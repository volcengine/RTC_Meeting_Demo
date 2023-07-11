// Copyright (c) 2023 Beijing Volcano Engine Technology Ltd.
// SPDX-License-Identifier: MIT

package com.volcengine.vertcdemo.meeting.feature.settings;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Pair;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.ss.bytertc.engine.ScreenVideoEncoderConfig;
import com.ss.bytertc.engine.VideoEncoderConfig;
import com.volcengine.vertcdemo.common.CommonSeekbarSettingDialog;
import com.volcengine.vertcdemo.common.CommonTitleLayout;
import com.volcengine.vertcdemo.common.SolutionBaseActivity;
import com.volcengine.vertcdemo.core.eventbus.SolutionDemoEventManager;
import com.volcengine.vertcdemo.core.eventbus.AppTokenExpiredEvent;
import com.volcengine.vertcdemo.meeting.R;
import com.volcengine.vertcdemo.meeting.bean.SettingsConfigEntity;
import com.volcengine.vertcdemo.meeting.core.MeetingDataManager;
import com.volcengine.vertcdemo.meeting.core.MeetingRTCManager;
import com.volcengine.vertcdemo.meeting.event.MeetingEndEvent;
import com.volcengine.vertcdemo.meeting.event.RoomCloseEvent;
import com.volcengine.vertcdemo.meeting.feature.history.HistoryActivity;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SettingsActivity extends SolutionBaseActivity {

    public static final String EXTRA_REFER = Intent.EXTRA_REFERRER;
    public static final String EXTRA_VIEW_TYPE = "extra_view_type";

    public static final String VIEW_TYPE_ALL = "all";
    public static final String VIEW_TYPE_MINE = "mine";

    public static final String REFER_ROOM = "room";
    public static final String REFER_LOGIN = "login";

    private String mRefer = "";
    private SettingsConfigEntity mOriginConfigEntity;
    private SettingsConfigEntity mCurrentConfigEntity;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        mRefer = intent.getStringExtra(EXTRA_REFER);
        mOriginConfigEntity = MeetingDataManager.sSettingsConfigEntity;
        mCurrentConfigEntity = mOriginConfigEntity.deepCopy();
        setContentView(R.layout.activity_settings);

        CommonTitleLayout titleLayout = findViewById(R.id.room_title_bar_layout);
        titleLayout.setLeftBack(v -> onBackPressed());
        titleLayout.setTitle(R.string.settings_title);

        setResolution();
        setFrameRate();
        setBitRate();
        setScreenResolution();
        setScreenFrameRate();
        setScreenBitRate();
        setRealTimeStat();
        setAllHistory();
        setMyHistory();

        SolutionDemoEventManager.register(this);
    }

    @Override
    public void finish() {
        super.finish();
        SolutionDemoEventManager.unregister(this);
        if (mCurrentConfigEntity != mOriginConfigEntity) {
            VideoEncoderConfig videoEncoderConfig = new VideoEncoderConfig();
            videoEncoderConfig.width = mCurrentConfigEntity.getResolution().first;
            videoEncoderConfig.height = mCurrentConfigEntity.getResolution().second;
            videoEncoderConfig.frameRate = mCurrentConfigEntity.getFrameRate();
            videoEncoderConfig.maxBitrate = mCurrentConfigEntity.getBitRate();
            MeetingRTCManager.ins().setVideoEncoderConfig(videoEncoderConfig);
        }
        MeetingDataManager.sSettingsConfigEntity = mCurrentConfigEntity;
        Pair<Integer, Integer> pair = MeetingDataManager.sSettingsConfigEntity.getScreenResolution();
        ScreenVideoEncoderConfig videoEncoderConfig = new ScreenVideoEncoderConfig();
        videoEncoderConfig.width = pair.first;
        videoEncoderConfig.height = pair.second;
        videoEncoderConfig.frameRate = MeetingDataManager.sSettingsConfigEntity.getScreenFrameRate();
        videoEncoderConfig.maxBitrate = MeetingDataManager.sSettingsConfigEntity.getScreenBitRate();
        MeetingRTCManager.ins().setScreenShareProfiles(videoEncoderConfig);
    }

    public void setResolution() {
        View videoResolution = findViewById(R.id.settings_video_resolution);
        TextView vrLeftTv = videoResolution.findViewById(R.id.left_tv);
        vrLeftTv.setText(R.string.settings_video_resolution);
        TextView vrRightTv = videoResolution.findViewById(R.id.right_tv);
        String[] resolutions = mCurrentConfigEntity.getResolutions();
        vrRightTv.setText(resolutions[mCurrentConfigEntity.index]);
        videoResolution.setOnClickListener(v -> {
            List<String> list = new ArrayList<>(Arrays.asList(resolutions));
            ListSettingDialog dialog = new ListSettingDialog(
                    SettingsActivity.this, list, mCurrentConfigEntity.index, "分辨率",
                    new ListSettingDialog.ListDialogListener() {
                        @Override
                        public void onConfirmClick(int index, String str) {
                            mCurrentConfigEntity.index = index;
                            vrRightTv.setText(resolutions[mCurrentConfigEntity.index]);
                            setBitRate();
                        }

                        @Override
                        public void onItemClick(int index, String str) {

                        }
                    });
            dialog.show();
        });
    }

    public void setFrameRate() {
        View videoFrameRate = findViewById(R.id.settings_video_frame_rate);
        TextView vfrLeftTv = videoFrameRate.findViewById(R.id.left_tv);
        vfrLeftTv.setText(R.string.settings_video_frame_rate);
        TextView vfrRightTv = videoFrameRate.findViewById(R.id.right_tv);
        int frameRate = mCurrentConfigEntity.getFrameRate();
        vfrRightTv.setText(getString(R.string.settings_frame_rate, frameRate));
        videoFrameRate.setOnClickListener(v -> {
            List<String> list = new ArrayList<>(Arrays.asList(mCurrentConfigEntity.getFrameRates()));
            int index = 0;
            for (int i = 0; i < list.size(); i++) {
                if (TextUtils.equals(list.get(i), frameRate + "")) {
                    index = i;
                    break;
                }
            }
            ListSettingDialog dialog = new ListSettingDialog(
                    SettingsActivity.this, list, index, "帧率",
                    new ListSettingDialog.ListDialogListener() {
                        @Override
                        public void onConfirmClick(int index, String str) {
                            mCurrentConfigEntity.setFrameRate(Integer.parseInt(list.get(index)));
                            vfrRightTv.setText(getString(R.string.settings_frame_rate,
                                    mCurrentConfigEntity.getFrameRate()));
                        }

                        @Override
                        public void onItemClick(int index, String str) {

                        }
                    });
            dialog.show();
        });
    }

    public void setBitRate() {
        View videoBitRate = findViewById(R.id.settings_video_bit_rate);
        TextView vbrLeftTv = videoBitRate.findViewById(R.id.left_tv);
        vbrLeftTv.setText(R.string.settings_video_bit_rate);
        TextView vbrRightTv = videoBitRate.findViewById(R.id.right_tv);

        Pair<Integer, Integer> bitRatePair = mCurrentConfigEntity.getBitRatePair();
        int currentValue = mCurrentConfigEntity.getBitRate();
        int adjustValue = currentValue;
        if (currentValue < bitRatePair.first) {
            adjustValue = bitRatePair.first;
        } else if (currentValue > bitRatePair.second) {
            adjustValue = bitRatePair.second;
        }
        mCurrentConfigEntity.setBitRate(adjustValue);
        vbrRightTv.setText(getString(R.string.settings_bit_rate, adjustValue));

        videoBitRate.setOnClickListener(v -> {
            CommonSeekbarSettingDialog dialog = new CommonSeekbarSettingDialog(
                    SettingsActivity.this, bitRatePair.first, bitRatePair.second,
                    mCurrentConfigEntity.getBitRate(), "码率", "Kbps",
                    new CommonSeekbarSettingDialog.CommonSeekbarDialogListener() {
                        @Override
                        public void onConfirmClick(int value) {
                            mCurrentConfigEntity.setBitRate(value);
                            vbrRightTv.setText(getString(R.string.settings_bit_rate, value));
                        }

                        @Override
                        public void onValueChanged(int value) {

                        }
                    }
            );
            dialog.show();
        });
    }

    public void setRealTimeStat() {
        View videoRealTimeLog = findViewById(R.id.settings_video_log);
        TextView vrtlLeftTv = videoRealTimeLog.findViewById(R.id.left_tv);
        vrtlLeftTv.setText(R.string.settings_video_log);
        Switch vrtlSwitch = videoRealTimeLog.findViewById(R.id.switch_lg);
        vrtlSwitch.setChecked(mCurrentConfigEntity.enableLog);
        vrtlSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> mCurrentConfigEntity.enableLog = isChecked);
    }

    public void setScreenResolution() {
        View videoResolution = findViewById(R.id.settings_screen_resolution);
        TextView vrLeftTv = videoResolution.findViewById(R.id.left_tv);
        vrLeftTv.setText(R.string.settings_screen_resolution);
        TextView vrRightTv = videoResolution.findViewById(R.id.right_tv);
        String[] resolutions = mCurrentConfigEntity.getResolutions();
        vrRightTv.setText(resolutions[mCurrentConfigEntity.screenIndex]);
        videoResolution.setOnClickListener(v -> {
            List<String> list = new ArrayList<>(Arrays.asList(resolutions));
            ListSettingDialog dialog = new ListSettingDialog(
                    SettingsActivity.this, list, mCurrentConfigEntity.screenIndex, "屏幕共享分辨率",
                    new ListSettingDialog.ListDialogListener() {
                        @Override
                        public void onConfirmClick(int index, String str) {
                            mCurrentConfigEntity.screenIndex = index;
                            vrRightTv.setText(resolutions[mCurrentConfigEntity.screenIndex]);
                            setScreenBitRate();
                        }

                        @Override
                        public void onItemClick(int index, String str) {

                        }
                    });
            dialog.show();
        });
    }

    public void setScreenFrameRate() {
        View videoFrameRate = findViewById(R.id.settings_screen_frame_rate);
        TextView vfrLeftTv = videoFrameRate.findViewById(R.id.left_tv);
        vfrLeftTv.setText(R.string.settings_screen_frame_rate);
        TextView vfrRightTv = videoFrameRate.findViewById(R.id.right_tv);
        int frameRate = mCurrentConfigEntity.getScreenFrameRate();
        vfrRightTv.setText(getString(R.string.settings_frame_rate, frameRate));
        videoFrameRate.setOnClickListener(v -> {
            List<String> list = new ArrayList<>(Arrays.asList(mCurrentConfigEntity.getFrameRates()));
            int index = 0;
            for (int i = 0; i < list.size(); i++) {
                if (TextUtils.equals(list.get(i), frameRate + "")) {
                    index = i;
                    break;
                }
            }
            ListSettingDialog dialog = new ListSettingDialog(
                    SettingsActivity.this, list, index, getString(R.string.settings_screen_frame_rate),
                    new ListSettingDialog.ListDialogListener() {
                        @Override
                        public void onConfirmClick(int index, String str) {
                            mCurrentConfigEntity.setScreenFrameRate(Integer.parseInt(list.get(index)));
                            vfrRightTv.setText(getString(R.string.settings_frame_rate,
                                    mCurrentConfigEntity.getScreenFrameRate()));
                        }

                        @Override
                        public void onItemClick(int index, String str) {

                        }
                    });
            dialog.show();
        });
    }

    public void setScreenBitRate() {
        View videoBitRate = findViewById(R.id.settings_screen_bit_rate);
        TextView vbrLeftTv = videoBitRate.findViewById(R.id.left_tv);
        vbrLeftTv.setText(R.string.settings_screen_bit_rate);
        TextView vbrRightTv = videoBitRate.findViewById(R.id.right_tv);

        Pair<Integer, Integer> bitRatePair = mCurrentConfigEntity.getScreenBitRatePair();
        int currentValue = mCurrentConfigEntity.getScreenBitRate();
        int adjustValue = currentValue;
        if (currentValue < bitRatePair.first) {
            adjustValue = bitRatePair.first;
        } else if (currentValue > bitRatePair.second) {
            adjustValue = bitRatePair.second;
        }
        mCurrentConfigEntity.setScreenBitRate(adjustValue);
        vbrRightTv.setText(getString(R.string.settings_bit_rate, adjustValue));

        videoBitRate.setOnClickListener(v -> {
            CommonSeekbarSettingDialog dialog = new CommonSeekbarSettingDialog(
                    SettingsActivity.this, bitRatePair.first, bitRatePair.second,
                    mCurrentConfigEntity.getScreenBitRate(), getString(R.string.settings_screen_bit_rate), "Kbps",
                    new CommonSeekbarSettingDialog.CommonSeekbarDialogListener() {
                        @Override
                        public void onConfirmClick(int value) {
                            mCurrentConfigEntity.setScreenBitRate(value);
                            vbrRightTv.setText(getString(R.string.settings_bit_rate, value));
                        }

                        @Override
                        public void onValueChanged(int value) {

                        }
                    }
            );
            dialog.show();
        });
    }

    public void setAllHistory() {
        View historyMeetings = findViewById(R.id.settings_history_meetings);
        TextView historyMeetingsTv = historyMeetings.findViewById(R.id.left_tv);
        historyMeetingsTv.setText(R.string.settings_view_history_meetings);
        historyMeetings.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, HistoryActivity.class);
            intent.putExtra(EXTRA_REFER, mRefer);
            intent.putExtra(EXTRA_VIEW_TYPE, VIEW_TYPE_ALL);
            startActivity(intent);
        });
    }

    public void setMyHistory() {
        View historyMeetings = findViewById(R.id.settings_my_history_meetings);
        TextView historyMeetingsTv = historyMeetings.findViewById(R.id.left_tv);
        historyMeetingsTv.setText(R.string.settings_view_my_history_meetings);
        historyMeetings.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, HistoryActivity.class);
            intent.putExtra(EXTRA_REFER, mRefer);
            intent.putExtra(EXTRA_VIEW_TYPE, VIEW_TYPE_MINE);
            startActivity(intent);
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMeetingEndEvent(MeetingEndEvent endEvent) {
        if (REFER_ROOM.equals(mRefer)) {
            finish();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRoomCloseEvent(RoomCloseEvent event) {
        if (REFER_ROOM.equals(mRefer)) {
            finish();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTokenExpiredEvent(AppTokenExpiredEvent event) {
        if (REFER_ROOM.equals(mRefer)) {
            finish();
        }
    }

}
