// 
// Copyright (c) 2023 Beijing Volcano Engine Technology Ltd.
// SPDX-License-Identifier: MIT
// 

#import "SettingsService.h"
#import "MeetingRTCManager.h"

@interface MeetingRTCManager () <ByteRTCVideoDelegate>
// RTC / RTS 房间
@property (nonatomic, strong, nullable) ByteRTCRoom *rtcRoom;

@property (nonatomic, strong) RoomParamInfoModel *paramInfoModel;
@property (nonatomic, strong) RoomVideoSession *localVideoSession;

@property (nonatomic, strong) NSMutableDictionary *subscribeUidDic;
@property (nonatomic, assign) NSInteger currnetCameraID;

@property (nonatomic, strong) ByteRTCVideoEncoderConfig *solution;
@end

@implementation MeetingRTCManager

+ (MeetingRTCManager *_Nullable)shareRtc {
    static MeetingRTCManager *meetingRTCManager = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        meetingRTCManager = [[MeetingRTCManager alloc] init];
    });
    return meetingRTCManager;
}

- (instancetype)init {
    self = [super init];
    if (self) {
        self.currnetCameraID = ByteRTCCameraIDFront;
    }
    return self;
}

#pragma mark - Publish Action

- (void)configeRTCEngine {
    [super configeRTCEngine];
    [self.rtcEngineKit setExtensionConfig:APPGroupId];
}

- (void)joinChannelWithRoomVideoSession:(RoomVideoSession *)videoSession {
    // 设置音频场景类型
    [self.rtcEngineKit setAudioScenario:ByteRTCAudioScenarioCommunication];
    
    //设置订阅的音视频流回退选项
    //Set the subscribed audio and video stream fallback options
    [self.rtcEngineKit setSubscribeFallbackOption:ByteRTCSubscribeFallbackOptionDisabled];
    
    //Turn on/off local video capture
    [self enableLocalVideo:videoSession.isEnableVideo];
    [self.rtcEngineKit startAudioCapture];
    
    //设置音频路由模式，YES 扬声器/NO 听筒
    //Set the audio routing mode, YES speaker/NO earpiece
    [self setEnableSpeakerphone:YES];

    //设置本地视频镜像
    //Set local video mirroring
    if (self.currnetCameraID == ByteRTCCameraIDFront) {
        [self.rtcEngineKit setLocalVideoMirrorType:ByteRTCMirrorTypeRenderAndEncoder];
    } else {
        [self.rtcEngineKit setLocalVideoMirrorType:ByteRTCMirrorTypeNone];
    }
    
    //开启/关闭发言者音量键控
    //Turn on/off speaker volume keying
    ByteRTCAudioPropertiesConfig *audioPropertiesConfig = [[ByteRTCAudioPropertiesConfig alloc] init];
    audioPropertiesConfig.interval = 2000;
    [self.rtcEngineKit enableAudioPropertiesReport:audioPropertiesConfig];
    
    //加入房间，开始连麦,需要申请AppId和Token
    //Join the room, start connecting the microphone, you need to apply for AppId and Token
    ByteRTCUserInfo *userInfo = [[ByteRTCUserInfo alloc] init];
    userInfo.userId = videoSession.uid;
    
    ByteRTCRoomConfig *config = [[ByteRTCRoomConfig alloc] init];
    config.profile = ByteRTCRoomProfileCommunication;
    config.isAutoPublish = YES;
    config.isAutoSubscribeAudio = YES;
    config.isAutoSubscribeVideo = NO;
    
    self.rtcRoom = [self.rtcEngineKit createRTCRoom:videoSession.roomId];
    self.rtcRoom.delegate = self;
    [self.rtcRoom joinRoom:videoSession.token userInfo:userInfo roomConfig:config];
    [self publishStreamAudio:videoSession.isEnableAudio];
}

#pragma mark - rtc method

- (void)updateRtcVideoParams {
    ByteRTCVideoEncoderConfig *localSolution = [[ByteRTCVideoEncoderConfig alloc] init];
    localSolution.width = [SettingsService getResolution].width;
    localSolution.height = [SettingsService getResolution].height;

    localSolution.frameRate = [SettingsService getFrameRate];
    localSolution.maxBitrate = [SettingsService getKBitRate];
    
    self.solution = localSolution;
    
    //设置本地视频属性
    //Set local video attributes
    [self.rtcEngineKit setMaxVideoEncoderConfig:localSolution];
}

- (void)switchCamera {
    //切换 前置/后置 摄像头
    //Switch front/rear camera
    ByteRTCCameraID cameraID = self.currnetCameraID;
    if (cameraID == ByteRTCCameraIDFront) {
        cameraID = ByteRTCCameraIDBack;
    } else {
        cameraID = ByteRTCCameraIDFront;
    }
    
    if (cameraID == ByteRTCCameraIDFront) {
        [self.rtcEngineKit setLocalVideoMirrorType:ByteRTCMirrorTypeRenderAndEncoder];
    } else {
        [self.rtcEngineKit setLocalVideoMirrorType:ByteRTCMirrorTypeNone];
    }
    
    [self.rtcEngineKit switchCamera:cameraID];
    self.currnetCameraID = cameraID;
}

- (void)setEnableSpeakerphone:(BOOL)enableSpeaker {
    //打开/关闭 外放
    //Turn on/off the loudspeaker
    if (enableSpeaker) {
        [self.rtcEngineKit setDefaultAudioRoute:ByteRTCAudioRouteSpeakerphone];
    } else {
        [self.rtcEngineKit setDefaultAudioRoute:ByteRTCAudioRouteEarpiece];
    }
}

- (void)publishStreamAudio:(BOOL)isPublish {
    if (isPublish) {
        [self.rtcRoom publishStream:ByteRTCMediaStreamTypeAudio];
    } else {
        [self.rtcRoom unpublishStream:ByteRTCMediaStreamTypeAudio];
    }
}

- (void)enableLocalVideo:(BOOL)enable {
    //开启/关闭 本地视频采集
    //Turn on/off local video capture
    if (enable) {
        [self.rtcEngineKit startVideoCapture];
    } else {
        [self.rtcEngineKit stopVideoCapture];
    }
}

- (void)leaveChannel {
    //离开频道
    //Leave the channel
    [self.rtcEngineKit stopAudioCapture];
    [self.rtcRoom leaveRoom];
    [self.subscribeUidDic removeAllObjects];
}

- (void)startPreview:(UIView *)view {
    ByteRTCVideoCanvas *canvas = [[ByteRTCVideoCanvas alloc] init];
    canvas.view = view;
    canvas.renderMode = ByteRTCRenderModeHidden;
    canvas.view.backgroundColor = [UIColor clearColor];
    //设置本地视频显示信息
    //Set local video display information
    [self.rtcEngineKit setLocalVideoCanvas:ByteRTCStreamIndexMain withCanvas:canvas];
    [self.rtcEngineKit startVideoCapture];
}

- (void)subscribeStream:(NSString *_Nullable)uid {
    if (IsEmptyStr(uid)) {
        return;
    }
    NSString *value = [self.subscribeUidDic objectForKey:uid];
    if ([value integerValue] != 1) {
        [self.rtcRoom subscribeStream:uid mediaStreamType:ByteRTCMediaStreamTypeBoth];
        
        [self.subscribeUidDic setValue:@"1" forKey:uid];
    }
}

- (void)subscribeScreenStream:(NSString *)uid {
    if (IsEmptyStr(uid)) {
        return;
    }
    [self.rtcRoom subscribeScreen:uid mediaStreamType:ByteRTCMediaStreamTypeBoth];
}

- (void)unsubscribe:(NSString *_Nullable)uid {
    if (IsEmptyStr(uid)) {
        return;
    }
    NSString *value = [self.subscribeUidDic objectForKey:uid];
    if ([value integerValue] == 1) {
        [self.rtcRoom unsubscribeStream:uid mediaStreamType:ByteRTCMediaStreamTypeVideo];
        [self.rtcRoom subscribeStream:uid mediaStreamType:ByteRTCMediaStreamTypeAudio];

        [self.subscribeUidDic setValue:@"0" forKey:uid];
    }
}

- (void)unsubscribeScreen:(NSString *)uid {
    if (IsEmptyStr(uid)) {
        return;
    }
    
    [self.rtcRoom unsubscribeScreen:uid mediaStreamType:ByteRTCMediaStreamTypeBoth];
}

- (NSDictionary *_Nullable)getSubscribeUidDic {
    return [self.subscribeUidDic copy];
}

- (void)setupRemoteStreamKey:(ByteRTCRemoteStreamKey *)streamKey
                      canvas:(ByteRTCVideoCanvas *)canvas {
    if (canvas) {
        [self.rtcEngineKit setRemoteVideoCanvas:streamKey withCanvas:canvas];
    }
}

- (void)setupLocalVideo:(ByteRTCVideoCanvas *)canvas {
    [self.rtcEngineKit setLocalVideoCanvas:ByteRTCStreamIndexMain withCanvas:canvas];
}

- (void)setupRemoteScreen:(UIView *)view uid:(NSString *)uid {
    
    ByteRTCVideoCanvas *cans = [[ByteRTCVideoCanvas alloc] init];
    cans.renderMode = ByteRTCRenderModeFit;
    cans.view.backgroundColor = [UIColor clearColor];
    cans.view = view;

    ByteRTCRemoteStreamKey *streamKey = [[ByteRTCRemoteStreamKey alloc] init];
    streamKey.userId = uid;
    streamKey.roomId = self.rtcRoom.getRoomId;
    streamKey.streamIndex = ByteRTCStreamIndexScreen;
    
    [self.rtcEngineKit setRemoteVideoCanvas:streamKey withCanvas:cans];
}

#pragma mark - ByteRTCVideoDelegate

- (void)rtcRoom:(ByteRTCRoom *)rtcRoom onRoomStateChanged:(NSString *)roomId
        withUid:(NSString *)uid
          state:(NSInteger)state
      extraInfo:(NSString *)extraInfo {
    [super rtcRoom:rtcRoom onRoomStateChanged:roomId withUid:uid state:state extraInfo:extraInfo];
    
    dispatch_queue_async_safe(dispatch_get_main_queue(), ^{
        RTCJoinModel *joinModel = [RTCJoinModel modelArrayWithClass:extraInfo state:state roomId:roomId];
        if ([self.delegate respondsToSelector:@selector(meetingRTCManager:onRoomStateChanged:)]) {
            [self.delegate meetingRTCManager:self onRoomStateChanged:joinModel];
        }
    });
}

- (void)rtcRoom:(ByteRTCRoom *)rtcRoom onUserPublishStream:(NSString *)userId type:(ByteRTCMediaStreamType)type {
    if (type != ByteRTCMediaStreamTypeAudio) {
        if ([self.delegate respondsToSelector:@selector(rtcManager:didStreamAdded:)]) {
            [self.delegate rtcManager:self didStreamAdded:userId];
        }
    }
}

- (void)rtcRoom:(ByteRTCRoom *)rtcRoom onUserUnpublishStream:(NSString *)userId type:(ByteRTCMediaStreamType)type reason:(ByteRTCStreamRemoveReason)reason {
    if (type != ByteRTCMediaStreamTypeAudio) {
        [self.subscribeUidDic setValue:@"0" forKey:userId];
        if ([self.delegate respondsToSelector:@selector(rtcManager:didStreamRemoved:)]) {
            [self.delegate rtcManager:self didStreamRemoved:userId];
        }
    }
}

- (void)rtcRoom:(ByteRTCRoom *)rtcRoom onUserPublishScreen:(NSString *)userId type:(ByteRTCMediaStreamType)type {
    if ([self.delegate respondsToSelector:@selector(rtcManager:didScreenStreamAdded:)]) {
        [self.delegate rtcManager:self didScreenStreamAdded:userId];
    }
}

- (void)rtcRoom:(ByteRTCRoom *)rtcRoom onUserUnpublishScreen:(NSString *)userId type:(ByteRTCMediaStreamType)type reason:(ByteRTCStreamRemoveReason)reason {
    if ([self.delegate respondsToSelector:@selector(rtcManager:didScreenStreamRemoved:)]) {
        [self.delegate rtcManager:self didScreenStreamRemoved:userId];
    }
}

- (void)rtcEngine:(ByteRTCVideo *_Nonnull)engine onRemoteVideoSizeChanged:(ByteRTCRemoteStreamKey *_Nonnull)streamKey withFrameInfo:(ByteRTCVideoFrameInfo *_Nonnull)frameInfo {
    
    if(streamKey.streamIndex == ByteRTCStreamIndexScreen) {
        if ([self.delegate respondsToSelector:@selector(rtcManager:didScreenStreamRemoved:)]) {
            [self.delegate rtcManager:self onRemoteVideoSizeChanged:streamKey.userId];
        }
    }
    
}

- (void)rtcEngine:(ByteRTCVideo *)engine onVideoDeviceStateChanged:(NSString *)device_id device_type:(ByteRTCVideoDeviceType)device_type device_state:(ByteRTCMediaDeviceState)device_state device_error:(ByteRTCMediaDeviceError)device_error {

    if (device_type == ByteRTCVideoDeviceTypeScreenCaptureDevice) {
        if (device_state == ByteRTCMediaDeviceStateStarted) {
            [self.rtcRoom publishScreen:ByteRTCMediaStreamTypeBoth];
            NSLog(@"onVideoDeviceStateChanged Started");
        } else if (device_state == ByteRTCMediaDeviceStateStopped ||
                   device_state == ByteRTCMediaDeviceStateRuntimeError) {
            [self.rtcRoom unpublishScreen:ByteRTCMediaStreamTypeBoth];
            NSLog(@"onVideoDeviceStateChanged Stopped");
        }
    }
}

#pragma mark - Rtc Stats

- (void)rtcRoom:(ByteRTCRoom *)rtcRoom onLocalStreamStats:(ByteRTCLocalStreamStats *)stats {

    self.paramInfoModel.local_bit_video = [NSString stringWithFormat:@"%.0f",stats.video_stats.sentKBitrate];
    self.paramInfoModel.local_fps = [NSString stringWithFormat:@"%ld",(long)stats.video_stats.inputFrameRate];
    self.paramInfoModel.local_res = [NSString stringWithFormat:@"%ld*%ld",(long)stats.video_stats.encodedFrameWidth, (long)stats.video_stats.encodedFrameHeight];
    
    self.paramInfoModel.local_bit_audio = [NSString stringWithFormat:@"%.0f",stats.audio_stats.sentKBitrate];
    
    [self updateRoomParamInfoModel];
}

- (void)rtcRoom:(ByteRTCRoom *)rtcRoom onRemoteStreamStats:(ByteRTCRemoteStreamStats *)stats {
    self.paramInfoModel.remote_rtt_audio = [NSString stringWithFormat:@"%ld",(long)stats.audio_stats.rtt];
    self.paramInfoModel.remote_bit_audio = [NSString stringWithFormat:@"%ld",(long)stats.audio_stats.receivedKBitrate];
    self.paramInfoModel.remote_loss_audio = [NSString stringWithFormat:@"%ld",(long)stats.audio_stats.audioLossRate];
 
    self.paramInfoModel.remote_rtt_video = [NSString stringWithFormat:@"%ld",(long)stats.video_stats.rtt];
    self.paramInfoModel.remote_bit_video = [NSString stringWithFormat:@"%ld",(long)stats.video_stats.receivedKBitrate];
    
    if (stats.video_stats.width * stats.video_stats.height <= self.paramInfoModel.remote_res_min_w * self.paramInfoModel.remote_res_min_h ||
        (self.paramInfoModel.remote_res_min_w == 0 && self.paramInfoModel.remote_res_min_h == 0)) {
        self.paramInfoModel.remote_res_min_w = stats.video_stats.width;
        self.paramInfoModel.remote_res_min_h = stats.video_stats.height;
    }
    if (stats.video_stats.width * stats.video_stats.height > self.paramInfoModel.remote_res_max_w * self.paramInfoModel.remote_res_max_h) {
        self.paramInfoModel.remote_res_max_w = stats.video_stats.width;
        self.paramInfoModel.remote_res_max_h = stats.video_stats.height;
    }
    self.paramInfoModel.remote_res_min = [NSString stringWithFormat:@"%ld*%ld",
                                             (long)self.paramInfoModel.remote_res_min_w,
                                             (long)self.paramInfoModel.remote_res_min_h];
    self.paramInfoModel.remote_res_max = [NSString stringWithFormat:@"%ld*%ld",
                                             (long)self.paramInfoModel.remote_res_max_w,
                                             (long)self.paramInfoModel.remote_res_max_h];
    self.paramInfoModel.remote_fps_min = [NSString stringWithFormat:@"%ld",(long)stats.video_stats.receivedFrameRate];
    self.paramInfoModel.remote_fps_max = [NSString stringWithFormat:@"%ld",(long)stats.video_stats.receivedFrameRate];
    self.paramInfoModel.remote_loss_video = [NSString stringWithFormat:@"%ld",(long)stats.video_stats.videoLossRate];
    
    [self updateRoomParamInfoModel];
}

- (void)rtcEngine:(ByteRTCVideo *)engine reportSysStats:(const ByteRTCSysStats *)stats {
    self.paramInfoModel.remote_cpu_avg = [NSString stringWithFormat:@"%.2f", stats.cpu_app_usage * 100];
    if (stats.cpu_app_usage > [self.paramInfoModel.remote_cpu_max floatValue] / 100.0) {
        self.paramInfoModel.remote_cpu_max = [NSString stringWithFormat:@"%.2f", stats.cpu_app_usage * 100];
    }
    
    [self updateRoomParamInfoModel];
}

- (void)rtcEngine:(ByteRTCVideo *)engine onRemoteAudioPropertiesReport:(NSArray<ByteRTCRemoteAudioPropertiesInfo *> *)audioPropertiesInfos totalRemoteVolume:(NSInteger)totalRemoteVolume {
    
    NSInteger minVolume = 10;
    NSMutableDictionary *parDic = [[NSMutableDictionary alloc] init];
    for (int i = 0; i < audioPropertiesInfos.count; i++) {
        ByteRTCRemoteAudioPropertiesInfo *model = audioPropertiesInfos[i];
        if (model.audioPropertiesInfo.linearVolume > minVolume) {
            [parDic setValue:@(model.audioPropertiesInfo.linearVolume) forKey:model.streamKey.userId];
        }
    }
    if ([self.delegate respondsToSelector:@selector(rtcManager:reportAllAudioVolume:)]) {
        [self.delegate rtcManager:self reportAllAudioVolume:[parDic copy]];
    }
}

- (void)startScreenSharingWithParam:(ByteRTCScreenCaptureParam *_Nonnull)param preferredExtension:(NSString *)extension groupId:(NSString *)groupId {
    ByteRTCScreenVideoEncoderConfig *screenSolution = [[ByteRTCScreenVideoEncoderConfig alloc] init];
    CGSize videoSize = [SettingsService getScreenResolution];
    screenSolution.width = videoSize.width;
    screenSolution.height = videoSize.height;
    screenSolution.frameRate = [SettingsService getScreenFrameRate];
    screenSolution.maxBitrate = [SettingsService getScreenKBitRate];
    [self.rtcEngineKit setScreenVideoEncoderConfig:screenSolution];
    
    // startScreenCapture 后需要在 -onVideoDeviceStateChanged 回调中开启推流 -publishScreen
    [self.rtcEngineKit startScreenCapture:ByteRTCScreenMediaTypeVideoAndAudio bundleId:extension];
}



- (void)stopScreenSharing {
    [self.rtcEngineKit stopScreenCapture];
}

#pragma mark - Private Action

- (void)updateRoomParamInfoModel {
    if ([self.delegate respondsToSelector:@selector(meetingRTCManager:changeParamInfo:)]) {
        [self.delegate meetingRTCManager:self changeParamInfo:self.paramInfoModel];
    }
}

#pragma mark - getter
- (RoomParamInfoModel *)paramInfoModel {
    if (!_paramInfoModel) {
        _paramInfoModel = [[RoomParamInfoModel alloc] init];
    }
    return _paramInfoModel;
}

- (NSMutableDictionary *)subscribeUidDic {
    if (!_subscribeUidDic) {
        _subscribeUidDic = [[NSMutableDictionary alloc] init];
    }
    return _subscribeUidDic;
}

- (void)dealloc {
    [[NSNotificationCenter defaultCenter] removeObserver:self];
}

@end