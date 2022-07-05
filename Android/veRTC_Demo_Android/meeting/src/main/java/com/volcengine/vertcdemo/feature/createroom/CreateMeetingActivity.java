package com.volcengine.vertcdemo.feature.createroom;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.ss.bytertc.engine.RTCEngine;
import com.ss.video.rtc.demo.basic_module.acivities.BaseActivity;
import com.ss.video.rtc.demo.basic_module.adapter.TextWatcherAdapter;
import com.ss.video.rtc.demo.basic_module.ui.CommonDialog;
import com.ss.video.rtc.demo.basic_module.utils.IMEUtils;
import com.ss.video.rtc.demo.basic_module.utils.Utilities;
import com.ss.video.rtc.demo.basic_module.utils.WindowUtils;
import com.volcengine.vertcdemo.bean.VideoCanvasWrapper;
import com.volcengine.vertcdemo.common.LengthFilterWithCallback;
import com.volcengine.vertcdemo.core.MeetingDataManager;
import com.volcengine.vertcdemo.core.MeetingRTCManager;
import com.volcengine.vertcdemo.core.SolutionDataManager;
import com.volcengine.vertcdemo.core.eventbus.CameraStatusChangedEvent;
import com.volcengine.vertcdemo.core.eventbus.MicStatusChangeEvent;
import com.volcengine.vertcdemo.core.eventbus.SolutionDemoEventManager;
import com.volcengine.vertcdemo.core.eventbus.TokenExpiredEvent;
import com.volcengine.vertcdemo.core.net.rtm.RtmInfo;
import com.volcengine.vertcdemo.meeting.R;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.regex.Pattern;

public class CreateMeetingActivity extends BaseActivity implements View.OnClickListener {

    public static final String ROOM_INPUT_REGEX = "^[A-Za-z0-9@._-]+$";
    public static final String NAME_INPUT_REGEX = "^[\\u4e00-\\u9fa5a-zA-Z0-9]+$";

    private boolean mHasLayoutCompleted = false;
    private boolean mIsJoinRoom = false;
    private boolean mMeetingIdOverflow = false;
    private boolean mUserIdOverflow = false;

    private RtmInfo mRtmInfo;
    private View mRootView;
    private FrameLayout mVideoContainer;
    private EditText mInputMeetingId;
    private EditText mInputUserId;
    private TextView mInputMeetingIdError;
    private TextView mInputUserIdError;
    private TextView mJoinMeeting;
    private ImageView mCameraStatus;
    private ImageView mCameraSwitch;
    private ImageView mMicSwitch;
    private ImageView mSetting;

    private CreateMeetingPresenter mLoginPresenter;

    private final Runnable mMeetingIdDismissRunnable = () -> mInputMeetingIdError.setVisibility(View.GONE);
    private final Runnable mUserIdDismissRunnable = () -> mInputUserIdError.setVisibility(View.GONE);

    private final TextWatcherAdapter mTextWatcher = new TextWatcherAdapter() {
        @Override
        public void afterTextChanged(Editable s) {
            setupInputStatus();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MeetingDataManager.init();
        initRtmInfo();

        setContentView(R.layout.activity_create_meeting);

        mLoginPresenter = new CreateMeetingPresenter(this);
        mLoginPresenter.init();
    }

    private void initRtmInfo() {
        Intent intent = getIntent();
        if (intent == null) {
            return;
        }
        mRtmInfo = intent.getParcelableExtra(RtmInfo.KEY_RTM);
        if (mRtmInfo == null || !mRtmInfo.isValid()) {
            finish();
        }
        MeetingRTCManager.ins().rtcConnect(mRtmInfo);
    }

    @Override
    protected void setupStatusBar() {
        WindowUtils.setLayoutFullScreen(getWindow());
    }

    @Override
    protected void onGlobalLayoutCompleted() {
        super.onGlobalLayoutCompleted();
        if (mHasLayoutCompleted) {
            return;
        }
        mHasLayoutCompleted = true;

        mRootView = findViewById(R.id.login_root);
        mInputMeetingId = findViewById(R.id.login_input_meeting_id_et);
        mVideoContainer = findViewById(R.id.login_preview_container);
        mInputUserId = findViewById(R.id.login_input_user_id_et);
        mInputMeetingIdError = findViewById(R.id.login_input_meeting_id_waring_tv);
        mInputUserIdError = findViewById(R.id.login_input_user_id_waring_tv);
        mJoinMeeting = findViewById(R.id.login_join_meeting);
        mCameraStatus = findViewById(R.id.login_camera_status);
        mCameraSwitch = findViewById(R.id.login_camera);
        mMicSwitch = findViewById(R.id.login_mic);
        mSetting = findViewById(R.id.login_settings);
        TextView version = findViewById(R.id.login_version);

        mRootView.setOnClickListener(this);
        mJoinMeeting.setOnClickListener(this);
        mCameraSwitch.setOnClickListener(this);
        mMicSwitch.setOnClickListener(this);
        mSetting.setOnClickListener(this);

        setupInputStatus();
        mInputUserId.setText(SolutionDataManager.ins().getUserName());
        mInputMeetingId.addTextChangedListener(mTextWatcher);
        mInputUserId.addTextChangedListener(mTextWatcher);

        InputFilter meetingIDFilter = new LengthFilterWithCallback(18, (overflow) -> {
            if (overflow) {
                mInputMeetingIdError.setVisibility(View.VISIBLE);
                mInputMeetingIdError.setText(R.string.login_input_meeting_id_waring);
                mInputMeetingIdError.removeCallbacks(mMeetingIdDismissRunnable);
                mInputMeetingIdError.postDelayed(mMeetingIdDismissRunnable, 2500);
            } else {
                mInputMeetingIdError.setVisibility(View.GONE);
                mInputMeetingIdError.removeCallbacks(mMeetingIdDismissRunnable);
            }
            mMeetingIdOverflow = overflow;
        });
        InputFilter[] meetingIDFilters = new InputFilter[]{meetingIDFilter};
        mInputMeetingId.setFilters(meetingIDFilters);

        InputFilter userIDFilter = new LengthFilterWithCallback(18, (overflow) -> {
            if (overflow) {
                mInputUserIdError.setVisibility(View.VISIBLE);
                mInputUserIdError.setText(R.string.login_input_meeting_id_waring);
                mInputUserIdError.removeCallbacks(mUserIdDismissRunnable);
                mInputUserIdError.postDelayed(mUserIdDismissRunnable, 2500);
            } else {
                mInputUserIdError.setVisibility(View.GONE);
                mInputUserIdError.removeCallbacks(mUserIdDismissRunnable);
            }
            mUserIdOverflow = overflow;
        });
        InputFilter[] userIDFilters = new InputFilter[]{userIDFilter};
        mInputUserId.setFilters(userIDFilters);

        requestPermissions(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA);

        SolutionDemoEventManager.register(this);

        version.setText(String.format("Demo版本 v%1$s / SDK版本 %2$s", "", RTCEngine.getSdkVersion()));

        updateAudioStatus(MeetingDataManager.getMicStatus());
        updateVideoStatus(MeetingDataManager.getCameraStatus());

        showLimitedServiceDialog();
    }

    @Override
    public void onClick(View v) {
        if (v == mJoinMeeting) {
            mLoginPresenter.onClickMeeting(v);
        } else if (v == mCameraSwitch) {
            mLoginPresenter.onClickCamera();
        } else if (v == mMicSwitch) {
            mLoginPresenter.onClickMic();
        } else if (v == mSetting) {
            mLoginPresenter.onClickSetting();
        } else if (v == mRootView) {
            IMEUtils.closeIME(mRootView);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsJoinRoom = false;
        MeetingRTCManager.startPreview();
        if (MeetingDataManager.getCameraStatus()) {
            MeetingRTCManager.enableLocalVideo(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!mIsJoinRoom) {
            MeetingRTCManager.stopPreview();
        }
    }

    @Override
    public void finish() {
        super.finish();
        mLoginPresenter.release();
        SolutionDemoEventManager.unregister(this);
        MeetingRTCManager.ins().getRTMClient().removeEventListener();
        MeetingRTCManager.ins().getRTMClient().logout();
        MeetingDataManager.release();
        MeetingRTCManager.destroyEngine();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (permissions.length == 0) {
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PERMISSION_GRANTED) {
            if (!MeetingDataManager.getMicStatus()) {
                MeetingDataManager.switchMic(false);
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PERMISSION_GRANTED) {
            if (!MeetingDataManager.getCameraStatus()) {
                MeetingDataManager.switchCamera(false);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mLoginPresenter.dealResult(requestCode, resultCode);
        updateVideoStatus(MeetingDataManager.getCameraStatus());
        updateAudioStatus(MeetingDataManager.getMicStatus());
    }

    private void showLimitedServiceDialog() {
        CommonDialog dialog = new CommonDialog(this);
        dialog.setCancelable(false);
        dialog.setMessage(getString(R.string.login_limited_service));
        dialog.setPositiveListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void setupInputStatus() {
        int meetingIDLength = mInputMeetingId.getText().length();
        int userIDLength = mInputUserId.getText().length();
        boolean meetingIdRegexMatch = false;
        boolean userIdRegexMatch = false;
        if (Pattern.matches(ROOM_INPUT_REGEX, mInputMeetingId.getText().toString())) {
            if (mMeetingIdOverflow) {
                mInputMeetingIdError.setVisibility(View.VISIBLE);
                mInputMeetingIdError.setText(R.string.login_input_meeting_id_waring);
                mInputMeetingIdError.removeCallbacks(mMeetingIdDismissRunnable);
                mInputMeetingIdError.postDelayed(mMeetingIdDismissRunnable, 2500);
            } else {
                mInputMeetingIdError.setVisibility(View.INVISIBLE);
                meetingIdRegexMatch = true;
            }
        } else {
            if (meetingIDLength > 0) {
                mInputMeetingIdError.setVisibility(View.VISIBLE);
                mInputMeetingIdError.setText(R.string.login_input_wrong_content_waring);
            } else {
                mInputMeetingIdError.setVisibility(View.INVISIBLE);
            }
        }
        if (Pattern.matches(NAME_INPUT_REGEX, mInputUserId.getText().toString())) {
            if (mUserIdOverflow) {
                mInputUserIdError.setVisibility(View.VISIBLE);
                mInputUserIdError.setText(R.string.login_input_meeting_id_waring);
                mInputUserIdError.removeCallbacks(mUserIdDismissRunnable);
                mInputUserIdError.postDelayed(mUserIdDismissRunnable, 2500);
            } else {
                mInputUserIdError.setVisibility(View.INVISIBLE);
                userIdRegexMatch = true;
            }
        } else {
            if (userIDLength > 0) {
                mInputUserIdError.setVisibility(View.VISIBLE);
                mInputUserIdError.setText(R.string.audio_input_wrong_content_waring);
            } else {
                mInputUserIdError.setVisibility(View.INVISIBLE);
            }
        }

        boolean joinBtnEnable = meetingIDLength > 0 && meetingIDLength <= 18
                && userIDLength > 0 && userIDLength <= 18
                && meetingIdRegexMatch && userIdRegexMatch;
        mJoinMeeting.setEnabled(joinBtnEnable);
    }

    public void updateVideoStatus(boolean on) {
        mCameraStatus.setVisibility(on ? View.GONE : View.VISIBLE);
        mCameraSwitch.setImageResource(on ? R.drawable.camera_on : R.drawable.camera_off_red);

        if (on) {
            MeetingRTCManager.startPreview();
            VideoCanvasWrapper wrapper = MeetingDataManager.getMyRenderView();
            SurfaceView view = wrapper.getSurfaceView();
            Utilities.removeFromParent(view);
            mVideoContainer.addView(view, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            MeetingRTCManager.setupLocalVideo(wrapper.videoCanvas);
        } else {
            MeetingRTCManager.stopPreview();
            mVideoContainer.removeAllViews();
        }
    }

    public void updateAudioStatus(boolean on) {
        mMicSwitch.setImageResource(on ? R.drawable.mic_on : R.drawable.mic_off_red);
    }

    public String getMeetingId() {
        return mInputMeetingId.getText().toString().trim();
    }

    public void onJoinRoom() {
        mIsJoinRoom = true;
        SolutionDataManager.ins().setUserName(getUserName());
    }

    public String getUserName() {
        return mInputUserId.getText().toString().trim();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCameraStatusChanged(CameraStatusChangedEvent event) {
        if (!mIsJoinRoom) {
            updateVideoStatus(event.status);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMicStatusChanged(MicStatusChangeEvent event) {
        if (!mIsJoinRoom) {
            updateAudioStatus(event.status);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTokenExpiredEvent(TokenExpiredEvent event) {
        finish();
    }
}