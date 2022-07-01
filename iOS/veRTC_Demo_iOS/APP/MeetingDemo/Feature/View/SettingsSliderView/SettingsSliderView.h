//
//  SettingsSliderView.h
//  quickstart
//
//  Created by bytedance on 2021/3/23.
//  Copyright © 2021 . All rights reserved.
//

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

@interface SettingsSliderView : UIView

@property (nonatomic, copy) void (^clickDismissBlock)(BOOL isCancel, id selectItem);

- (void)show:(CGFloat)minValue maxValue:(CGFloat)maxValue value:(CGFloat)value;

@end

NS_ASSUME_NONNULL_END
