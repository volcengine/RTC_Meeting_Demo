// Copyright (c) 2023 Beijing Volcano Engine Technology Ltd.
// SPDX-License-Identifier: MIT

package com.volcengine.vertcdemo.meeting.core;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.ss.bytertc.engine.RTCVideo;
import com.volcengine.vertcdemo.common.AppExecutors;
import com.volcengine.vertcdemo.meeting.bean.ReconnectResponse;
import com.volcengine.vertcdemo.meeting.event.HostChangedEvent;
import com.volcengine.vertcdemo.meeting.bean.MeetingRecordInfoList;
import com.volcengine.vertcdemo.meeting.bean.MeetingResponse;
import com.volcengine.vertcdemo.meeting.bean.MeetingTokenInfo;
import com.volcengine.vertcdemo.meeting.bean.MeetingUserInfo;
import com.volcengine.vertcdemo.meeting.bean.MeetingUsersInfo;
import com.volcengine.vertcdemo.common.AbsBroadcast;
import com.volcengine.vertcdemo.core.SolutionDataManager;
import com.volcengine.vertcdemo.meeting.event.AskOpenCameraEvent;
import com.volcengine.vertcdemo.meeting.event.AskOpenMicEvent;
import com.volcengine.vertcdemo.meeting.event.CameraStatusChangedEvent;
import com.volcengine.vertcdemo.meeting.event.KickedOffEvent;
import com.volcengine.vertcdemo.meeting.event.MeetingEndEvent;
import com.volcengine.vertcdemo.meeting.event.MicStatusChangeEvent;
import com.volcengine.vertcdemo.meeting.event.MuteAllEvent;
import com.volcengine.vertcdemo.meeting.event.MuteEvent;
import com.volcengine.vertcdemo.meeting.event.RecordEvent;
import com.volcengine.vertcdemo.meeting.event.ShareScreenEvent;
import com.volcengine.vertcdemo.core.eventbus.SolutionDemoEventManager;
import com.volcengine.vertcdemo.meeting.event.UserJoinEvent;
import com.volcengine.vertcdemo.meeting.event.UserLeaveEvent;
import com.volcengine.vertcdemo.core.net.IRequestCallback;
import com.volcengine.vertcdemo.core.net.rts.RTSBaseClient;
import com.volcengine.vertcdemo.core.net.rts.RTSBizInform;
import com.volcengine.vertcdemo.core.net.rts.RTSBizResponse;
import com.volcengine.vertcdemo.core.net.rts.RTSInfo;

import java.util.List;
import java.util.UUID;

public class MeetingRTSClient extends RTSBaseClient {

    private static final String CMD_MEETING_JOIN_MEETING = "vcJoinMeeting";
    private static final String CMD_MEETING_TURN_ON_MIC = "vcTurnOnMic";
    private static final String CMD_MEETING_TURN_OFF_MIC = "vcTurnOffMic";
    private static final String CMD_MEETING_TURN_ON_CAMERA = "vcTurnOnCamera";
    private static final String CMD_MEETING_TURN_OFF_CAMERA = "vcTurnOffCamera";
    private static final String CMD_MEETING_GET_MEETING_USER_INFO = "vcGetMeetingUserInfo";
    private static final String CMD_MEETING_START_SHARE_SCREEN = "vcStartShareScreen";
    private static final String CMD_MEETING_END_SHARE_SCREEN = "vcEndShareScreen";
    private static final String CMD_MEETING_RECORD_MEETING = "vcRecordMeeting";
    private static final String CMD_MEETING_UPDATE_RECORD_LAYOUT = "vcUpdateRecordLayout";
    private static final String CMD_MEETING_GET_HISTORY_VIDEO_RECORD = "vcGetHistoryVideoRecord";
    private static final String CMD_MEETING_DELETE_VIDEO_RECORD = "vcDeleteVideoRecord";
    private static final String CMD_MEETING_CHANGE_HOST = "vcChangeHost";
    private static final String CMD_MEETING_ASK_MIC_ON = "vcAskMicOn";
    private static final String CMD_MEETING_MUTE_USER = "vcMuteUser";
    private static final String CMD_MEETING_LEAVE_MEETING = "vcLeaveMeeting";
    private static final String CMD_MEETING_END_MEETING = "vcEndMeeting";
    private static final String CMD_RECONNECT = "vcReconnect";

    private static final String ON_USER_MIC_STATUS_CHANGE = "onUserMicStatusChange";
    private static final String ON_USER_CAMERA_STATUS_CHANGE = "onUserCameraStatusChange";
    private static final String ON_HOST_CHANGE = "onHostChange";
    private static final String ON_USER_JOIN_MEETING = "onUserJoinMeeting";
    private static final String ON_USER_LEAVE_MEETING = "onUserLeaveMeeting";
    private static final String ON_SHARE_SCREEN_STATUS_CHANGED = "onShareScreenStatusChanged";
    private static final String ON_RECORD = "onRecord";
    private static final String ON_MEETING_END = "onMeetingEnd";
    private static final String ON_MUTE_ALL = "onMuteAll";
    private static final String ON_RECORD_FINISHED = "onRecordFinished";
    private static final String ON_USER_KICKED_OFF = "onUserKickedOff";
    private static final String ON_MUTE_USER = "onMuteUser";
    private static final String ON_ASKING_MIC_ON = "onAskingMicOn";
    private static final String ON_ASKING_CAMERA_ON = "onAskingCameraOn";

    public MeetingRTSClient(@NonNull RTCVideo engine, @NonNull RTSInfo rtmInfo) {
        super(engine, rtmInfo);
        initEventListener();
    }

    public void reconnect(String roomId, IRequestCallback<ReconnectResponse> callback) {
        JsonObject params = getCommonParams(CMD_RECONNECT);
        params.addProperty("login_token", SolutionDataManager.ins().getToken());
        sendServerMessageOnNetwork(roomId, params, ReconnectResponse.class, callback);
    }

    private JsonObject getCommonParams(String cmd) {
        JsonObject params = new JsonObject();
        params.addProperty("app_id", mRTSInfo.appId);
        params.addProperty("room_id", "");
        params.addProperty("user_id", SolutionDataManager.ins().getUserId());
        params.addProperty("user_name", SolutionDataManager.ins().getUserName());
        params.addProperty("event_name", cmd);
        params.addProperty("request_id", UUID.randomUUID().toString());
        params.addProperty("device_id", SolutionDataManager.ins().getDeviceId());
        return params;
    }

    private void initEventListener() {
        putEventListener(new AbsBroadcast<>(ON_USER_MIC_STATUS_CHANGE, MicStatusChangeEvent.class, (data) -> {

            if (MeetingDataManager.isSelf(data.userId)) {
                if (MeetingDataManager.getMicStatus() != data.status) {
                    MeetingDataManager.switchMic(false);
                }
            } else {
                SolutionDemoEventManager.post(data);
            }
        }));
        putEventListener(new AbsBroadcast<>(ON_USER_CAMERA_STATUS_CHANGE, CameraStatusChangedEvent.class, (data) -> {
            if (MeetingDataManager.isSelf(data.userId)) {
                if (MeetingDataManager.getCameraStatus() != data.status) {
                    MeetingDataManager.switchCamera(false);
                }
            } else {
                SolutionDemoEventManager.post(data);
            }
        }));
        putEventListener(new AbsBroadcast<>(ON_HOST_CHANGE, HostChangedEvent.class,
                SolutionDemoEventManager::post));
        putEventListener(new AbsBroadcast<>(ON_USER_JOIN_MEETING, MeetingUserInfo.class,
                (data) -> SolutionDemoEventManager.post(new UserJoinEvent(data))));
        putEventListener(new AbsBroadcast<>(ON_USER_LEAVE_MEETING, MeetingUserInfo.class,
                (data) -> SolutionDemoEventManager.post(new UserLeaveEvent(data))));
        putEventListener(new AbsBroadcast<>(ON_SHARE_SCREEN_STATUS_CHANGED, ShareScreenEvent.class,
                SolutionDemoEventManager::post));
        putEventListener(new AbsBroadcast<>(ON_RECORD, MeetingResponse.class,
                (data) -> SolutionDemoEventManager.post(new RecordEvent(true))));
        putEventListener(new AbsBroadcast<>(ON_MEETING_END, MeetingResponse.class,
                (data) -> SolutionDemoEventManager.post(new MeetingEndEvent())));
        putEventListener(new AbsBroadcast<>(ON_MUTE_ALL, MeetingResponse.class, (data) -> {
            if (!MeetingDataManager.isSelfHost()) {
                SolutionDemoEventManager.post(new MuteEvent(MeetingDataManager.getSelfId()));
            }
            SolutionDemoEventManager.post(new MuteAllEvent());
        }));
        putEventListener(new AbsBroadcast<>(ON_RECORD_FINISHED, MeetingResponse.class,
                (data) -> SolutionDemoEventManager.post(new RecordEvent(false))));
        putEventListener(new AbsBroadcast<>(ON_USER_KICKED_OFF, MeetingResponse.class,
                (data) -> SolutionDemoEventManager.post(new KickedOffEvent())));
        putEventListener(new AbsBroadcast<>(ON_MUTE_USER, MeetingUserInfo.class, (data) -> {
            if (TextUtils.equals(data.userId, SolutionDataManager.ins().getUserId())) {
                SolutionDemoEventManager.post(new MuteEvent(data.userId));
            }
        }));
        putEventListener(new AbsBroadcast<>(ON_ASKING_MIC_ON, MeetingUserInfo.class, (data) -> {
            if (TextUtils.equals(data.userId, SolutionDataManager.ins().getUserId())) {
                SolutionDemoEventManager.post(new AskOpenMicEvent());
            }
        }));
        putEventListener(new AbsBroadcast<>(ON_ASKING_CAMERA_ON, MeetingUserInfo.class, (data) -> {
            if (TextUtils.equals(data.userId, SolutionDataManager.ins().getUserId())) {
                SolutionDemoEventManager.post(new AskOpenCameraEvent());
            }
        }));
    }

    private void putEventListener(AbsBroadcast<? extends RTSBizInform> absBroadcast) {
        mEventListeners.put(absBroadcast.getEvent(), absBroadcast);
    }

    public void removeEventListener() {
        mEventListeners.remove(ON_USER_MIC_STATUS_CHANGE);
        mEventListeners.remove(ON_USER_CAMERA_STATUS_CHANGE);
        mEventListeners.remove(ON_HOST_CHANGE);
        mEventListeners.remove(ON_USER_JOIN_MEETING);
        mEventListeners.remove(ON_USER_LEAVE_MEETING);
        mEventListeners.remove(ON_SHARE_SCREEN_STATUS_CHANGED);
        mEventListeners.remove(ON_RECORD);
        mEventListeners.remove(ON_MEETING_END);
        mEventListeners.remove(ON_MUTE_ALL);
        mEventListeners.remove(ON_RECORD_FINISHED);
        mEventListeners.remove(ON_USER_KICKED_OFF);
        mEventListeners.remove(ON_MUTE_USER);
        mEventListeners.remove(ON_ASKING_MIC_ON);
        mEventListeners.remove(ON_ASKING_CAMERA_ON);
    }

    private <T extends RTSBizResponse> void sendServerMessageOnNetwork(String roomId, JsonObject content, Class<T> resultClass, IRequestCallback<T> callback) {
        String cmd = content.get("event_name").getAsString();
        if (TextUtils.isEmpty(cmd)) {
            return;
        }
        AppExecutors.networkIO().execute(() -> sendServerMessage(cmd, roomId, content, resultClass, callback));
    }

    public void requestJoinMeeting(String roomId, boolean micStatus, boolean cameraStatus,
                                   IRequestCallback<MeetingTokenInfo> callback) {
        JsonObject params = getCommonParams(CMD_MEETING_JOIN_MEETING);
        params.addProperty("mic", micStatus);
        params.addProperty("camera", cameraStatus);
        params.addProperty("room_id", roomId);
        sendServerMessageOnNetwork(roomId, params, MeetingTokenInfo.class, callback);
    }

    public void updateMicStatus(String roomId, boolean micStatus, IRequestCallback<RTSBizResponse> callback) {
        String cmd = micStatus ? CMD_MEETING_TURN_ON_MIC : CMD_MEETING_TURN_OFF_MIC;
        JsonObject params = getCommonParams(cmd);
        sendServerMessageOnNetwork(roomId, params, RTSBizResponse.class, callback);
    }

    public void updateCameraStatus(String roomId, boolean cameraStatus, IRequestCallback<RTSBizResponse> callback) {
        String cmd = cameraStatus ? CMD_MEETING_TURN_ON_CAMERA : CMD_MEETING_TURN_OFF_CAMERA;
        JsonObject params = getCommonParams(cmd);
        sendServerMessageOnNetwork(roomId, params, RTSBizResponse.class, callback);
    }

    public void getRoomUser(String roomId, IRequestCallback<MeetingUsersInfo> callback) {
        JsonObject params = getCommonParams(CMD_MEETING_GET_MEETING_USER_INFO);
        sendServerMessageOnNetwork(roomId, params, MeetingUsersInfo.class, callback);
    }

    public void requestScreenShare(String roomId, boolean isStart, IRequestCallback<RTSBizResponse> callback) {
        String cmd = isStart ? CMD_MEETING_START_SHARE_SCREEN : CMD_MEETING_END_SHARE_SCREEN;
        JsonObject params = getCommonParams(cmd);
        sendServerMessageOnNetwork(roomId, params, RTSBizResponse.class, callback);
    }

    public void requestRecordMeeting(String roomId, List<MeetingUserInfo> userInfoList, String screenUserId,
                                     IRequestCallback<RTSBizResponse> callback) {
        JsonObject params = getCommonParams(CMD_MEETING_RECORD_MEETING);

        JsonArray array = new JsonArray();
        if (userInfoList != null) {
            for (MeetingUserInfo info : userInfoList) {
                array.add(info.userId);
            }
        }

        params.add("users", array);
        params.addProperty("screen_uid", screenUserId == null ? "" : screenUserId);
        sendServerMessageOnNetwork(roomId, params, RTSBizResponse.class, callback);
    }

    public void requestUpdateRecordLayout(String roomId, List<String> userIdList,
                                          String screenUserId, IRequestCallback<RTSBizResponse> callback) {
        JsonObject params = getCommonParams(CMD_MEETING_UPDATE_RECORD_LAYOUT);

        JsonArray array = new JsonArray();
        if (userIdList != null) {
            for (String userId : userIdList) {
                array.add(userId);
            }
        }

        params.add("users", array);
        params.addProperty("screen_uid", screenUserId == null ? "" : screenUserId);
        sendServerMessageOnNetwork(roomId, params, RTSBizResponse.class, callback);
    }

    public void requestGetHistoryVideoRecord(IRequestCallback<MeetingRecordInfoList> callback) {
        JsonObject params = getCommonParams(CMD_MEETING_GET_HISTORY_VIDEO_RECORD);
        sendServerMessageOnNetwork(MeetingDataManager.getMeetingId(), params, MeetingRecordInfoList.class, callback);
    }

    public void requestGetMeetingUserInfo(IRequestCallback<MeetingUsersInfo> callback) {
        JsonObject params = getCommonParams(CMD_MEETING_GET_MEETING_USER_INFO);
        sendServerMessageOnNetwork(MeetingDataManager.getMeetingId(), params, MeetingUsersInfo.class, callback);
    }

    public void requestMuteUsers(String userId, IRequestCallback<RTSBizResponse> callback) {
        JsonObject params = getCommonParams(CMD_MEETING_MUTE_USER);
        params.addProperty("user_id", userId);
        sendServerMessageOnNetwork(MeetingDataManager.getMeetingId(), params, RTSBizResponse.class, callback);
    }

    public void requestUserMicOn(String userId, IRequestCallback<RTSBizResponse> callback) {
        JsonObject params = getCommonParams(CMD_MEETING_ASK_MIC_ON);
        params.addProperty("user_id", userId);
        sendServerMessageOnNetwork(MeetingDataManager.getMeetingId(), params, RTSBizResponse.class, callback);
    }

    public void requestChangedHost(String userId, IRequestCallback<RTSBizResponse> callback) {
        JsonObject params = getCommonParams(CMD_MEETING_CHANGE_HOST);
        params.addProperty("user_id", userId);
        sendServerMessageOnNetwork(MeetingDataManager.getMeetingId(), params, RTSBizResponse.class, callback);
    }

    public void requestDeleteRecord(String vid, IRequestCallback<RTSBizResponse> callback) {
        JsonObject params = getCommonParams(CMD_MEETING_DELETE_VIDEO_RECORD);
        params.addProperty("vid", vid);
        sendServerMessageOnNetwork(MeetingDataManager.getMeetingId(), params, RTSBizResponse.class, callback);
    }

    public void requestLeaveMeeting(String roomId, IRequestCallback<RTSBizResponse> callback) {
        JsonObject params = getCommonParams(CMD_MEETING_LEAVE_MEETING);
        params.addProperty("room_id", roomId);
        sendServerMessageOnNetwork(roomId, params, RTSBizResponse.class, callback);
    }

    public void requestFinishMeeting(String roomId, IRequestCallback<RTSBizResponse> callback) {
        JsonObject params = getCommonParams(CMD_MEETING_END_MEETING);
        params.addProperty("room_id", roomId);
        sendServerMessageOnNetwork(roomId, params, RTSBizResponse.class, callback);
    }
}
