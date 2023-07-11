// 
// Copyright (c) 2023 Beijing Volcano Engine Technology Ltd.
// SPDX-License-Identifier: MIT
// 

#import <UIKit/UIKit.h>
#import "RoomVideoSession.h"

NS_ASSUME_NONNULL_BEGIN

@interface RoomAvatarRenderTagView : UIView

@property (nonatomic, strong) RoomVideoSession *userModel;

@property (nonatomic, assign) BOOL hiddenBG;

@property (nonatomic, strong) UIFont *font;

@property (nonatomic, strong) UIColor *textColor;

@end

NS_ASSUME_NONNULL_END
