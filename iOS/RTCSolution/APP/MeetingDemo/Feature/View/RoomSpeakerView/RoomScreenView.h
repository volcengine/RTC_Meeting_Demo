// 
// Copyright (c) 2023 Beijing Volcano Engine Technology Ltd.
// SPDX-License-Identifier: MIT
// 

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

@interface RoomScreenView : UIView

@property (nonatomic, copy) void (^clickCloseBlock) (void);

@end

NS_ASSUME_NONNULL_END