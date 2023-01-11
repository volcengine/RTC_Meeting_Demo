package com.volcengine.vertcdemo.meeting.feature.createroom;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.volcengine.vertcdemo.core.net.rts.RTSInfo.KEY_RTM;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.util.Log;
import android.view.View;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.ss.bytertc.engine.RTCEngine;
import com.ss.bytertc.engine.VideoCanvas;
import com.ss.video.rtc.demo.basic_module.acivities.BaseActivity;
import com.ss.video.rtc.demo.basic_module.adapter.TextWatcherAdapter;
import com.ss.video.rtc.demo.basic_module.ui.CommonDialog;
import com.ss.video.rtc.demo.basic_module.utils.IMEUtils;
import com.ss.video.rtc.demo.basic_module.utils.Utilities;
import com.ss.video.rtc.demo.basic_module.utils.WindowUtils;
import com.vertcdemo.joinrtsparams.bean.JoinRTSRequest;
import com.vertcdemo.joinrtsparams.common.JoinRTSManager;
import com.volcengine.vertcdemo.common.IAction;
import com.volcengine.vertcdemo.common.LengthFilterWithCallback;
import com.volcengine.vertcdemo.core.SolutionDataManager;
import com.volcengine.vertcdemo.core.eventbus.SolutionDemoEventManager;
import com.volcengine.vertcdemo.core.eventbus.TokenExpiredEvent;
import com.volcengine.vertcdemo.core.net.IRequestCallback;
import com.volcengine.vertcdemo.core.net.ServerResponse;
import com.volcengine.vertcdemo.core.net.rts.RTSInfo;
import com.volcengine.vertcdemo.meeting.R;
import com.volcengine.vertcdemo.meeting.core.Constants;
import com.volcengine.vertcdemo.meeting.core.MeetingDataManager;
import com.volcengine.vertcdemo.meeting.core.MeetingRTCManager;
import com.volcengine.vertcdemo.meeting.databinding.ActivityCreateMeetingBinding;
import com.volcengine.vertcdemo.meeting.event.CameraStatusChangedEvent;
import com.volcengine.vertcdemo.meeting.event.MicStatusChangeEvent;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.regex.Pattern;

public class CreateMeetingActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = "CreateMeetingActivity";

    public static final String ROOM_INPUT_REGEX = "^[A-Za-z0-9@._-]+$";
    public static final String NAME_INPUT_REGEX = "^[\\u4e00-\\u9fa5a-zA-Z0-9]+$";

    private boolean mHasLayoutCompleted = false;
    private boolean mIsJoinRoom = false;
    private boolean mMeetingIdOverflow = false;
    private boolean mUserIdOverflow = false;

    private ActivityCreateMeetingBinding mViewBinding;

    private CreateMeetingPresenter mLoginPresenter;

    private final Runnable mMeetingIdDismissRunnable = () -> mViewBinding.loginInputMeetingIdWaringTv.setVisibility(View.GONE);
    private final Runnable mUserIdDismissRunnable = () -> mViewBinding.loginInputUserIdWaringTv.setVisibility(View.GONE);

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
        initRTSInfo();

        mViewBinding = ActivityCreateMeetingBinding.inflate(getLayoutInflater());
        setContentView(mViewBinding.getRoot());

        mLoginPresenter = new CreateMeetingPresenter(this);
    }

    private void initRTSInfo() {
        Intent intent = getIntent();
        if (intent == null) {
            return;
        }
        RTSInfo info = intent.getParcelableExtra(RTSInfo.KEY_RTM);
        if (info == null || !info.isValid()) {
            finish();
            return;
        }
        MeetingRTCManager.ins().rtcConnect(info);
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

        mViewBinding.loginRoot.setOnClickListener(this);
        mViewBinding.loginJoinMeeting.setOnClickListener(this);
        mViewBinding.loginCamera.setOnClickListener(this);
        mViewBinding.loginMic.setOnClickListener(this);
        mViewBinding.loginSettings.setOnClickListener(this);

        setupInputStatus();
        mViewBinding.loginInputUserIdEt.setText(SolutionDataManager.ins().getUserName());
        mViewBinding.loginInputMeetingIdEt.addTextChangedListener(mTextWatcher);
        mViewBinding.loginInputUserIdEt.addTextChangedListener(mTextWatcher);

        InputFilter meetingIDFilter = new LengthFilterWithCallback(18, (overflow) -> {
            mMeetingIdOverflow = overflow;
            setupInputStatus();
        });
        InputFilter[] meetingIDFilters = new InputFilter[]{meetingIDFilter};
        mViewBinding.loginInputMeetingIdEt.setFilters(meetingIDFilters);

        InputFilter userIDFilter = new LengthFilterWithCallback(18, (overflow) -> {
            mUserIdOverflow = overflow;
            setupInputStatus();
        });
        InputFilter[] userIDFilters = new InputFilter[]{userIDFilter};
        mViewBinding.loginInputUserIdEt.setFilters(userIDFilters);

        requestPermissions(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA);

        SolutionDemoEventManager.register(this);

        mViewBinding.loginVersion.setText(String.format("App版本 v%1$s / SDK版本 %2$s",
                SolutionDataManager.ins().getAppVersionName(),
                RTCEngine.getSdkVersion()));

        updateAudioStatus(MeetingDataManager.getMicStatus());
        updateVideoStatus(MeetingDataManager.getCameraStatus());

        showLimitedServiceDialog();
    }

    @Override
    public void onClick(View v) {
        if (v == mViewBinding.loginJoinMeeting) {
            mLoginPresenter.onClickMeeting(v);
        } else if (v == mViewBinding.loginCamera) {
            mLoginPresenter.onClickCamera();
        } else if (v == mViewBinding.loginMic) {
            mLoginPresenter.onClickMic();
        } else if (v == mViewBinding.loginSettings) {
            mLoginPresenter.onClickSetting();
        } else if (v == mViewBinding.loginRoot) {
            IMEUtils.closeIME(mViewBinding.loginRoot);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsJoinRoom = false;
        MeetingRTCManager.ins().enableLocalVideo(MeetingDataManager.getCameraStatus());
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!mIsJoinRoom) {
            MeetingRTCManager.ins().stopVideoCapture();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLoginPresenter.release();
        SolutionDemoEventManager.unregister(this);
        MeetingRTCManager.ins().getRTSClient().removeEventListener();
        MeetingRTCManager.ins().getRTSClient().logout();
        MeetingDataManager.release();
        MeetingRTCManager.ins().destroyEngine();
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
        int meetingIDLength = mViewBinding.loginInputMeetingIdEt.getText().length();
        int userIDLength = mViewBinding.loginInputUserIdEt.getText().length();
        boolean meetingIdRegexMatch = Pattern.matches(ROOM_INPUT_REGEX, mViewBinding.loginInputMeetingIdEt.getText().toString());
        boolean userIdRegexMatch = Pattern.matches(NAME_INPUT_REGEX, mViewBinding.loginInputUserIdEt.getText().toString());
        mViewBinding.loginInputMeetingIdWaringTv.removeCallbacks(mMeetingIdDismissRunnable);
        if (meetingIdRegexMatch) {
            if (mMeetingIdOverflow) {
                mViewBinding.loginInputMeetingIdWaringTv.setVisibility(View.VISIBLE);
                mViewBinding.loginInputMeetingIdWaringTv.setText(R.string.login_input_meeting_id_waring);
                mViewBinding.loginInputMeetingIdWaringTv.postDelayed(mMeetingIdDismissRunnable, 2500);
            } else {
                mViewBinding.loginInputMeetingIdWaringTv.setVisibility(View.INVISIBLE);
            }
        } else {
            if (meetingIDLength > 0) {
                mViewBinding.loginInputMeetingIdWaringTv.setVisibility(View.VISIBLE);
                mViewBinding.loginInputMeetingIdWaringTv.setText(R.string.login_input_wrong_content_waring);
            } else {
                mViewBinding.loginInputMeetingIdWaringTv.setVisibility(View.INVISIBLE);
            }
        }
        mViewBinding.loginInputUserIdWaringTv.removeCallbacks(mUserIdDismissRunnable);
        if (userIdRegexMatch) {
            if (mUserIdOverflow) {
                mViewBinding.loginInputUserIdWaringTv.setVisibility(View.VISIBLE);
                mViewBinding.loginInputUserIdWaringTv.setText(R.string.login_input_meeting_id_waring);
                mViewBinding.loginInputUserIdWaringTv.postDelayed(mUserIdDismissRunnable, 2500);
            } else {
                mViewBinding.loginInputUserIdWaringTv.setVisibility(View.INVISIBLE);
            }
        } else {
            if (userIDLength > 0) {
                mViewBinding.loginInputUserIdWaringTv.setVisibility(View.VISIBLE);
                mViewBinding.loginInputUserIdWaringTv.setText(R.string.audio_input_wrong_content_waring);
            } else {
                mViewBinding.loginInputUserIdWaringTv.setVisibility(View.INVISIBLE);
            }
        }

        boolean joinBtnEnable = meetingIDLength > 0 && meetingIDLength <= 18
                && userIDLength > 0 && userIDLength <= 18
                && meetingIdRegexMatch && userIdRegexMatch;
        mViewBinding.loginJoinMeeting.setEnabled(joinBtnEnable);
    }

    public void updateVideoStatus(boolean on) {
        mViewBinding.loginCameraStatus.setVisibility(on ? View.GONE : View.VISIBLE);
        mViewBinding.loginCamera.setImageResource(on ? R.drawable.camera_on : R.drawable.camera_off_red);
        mViewBinding.loginCameraStatus.setImageResource(on ? R.drawable.camera_on : R.drawable.camera_off_red);

        mViewBinding.loginPreviewContainer.setVisibility(on ? View.VISIBLE : View.GONE);
        if (on) {
            MeetingRTCManager.ins().startVideoCapture();
            final VideoCanvas canvas = new VideoCanvas();
            canvas.renderMode = VideoCanvas.RENDER_MODE_HIDDEN;
            canvas.renderView = mViewBinding.loginPreviewContainer;
            MeetingRTCManager.ins().setLocalVideoCanvas(canvas);
        } else {
            MeetingRTCManager.ins().stopVideoCapture();
        }
    }

    public void updateAudioStatus(boolean on) {
        mViewBinding.loginMic.setImageResource(on ? R.drawable.mic_on : R.drawable.mic_off_red);
    }

    public String getMeetingId() {
        return mViewBinding.loginInputMeetingIdEt.getText().toString().trim();
    }

    public void onJoinRoom() {
        mIsJoinRoom = true;
        SolutionDataManager.ins().setUserName(getUserName());
    }

    public String getUserName() {
        return mViewBinding.loginInputUserIdEt.getText().toString().trim();
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

    @Keep
    @SuppressWarnings("unused")
    public static void prepareSolutionParams(Activity activity, IAction<Object> doneAction) {
        Log.d(TAG, "prepareSolutionParams() invoked");
        IRequestCallback<ServerResponse<RTSInfo>> callback = new IRequestCallback<ServerResponse<RTSInfo>>() {
            @Override
            public void onSuccess(ServerResponse<RTSInfo> response) {
                RTSInfo data = response == null ? null : response.getData();
                if (data == null || !data.isValid()) {
                    onError(-1, "");
                    return;
                }
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.setClass(Utilities.getApplicationContext(), CreateMeetingActivity.class);
                intent.putExtra(KEY_RTM, data);
                activity.startActivity(intent);
                if (doneAction != null) {
                    doneAction.act(null);
                }
            }

            @Override
            public void onError(int errorCode, String message) {
                if (doneAction != null) {
                    doneAction.act(null);
                }
            }
        };
        JoinRTSRequest request = new JoinRTSRequest();
        request.scenesName = Constants.SOLUTION_NAME_ABBR;
        request.loginToken = SolutionDataManager.ins().getToken();
        request.volcAccountId = Constants.ACCOUNT_ID;
        request.vodSpace = Constants.VOD_SPACE;

        JoinRTSManager.setAppInfoAndJoinRTM(request, callback);
    }
}