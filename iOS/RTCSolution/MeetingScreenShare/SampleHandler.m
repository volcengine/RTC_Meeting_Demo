// 
// Copyright (c) 2023 Beijing Volcano Engine Technology Ltd.
// SPDX-License-Identifier: MIT
// 

#import "SampleHandler.h"
#import "MeetingScreenShareConstants.h"

#import <VolcEngineRTCScreenCapturer/ByteRTCScreenCapturerExt.h>

@interface SampleHandler ()
<ByteRtcScreenCapturerExtDelegate>

@end

@implementation SampleHandler

- (instancetype)init {
    self = [super init];
    if (self) {
        
    }
    return self;
}

- (void)broadcastStartedWithSetupInfo:(NSDictionary<NSString *,NSObject *> *)setupInfo {
    [[ByteRtcScreenCapturerExt shared] startWithDelegate:self
                                                 groupId:APPGroupId];
}

- (void)broadcastPaused {
}

- (void)broadcastResumed {
}

- (void)broadcastFinished {
    [[ByteRtcScreenCapturerExt shared] stop];
}

- (void)processSampleBuffer:(CMSampleBufferRef)sampleBuffer withType:(RPSampleBufferType)sampleBufferType {
    
    switch (sampleBufferType) {
        case RPSampleBufferTypeVideo:
        case RPSampleBufferTypeAudioApp:
            [[ByteRtcScreenCapturerExt shared] processSampleBuffer:sampleBuffer
                                                          withType:sampleBufferType];

            break;
        case RPSampleBufferTypeAudioMic:
            break;
            
        default:
            break;
    }
}


#pragma mark - ByteRtcScreenCapturerExtDelegate


// 通知 Broadcast Upload Extension 停止采集屏幕并退出。
- (void)onQuitFromApp {
    NSDictionary *dic = @{
        NSLocalizedFailureReasonErrorKey : @"stopped screen sharing"};
    NSError *error = [NSError errorWithDomain:RPRecordingErrorDomain
                                         code:RPRecordingErrorUserDeclined
                                     userInfo:dic];
    [self finishBroadcastWithError:error];
}

// message App 侧发送的消息
- (void)onReceiveMessageFromApp:(NSData *)message {
    NSLog(@"SampleHandler onReceiveMessageFromApp");
}

// 连接断开时触发此回调
- (void)onSocketDisconnect {
    NSLog(@"SampleHandler onSocketDisconnect");
}

// 连接成功时触发此回调
- (void)onSocketConnect {
    NSLog(@"SampleHandler onSocketConnect");
}

// 检测到 App 正在进行音视频通话时触发此回调。
- (void)onNotifyAppRunning {
    NSLog(@"SampleHandler onNotifyAppRunning");
}


@end
