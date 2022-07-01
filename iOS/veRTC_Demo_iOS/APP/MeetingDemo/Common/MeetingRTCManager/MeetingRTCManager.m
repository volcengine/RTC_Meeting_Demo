#import "SettingsService.h"
#import "MeetingRTCManager.h"
#import "AlertActionManager.h"

@interface MeetingRTCManager () <ByteRTCEngineDelegate>

@property (nonatomic, strong) RoomParamInfoModel *paramInfoModel;
@property (nonatomic, strong) RoomVideoSession *localVideoSession;

@property (nonatomic, strong) NSMutableDictionary *subscribeUidDic;
@property (nonatomic, assign) NSInteger currnetCameraID;

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
    //设置订阅的音视频流回退选项
    //Set the subscribed audio and video stream fallback options
    [self.rtcEngineKit setSubscribeFallbackOption:ByteRTCSubscribeFallbackOptionAudioOnly];

    //开启/关闭 本地音频采集
    //Turn on/off local audio capture
    [self enableLocalAudio:videoSession.isEnableAudio];
    
    //Turn on/off local video capture
    [self enableLocalVideo:videoSession.isEnableVideo];
    [self.rtcEngineKit startAudioCapture];
    
    //设置音频路由模式，YES 扬声器/NO 听筒
    //Set the audio routing mode, YES speaker/NO earpiece
    [self setEnableSpeakerphone:YES];

    //设置本地视频镜像
    //Set local video mirroring
    [self.rtcEngineKit setLocalVideoMirrorMode:ByteRTCMirrorModeOn];
    
    //开启/关闭发言者音量键控
    //Turn on/off speaker volume keying
    [self.rtcEngineKit setAudioVolumeIndicationInterval:2000];

    //加入房间，开始连麦,需要申请AppId和Token
    //Join the room, start connecting the microphone, you need to apply for AppId and Token
    ByteRTCUserInfo *userInfo = [[ByteRTCUserInfo alloc] init];
    userInfo.userId = videoSession.uid;
    
    ByteRTCRoomConfig *config = [[ByteRTCRoomConfig alloc] init];
    config.profile = ByteRTCRoomProfileLiveBroadcasting;
    config.isAutoPublish = YES;
    config.isAutoSubscribeAudio = YES;
    config.isAutoSubscribeVideo = NO;
    
    [self.rtcEngineKit joinRoomByKey:videoSession.token
                              roomId:videoSession.roomId
                            userInfo:userInfo
                       rtcRoomConfig:config];
}

#pragma mark - rtc method

- (void)updateRtcVideoParams {
    ByteRTCVideoSolution *localSolution = [[ByteRTCVideoSolution alloc] init];
    localSolution.videoSize = [SettingsService getResolution];
    localSolution.frameRate = [SettingsService getFrameRate];
    localSolution.maxKbps = [SettingsService getKBitRate];
    
    //设置本地视频属性
    //Set local video attributes
    [self.rtcEngineKit setVideoEncoderConfig:@[localSolution]];
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
    int result = [self.rtcEngineKit switchCamera:cameraID];
    if (0 == result) {
        self.currnetCameraID = cameraID;
    }
}

- (int)setEnableSpeakerphone:(BOOL)enableSpeaker {
    //打开/关闭 外放
    //Turn on/off the loudspeaker
    int result = 0;
    if (enableSpeaker) {
        result = [self.rtcEngineKit setAudioPlaybackDevice:ByteRTCAudioPlaybackDeviceEarpiece];
    } else {
        result = [self.rtcEngineKit setAudioPlaybackDevice:ByteRTCAudioPlaybackDeviceSpeakerphone];
    }
    return result;
}

- (void)enableLocalAudio:(BOOL)enable {
    //开启/关闭 本地音频采集
    //Turn on/off local audio capture
    [self.rtcEngineKit muteLocalAudio:!enable];
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
    [self.rtcEngineKit leaveRoom];
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
        ByteRTCSubscribeVideoConfig *config = [[ByteRTCSubscribeVideoConfig alloc] init];
        config.videoIndex = 0;
        [self.rtcEngineKit subscribeUserStream:uid
                              streamType:ByteRTCStreamIndexMain
                               mediaType:ByteRTCSubscribeMediaTypeAudioAndVideo
                             videoConfig:config];
         
        [self.subscribeUidDic setValue:@"1" forKey:uid];
    }
}

- (void)subscribeScreenStream:(NSString *)uid {
    if (IsEmptyStr(uid)) {
        return;
    }
    ByteRTCSubscribeVideoConfig *config = [[ByteRTCSubscribeVideoConfig alloc] init];
    config.videoIndex = 0;
    [self.rtcEngineKit subscribeUserStream:uid
                          streamType:ByteRTCStreamIndexScreen
                           mediaType:ByteRTCSubscribeMediaTypeAudioAndVideo
                         videoConfig:config];
}

- (void)unsubscribe:(NSString *_Nullable)uid {
    if (IsEmptyStr(uid)) {
        return;
    }
    NSString *value = [self.subscribeUidDic objectForKey:uid];
    if ([value integerValue] == 1) {
        ByteRTCSubscribeVideoConfig *config = [[ByteRTCSubscribeVideoConfig alloc] init];
        config.videoIndex = 0;
        
        [self.rtcEngineKit subscribeUserStream:uid
                              streamType:ByteRTCStreamIndexMain
                               mediaType:ByteRTCSubscribeMediaTypeAudioOnly
                             videoConfig:config];

        [self.subscribeUidDic setValue:@"0" forKey:uid];
    }
}

- (void)unsubscribeScreen:(NSString *)uid {
    if (IsEmptyStr(uid)) {
        return;
    }
    ByteRTCSubscribeVideoConfig *config = [[ByteRTCSubscribeVideoConfig alloc] init];
    config.videoIndex = 0;
    
    [self.rtcEngineKit subscribeUserStream:uid
                          streamType:ByteRTCStreamIndexScreen
                           mediaType:ByteRTCSubscribeMediaTypeNone
                         videoConfig:config];
}

- (NSDictionary *_Nullable)getSubscribeUidDic {
    return [self.subscribeUidDic copy];
}

- (void)setupRemoteVideo:(ByteRTCVideoCanvas *)canvas {
    [self.rtcEngineKit setRemoteVideoCanvas:canvas.uid withIndex:ByteRTCStreamIndexMain withCanvas:canvas];
}

- (void)setupLocalVideo:(ByteRTCVideoCanvas *)canvas {
    [self.rtcEngineKit setLocalVideoCanvas:ByteRTCStreamIndexMain withCanvas:canvas];
}

- (void)setupRemoteScreen:(UIView *)view uid:(NSString *)uid {
    ByteRTCVideoCanvas *cans = [[ByteRTCVideoCanvas alloc] init];
    cans.uid = uid;
    cans.renderMode = ByteRTCRenderModeFit;
    cans.view.backgroundColor = [UIColor clearColor];
    cans.view = view;
    [self.rtcEngineKit setRemoteVideoCanvas:cans.uid withIndex:ByteRTCStreamIndexScreen withCanvas:cans];
}

#pragma mark - ByteRTCEngineDelegate

- (void)rtcEngine:(ByteRTCEngineKit * _Nonnull)engine
      onStreamAdd:(id<ByteRTCStream> _Nonnull)stream {
    if (stream.isScreen) {
        if ([self.delegate respondsToSelector:@selector(rtcManager:didScreenStreamAdded:)]) {
            [self.delegate rtcManager:self didScreenStreamAdded:stream.userId];
        }
    } else {
        if ([self.delegate respondsToSelector:@selector(rtcManager:didStreamAdded:)]) {
            [self.delegate rtcManager:self didStreamAdded:stream.userId];
        }
    }
}

- (void)rtcEngine:(ByteRTCEngineKit *_Nonnull)engine
    didStreamRemoved:(NSString *_Nonnull)uid
           stream:(id<ByteRTCStream> _Nonnull)stream
           reason:(ByteRTCStreamRemoveReason)reason {
    if (stream.isScreen) {
        if ([self.delegate respondsToSelector:@selector(rtcManager:didScreenStreamRemoved:)]) {
            [self.delegate rtcManager:self didScreenStreamRemoved:stream.userId];
        }
    } else {
        [self.subscribeUidDic setValue:@"0" forKey:stream.userId];
        if ([self.delegate respondsToSelector:@selector(rtcManager:didStreamRemoved:)]) {
            [self.delegate rtcManager:self didStreamRemoved:stream.userId];
        }
    }
}

- (void)rtcEngine:(ByteRTCEngineKit *)engine onVideoDeviceStateChanged:(NSString *)device_id device_type:(ByteRTCVideoDeviceType)device_type device_state:(ByteRTCMediaDeviceState)device_state device_error:(ByteRTCMediaDeviceError)device_error {
    if (device_type == ByteRTCVideoDeviceTypeScreenCaptureDevice) {
        if (device_state == ByteRTCMediaDeviceStateStarted) {
            [engine publishScreen:ByteRTCMediaStreamTypeBoth];
            NSLog(@"onVideoDeviceStateChanged Started");
        } else if (device_state == ByteRTCMediaDeviceStateStopped ||
                   device_state == ByteRTCMediaDeviceStateRuntimeError) {
            [engine unpublishScreen:ByteRTCMediaStreamTypeBoth];
            NSLog(@"onVideoDeviceStateChanged Stopped");
        }
    }
}

#pragma mark - Rtc Stats

- (void)rtcEngine:(ByteRTCEngineKit *_Nonnull)engine onLocalStreamStats:(const ByteRTCLocalStreamStats *_Nonnull)stats {
    self.paramInfoModel.local_bit_video = [NSString stringWithFormat:@"%.0f",stats.video_stats.sentKBitrate];
    self.paramInfoModel.local_fps = [NSString stringWithFormat:@"%ld",(long)stats.video_stats.inputFrameRate];
    self.paramInfoModel.local_res = [NSString stringWithFormat:@"%ld*%ld",(long)stats.video_stats.encodedFrameWidth, stats.video_stats.encodedFrameHeight];
    
    self.paramInfoModel.local_bit_audio = [NSString stringWithFormat:@"%.0f",stats.audio_stats.sentKBitrate];
    
    [self updateRoomParamInfoModel];
}

- (void)rtcEngine:(ByteRTCEngineKit *_Nonnull)engine onRemoteStreamStats:(const ByteRTCRemoteStreamStats *_Nonnull)stats {
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

- (void)rtcEngine:(ByteRTCEngineKit * _Nonnull)engine reportSysStats:(const ByteRTCSysStats * _Nonnull)stats {
    self.paramInfoModel.remote_cpu_avg = [NSString stringWithFormat:@"%.2f", stats.cpu_app_usage * 100];
    if (stats.cpu_app_usage > [self.paramInfoModel.remote_cpu_max floatValue] / 100.0) {
        self.paramInfoModel.remote_cpu_max = [NSString stringWithFormat:@"%.2f", stats.cpu_app_usage * 100];
    }
    
    [self updateRoomParamInfoModel];
}

- (void)rtcEngine:(ByteRTCEngineKit * _Nonnull)engine onAudioVolumeIndication:(NSArray<ByteRTCAudioVolumeInfo *> * _Nonnull)speakers totalRemoteVolume:(NSInteger)totalRemoteVolume {
    NSInteger minVolume = 10;
    NSMutableDictionary *parDic = [[NSMutableDictionary alloc] init];
    for (int i = 0; i < speakers.count; i++) {
        ByteRTCAudioVolumeInfo *model = speakers[i];
        if (model.linearVolume > minVolume) {
            [parDic setValue:@(model.linearVolume) forKey:model.uid];
        }
    }
    if ([self.delegate respondsToSelector:@selector(rtcManager:reportAllAudioVolume:)]) {
        [self.delegate rtcManager:self reportAllAudioVolume:[parDic copy]];
    }
}

- (void)startScreenSharingWithParam:(ByteRTCScreenCaptureParam *_Nonnull)param preferredExtension:(NSString *)extension groupId:(NSString *)groupId {
    ByteRTCVideoSolution *screenSolution = [[ByteRTCVideoSolution alloc] init];
    screenSolution.videoSize = [SettingsService getScreenResolution];
    screenSolution.frameRate = [SettingsService getScreenFrameRate];
    screenSolution.maxKbps = [SettingsService getScreenKBitRate];
    [self.rtcEngineKit setVideoEncoderConfig:ByteRTCStreamIndexScreen config:@[screenSolution]];
    
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
