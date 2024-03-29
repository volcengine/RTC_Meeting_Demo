// 
// Copyright (c) 2023 Beijing Volcano Engine Technology Ltd.
// SPDX-License-Identifier: MIT
// 

#import <UIKit/UIKit.h>
#import "RoomVideoSession.h"
#import "MeetingControlRoomModel.h"

@interface RoomSpeakerView : UIView

@property (nonatomic, strong) RoomVideoSession *localVideoSession;

@property (nonatomic, strong) MeetingControlRoomModel *currentRoomModel;

@property (nonatomic, copy) void (^clickOrientationBlock)(void);

@property (nonatomic, copy) void (^clickCloseBlock)(void);

@property (nonatomic, assign) BOOL isLandscape;

- (void)addScreenStreamsUid:(NSString *)streamsUid;

- (void)removeScreenStreamsUid:(NSString *)streamsUid;

- (void)bindVideoSessions:(NSArray<RoomVideoSession *> *)videoSessions;

@end

