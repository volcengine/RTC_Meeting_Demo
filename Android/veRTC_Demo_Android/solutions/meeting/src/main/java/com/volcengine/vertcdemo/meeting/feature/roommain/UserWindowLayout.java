package com.volcengine.vertcdemo.meeting.feature.roommain;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ss.video.rtc.demo.basic_module.utils.Utilities;
import com.volcengine.vertcdemo.core.AudioVideoConfig;
import com.volcengine.vertcdemo.meeting.core.MeetingDataManager;
import com.volcengine.vertcdemo.meeting.core.MeetingRTCManager;
import com.volcengine.vertcdemo.meeting.bean.MeetingUserInfo;
import com.volcengine.vertcdemo.meeting.bean.VideoCanvasWrapper;
import com.volcengine.vertcdemo.meeting.R;

public class UserWindowLayout extends RelativeLayout {
    public MeetingUserInfo mMeetingUserInfo;
    public View mTalkStatus;
    public View mBigTalkStatus;
    public TextView mUidPrefix;
    public TextView mBigUid;
    public TextView mSmallUid;
    public TextView mBigHostTag;
    public TextView mSmallHostTag;
    public ImageView mBigScreenShareTag;
    public ImageView mSmallScreenShareTag;
    public FrameLayout mVideoContainer;
    public LinearLayout mBigTags;
    public LinearLayout mSmallTags;

    private final Runnable mAutoDismissRunnable = () -> {
        if (mMeetingUserInfo == null) {
            return;
        }
        if (mMeetingUserInfo.isMicOn && MeetingDataManager.getUserVolume(mMeetingUserInfo.userId)
                >= AudioVideoConfig.VOLUME_OVERFLOW_THRESHOLD) {
            if (mMeetingUserInfo.isCameraOn) {
                mBigTalkStatus.setVisibility(View.INVISIBLE);
                mTalkStatus.setVisibility(View.VISIBLE);
            } else {
                mBigTalkStatus.setVisibility(View.VISIBLE);
                mTalkStatus.setVisibility(View.INVISIBLE);
            }
        } else {
            mBigTalkStatus.setVisibility(View.INVISIBLE);
            mTalkStatus.setVisibility(View.INVISIBLE);
        }
    };

    public UserWindowLayout(Context context) {
        this(context, null);
    }

    public UserWindowLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public UserWindowLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.layout_user_window, this);
        mTalkStatus = findViewById(R.id.meeting_user_talking);
        mBigTalkStatus = findViewById(R.id.meeting_user_big_highlight);
        mUidPrefix = findViewById(R.id.meeting_user_big_uid);
        mBigUid = findViewById(R.id.meeting_user_big_full_id);
        mSmallUid = findViewById(R.id.meeting_user_small_full_id);
        mBigScreenShareTag = findViewById(R.id.meeting_user_big_share_screen_status);
        mSmallScreenShareTag = findViewById(R.id.meeting_user_small_share_screen_status);
        mBigHostTag = findViewById(R.id.meeting_user_big_host_status);
        mSmallHostTag = findViewById(R.id.meeting_user_small_host_status);
        mVideoContainer = findViewById(R.id.meeting_view_container);
        mBigTags = findViewById(R.id.meeting_user_big_tags);
        mSmallTags = findViewById(R.id.meeting_user_small_tags);

        mBigTags.setVisibility(View.GONE);
        mSmallTags.setVisibility(View.GONE);
    }

    public void bind(MeetingUserInfo info) {
        if (info == null) {
            mTalkStatus.setVisibility(View.INVISIBLE);
            mBigTalkStatus.setVisibility(View.INVISIBLE);
            mUidPrefix.setVisibility(View.VISIBLE);
            mBigUid.setVisibility(View.VISIBLE);
            mBigTags.setVisibility(View.GONE);
            mSmallTags.setVisibility(View.GONE);
            mUidPrefix.setText("");
            mBigUid.setText("");
            mMeetingUserInfo = null;
            mVideoContainer.removeAllViews();
        } else {
            String uid = info.userId;
            String userName = info.userName;
            String originUid = mMeetingUserInfo == null ? "" : mMeetingUserInfo.userId;
            if (uid == null || !uid.equals(originUid) || !info.isCameraOn) {
                mVideoContainer.removeAllViews();
            }
            if (info.isCameraOn) {
                mUidPrefix.setVisibility(View.GONE);
                mBigUid.setVisibility(View.GONE);
                mBigTags.setVisibility(View.GONE);
                mSmallUid.setVisibility(View.VISIBLE);
                mSmallUid.setText(userName);
                mSmallTags.setVisibility(View.VISIBLE);
                mSmallHostTag.setVisibility(info.isHost ? View.VISIBLE : View.GONE);
                mSmallScreenShareTag.setVisibility(info.isSharing ? View.VISIBLE : View.GONE);

                if (mVideoContainer.getChildCount() == 0) {
                    VideoCanvasWrapper wrapper = MeetingDataManager.getRenderView(uid);
                    if (wrapper == null) {
                        wrapper = MeetingDataManager.addOrUpdateRenderView(uid, true);
                    }
                    if (MeetingDataManager.isSelf(uid)) {
                        MeetingRTCManager.ins().setLocalVideoCanvas(wrapper.videoCanvas);
                    } else {
                        wrapper.videoCanvas.roomId = info.roomId;
                        MeetingRTCManager.ins().setupRemoteVideo(wrapper.videoCanvas);
                    }
                    SurfaceView view = wrapper.getSurfaceView();
                    if (view != null && view.getParent() != mVideoContainer) {
                        Utilities.removeFromParent(view);
                        mVideoContainer.addView(view, new FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT));
                    }
                }
            } else {
                mUidPrefix.setVisibility(View.VISIBLE);
                mBigUid.setVisibility(View.VISIBLE);
                mBigTags.setVisibility(View.VISIBLE);
                mUidPrefix.setText(userName == null ? "" : userName.substring(0, 1));
                mBigUid.setText(userName);
                mBigHostTag.setVisibility(info.isHost ? View.VISIBLE : View.GONE);
                mBigScreenShareTag.setVisibility(info.isSharing ? View.VISIBLE : View.GONE);
                mSmallTags.setVisibility(View.GONE);
                mVideoContainer.removeAllViews();
            }

            mTalkStatus.removeCallbacks(mAutoDismissRunnable);
            if (info.isMicOn) {
                if (MeetingDataManager.getUserVolume(info.userId) >= AudioVideoConfig.VOLUME_OVERFLOW_THRESHOLD) {
                    if (info.isCameraOn) {
                        mBigTalkStatus.setVisibility(View.INVISIBLE);
                        mTalkStatus.setVisibility(View.VISIBLE);
                    } else {
                        mBigTalkStatus.setVisibility(View.VISIBLE);
                        mTalkStatus.setVisibility(View.INVISIBLE);
                    }
                    mTalkStatus.postDelayed(mAutoDismissRunnable, MeetingRTCManager.VOLUME_INTERVAL_MS);
                } else {
                    mTalkStatus.setVisibility(View.INVISIBLE);
                    mBigTalkStatus.setVisibility(View.INVISIBLE);
                }
            } else {
                mBigTalkStatus.setVisibility(View.INVISIBLE);
                mTalkStatus.setVisibility(View.INVISIBLE);
            }

            mMeetingUserInfo = info.deepCopy(new MeetingUserInfo());
        }
    }
}
