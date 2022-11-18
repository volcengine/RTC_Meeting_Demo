package com.volcengine.vertcdemo.meeting.core;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.SurfaceView;

import androidx.annotation.DrawableRes;

import com.ss.bytertc.engine.VideoCanvas;
import com.ss.bytertc.engine.video.ScreenSharingParameters;
import com.ss.video.rtc.demo.basic_module.utils.SafeToast;
import com.ss.video.rtc.demo.basic_module.utils.Utilities;
import com.volcengine.vertcdemo.meeting.event.HostChangedEvent;
import com.volcengine.vertcdemo.meeting.bean.MeetingUserInfo;
import com.volcengine.vertcdemo.meeting.bean.MeetingUsersInfo;
import com.volcengine.vertcdemo.meeting.bean.SettingsConfigEntity;
import com.volcengine.vertcdemo.meeting.bean.VideoCanvasWrapper;
import com.volcengine.vertcdemo.common.MLog;
import com.volcengine.vertcdemo.core.AudioVideoConfig;
import com.volcengine.vertcdemo.core.SolutionDataManager;
import com.volcengine.vertcdemo.meeting.event.CameraStatusChangedEvent;
import com.volcengine.vertcdemo.meeting.event.MicStatusChangeEvent;
import com.volcengine.vertcdemo.meeting.event.MuteAllEvent;
import com.volcengine.vertcdemo.meeting.event.MuteEvent;
import com.volcengine.vertcdemo.meeting.event.RecordEvent;
import com.volcengine.vertcdemo.core.eventbus.RefreshRoomUserEvent;
import com.volcengine.vertcdemo.meeting.event.SDKAudioPropertiesEvent;
import com.volcengine.vertcdemo.meeting.event.ShareScreenEvent;
import com.volcengine.vertcdemo.core.eventbus.SolutionDemoEventManager;
import com.volcengine.vertcdemo.meeting.event.SDKSpeakerStatusChangedEvent;
import com.volcengine.vertcdemo.meeting.event.UserJoinEvent;
import com.volcengine.vertcdemo.meeting.event.UserLeaveEvent;
import com.volcengine.vertcdemo.core.eventbus.VolumeEvent;
import com.volcengine.vertcdemo.core.net.IRequestCallback;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.webrtc.RXScreenCaptureService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Meeting Data Manager.
 * The class manager all data of meeting.while the class is bridge between user and server.
 * The class transfers User's requests and movements to server.
 * When data update,the class send event to UI component, then UI component should update themselves.
 */
public class MeetingDataManager {

    public static final int MAX_SHOW_USER_COUNT = 6;

    public static final int REQUEST_CODE_OF_SCREEN_SHARING = 101;

    private static String sSelfUserName = "";
    private static String sSelfUniformName = "";
    private static String sHostUid = "";
    private static String sMeetingId = "";
    private static String sLoudestUid = "";
    private static String sScreenShareUid = null;
    private static String sScreenShareUserName = null;
    private static boolean sMicStatus = true;
    private static boolean sCameraStatus = true;
    private static boolean sSpeakerStatus = false;
    private static boolean sIsFrontCamera = true;
    private static boolean sIsRecordMeeting = false;

    private static final MeetingUserInfo sMyUserInfo = new MeetingUserInfo();
    private static final Map<String, MeetingUserInfo> mMeetingUserInfoMap = new LinkedHashMap<>();

    private static final Map<String, Integer> sUidVolumeMap = new LinkedHashMap<>();

    private static final List<String> sSubscribeList = new ArrayList<>();

    private static VideoCanvasWrapper sMyWrapper;
    private static final HashMap<String, VideoCanvasWrapper> sUidVideoViewMap = new LinkedHashMap<>();
    private static final HashMap<String, VideoCanvasWrapper> sUidScreenViewMap = new LinkedHashMap<>();

    private static MeetingDataManager sMeetingInstance = null;

    public static SettingsConfigEntity sSettingsConfigEntity = new SettingsConfigEntity();

    private static final Comparator<SDKAudioPropertiesEvent.SDKAudioProperties> sVolumeComparator
            = new Comparator<SDKAudioPropertiesEvent.SDKAudioProperties>() {

        @Override
        public int compare(SDKAudioPropertiesEvent.SDKAudioProperties o1, SDKAudioPropertiesEvent.SDKAudioProperties o2) {
            int volume0 = o1 == null ? 0 : o1.audioPropertiesInfo.linearVolume;
            int volume1 = o2 == null ? 0 : o2.audioPropertiesInfo.linearVolume;
            int res = Integer.compare(volume1, volume0); //声音降序
            if (res == 0) {
                String uid1 = o1 == null ? "" : o1.userId;
                String uid2 = o2 == null ? "" : o2.userId;
                return compareString(uid1, uid2); //名字升序
            } else {
                return res;
            }
        }

        public int compareString(String o1, String o2) {
            char[] chars1 = o1.toLowerCase().toCharArray();
            char[] chars2 = o2.toLowerCase().toCharArray();
            int i = 0;
            while (i < chars1.length && i < chars2.length) {
                if (chars1[i] > chars2[i]) {
                    return 1;
                } else if (chars1[i] < chars2[i]) {
                    return -1;
                } else {
                    i++;
                }
            }
            if (i == chars1.length) {
                return -1;
            }
            if (i == chars2.length) {
                return 1;
            }
            return 0;
        }
    };

    private MeetingDataManager() {
    }

    public static void init() {
        sSelfUserName = "";
        sSelfUniformName = "";
        sHostUid = "";
        sMeetingId = "";
        sLoudestUid = "";
        sScreenShareUid = null;
        sScreenShareUserName = null;
        sMicStatus = true;
        sCameraStatus = true;
        sSpeakerStatus = false;
        sIsRecordMeeting = false;
        mMeetingUserInfoMap.clear();
        sLastShowingList.clear();
        sUidVolumeMap.clear();

        sMeetingInstance = new MeetingDataManager();
        SolutionDemoEventManager.register(sMeetingInstance);

        VideoCanvas canvas = new VideoCanvas(new SurfaceView(Utilities.getApplicationContext()),
                VideoCanvas.RENDER_MODE_HIDDEN, "", true);
        sMyWrapper = new VideoCanvasWrapper(canvas, true);
    }

    public static void release() {
        SolutionDemoEventManager.unregister(sMeetingInstance);
        sMeetingInstance = null;
    }

    public static MeetingUserInfo getMyUserInfo() {
        sMyUserInfo.userId = SolutionDataManager.ins().getUserId();
        sMyUserInfo.userName = sSelfUserName;
        sMyUserInfo.userUniformName = sSelfUniformName;
        sMyUserInfo.isHost = isSelfHost();
        sMyUserInfo.isMicOn = sMicStatus;
        sMyUserInfo.isCameraOn = sCameraStatus;
        sMyUserInfo.isSharing = !TextUtils.isEmpty(SolutionDataManager.ins().getUserId()) && isSelf(sScreenShareUid);
        return sMyUserInfo.deepCopy(new MeetingUserInfo());
    }

    public static String getSelfId() {
        return SolutionDataManager.ins().getUserId();
    }

    public static boolean isRecording() {
        return sIsRecordMeeting;
    }

    public static boolean isInMeeting() {
        return !TextUtils.isEmpty(sMeetingId);
    }

    public static boolean isSelf(String uid) {
        return !TextUtils.isEmpty(SolutionDataManager.ins().getUserId()) && SolutionDataManager.ins().getUserId().equals(uid);
    }

    public static void setHostUid(String uid) {
        sHostUid = uid;
    }

    public static String getHostUid() {
        return sHostUid;
    }

    public static boolean isSelfHost() {
        return !TextUtils.isEmpty(SolutionDataManager.ins().getUserId()) && SolutionDataManager.ins().getUserId().equals(sHostUid);
    }

    public static String getLoudestUid() {
        return sLoudestUid;
    }

    public static void startMeeting(String uid, String name, String uniformName, String meetingId) {
        sSelfUserName = name;
        sSelfUniformName = uniformName;
        sMeetingId = meetingId;
        mMeetingUserInfoMap.clear();
        sLastShowingList.clear();
        sUidVolumeMap.clear();
    }

    public static void endMeeting() {
        if (isSelf(sScreenShareUid)) {
            stopScreenSharing();
        }
        if (!sIsFrontCamera) {
            switchCameraType(false);
        }
        if (!sSpeakerStatus) {
            switchSpeaker();
        }
        sHostUid = "";
        sMeetingId = "";
        sScreenShareUid = "";
        sScreenShareUserName = "";
        sLoudestUid = "";
        sIsRecordMeeting = false;
        mMeetingUserInfoMap.clear();
        sUidScreenViewMap.clear();
        sUidVideoViewMap.clear();
        sLastShowingList.clear();
        sUidVolumeMap.clear();
    }

    public static String getMeetingId(){
        return sMeetingId;
    }


    public static void switchMic(boolean isFromUser) {
        if (!hasAudioPermission()) {
            if (isFromUser) {
                SafeToast.show("麦克风权限已关闭，请至设备设置页开启。");
            }
            if (!sMicStatus) {
                return;
            }
        }

        sMicStatus = !sMicStatus;
        sMyUserInfo.isMicOn = sMicStatus;

        MeetingUserInfo aInfo = mMeetingUserInfoMap.get(SolutionDataManager.ins().getUserId());
        if (aInfo != null) {
            aInfo.isMicOn = sMicStatus;
        }
        for (MeetingUserInfo info : sLastShowingList) {
            if (TextUtils.equals(info.userId, SolutionDataManager.ins().getUserId())) {
                info.isMicOn = sMicStatus;
            }
        }


        MeetingRTCManager.ins().getRTSClient().updateMicStatus(sMeetingId, sMicStatus, null);
        SolutionDemoEventManager.post(new MicStatusChangeEvent(SolutionDataManager.ins().getUserId(), sMicStatus));
        MeetingRTCManager.ins().muteLocalAudioStream(!sMicStatus);
    }

    public static void switchCamera(boolean isFromUser) {
        if (!hasVideoPermission()) {
            if (isFromUser) {
                SafeToast.show("摄像头权限已关闭，请至设备设置页开启。");
            }
            if (!sCameraStatus) {
                return;
            }
        }

        if (isSelf(sScreenShareUid) && !sCameraStatus) {
            SafeToast.show("结束共享后方可打开摄像头");
            return;
        }

        sCameraStatus = !sCameraStatus;
        sMyWrapper.showVideo = sCameraStatus;
        sMyUserInfo.isCameraOn = sCameraStatus;

        MeetingUserInfo aInfo = mMeetingUserInfoMap.get(SolutionDataManager.ins().getUserId());
        if (aInfo != null) {
            aInfo.isCameraOn = sCameraStatus;
        }

        for (MeetingUserInfo info : sLastShowingList) {
            if (TextUtils.equals(info.userId, SolutionDataManager.ins().getUserId())) {
                info.isCameraOn = sCameraStatus;
            }
        }

        MeetingRTCManager.ins().getRTSClient().updateCameraStatus(sMeetingId, sCameraStatus, null);
        SolutionDemoEventManager.post(new CameraStatusChangedEvent(SolutionDataManager.ins().getUserId(), sCameraStatus));
        MeetingRTCManager.ins().enableLocalVideo(sCameraStatus);
    }

    public static void switchSpeaker() {
        sSpeakerStatus = !sSpeakerStatus;
        MeetingRTCManager.ins().setEnableSpeakerphone(sSpeakerStatus);
        SolutionDemoEventManager.post(new SDKSpeakerStatusChangedEvent(sSpeakerStatus));
    }

    public static boolean getMicStatus() {
        return sMicStatus && hasAudioPermission();
    }

    public static boolean getCameraStatus() {
        return sCameraStatus && hasVideoPermission();
    }

    public static boolean hasSomeoneScreenShare() {
        return !TextUtils.isEmpty(sScreenShareUid);
    }

    public static String getScreenShareUid() {
        return sScreenShareUid;
    }

    public static String getScreenShareUserName() {
        return sScreenShareUserName;
    }

    private static final List<MeetingUserInfo> sLastShowingList = new ArrayList<>();

    public static void updateUserVolume(SDKAudioPropertiesEvent event) {
        SDKAudioPropertiesEvent.SDKAudioProperties[] speakers = event.audioPropertiesList.toArray(
                new SDKAudioPropertiesEvent.SDKAudioProperties[0]);
        //sort by volume's value
        Arrays.sort(speakers, sVolumeComparator);
        if (speakers.length >= 1) {
            if (speakers[0].audioPropertiesInfo.linearVolume >= AudioVideoConfig.VOLUME_MIN_THRESHOLD) {
                speakers[0].audioPropertiesInfo.linearVolume = AudioVideoConfig.VOLUME_OVERFLOW_THRESHOLD;
                sLoudestUid = speakers[0].userId;
            }
        }

        ArrayList<MeetingUserInfo> meetingUserInfoList = new ArrayList<>();

        MeetingUserInfo myInfo = getMyUserInfo();
        meetingUserInfoList.add(myInfo);

        sUidVolumeMap.clear();
        for (SDKAudioPropertiesEvent.SDKAudioProperties info : speakers) {
            if (info == null || info.audioPropertiesInfo.linearVolume < AudioVideoConfig.VOLUME_MIN_THRESHOLD) {
                continue;
            }
            if (meetingUserInfoList.size() >= MAX_SHOW_USER_COUNT) {
                break;
            }

            sUidVolumeMap.put(info.userId, info.audioPropertiesInfo.linearVolume);

            if (!containsInfo(meetingUserInfoList, info.userId)) {
                MeetingUserInfo userInfo = mMeetingUserInfoMap.get(info.userId);
                if (userInfo != null) {
                    userInfo.volume = info.audioPropertiesInfo.linearVolume;
                    meetingUserInfoList.add(userInfo);
                }
            }
        }

        if (sUidVolumeMap.containsKey(SolutionDataManager.ins().getUserId()) && TextUtils.equals(SolutionDataManager.ins().getUserId(), meetingUserInfoList.get(0).userId)) {
            meetingUserInfoList.get(0).volume = sUidVolumeMap.get(SolutionDataManager.ins().getUserId());
        }

        if (!sUidVolumeMap.isEmpty()) {
            MLog.d("updateUserVolume", "" + sUidVolumeMap);
            SolutionDemoEventManager.post(new VolumeEvent(new LinkedHashMap<>(sUidVolumeMap)));
        }

        for (MeetingUserInfo info : sLastShowingList) {
            if (meetingUserInfoList.size() >= MAX_SHOW_USER_COUNT) {
                break;
            }
            if (info != null && !containsInfo(meetingUserInfoList, info.userId)) {
                MeetingUserInfo userInfo = mMeetingUserInfoMap.get(info.userId);
                if (userInfo != null) {
                    userInfo.volume = 0;
                    meetingUserInfoList.add(userInfo);
                }
            }
        }

        for (MeetingUserInfo info : mMeetingUserInfoMap.values()) {
            if (meetingUserInfoList.size() >= MAX_SHOW_USER_COUNT) {
                break;
            }
            if (info != null && !containsInfo(meetingUserInfoList, info.userId)) {
                MeetingUserInfo userInfo = mMeetingUserInfoMap.get(info.userId);
                if (userInfo != null) {
                    userInfo.volume = 0;
                    meetingUserInfoList.add(userInfo);
                }
            }
        }

        ArrayList<String> uidList = new ArrayList<>();
        String uid;
        for (MeetingUserInfo info : meetingUserInfoList) {
            uid = info.userId;
            uidList.add(uid);

            if (!sSubscribeList.contains(info.userId)) {
                MeetingRTCManager.ins().subscribeStream(uid);
            }
        }
        for (String subs : sSubscribeList) {
            if (!uidList.contains(subs)) {
                MeetingRTCManager.ins().unSubscribeStream(subs);
            }
        }
        sSubscribeList.clear();
        sSubscribeList.addAll(uidList);

        if (sIsRecordMeeting && isSelfHost()) {

            MeetingRTCManager.ins().getRTSClient().requestUpdateRecordLayout(
                    sMeetingId, uidList, sScreenShareUid, null);
        }
        sLastShowingList.clear();
        MeetingUserInfo temp;
        for (String userId : uidList) {
            temp = mMeetingUserInfoMap.get(userId);
            if (temp != null) {
                sLastShowingList.add(temp);
            }
        }

        SolutionDemoEventManager.post(new RefreshRoomUserEvent());
    }

    private static boolean containsInfo(List<MeetingUserInfo> infoList, String uid) {
        if (infoList == null || infoList.isEmpty()) {
            return false;
        }
        for (MeetingUserInfo info : infoList) {
            if (info != null) {
                if (info.userId == null && uid == null) {
                    return true;
                }
                if (info.userId != null && info.userId.equals(uid)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static int getUserVolume(String uid) {
        Object volume = sUidVolumeMap.get(uid);
        return volume == null ? 0 : (int) volume;
    }

    public static void initRoomUsers() {
        MeetingRTCManager.ins().getRTSClient().getRoomUser(sMeetingId, new IRequestCallback<MeetingUsersInfo>() {
            @Override
            public void onSuccess(MeetingUsersInfo data) {
                if (data == null) {
                    return;
                }
                List<MeetingUserInfo> userInfoList = data.getUsers();
                if (userInfoList == null) {
                    return;
                }
                synchronized (mMeetingUserInfoMap) {
                    mMeetingUserInfoMap.clear();
                    for (MeetingUserInfo info : userInfoList) {
                        mMeetingUserInfoMap.put(info.userId, info);
                        if (info.isSharing) {
                            sScreenShareUid = info.userId;
                            sScreenShareUserName = info.userName;
                        }
                        if (info.isHost) {
                            sHostUid = info.userId;
                        }
                    }

                    int max = Math.min(userInfoList.size(), MAX_SHOW_USER_COUNT);
                    MeetingUserInfo info = null;
                    for (int i = 0; i < max; i++) {
                        info = userInfoList.get(i);
                        if (info == null) {
                            continue;
                        }
                        sLastShowingList.add(info);
                        MeetingRTCManager.ins().subscribeStream(info.userId);
                        addOrUpdateRenderView(info.userId, true);

                        if (info.isSharing) {
                            addOrUpdateScreenView(info.userId, info.userName, true);
                        }
                    }

                    for (MeetingUserInfo temp : sLastShowingList) {
                        if (temp != null && isSelf(temp.userId)) {
                            info = temp;
                            sLastShowingList.remove(temp);
                            break;
                        }
                    }
                    if (info != null) {
                        sLastShowingList.add(0, info);
                    }
                }
                SolutionDemoEventManager.post(new RefreshRoomUserEvent());
            }

            @Override
            public void onError(int errorCode, String message) {

            }
        });
    }

    public static boolean isUserMute(String uid) {
        MeetingUserInfo info = mMeetingUserInfoMap.get(uid);
        return info == null || !(info.isMicOn);
    }

    public static List<MeetingUserInfo> getAllMeetingUserInfoList() {
        return new ArrayList<>(mMeetingUserInfoMap.values());
    }

    public static List<MeetingUserInfo> getMeetingShowingUserInfoList() {
        return new ArrayList<>(sLastShowingList);
    }

    public static MeetingUserInfo getSecondUserInfo() {
        for (MeetingUserInfo info : sLastShowingList) {
            if (info != null && !isSelf(info.userId)) {
                return info;
            }
        }
        return null;
    }

    public static VideoCanvasWrapper getMyRenderView() {
        return sMyWrapper;
    }

    public static VideoCanvasWrapper getRenderView(String uid) {
        if (isSelf(uid)) {
            return sMyWrapper;
        } else {
            return sUidVideoViewMap.get(uid);
        }
    }

    public static VideoCanvasWrapper addOrUpdateRenderView(String uid, boolean hasVideo) {
        if (TextUtils.isEmpty(uid)) {
            return null;
        }
        VideoCanvasWrapper wrapper = sUidVideoViewMap.get(uid);
        if (wrapper == null) {
            VideoCanvas canvas = new VideoCanvas(new SurfaceView(Utilities.getApplicationContext()),
                    VideoCanvas.RENDER_MODE_HIDDEN, uid, false);
            wrapper = new VideoCanvasWrapper(canvas, hasVideo);
        } else {
            wrapper.showVideo = hasVideo;
        }
        wrapper.videoCanvas.roomId = MeetingDataManager.getMeetingId();
        sUidVideoViewMap.put(uid, wrapper);
        return wrapper;
    }

    public static VideoCanvasWrapper getScreenView() {
        return sUidScreenViewMap.get(sScreenShareUid);
    }

    public static VideoCanvasWrapper addOrUpdateScreenView(String uid, String userName, boolean hasVideo) {
        if (TextUtils.isEmpty(uid)) {
            return null;
        }
        sScreenShareUid = uid;
        sScreenShareUserName = userName;
        VideoCanvasWrapper wrapper = sUidScreenViewMap.get(uid);
        if (wrapper == null) {
            VideoCanvas canvas = new VideoCanvas(new SurfaceView(Utilities.getApplicationContext()),
                    VideoCanvas.RENDER_MODE_FIT, uid, true);
            wrapper = new VideoCanvasWrapper(canvas, hasVideo);
        } else {
            wrapper.showVideo = hasVideo;
        }
        wrapper.videoCanvas.roomId = MeetingDataManager.getMeetingId();
        MeetingRTCManager.ins().subscribeScreen(uid);
        MeetingRTCManager.ins().setupRemoteScreen(wrapper.videoCanvas);

        sUidScreenViewMap.put(uid, wrapper);
        return wrapper;
    }

    public static void removeScreenView(String uid) {
        if (sScreenShareUid.equals(uid)) {
            sScreenShareUid = "";
            sScreenShareUserName = "";
        }
        sUidScreenViewMap.remove(uid);
        MeetingRTCManager.ins().unSubscribeScreen(uid);
    }

    public static void startScreenSharing(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            SafeToast.show("当前系统版本过低，无法支持屏幕共享");
            return;
        }
        MediaProjectionManager manager = (MediaProjectionManager) Utilities.getApplicationContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (manager == null) {
            SafeToast.show("此手机无法开启屏幕分享");
            return;
        }
        activity.startActivityForResult(manager.createScreenCaptureIntent(), REQUEST_CODE_OF_SCREEN_SHARING);
    }

    public static void onScreenSharingIntent(Activity activity,
                                             @DrawableRes int largeIcon,
                                             @DrawableRes int smallIcon,
                                             Intent intent) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            Intent idata = new Intent();
            idata.putExtra(RXScreenCaptureService.KEY_LARGE_ICON, largeIcon);
            idata.putExtra(RXScreenCaptureService.KEY_SMALL_ICON, smallIcon);
            idata.putExtra(RXScreenCaptureService.KEY_CONTENT_TEXT, "正在录制/投射您的屏幕");
            idata.putExtra(RXScreenCaptureService.KEY_LAUNCH_ACTIVITY, activity.getClass().getName());
            idata.putExtra(RXScreenCaptureService.KEY_RESULT_DATA, intent);
            activity.startForegroundService(RXScreenCaptureService.getServiceIntent(activity, RXScreenCaptureService.COMMAND_LAUNCH, idata));
        }

        sScreenShareUid = SolutionDataManager.ins().getUserId();
        sScreenShareUserName = SolutionDataManager.ins().getUserName();
        sMyUserInfo.isCameraOn = false;
        sMyUserInfo.isSharing = true;

        if (sCameraStatus) {
            switchCamera(false);
            SolutionDemoEventManager.post(new CameraStatusChangedEvent(SolutionDataManager.ins().getUserId(), false));
        }
        SolutionDemoEventManager.post(new ShareScreenEvent(true,
                SolutionDataManager.ins().getUserId(),
                SolutionDataManager.ins().getUserName()));

        ScreenSharingParameters parameters = new ScreenSharingParameters();
        Pair<Integer, Integer> resolution = sSettingsConfigEntity.getScreenResolution();
        parameters.maxWidth = resolution.second;
        parameters.maxHeight = resolution.first;
        parameters.frameRate = sSettingsConfigEntity.getScreenFrameRate();
        parameters.bitrate = sSettingsConfigEntity.getScreenBitRate();
        MeetingRTCManager.ins().startScreenSharing(intent, parameters);

        MeetingRTCManager.ins().getRTSClient().requestScreenShare(sMeetingId, true, null);
    }

    public static void stopScreenSharing() {
        sScreenShareUid = null;
        sScreenShareUserName = null;
        sMyUserInfo.isCameraOn = sCameraStatus;
        sMyUserInfo.isSharing = false;

        SolutionDemoEventManager.post(new ShareScreenEvent(false,
                SolutionDataManager.ins().getUserId(),
                SolutionDataManager.ins().getUserName()));
        MeetingRTCManager.ins().stopScreenSharing();

        MeetingRTCManager.ins().getRTSClient().requestScreenShare(sMeetingId, false, null);

        MeetingRTCManager.ins().enableLocalVideo(sCameraStatus);
        SolutionDemoEventManager.post(new CameraStatusChangedEvent(SolutionDataManager.ins().getUserId(), sCameraStatus));
    }

    public static void switchCameraType(boolean isFromUser) {
        if (!hasVideoPermission()) {
            if (isFromUser) {
                SafeToast.show("摄像头权限已关闭，请至设备设置页开启。");
            }
            return;
        }
        if (sCameraStatus) {
            MeetingRTCManager.ins().switchCamera();
            sIsFrontCamera = !sIsFrontCamera;
        }
    }

    public static void startMeetingRecord() {
        if (sIsRecordMeeting) {
            return;
        }
        sIsRecordMeeting = true;
        MeetingRTCManager.ins().getRTSClient().requestRecordMeeting(sMeetingId, sLastShowingList,
                sScreenShareUid, null);
    }

    public static boolean hasAudioPermission() {
        int res = Utilities.getApplicationContext().checkPermission(
                Manifest.permission.RECORD_AUDIO, android.os.Process.myPid(), Process.myUid());
        return res == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasVideoPermission() {
        int res = Utilities.getApplicationContext().checkPermission(
                Manifest.permission.CAMERA, android.os.Process.myPid(), Process.myUid());
        return res == PackageManager.PERMISSION_GRANTED;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUserJoined(UserJoinEvent event) {
        if (event == null) {
            return;
        }
        MeetingUserInfo userInfo = event.meetingUserInfo;
        if (userInfo == null || TextUtils.isEmpty(userInfo.userId)) {
            return;
        }

        Log.d("onUserJoined", userInfo.toString());

        mMeetingUserInfoMap.put(userInfo.userId, userInfo);

        if (!containsInfo(sLastShowingList, userInfo.userId) && sLastShowingList.size() < MAX_SHOW_USER_COUNT) {
            sLastShowingList.add(userInfo);
            MeetingRTCManager.ins().subscribeStream(userInfo.userId);
            addOrUpdateRenderView(userInfo.userId, userInfo.isCameraOn);
            SolutionDemoEventManager.post(new RefreshRoomUserEvent());
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUserLeave(UserLeaveEvent event) {
        if (event == null) {
            return;
        }
        MeetingUserInfo userInfo = event.meetingUserInfo;
        if (userInfo == null || TextUtils.isEmpty(userInfo.userId)) {
            return;
        }

        String uid = userInfo.userId;
        mMeetingUserInfoMap.remove(uid);
        for (MeetingUserInfo info : sLastShowingList) {
            if (info != null && TextUtils.equals(info.userId, uid)) {
                sLastShowingList.remove(info);
                SolutionDemoEventManager.post(new RefreshRoomUserEvent());
                break;
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onHostChangedInfo(HostChangedEvent event) {
        sHostUid = event.currentUid;
        MeetingUserInfo aFormerInfo = mMeetingUserInfoMap.get(event.formerUid);
        if (aFormerInfo != null) {
            aFormerInfo.isHost = false;
        }
        MeetingUserInfo aCurrentInfo = mMeetingUserInfoMap.get(event.currentUid);
        if (aCurrentInfo != null) {
            aCurrentInfo.isHost = true;
        }

        for (MeetingUserInfo info : sLastShowingList) {
            if (info != null && TextUtils.equals(info.userId, event.formerUid)) {
                info.isHost = false;
            }
            if (info != null && TextUtils.equals(info.userId, event.currentUid)) {
                info.isHost = true;
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onShareScreenEvent(ShareScreenEvent event) {
        MeetingUserInfo aInfo = mMeetingUserInfoMap.get(sScreenShareUid);
        if (aInfo != null) {
            aInfo.isSharing = false;
        }
        MeetingUserInfo sInfo = mMeetingUserInfoMap.get(sScreenShareUid);
        if (sInfo != null) {
            sInfo.isSharing = false;
        }
        sScreenShareUid = event.status ? event.userId : "";
        sScreenShareUserName = event.status ? event.userName : "";

        MeetingUserInfo aaInfo = mMeetingUserInfoMap.get(event.userId);
        if (aaInfo != null) {
            aaInfo.isSharing = event.status;
        }
        MeetingUserInfo ssInfo = mMeetingUserInfoMap.get(event.userId);
        if (ssInfo != null) {
            ssInfo.isSharing = event.status;
            sScreenShareUserName = ssInfo.userName;
        }
        if (event.status) {
            addOrUpdateScreenView(sScreenShareUid, sScreenShareUserName, true);
        } else {
            removeScreenView(sScreenShareUid);
        }
        SolutionDemoEventManager.post(new RefreshRoomUserEvent());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMuteEvent(MuteEvent event) {
        if (!sMicStatus) {
            return;
        }
        SafeToast.show("你已被主持人静音");

        switchMic(false);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMicStatusChangeEvent(MicStatusChangeEvent event) {
        String uid = event.userId;
        if (TextUtils.isEmpty(uid)) {
            return;
        }

        for (MeetingUserInfo info : sLastShowingList) {
            if (info != null && TextUtils.equals(info.userId, uid)) {
                info.isMicOn = event.status;
            }
        }
        MeetingUserInfo aInfo = mMeetingUserInfoMap.get(uid);
        if (aInfo != null) {
            aInfo.isMicOn = event.status;
        }
        for (MeetingUserInfo info : sLastShowingList) {
            if (info != null && uid.equals(info.userId)) {
                info.isMicOn = event.status;
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCameraStatusChangedEvent(CameraStatusChangedEvent event) {
        String uid = event.userId;
        if (TextUtils.isEmpty(uid)) {
            return;
        }
        for (MeetingUserInfo info : sLastShowingList) {
            if (info != null && TextUtils.equals(info.userId, uid)) {
                info.isCameraOn = event.status;
            }
        }
        MeetingUserInfo aInfo = mMeetingUserInfoMap.get(uid);
        if (aInfo != null) {
            aInfo.isCameraOn = event.status;
        }
        for (MeetingUserInfo info : sLastShowingList) {
            if (info != null && uid.equals(info.userId)) {
                info.isCameraOn = event.status;
            }
        }
        SolutionDemoEventManager.post(new RefreshRoomUserEvent());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMuteAllEvent(MuteAllEvent event) {
        for (MeetingUserInfo info : mMeetingUserInfoMap.values()) {
            if (!TextUtils.isEmpty(sHostUid) && info != null && !sHostUid.equals(info.userId)) {
                info.isMicOn = false;
            }
        }
        for (MeetingUserInfo info : sLastShowingList) {
            if (!TextUtils.isEmpty(sHostUid) && info != null && !sHostUid.equals(info.userId)) {
                info.isMicOn = false;
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRecordEvent(RecordEvent event) {
        sIsRecordMeeting = event.isStart;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSDKAudioPropertiesEvent(SDKAudioPropertiesEvent event) {
        updateUserVolume(event);
    }
}
