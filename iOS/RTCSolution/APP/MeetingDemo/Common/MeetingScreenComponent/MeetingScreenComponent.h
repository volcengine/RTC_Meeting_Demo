// 
// Copyright (c) 2023 Beijing Volcano Engine Technology Ltd.
// SPDX-License-Identifier: MIT
// 

#import <Foundation/Foundation.h>

@interface MeetingScreenComponent : NSObject

@property (nonatomic, assign, readonly) BOOL isSharing;

@property (nonatomic, assign) BOOL isTurnOffCamera;

- (void)start:(void (^)(void))block;

- (void)stop;

@end
