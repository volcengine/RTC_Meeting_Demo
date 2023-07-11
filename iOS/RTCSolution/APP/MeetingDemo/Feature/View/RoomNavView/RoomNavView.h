// 
// Copyright (c) 2023 Beijing Volcano Engine Technology Ltd.
// SPDX-License-Identifier: MIT
// 

#import <UIKit/UIKit.h>
#import "RoomVideoSession.h"
@class RoomNavView;

typedef NS_ENUM(NSInteger, RoomNavStatus) {
    RoomNavStatusSwitchCamera,
    RoomNavStatusHangeup
};

NS_ASSUME_NONNULL_BEGIN

@protocol RoomNavViewDelegate <NSObject>

- (void)roomNavView:(RoomNavView *)roomNavView didSelectStatus:(RoomNavStatus)status;

@end

@interface RoomNavView : UIView

@property (nonatomic, strong) RoomVideoSession *localVideoSession;

@property (nonatomic, weak) id<RoomNavViewDelegate> delegate;

@property (nonatomic, assign) NSInteger meetingTime;

@end

NS_ASSUME_NONNULL_END
