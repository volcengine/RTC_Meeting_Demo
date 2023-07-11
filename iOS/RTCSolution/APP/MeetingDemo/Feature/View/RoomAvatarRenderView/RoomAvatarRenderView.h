// 
// Copyright (c) 2023 Beijing Volcano Engine Technology Ltd.
// SPDX-License-Identifier: MIT
// 

#import <UIKit/UIKit.h>
#import "RoomVideoSession.h"

typedef NS_ENUM(NSInteger, RoomAvatarStatus) {
    //Normal Jiugongge
    RoomAvatarStatusGalleryNone,
    //One person nine squares
    RoomAvatarStatusGalleryOne,
    //Full Jiugongge
    RoomAvatarStatusGalleryFull,
    //Speaker
    RoomAvatarStatusSpaker,
};

@interface RoomAvatarRenderView : UIView

@property (nonatomic, assign) RoomAvatarStatus avatarStatus;

@property (nonatomic, strong) RoomVideoSession *userModel;

@end
