// 
// Copyright (c) 2023 Beijing Volcano Engine Technology Ltd.
// SPDX-License-Identifier: MIT
// 

#import <Foundation/Foundation.h>
#import "MeetingEndView.h"

NS_ASSUME_NONNULL_BEGIN

@interface MeetingEndComponent : NSObject

@property (nonatomic, copy) void (^clickButtonBlock)(MeetingButtonStatus status);

- (void)showWithStatus:(MeetingEndStatus)status;

@end

NS_ASSUME_NONNULL_END
