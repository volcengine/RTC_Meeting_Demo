// 
// Copyright (c) 2023 Beijing Volcano Engine Technology Ltd.
// SPDX-License-Identifier: MIT
// 

#import <UIKit/UIKit.h>
#import "RoomVideoSession.h"

@interface RoomMatrixView : UIView

- (void)bindVideoSessions:(NSArray<RoomVideoSession *> *)videoSessions;

@end
