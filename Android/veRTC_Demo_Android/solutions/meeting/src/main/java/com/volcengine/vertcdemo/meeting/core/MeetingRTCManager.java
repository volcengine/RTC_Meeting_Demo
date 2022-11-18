package com.volcengine.vertcdemo.meeting.core;

import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.ss.bytertc.engine.RTCRoom;
import com.ss.bytertc.engine.RTCRoomConfig;
import com.ss.bytertc.engine.RTCStream;
import com.ss.bytertc.engine.RTCVideo;
import com.ss.bytertc.engine.UserInfo;
import com.ss.bytertc.engine.VideoCanvas;
import com.ss.bytertc.engine.VideoEncoderConfig;
import com.ss.bytertc.engine.data.AudioPropertiesConfig;
import com.ss.bytertc.engine.data.AudioRoute;
import com.ss.bytertc.engine.data.CameraId;
import com.ss.bytertc.engine.data.LocalAudioPropertiesInfo;
import com.ss.bytertc.engine.data.MirrorType;
import com.ss.bytertc.engine.data.RemoteAudioPropertiesInfo;
import com.ss.bytertc.engine.data.ScreenMediaType;
import com.ss.bytertc.engine.data.StreamIndex;
import com.ss.bytertc.engine.type.AudioScenarioType;
import com.ss.bytertc.engine.type.ChannelProfile;
import com.ss.bytertc.engine.type.LocalStreamStats;
import com.ss.bytertc.engine.type.MediaStreamType;
import com.ss.bytertc.engine.type.RTCRoomStats;
import com.ss.bytertc.engine.type.RemoteAudioStats;
import com.ss.bytertc.engine.type.RemoteStreamStats;
import com.ss.bytertc.engine.type.RemoteVideoStats;
import com.ss.bytertc.engine.type.SubscribeFallbackOptions;
import com.ss.bytertc.engine.video.ScreenSharingParameters;
import com.ss.video.rtc.demo.basic_module.utils.AppExecutors;
import com.ss.video.rtc.demo.basic_module.utils.Utilities;
import com.volcengine.vertcdemo.common.MLog;
import com.volcengine.vertcdemo.core.SolutionDataManager;
import com.volcengine.vertcdemo.core.eventbus.SDKJoinChannelSuccessEvent;
import com.volcengine.vertcdemo.core.eventbus.SolutionDemoEventManager;
import com.volcengine.vertcdemo.core.net.rts.RTCRoomEventHandlerWithRTS;
import com.volcengine.vertcdemo.core.net.rts.RTCVideoEventHandlerWithRTS;
import com.volcengine.vertcdemo.core.net.rts.RTSInfo;
import com.volcengine.vertcdemo.meeting.bean.MeetingUserInfo;
import com.volcengine.vertcdemo.meeting.event.SDKRTCStatEvent;
import com.volcengine.vertcdemo.meeting.event.SDKAudioPropertiesEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MeetingRTCManager {

    private static final String TAG = "MeetingRTCManager";

    public static final int VOLUME_INTERVAL_MS = 1000;
    public static final int VOLUME_SMOOTH = 8;

    private static boolean sIsFront;

    private final RTCVideoEventHandlerWithRTS mRTCVideoEventHandler = new RTCVideoEventHandlerWithRTS() {

        private SDKAudioPropertiesEvent.SDKAudioProperties mLocalProperties = null; // 本地音量记录

        /**
         * 本地音频包括使用 RTC SDK 内部机制采集的麦克风音频和屏幕音频。
         * @param audioPropertiesInfos 本地音频信息，详见 LocalAudioPropertiesInfo 。
         */
        @Override
        public void onLocalAudioPropertiesReport(LocalAudioPropertiesInfo[] audioPropertiesInfos) {
            super.onLocalAudioPropertiesReport(audioPropertiesInfos);
            if (audioPropertiesInfos == null) {
                return;
            }
            for (LocalAudioPropertiesInfo info : audioPropertiesInfos) {
                if (info.streamIndex == StreamIndex.STREAM_INDEX_MAIN) {
                    SDKAudioPropertiesEvent.SDKAudioProperties properties = new SDKAudioPropertiesEvent.SDKAudioProperties(
                            SolutionDataManager.ins().getUserId(),
                            info.audioPropertiesInfo);
                    mLocalProperties = properties;
                    List<SDKAudioPropertiesEvent.SDKAudioProperties> audioPropertiesList = new ArrayList<>();
                    audioPropertiesList.add(properties);
                    SolutionDemoEventManager.post(new SDKAudioPropertiesEvent(audioPropertiesList));
                    return;
                }
            }
        }

        /**
         * 远端用户的音频包括使用 RTC SDK 内部机制/自定义机制采集的麦克风音频和屏幕音频。
         * @param audioPropertiesInfos 远端音频信息，其中包含音频流属性、房间 ID、用户 ID ，详见 RemoteAudioPropertiesInfo。
         * @param totalRemoteVolume 订阅的所有远端流的总音量。
         */
        @Override
        public void onRemoteAudioPropertiesReport(RemoteAudioPropertiesInfo[] audioPropertiesInfos, int totalRemoteVolume) {
            super.onRemoteAudioPropertiesReport(audioPropertiesInfos, totalRemoteVolume);
            if (audioPropertiesInfos == null) {
                return;
            }
            List<SDKAudioPropertiesEvent.SDKAudioProperties> audioPropertiesList = new ArrayList<>();
            if (mLocalProperties != null) {
                audioPropertiesList.add(mLocalProperties);
            }
            for (RemoteAudioPropertiesInfo info : audioPropertiesInfos) {
                if (info.streamKey.getStreamIndex() == StreamIndex.STREAM_INDEX_MAIN) {
                    audioPropertiesList.add(new SDKAudioPropertiesEvent.SDKAudioProperties(
                            info.streamKey.getUserId(),
                            info.audioPropertiesInfo));
                }
            }
            SolutionDemoEventManager.post(new SDKAudioPropertiesEvent(audioPropertiesList));
        }

        @Override
        public void onError(int err) {
            Log.e(TAG, "onError:" + err);
        }
    };

    private final RTCRoomEventHandlerWithRTS mRTCRoomEventHandler = new RTCRoomEventHandlerWithRTS() {

        private RemoteAudioStats mAudioStats = new RemoteAudioStats();
        private RemoteVideoStats mVideoStats = new RemoteVideoStats();

        @Override
        public void onRoomStateChanged(String roomId, String uid, int state, String extraInfo) {
            super.onRoomStateChanged(roomId, uid, state, extraInfo);
            Log.d(TAG, String.format("onRoomStateChanged: %s, %s, %d, %s", roomId, uid, state, extraInfo));
            if (isFirstJoinRoomSuccess(state, extraInfo)) {
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
                MLog.d(TAG, "onStreamAdd: " + stream);
                String screenUid = MeetingDataManager.getScreenShareUid();
                if (TextUtils.equals(screenUid, stream.userId) && stream.isScreen) {
                    List<MeetingUserInfo> users = MeetingDataManager.getAllMeetingUserInfoList();
                    String userName = "";
                    for (MeetingUserInfo userInfo : users) {
                        if (TextUtils.equals(userInfo.userId, stream.userId)) {
                            userName = userInfo.userName;
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
                    if (TextUtils.equals(stream.userId, info.userId)) {
                        if (stream.isScreen) {
                            subscribeScreen(stream.userId);
                        } else {
                            subscribeStream(stream.userId);
                        }
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
            SolutionDemoEventManager.post(new SDKRTCStatEvent(sb.toString()));
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
    };

    private static MeetingRTCManager sInstance = null;
    private MeetingRTSClient mRTSClient;

    private RTCVideo mRTCVideo;
    private RTCRoom mRTCRoom;

    private MeetingRTCManager() {}

    public static MeetingRTCManager ins() {
        if (sInstance == null) {
            sInstance = new MeetingRTCManager();
        }
        return sInstance;
    }

    public MeetingRTSClient getRTSClient() {
        return mRTSClient;
    }

    public void rtcConnect(RTSInfo rtmInfo) {
        initEngine(rtmInfo.appId, rtmInfo.bid);
        mRTSClient = new MeetingRTSClient(mRTCVideo, rtmInfo);
        mRTCVideoEventHandler.setBaseClient(mRTSClient);
        mRTCRoomEventHandler.setBaseClient(mRTSClient);
        mRTSClient.login(rtmInfo.rtmToken, (resultCode, message) ->
                Log.d(TAG, String.format("notifyLoginResult: %d  %s", resultCode, message)));
    }

    private void initEngine(String appId, String bid) {
        Log.d(TAG, String.format("initEngine: appId: %s", appId));
        destroyEngine();
        mRTCVideo = RTCVideo.createRTCVideo(Utilities.getApplicationContext(), appId, mRTCVideoEventHandler, null, null);
        mRTCVideo.setBusinessId(bid);
        // 设置音频场景类型
        mRTCVideo.setAudioScenario(AudioScenarioType.AUDIO_SCENARIO_COMMUNICATION);
        enableLocalAudio(true);
        setRemoteSubscribeFallbackOption(SubscribeFallbackOptions.SUBSCRIBE_FALLBACK_OPTIONS_AUDIO_ONLY);
        setLocalVideoMirrorMode(MirrorType.MIRROR_TYPE_RENDER_AND_ENCODER);
        enableAudioVolumeIndication(VOLUME_INTERVAL_MS, VOLUME_SMOOTH);
    }

    public void destroyEngine() {
        MLog.d(TAG, "destroyEngine");
        if (mRTCRoom != null) {
            mRTCRoom.destroy();
        }
        if (mRTCVideo != null) {
            RTCVideo.destroyRTCVideo();
            mRTCVideo = null;
        }
    }

    public void muteLocalAudioStream(boolean mute) {
        MLog.d(TAG, "muteLocalAudioStream: " + mute);
        if (mRTCRoom == null) {
            return;
        }
        if (mute) {
            mRTCRoom.unpublishStream(MediaStreamType.RTC_MEDIA_STREAM_TYPE_AUDIO);
        } else {
            mRTCRoom.publishStream(MediaStreamType.RTC_MEDIA_STREAM_TYPE_AUDIO);
        }
    }

    public void enableLocalAudio(boolean enable) {
        MLog.d(TAG, "enableLocalAudio: " + enable);
        if (mRTCVideo == null) {
            return;
        }
        if (enable) {
            mRTCVideo.startAudioCapture();
        } else {
            mRTCVideo.stopAudioCapture();
        }
    }

    public void enableLocalVideo(boolean enable) {
        MLog.d(TAG, "enableLocalVideo: " + enable);
        if (mRTCVideo == null) {
            return;
        }
        if (enable) {
            mRTCVideo.startVideoCapture();
        } else {
            mRTCVideo.stopVideoCapture();
        }
    }

    public void startVideoCapture() {
        MLog.d(TAG, "startVideoCapture");
        if (mRTCVideo == null) {
            return;
        }
        mRTCVideo.startVideoCapture();
    }

    public void stopVideoCapture() {
        MLog.d("stopVideoCapture", "");
        if (mRTCVideo == null) {
            return;
        }
        mRTCVideo.stopVideoCapture();
    }

    public void setLocalVideoMirrorMode(MirrorType type) {
        MLog.d(TAG, "setLocalVideoMirrorMode: " + type);
        if (mRTCVideo == null) {
            return;
        }
        mRTCVideo.setLocalVideoMirrorType(type);
    }

    public void setVideoEncoderConfig(VideoEncoderConfig videoEncoderConfig) {
        MLog.d(TAG, "setVideoEncoderConfig: " + videoEncoderConfig);
        if (mRTCVideo == null) {
            return;
        }
        mRTCVideo.setVideoEncoderConfig(videoEncoderConfig);
    }

    public void setScreenShareProfiles(VideoEncoderConfig videoEncoderConfig) {
        MLog.d(TAG, "setScreenShareProfiles: " + videoEncoderConfig);
        if (mRTCVideo == null) {
            return;
        }
        mRTCVideo.setScreenVideoEncoderConfig(videoEncoderConfig);
    }

    public void setLocalVideoCanvas(VideoCanvas canvas) {
        MLog.d(TAG, "setLocalVideoCanvas");
        if (mRTCVideo == null) {
            return;
        }
        mRTCVideo.setLocalVideoCanvas(StreamIndex.STREAM_INDEX_MAIN, canvas);
    }

    public void setupRemoteVideo(VideoCanvas canvas) {
        MLog.d(TAG, "setupRemoteVideo: " + canvas.uid);
        if (mRTCVideo == null) {
            return;
        }
        mRTCVideo.setRemoteVideoCanvas(canvas.uid, StreamIndex.STREAM_INDEX_MAIN, canvas);
    }

    public void setupRemoteScreen(VideoCanvas canvas) {
        MLog.d(TAG, "setupRemoteScreen: " + canvas.uid);
        if (mRTCVideo == null) {
            return;
        }
        mRTCVideo.setRemoteVideoCanvas(canvas.uid, StreamIndex.STREAM_INDEX_SCREEN, canvas);
    }

    public void joinRoom(String token, String roomId, String userId) {
        MLog.d(TAG, String.format("joinRoom: %s  %s  %s", token, roomId, userId));
        leaveRoom();
        if (mRTCVideo == null) {
            return;
        }
        mRTCRoom = mRTCVideo.createRTCRoom(roomId);
        mRTCRoom.setRTCRoomEventHandler(mRTCRoomEventHandler);
        mRTCRoomEventHandler.setBaseClient(mRTSClient);
        UserInfo userInfo = new UserInfo(userId, null);
        RTCRoomConfig roomConfig = new RTCRoomConfig(ChannelProfile.CHANNEL_PROFILE_COMMUNICATION,
                true, true, false);
        mRTCRoom.joinRoom(token, userInfo, roomConfig);
    }

    public void leaveRoom() {
        MLog.d(TAG, "leaveRoom");
        if (mRTCRoom != null) {
            mRTCRoom.leaveRoom();
            mRTCRoom.destroy();
        }
    }

    public void switchCamera() {
        MLog.d(TAG, "switchCamera");
        if (mRTCVideo == null) {
            return;
        }
        sIsFront = !sIsFront;
        mRTCVideo.switchCamera(sIsFront ? CameraId.CAMERA_ID_FRONT : CameraId.CAMERA_ID_BACK);
    }

    public void setEnableSpeakerphone(boolean open) {
        MLog.d(TAG, "setEnableSpeakerphone: " + open);
        if (mRTCVideo == null) {
            return;
        }
        mRTCVideo.setAudioRoute(open
                ? AudioRoute.AUDIO_ROUTE_SPEAKERPHONE
                : AudioRoute.AUDIO_ROUTE_EARPIECE);
    }

    public void enableAudioVolumeIndication(int interval, int smooth) {
        MLog.d(TAG, String.format(Locale.ENGLISH, "enableAudioVolumeIndication: %d  %d", interval ,smooth));
        if (mRTCVideo == null) {
            return;
        }
        AudioPropertiesConfig config = new AudioPropertiesConfig(interval);
        mRTCVideo.enableAudioPropertiesReport(config);
    }

    public void subscribeStream(String uid) {
        MLog.d(TAG, "subscribeStream: " + uid);
        if (mRTCRoom == null) {
            return;
        }
        mRTCRoom.subscribeStream(uid, MediaStreamType.RTC_MEDIA_STREAM_TYPE_VIDEO);
    }

    public void subscribeScreen(String uid) {
        MLog.d(TAG, "subscribeScreen: " + uid);
        if (mRTCRoom == null) {
            return;
        }
        mRTCRoom.subscribeScreen(uid, MediaStreamType.RTC_MEDIA_STREAM_TYPE_VIDEO);
    }

    public void unSubscribeStream(String uid) {
        MLog.d(TAG, "unSubscribeStream: " + uid);
        if (mRTCRoom == null) {
            return;
        }
        mRTCRoom.unsubscribeStream(uid, MediaStreamType.RTC_MEDIA_STREAM_TYPE_VIDEO);
    }

    public void unSubscribeScreen(String uid) {
        MLog.d(TAG, "unSubscribeScreen: " + uid);
        if (mRTCRoom == null) {
            return;
        }
        mRTCRoom.unsubscribeScreen(uid, MediaStreamType.RTC_MEDIA_STREAM_TYPE_VIDEO);
    }

    public void setRemoteSubscribeFallbackOption(SubscribeFallbackOptions options) {
        MLog.d(TAG, "setRemoteSubscribeFallbackOption: " + options.toString());
        if (mRTCVideo == null) {
            return;
        }
        mRTCVideo.setSubscribeFallbackOption(options);
    }

    public void startScreenSharing(Intent intent, ScreenSharingParameters params) {
        MLog.d(TAG, "startScreenCapture: " + params);
        if (mRTCVideo != null) {
            VideoEncoderConfig config = new VideoEncoderConfig();
            config.frameRate = params.frameRate;
            config.height = params.maxHeight;
            config.width = params.maxWidth;
            config.maxBitrate = params.bitrate;
            mRTCVideo.setScreenVideoEncoderConfig(config);
            mRTCVideo.startScreenCapture(ScreenMediaType.SCREEN_MEDIA_TYPE_VIDEO_AND_AUDIO, intent);
            if (mRTCRoom != null) {
                mRTCRoom.publishScreen(MediaStreamType.RTC_MEDIA_STREAM_TYPE_BOTH);
            }
        }
    }

    public void stopScreenSharing() {
        MLog.d(TAG, "stopScreenSharing");
        if (mRTCRoom != null) {
            mRTCRoom.unpublishScreen(MediaStreamType.RTC_MEDIA_STREAM_TYPE_BOTH);
        }
    }

    public void feedback(int grade, int type, String description) {
        MLog.d(TAG, String.format(Locale.ENGLISH, "feedback: %d  %d  %s", grade, type, description));
    }
}
