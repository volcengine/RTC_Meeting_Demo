//
//  RoomVideoSession.m
//  quickstart
//
//  Created by bytedance on 2021/4/2.
//  Copyright © 2021 . All rights reserved.
//

#import "RoomVideoSession.h"
#import "MeetingRTCManager.h"

@implementation RoomVideoSession

- (instancetype)initWithUid:(NSString *)uid {
    self = [super init];
    if (self) {
        self.uid = uid;
    }
    return self;
}

- (UIView *)streamView {
    if (!_streamView) {
        _streamView = [[UIView alloc] init];
        ByteRTCVideoCanvas *canvas = [[ByteRTCVideoCanvas alloc] init];
        canvas.uid = self.uid;
        canvas.renderMode = ByteRTCRenderModeHidden;
        canvas.view.backgroundColor = [UIColor clearColor];
        canvas.view = _streamView;
        if (self.isLoginUser) {
            //Local Video
            [[MeetingRTCManager shareRtc] setupLocalVideo:canvas];
        } else {
            //Remote Video
            [[MeetingRTCManager shareRtc] setupRemoteVideo:canvas];
        }
    }
    return _streamView;
}

+ (RoomVideoSession *)roomVideoSessionToMeetingControlUserModel:(MeetingControlUserModel *)meetingControlUserModel {
    RoomVideoSession *roomVideoSession = [[RoomVideoSession alloc] initWithUid:meetingControlUserModel.user_id];
    roomVideoSession.name = meetingControlUserModel.user_name;
    roomVideoSession.roomId = meetingControlUserModel.room_id;
    roomVideoSession.isScreen = meetingControlUserModel.is_sharing;
    roomVideoSession.isLoginUser = NO;
    roomVideoSession.isHost = meetingControlUserModel.is_host;
    roomVideoSession.userUniform = meetingControlUserModel.user_uniform_name;

    roomVideoSession.isEnableAudio = meetingControlUserModel.is_mic_on;
    roomVideoSession.isEnableVideo = meetingControlUserModel.is_camera_on;
    return roomVideoSession;
}

- (BOOL)isLoginUser {
    if ([self.uid isEqualToString:[LocalUserComponents userModel].uid]) {
        return YES;
    } else {
        return NO;
    }
}

- (BOOL)isVideoStream {
    if ([self.uid isEqualToString:[LocalUserComponents userModel].uid]) {
        return YES;
    } else {
        return _isVideoStream;
    }
}

@end
