// 
// Copyright (c) 2023 Beijing Volcano Engine Technology Ltd.
// SPDX-License-Identifier: MIT
// 

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

@interface SettingsSliderView : UIView

@property (nonatomic, copy) void (^clickDismissBlock)(BOOL isCancel, id selectItem);

- (void)show:(CGFloat)minValue maxValue:(CGFloat)maxValue value:(CGFloat)value;

@end

NS_ASSUME_NONNULL_END
