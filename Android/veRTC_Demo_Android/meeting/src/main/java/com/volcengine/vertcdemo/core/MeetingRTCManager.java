package com.volcengine.vertcdemo.core;

import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.ss.bytertc.engine.PublisherConfiguration;
import com.ss.bytertc.engine.RTCEngine;
import com.ss.bytertc.engine.RTCStream;
import com.ss.bytertc.engine.SubscribeConfig;
import com.ss.bytertc.engine.UserInfo;
import com.ss.bytertc.engine.VideoCanvas;
import com.ss.bytertc.engine.VideoEncoderConfig;
import com.ss.bytertc.engine.VideoStreamDescription;
import com.ss.bytertc.engine.data.AudioPlaybackDevice;
import com.ss.bytertc.engine.data.CameraId;
import com.ss.bytertc.engine.data.MirrorMode;
import com.ss.bytertc.engine.data.MuteState;
import com.ss.bytertc.engine.data.ScreenMediaType;
import com.ss.bytertc.engine.data.StreamIndex;
import com.ss.bytertc.engine.handler.IRTCEngineEventHandler;
import com.ss.bytertc.engine.video.ScreenSharingParameters;
import com.ss.video.rtc.demo.basic_module.utils.AppExecutors;
import com.ss.video.rtc.demo.basic_module.utils.Utilities;
import com.volcengine.vertcdemo.common.MLog;
import com.volcengine.vertcdemo.core.eventbus.RTCStatEvent;
import com.volcengine.vertcdemo.core.eventbus.MeetingVolumeEvent;
import com.volcengine.vertcdemo.core.eventbus.SDKJoinChannelSuccessEvent;
import com.volcengine.vertcdemo.core.eventbus.SolutionDemoEventManager;
import com.volcengine.vertcdemo.bean.MeetingUserInfo;
import com.volcengine.vertcdemo.core.net.rtm.RTCEventHandlerWithRTM;
import com.volcengine.vertcdemo.core.net.rtm.RtmInfo;

import java.util.List;

public class MeetingRTCManager {

    private static final String TAG = "MeetingRTCManager";

    public static final int VOLUME_INTERVAL_MS = 1000;
    public static final int VOLUME_SMOOTH = 8;

    private static boolean sIsFront;

    private static final RTCEventHandlerWithRTM sRtcEventHandler = new RTCEventHandlerWithRTM() {

        private RemoteAudioStats mAudioStats = new RemoteAudioStats();
        private RemoteVideoStats mVideoStats = new RemoteVideoStats();

        @Override
        public void onRoomStateChanged(String roomId, String uid, int state, String extraInfo) {
            super.onRoomStateChanged(roomId, uid, state, extraInfo);
            Log.d(TAG, String.format("onRoomStateChanged: %s, %s, %d, %s", roomId, uid, state, extraInfo));
            if (joinRoomSuccessWhenFirst(state,extraInfo)) {
                SolutionDemoEventManager.post(new SDKJoinChannelSuccessEvent(roomId, uid));
            }
        }

        @Override
        public void onStreamAdd(RTCStream stream) {
            super.onStreamAdd(stream);
            AppExecutors.mainThread().execute(() -> {
                if (stream == null || TextUtils.isEmpty(stream.userId)) {
                    return;
                }
                MLog.d("onStreamAdd", stream.toString());
                String screenUid = MeetingDataManager.getScreenShareUid();
                if (TextUtils.equals(screenUid, stream.userId) && stream.isScreen) {
                    List<MeetingUserInfo> users = MeetingDataManager.getAllMeetingUserInfoList();
                    String userName = "";
                    for (MeetingUserInfo userInfo : users) {
                        if (TextUtils.equals(userInfo.user_id, stream.userId)) {
                            userName = userInfo.user_name;
                            break;
                        }
                    }
                    MeetingDataManager.addOrUpdateScreenView(stream.userId, userName, true);
                    return;
                }
                List<MeetingUserInfo> users = MeetingDataManager.getMeetingShowingUserInfoList();
                if (users.isEmpty()) {
                    return;
                }
                for (MeetingUserInfo info : users) {
                    if (info == null) {
                        continue;
                    }
                    if (TextUtils.equals(stream.userId, info.user_id)) {
                        SubscribeConfig config = new SubscribeConfig(stream.isScreen, true, true, 0);
                        subscribeStream(stream.userId, config);
                        return;
                    }
                }
            });
        }

        @Override
        public void onRoomStats(RTCRoomStats stats) {
            super.onRoomStats(stats);
            if (!MeetingDataManager.sSettingsConfigEntity.enableLog) {
                return;
            }
            Pair<Integer, Integer> resolution = MeetingDataManager.sSettingsConfigEntity.getResolution();
            int frameRate = MeetingDataManager.sSettingsConfigEntity.getFrameRate();
            StringBuilder sb = new StringBuilder();
            sb.append("[LOCAL]").append("\n");
            sb.append("RES: ").append(resolution.second).append("*").append(resolution.first).append("\n");
            sb.append("FPS: ").append(frameRate).append("\n");
            sb.append("BIT(VIDEO): ").append(stats.txVideoKBitRate).append("kbps").append("\n");
            sb.append("BIT(AUDIO): ").append(stats.txAudioKBitRate).append("kbps").append("\n");
            sb.append("\n");
            sb.append("[REMOTE]").append("\n");
            sb.append("RTT(VIDEO): ").append(mVideoStats.rtt).append("ms").append("\n");
            sb.append("RTT(AUDIO): ").append(mAudioStats.rtt).append("ms").append("\n");
            sb.append("CPU: ").append(stats.cpuAppUsage).append("%|").append(stats.cpuTotalUsage).append("%").append("\n");
            sb.append("BIT(VIDEO): ").append(mVideoStats.receivedKBitrate).append("kbps").append("\n");
            sb.append("BIT(AUDIO): ").append(mAudioStats.receivedKBitrate).append("kbps").append("\n");
            sb.append("RES: ").append(mVideoStats.width).append("*").append(mVideoStats.height).append("\n");
            sb.append("FPS: ").append(mVideoStats.rendererOutputFrameRate).append("-").append(mVideoStats.receivedKBitrate).append("\n");
            sb.append("LOSS（VIDEO）: ").append(mVideoStats.videoLossRate).append("%").append("\n");
            sb.append("LOSS(AUDIO): ").append(mAudioStats.audioLossRate).append("%").append("\n");
            SolutionDemoEventManager.post(new RTCStatEvent(sb.toString()));
        }

        @Override
        public void onRemoteStreamStats(RemoteStreamStats stats) {
            super.onRemoteStreamStats(stats);
            mAudioStats = stats.audioStats;
            mVideoStats = stats.videoStats;
        }

        @Override
        public void onLocalStreamStats(LocalStreamStats stats) {
            super.onLocalStreamStats(stats);
            SolutionDemoEventManager.post(stats.audioStats);
        }

        @Override
        public void onAudioVolumeIndication(IRTCEngineEventHandler.AudioVolumeInfo[] speakers, int totalVolume) {
            if (speakers == null || speakers.length == 0) {
                return;
            }
            SolutionDemoEventManager.post(new MeetingVolumeEvent(speakers, totalVolume));
        }

        @Override
        public void onError(int err) {
            Log.e("RTCManager", "onError:" + err);
        }
    };


    private static MeetingRTCManager sInstance = null;
    private static RTCEngine mEngine;
    public static MeetingRTCManager ins() {
        if (sInstance == null) {
            sInstance = new MeetingRTCManager();
        }
        return sInstance;
    }

    public MeetingRtmClient getRTMClient() {
        return mRTMClient;
    }

    private RtmInfo mRtmInfo;
    private MeetingRtmClient mRTMClient;
    public void rtcConnect(RtmInfo rtmInfo) {
        mRtmInfo = rtmInfo;
        initEngine(rtmInfo.appId);
        mRTMClient = new MeetingRtmClient(mEngine, rtmInfo);
        sRtcEventHandler.setBaseClient(mRTMClient);
        mRTMClient.login(mRtmInfo.rtmToken, (resultCode, message) ->
                        Log.d("MeetingRTCManager", String.format("notifyLoginResult: %d  %s", resultCode, message)));
    }

    public void initEngine(String appId) {
        Log.d(TAG, String.format("initEngine: appId: %s", appId));
        destroyEngine();
        mEngine = RTCEngine.createEngine(Utilities.getApplicationContext(), appId, sRtcEventHandler, null, null);
        enableAutoSubscribe(true, false);
        enableLocalAudio(true);
        setRemoteSubscribeFallbackOption(RTCEngine.SubscribeFallbackOptions.SUBSCRIBE_FALLBACK_OPTIONS_AUDIO_ONLY);
        setLocalVideoMirrorMode(MirrorMode.MIRROR_MODE_ON);
        enableAudioVolumeIndication(VOLUME_INTERVAL_MS, VOLUME_SMOOTH);
    }

    public static void destroyEngine() {
        MLog.d("destroyEngine", "");
        if (mEngine == null) {
            return;
        }
        mEngine = null;
        RTCEngine.destroyEngine(mEngine);
        RTCEngine.destroy();
    }

    public static void enableAutoSubscribe(boolean audioSubscribe, boolean videoSubscribe) {
        MLog.d("enableAutoSubscribe", "audioSubscribe :" + audioSubscribe + ", videoSubscribe: " + videoSubscribe);
        if (mEngine == null) {
            return;
        }
        RTCEngine.SubscribeMode audioMode = audioSubscribe ? RTCEngine.SubscribeMode.AUTO_SUBSCRIBE_MODE : RTCEngine.SubscribeMode.MANUAL_SUBSCRIBE_MODE;
        RTCEngine.SubscribeMode videoMode = videoSubscribe ? RTCEngine.SubscribeMode.AUTO_SUBSCRIBE_MODE : RTCEngine.SubscribeMode.MANUAL_SUBSCRIBE_MODE;
        mEngine.enableAutoSubscribe(audioMode, videoMode);
    }

    public static void muteLocalAudioStream(boolean mute) {
        MLog.d("muteLocalAudioStream", "");
        if (mEngine == null) {
            return;
        }
        mEngine.muteLocalAudio(mute ? MuteState.MUTE_STATE_ON : MuteState.MUTE_STATE_OFF);
    }

    public static void enableLocalAudio(boolean enable) {
        MLog.d("enableLocalAudio", "");
        if (mEngine == null) {
            return;
        }
        if (enable) {
            mEngine.startAudioCapture();
        } else {
            mEngine.stopAudioCapture();
        }
    }

    public static void enableLocalVideo(boolean enable) {
        MLog.d("enableLocalVideo", "");
        if (mEngine == null) {
            return;
        }
        if (enable) {
            mEngine.startVideoCapture();
        } else {
            mEngine.stopVideoCapture();
        }
    }

    public static void startPreview() {
        MLog.d("startPreview", "");
        if (mEngine == null) {
            return;
        }
        mEngine.startVideoCapture();
    }

    public static void stopPreview() {
        MLog.d("stopPreview", "");
        if (mEngine == null) {
            return;
        }
        mEngine.stopVideoCapture();
    }

    public static void setLocalVideoMirrorMode(MirrorMode mode) {
        MLog.d("setLocalVideoMirrorMode", "" + mode);
        if (mEngine == null) {
            return;
        }
        mEngine.setLocalVideoMirrorMode(mode);
    }

    public static void setVideoProfiles(List<VideoStreamDescription> descriptions) {
        MLog.d("setVideoProfiles", "");
        if (mEngine == null) {
            return;
        }
        mEngine.setVideoEncoderConfig(descriptions);
    }

    public static void setScreenShareProfiles(VideoEncoderConfig description) {
        MLog.d("setScreenShareProfiles", "" + description);
        if (mEngine == null) {
            return;
        }
        mEngine.setScreenVideoEncoderConfig(description);
    }

    public static void setupLocalVideo(VideoCanvas canvas) {
        MLog.d("setupLocalVideo", "");
        if (mEngine == null) {
            return;
        }
        mEngine.setLocalVideoCanvas(StreamIndex.STREAM_INDEX_MAIN, canvas);
    }

    public static void setupRemoteVideo(VideoCanvas canvas) {
        MLog.d("setupRemoteVideo", ":" + canvas.uid);
        if (mEngine == null) {
            return;
        }
        mEngine.setRemoteVideoCanvas(canvas.uid, StreamIndex.STREAM_INDEX_MAIN, canvas);
    }

    public static void setupRemoteScreen(VideoCanvas canvas) {
        MLog.d("setupRemoteScreen", ":" + canvas.uid);
        if (mEngine == null) {
            return;
        }
        mEngine.setRemoteVideoCanvas(canvas.uid, StreamIndex.STREAM_INDEX_SCREEN, canvas);
    }

    public static void joinChannel(String token, String roomId, PublisherConfiguration configuration, String uid) {
        MLog.d("joinChannel", "token:" + token + " roomId:" + roomId + " uid:" + uid);
        if (mEngine == null) {
            return;
        }
        UserInfo userInfo = new UserInfo(uid, null);
        mEngine.joinRoom(token, roomId, userInfo, RTCEngine.ChannelProfile.CHANNEL_PROFILE_COMMUNICATION);
    }

    public static void leaveChannel() {
        MLog.d("leaveChannel", "");
        if (mEngine == null) {
            return;
        }
        mEngine.leaveRoom();
    }

    public static void switchCamera() {
        MLog.d("switchCamera", "");
        if (mEngine == null) {
            return;
        }
        sIsFront = !sIsFront;
        mEngine.switchCamera(sIsFront ? CameraId.CAMERA_ID_FRONT : CameraId.CAMERA_ID_BACK);
    }

    public static void setEnableSpeakerphone(boolean open) {
        MLog.d("setEnableSpeakerphone", "open");
        if (mEngine == null) {
            return;
        }
        mEngine.setAudioPlaybackDevice(open
                ? AudioPlaybackDevice.AUDIO_PLAYBACK_DEVICE_SPEAKERPHONE
                : AudioPlaybackDevice.AUDIO_PLAYBACK_DEVICE_EARPIECE);
    }

    public static void enableAudioVolumeIndication(int interval, int smooth) {
        MLog.d("enableAudioVolumeIndication", "" + interval + ":" + smooth);
        if (mEngine == null) {
            return;
        }
        mEngine.setAudioVolumeIndicationInterval(interval);
    }

    public static void subscribeStream(String uid, SubscribeConfig config) {
        MLog.d("subscribeStream", "" + uid + ":" + config.toString());
        if (mEngine == null) {
            return;
        }
        mEngine.subscribeStream(uid, config);
    }

    public static void unSubscribe(String uid, boolean isScreen) {
        MLog.d("unSubscribe", "" + uid + ":" + isScreen);
        if (mEngine == null) {
            return;
        }
        mEngine.unSubscribe(uid, isScreen);
    }

    public static void setRemoteSubscribeFallbackOption(RTCEngine.SubscribeFallbackOptions options) {
        MLog.d("setRemoteSubscribeFallbackOption", "" + options.toString());
        if (mEngine == null) {
            return;
        }
        mEngine.setSubscribeFallbackOption(options);
    }

    public static void startScreenSharing(Intent intent, ScreenSharingParameters params) {
        if (mEngine != null) {
            VideoEncoderConfig config = new VideoEncoderConfig();
            config.frameRate = params.frameRate;
            config.height = params.maxHeight;
            config.width = params.maxWidth;
            config.maxBitrate = params.bitrate;
            mEngine.setScreenVideoEncoderConfig(config);
            mEngine.startScreenCapture(ScreenMediaType.SCREEN_MEDIA_TYPE_VIDEO_AND_AUDIO, intent);
            mEngine.publishScreen(RTCEngine.MediaStreamType.RTC_MEDIA_STREAM_TYPE_BOTH);
        }
        MLog.d("startScreenCapture", "startScreenSharing");
    }

    public static int stopScreenSharing() {
        int res;
        if (mEngine != null) {
            res = mEngine.unpublishScreen(RTCEngine.MediaStreamType.RTC_MEDIA_STREAM_TYPE_BOTH);
        } else {
            res = -1;
        }
        MLog.d("stopScreenSharing", "" + res);
        return res;
    }


    public static int feedback(int grade, int type, String description) {
        int res = 0;
        MLog.d("feedback", "grade:" + grade + ", " + "type: " + type + ", " + "description:" + description + ", " + "res: " + res);
        return res;
    }
}
