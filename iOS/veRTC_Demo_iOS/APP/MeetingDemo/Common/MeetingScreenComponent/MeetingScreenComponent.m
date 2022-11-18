//
//  MeetingScreenComponent.m
//  quickstart
//
//  Created by on 2021/4/6.
//  
//

#import "MeetingScreenComponent.h"
#import "SettingsService.h"
#import "MeetingRTMManager.h"
#import "MeetingRTCManager.h"

@interface MeetingScreenComponent ()

@property (nonatomic, copy) void (^startBlock)(void);

@end

@implementation MeetingScreenComponent

- (instancetype)init
{
    self = [super init];
    if (self) {
        _isSharing = NO;
        _isTurnOffCamera = NO;
        [self updateAppdelegateScreenShareing:_isSharing];
        [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(extensionDidQuit) name:@"kNotificationByteOnExtensionQuit" object:nil];
        [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(extensionDidStartup) name:@"kNotificationByteOnExtensionStartup" object:nil];
    }
    return self;
}

#pragma mark - Publish Action

- (void)start:(void (^)(void))block {
    self.startBlock = block;
    ByteRTCScreenCaptureParam *screenShareParam = [[ByteRTCScreenCaptureParam alloc] init];
    screenShareParam.width = [SettingsService getScreenResolution].width;
    screenShareParam.height = [SettingsService getScreenResolution].height;
    screenShareParam.bitrate = [SettingsService getScreenKBitRate];
    screenShareParam.frameRate = [SettingsService getScreenFrameRate];
    
    NSString *extension = ExtensionName;
    NSString *groupId = APPGroupId;
    [[MeetingRTCManager shareRtc] startScreenSharingWithParam:screenShareParam preferredExtension:extension groupId:groupId];
}

- (void)stop {
    _isSharing = NO;
    [self updateAppdelegateScreenShareing:_isSharing];
    dispatch_async(dispatch_get_main_queue(), ^{
        [MeetingRTMManager endShareScreen];
    });
}

#pragma mark - NSNotificationCenter

- (void)extensionDidQuit {
    [self stop];
}

- (void)extensionDidStartup {
    _isSharing = YES;
    [self updateAppdelegateScreenShareing:_isSharing];
    dispatch_async(dispatch_get_main_queue(), ^{
        if (self.startBlock) {
            self.startBlock();
        }
    });
}

#pragma mark - Private Action

- (void)updateAppdelegateScreenShareing:(BOOL)isScreenShareing {

}

- (void)dealloc {
    [[NSNotificationCenter defaultCenter] removeObserver:self];
}

@end
