//
//  RoomSpeakerView.h
//  quickstart
//
//  Created by bytedance on 2021/3/25.
//  Copyright © 2021 . All rights reserved.
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

