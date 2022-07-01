//
//  SampleHandler.m
//  MeetingScreenShare
//
//  Created by bytedance on 2022/5/27.
//  Copyright © 2022 bytedance. All rights reserved.
//


#import "SampleHandler.h"
#import <VolcEngineRTCScreenCapturer/ByteRTCScreenCapturerExt.h>
#import "MeetingScreenShareConstants.h"

@interface SampleHandler () <ByteRtcScreenCapturerExtDelegate>

@end

@implementation SampleHandler

- (instancetype)init {
    self = [super init];
    if (self) {
        
    }
    return self;
}

- (void)broadcastStartedWithSetupInfo:(NSDictionary<NSString *,NSObject *> *)setupInfo {
    // User has requested to start the broadcast. Setup info from the UI extension can be supplied but optional.
    [[ByteRtcScreenCapturerExt shared] startWithDelegate:self
                                                 groupId:APPGroupId];
}

- (void)broadcastPaused {
    // User has requested to pause the broadcast. Samples will stop being delivered.
}

- (void)broadcastResumed {
    // User has requested to resume the broadcast. Samples delivery will resume.
}

- (void)broadcastFinished {
    // User has requested to finish the broadcast.
    [[ByteRtcScreenCapturerExt shared] stop];
}

- (void)processSampleBuffer:(CMSampleBufferRef)sampleBuffer withType:(RPSampleBufferType)sampleBufferType {
    
    switch (sampleBufferType) {
        case RPSampleBufferTypeVideo:
            [[ByteRtcScreenCapturerExt shared] processSampleBuffer:sampleBuffer
                                                          withType:sampleBufferType];
            break;
        case RPSampleBufferTypeAudioApp:
            // Handle audio sample buffer for app audio
            break;
        case RPSampleBufferTypeAudioMic:
            // Handle audio sample buffer for mic audio
            break;
            
        default:
            break;
    }
}


#pragma mark - ByteRtcScreenCapturerExtDelegate


// 通知 Broadcast Upload Extension 停止采集屏幕并退出。
- (void)onQuitFromApp {
    NSDictionary *dic = @{
        NSLocalizedFailureReasonErrorKey : @"您停止了屏幕共享"};
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
