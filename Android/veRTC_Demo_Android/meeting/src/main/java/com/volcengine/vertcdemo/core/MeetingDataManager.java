package com.volcengine.vertcdemo.core;

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

import com.ss.bytertc.engine.SubscribeConfig;
import com.ss.bytertc.engine.VideoCanvas;
import com.ss.bytertc.engine.handler.IRTCEngineEventHandler;
import com.ss.bytertc.engine.video.ScreenSharingParameters;
import com.ss.video.rtc.demo.basic_module.utils.SafeToast;
import com.ss.video.rtc.demo.basic_module.utils.Utilities;
import com.volcengine.vertcdemo.bean.HostChangedInfo;
import com.volcengine.vertcdemo.bean.MeetingUserInfo;
import com.volcengine.vertcdemo.bean.MeetingUsersInfo;
import com.volcengine.vertcdemo.bean.SettingsConfigEntity;
import com.volcengine.vertcdemo.bean.VideoCanvasWrapper;
import com.volcengine.vertcdemo.common.MLog;
import com.volcengine.vertcdemo.core.eventbus.CameraStatusChangedEvent;
import com.volcengine.vertcdemo.core.eventbus.MeetingVolumeEvent;
import com.volcengine.vertcdemo.core.eventbus.MicStatusChangeEvent;
import com.volcengine.vertcdemo.core.eventbus.MuteAllEvent;
import com.volcengine.vertcdemo.core.eventbus.MuteEvent;
import com.volcengine.vertcdemo.core.eventbus.RecordEvent;
import com.volcengine.vertcdemo.core.eventbus.RefreshRoomUserEvent;
import com.volcengine.vertcdemo.core.eventbus.ShareScreenEvent;
import com.volcengine.vertcdemo.core.eventbus.SolutionDemoEventManager;
import com.volcengine.vertcdemo.core.eventbus.SpeakerStatusChangedEvent;
import com.volcengine.vertcdemo.core.eventbus.UserJoinEvent;
import com.volcengine.vertcdemo.core.eventbus.UserLeaveEvent;
import com.volcengine.vertcdemo.core.eventbus.VolumeEvent;
import com.volcengine.vertcdemo.core.net.IRequestCallback;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.webrtc.RXScreenCaptureService;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
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

    private static final List<String> sSubscribeList = new LinkedList<>();

    private static VideoCanvasWrapper sMyWrapper;
    private static final HashMap<String, VideoCanvasWrapper> sUidVideoViewMap = new LinkedHashMap<>();
    private static final HashMap<String, VideoCanvasWrapper> sUidScreenViewMap = new LinkedHashMap<>();

    private static MeetingDataManager sMeetingInstance = null;

    public static SettingsConfigEntity sSettingsConfigEntity = new SettingsConfigEntity();

    private static final Comparator<IRTCEngineEventHandler.AudioVolumeInfo> sVolumeComparator = new Comparator<IRTCEngineEventHandler.AudioVolumeInfo>() {

        @Override
        public int compare(IRTCEngineEventHandler.AudioVolumeInfo o1, IRTCEngineEventHandler.AudioVolumeInfo o2) {
            int volume0 = o1 == null ? 0 : o1.linearVolume;
            int volume1 = o2 == null ? 0 : o2.linearVolume;
            int res = Integer.compare(volume1, volume0); //声音降序
            if (res == 0) {
                String uid1 = o1 == null ? "" : o1.uid;
                String uid2 = o2 == null ? "" : o2.uid;
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
        sMyUserInfo.user_id = SolutionDataManager.ins().getUserId();
        sMyUserInfo.user_name = sSelfUserName;
        sMyUserInfo.user_uniform_name = sSelfUniformName;
        sMyUserInfo.is_host = isSelfHost();
        sMyUserInfo.is_mic_on = sMicStatus;
        sMyUserInfo.is_camera_on = sCameraStatus;
        sMyUserInfo.is_sharing = !TextUtils.isEmpty(SolutionDataManager.ins().getUserId()) && isSelf(sScreenShareUid);
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
        sMyUserInfo.is_mic_on = sMicStatus;

        MeetingUserInfo aInfo = mMeetingUserInfoMap.get(SolutionDataManager.ins().getUserId());
        if (aInfo != null) {
            aInfo.is_mic_on = sMicStatus;
        }
        for (MeetingUserInfo info : sLastShowingList) {
            if (TextUtils.equals(info.user_id, SolutionDataManager.ins().getUserId())) {
                info.is_mic_on = sMicStatus;
            }
        }


        MeetingRTCManager.ins().getRTMClient().updateMicStatus(sMeetingId, sMicStatus, null);
        SolutionDemoEventManager.post(new MicStatusChangeEvent(SolutionDataManager.ins().getUserId(), sMicStatus));
        MeetingRTCManager.muteLocalAudioStream(!sMicStatus);
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
        sMyUserInfo.is_camera_on = sCameraStatus;

        MeetingUserInfo aInfo = mMeetingUserInfoMap.get(SolutionDataManager.ins().getUserId());
        if (aInfo != null) {
            aInfo.is_camera_on = sCameraStatus;
        }

        for (MeetingUserInfo info : sLastShowingList) {
            if (TextUtils.equals(info.user_id, SolutionDataManager.ins().getUserId())) {
                info.is_camera_on = sCameraStatus;
            }
        }

        MeetingRTCManager.ins().getRTMClient().updateCameraStatus(sMeetingId, sCameraStatus, null);
        SolutionDemoEventManager.post(new CameraStatusChangedEvent(SolutionDataManager.ins().getUserId(), sCameraStatus));
        MeetingRTCManager.enableLocalVideo(sCameraStatus);
    }

    public static void switchSpeaker() {
        sSpeakerStatus = !sSpeakerStatus;
        MeetingRTCManager.setEnableSpeakerphone(sSpeakerStatus);
        SolutionDemoEventManager.post(new SpeakerStatusChangedEvent(sSpeakerStatus));
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

    private static final List<MeetingUserInfo> sLastShowingList = new LinkedList<>();

    public static void updateUserVolume(IRTCEngineEventHandler.AudioVolumeInfo[] speakers) {
        //sort by volume's value
        Arrays.sort(speakers, sVolumeComparator);
        if (speakers.length >= 1) {
            if (speakers[0].linearVolume >= AudioVideoConfig.VOLUME_MIN_THRESHOLD) {
                speakers[0].linearVolume = AudioVideoConfig.VOLUME_OVERFLOW_THRESHOLD;
                sLoudestUid = speakers[0].uid;
            }
        }

        LinkedList<MeetingUserInfo> meetingUserInfoList = new LinkedList<>();

        MeetingUserInfo myInfo = getMyUserInfo();
        meetingUserInfoList.add(myInfo);

        sUidVolumeMap.clear();
        for (IRTCEngineEventHandler.AudioVolumeInfo info : speakers) {
            if (info == null || info.linearVolume < AudioVideoConfig.VOLUME_MIN_THRESHOLD) {
                continue;
            }
            if (meetingUserInfoList.size() >= MAX_SHOW_USER_COUNT) {
                break;
            }

            sUidVolumeMap.put(info.uid, info.linearVolume);

            if (info != null && !containsInfo(meetingUserInfoList, info.uid)) {
                MeetingUserInfo userInfo = mMeetingUserInfoMap.get(info.uid);
                if (userInfo != null) {
                    userInfo.volume = info.linearVolume;
                    meetingUserInfoList.add(userInfo);
                }
            }
        }

        if (sUidVolumeMap.containsKey(SolutionDataManager.ins().getUserId()) && TextUtils.equals(SolutionDataManager.ins().getUserId(), meetingUserInfoList.get(0).user_id)) {
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
            if (info != null && !containsInfo(meetingUserInfoList, info.user_id)) {
                MeetingUserInfo userInfo = mMeetingUserInfoMap.get(info.user_id);
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
            if (info != null && !containsInfo(meetingUserInfoList, info.user_id)) {
                MeetingUserInfo userInfo = mMeetingUserInfoMap.get(info.user_id);
                if (userInfo != null) {
                    userInfo.volume = 0;
                    meetingUserInfoList.add(userInfo);
                }
            }
        }

        LinkedList<String> uidList = new LinkedList<>();
        String uid;
        for (MeetingUserInfo info : meetingUserInfoList) {
            uid = info.user_id;
            uidList.add(uid);

            if (!sSubscribeList.contains(info.user_id)) {
                MeetingRTCManager.subscribeStream(uid, new SubscribeConfig(
                        false, true, true, 0));
            }
        }
        for (String subs : sSubscribeList) {
            if (!uidList.contains(subs)) {
                MeetingRTCManager.unSubscribe(subs, false);
            }
        }
        sSubscribeList.clear();
        sSubscribeList.addAll(uidList);

        if (sIsRecordMeeting && isSelfHost()) {

            MeetingRTCManager.ins().getRTMClient().requestUpdateRecordLayout(
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
                if (info.user_id == null && uid == null) {
                    return true;
                }
                if (info.user_id != null && info.user_id.equals(uid)) {
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
        MeetingRTCManager.ins().getRTMClient().getRoomUser(sMeetingId, new IRequestCallback<MeetingUsersInfo>() {
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
                        mMeetingUserInfoMap.put(info.user_id, info);
                        if (info.is_sharing) {
                            sScreenShareUid = info.user_id;
                            sScreenShareUserName = info.user_name;
                        }
                        if (info.is_host) {
                            sHostUid = info.user_id;
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
                        MeetingRTCManager.subscribeStream(info.user_id,
                                new SubscribeConfig(false, true, true, 0));
                        addOrUpdateRenderView(info.user_id, true);

                        if (info.is_sharing) {
                            addOrUpdateScreenView(info.user_id, info.user_name, true);
                        }
                    }

                    for (MeetingUserInfo temp : sLastShowingList) {
                        if (temp != null && isSelf(temp.user_id)) {
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
        return info == null || !(info.is_mic_on);
    }

    public static List<MeetingUserInfo> getAllMeetingUserInfoList() {
        return new LinkedList<>(mMeetingUserInfoMap.values());
    }

    public static List<MeetingUserInfo> getMeetingShowingUserInfoList() {
        return new LinkedList<>(sLastShowingList);
    }

    public static MeetingUserInfo getSecondUserInfo() {
        for (MeetingUserInfo info : sLastShowingList) {
            if (info != null && !isSelf(info.user_id)) {
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

        MeetingRTCManager.subscribeStream(uid, new SubscribeConfig(true, true, false, 0));
        MeetingRTCManager.setupRemoteScreen(wrapper.videoCanvas);

        sUidScreenViewMap.put(uid, wrapper);
        return wrapper;
    }

    public static void removeScreenView(String uid) {
        if (sScreenShareUid.equals(uid)) {
            sScreenShareUid = "";
            sScreenShareUserName = "";
        }
        sUidScreenViewMap.remove(uid);
        MeetingRTCManager.unSubscribe(uid, true);
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
        sMyUserInfo.is_camera_on = false;
        sMyUserInfo.is_sharing = true;

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
        MeetingRTCManager.startScreenSharing(intent, parameters);

        MeetingRTCManager.ins().getRTMClient().requestScreenShare(sMeetingId, true, null);
    }

    public static void stopScreenSharing() {
        sScreenShareUid = null;
        sScreenShareUserName = null;
        sMyUserInfo.is_camera_on = sCameraStatus;
        sMyUserInfo.is_sharing = false;

        SolutionDemoEventManager.post(new ShareScreenEvent(false,
                SolutionDataManager.ins().getUserId(),
                SolutionDataManager.ins().getUserName()));
        MeetingRTCManager.stopScreenSharing();

        MeetingRTCManager.ins().getRTMClient().requestScreenShare(sMeetingId, false, null);

        MeetingRTCManager.enableLocalVideo(sCameraStatus);
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
            MeetingRTCManager.switchCamera();
            sIsFrontCamera = !sIsFrontCamera;
        }
    }

    public static void startMeetingRecord() {
        if (sIsRecordMeeting) {
            return;
        }
        sIsRecordMeeting = true;
        MeetingRTCManager.ins().getRTMClient().requestRecordMeeting(sMeetingId, sLastShowingList,
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
        if (userInfo == null || TextUtils.isEmpty(userInfo.user_id)) {
            return;
        }

        Log.d("onUserJoined", userInfo.toString());

        mMeetingUserInfoMap.put(userInfo.user_id, userInfo);

        if (!containsInfo(sLastShowingList, userInfo.user_id) && sLastShowingList.size() < MAX_SHOW_USER_COUNT) {
            sLastShowingList.add(userInfo);
            MeetingRTCManager.subscribeStream(userInfo.user_id, new SubscribeConfig(false, true, true, 0));
            addOrUpdateRenderView(userInfo.user_id, userInfo.is_camera_on);
            SolutionDemoEventManager.post(new RefreshRoomUserEvent());
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUserLeave(UserLeaveEvent event) {
        if (event == null) {
            return;
        }
        MeetingUserInfo userInfo = event.meetingUserInfo;
        if (userInfo == null || TextUtils.isEmpty(userInfo.user_id)) {
            return;
        }

        String uid = userInfo.user_id;
        mMeetingUserInfoMap.remove(uid);
        for (MeetingUserInfo info : sLastShowingList) {
            if (info != null && TextUtils.equals(info.user_id, uid)) {
                sLastShowingList.remove(info);
                SolutionDemoEventManager.post(new RefreshRoomUserEvent());
                break;
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onHostChangedInfo(HostChangedInfo event) {
        sHostUid = event.currentUid;
        MeetingUserInfo aFormerInfo = mMeetingUserInfoMap.get(event.formerUid);
        if (aFormerInfo != null) {
            aFormerInfo.is_host = false;
        }
        MeetingUserInfo aCurrentInfo = mMeetingUserInfoMap.get(event.currentUid);
        if (aCurrentInfo != null) {
            aCurrentInfo.is_host = true;
        }

        for (MeetingUserInfo info : sLastShowingList) {
            if (info != null && TextUtils.equals(info.user_id, event.formerUid)) {
                info.is_host = false;
            }
            if (info != null && TextUtils.equals(info.user_id, event.currentUid)) {
                info.is_host = true;
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onShareScreenEvent(ShareScreenEvent event) {
        MeetingUserInfo aInfo = mMeetingUserInfoMap.get(sScreenShareUid);
        if (aInfo != null) {
            aInfo.is_sharing = false;
        }
        MeetingUserInfo sInfo = mMeetingUserInfoMap.get(sScreenShareUid);
        if (sInfo != null) {
            sInfo.is_sharing = false;
        }
        sScreenShareUid = event.status ? event.userId : "";
        sScreenShareUserName = event.status ? event.userName : "";

        MeetingUserInfo aaInfo = mMeetingUserInfoMap.get(event.userId);
        if (aaInfo != null) {
            aaInfo.is_sharing = event.status;
        }
        MeetingUserInfo ssInfo = mMeetingUserInfoMap.get(event.userId);
        if (ssInfo != null) {
            ssInfo.is_sharing = event.status;
            sScreenShareUserName = ssInfo.user_name;
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
            if (info != null && TextUtils.equals(info.user_id, uid)) {
                info.is_mic_on = event.status;
            }
        }
        MeetingUserInfo aInfo = mMeetingUserInfoMap.get(uid);
        if (aInfo != null) {
            aInfo.is_mic_on = event.status;
        }
        for (MeetingUserInfo info : sLastShowingList) {
            if (info != null && uid.equals(info.user_id)) {
                info.is_mic_on = event.status;
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
            if (info != null && TextUtils.equals(info.user_id, uid)) {
                info.is_camera_on = event.status;
            }
        }
        MeetingUserInfo aInfo = mMeetingUserInfoMap.get(uid);
        if (aInfo != null) {
            aInfo.is_camera_on = event.status;
        }
        for (MeetingUserInfo info : sLastShowingList) {
            if (info != null && uid.equals(info.user_id)) {
                info.is_camera_on = event.status;
            }
        }
        SolutionDemoEventManager.post(new RefreshRoomUserEvent());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMuteAllEvent(MuteAllEvent event) {
        for (MeetingUserInfo info : mMeetingUserInfoMap.values()) {
            if (!TextUtils.isEmpty(sHostUid) && info != null && !sHostUid.equals(info.user_id)) {
                info.is_mic_on = false;
            }
        }
        for (MeetingUserInfo info : sLastShowingList) {
            if (!TextUtils.isEmpty(sHostUid) && info != null && !sHostUid.equals(info.user_id)) {
                info.is_mic_on = false;
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRecordEvent(RecordEvent event) {
        sIsRecordMeeting = event.isStart;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRTCVolumeEvent(MeetingVolumeEvent event) {
        updateUserVolume(event.speakers);
    }
}
