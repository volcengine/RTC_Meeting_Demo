// 
// Copyright (c) 2023 Beijing Volcano Engine Technology Ltd.
// SPDX-License-Identifier: MIT
// 

#import "MeetingNavViewController.h"

NS_ASSUME_NONNULL_BEGIN

@interface SettingViewController : MeetingNavViewController

/*
 * On/off real-time information callback
 */
@property (nonatomic, copy) void (^switchValueChangeBlock)(BOOL on);

@end

NS_ASSUME_NONNULL_END
