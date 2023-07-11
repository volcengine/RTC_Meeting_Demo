// 
// Copyright (c) 2023 Beijing Volcano Engine Technology Ltd.
// SPDX-License-Identifier: MIT
// 

#import "GalleryModeLineView.h"

@interface GalleryModeLineView ()

@end

@implementation GalleryModeLineView

- (instancetype)init {
    self = [super init];
    if (self) {
        NSMutableArray *lists = [[NSMutableArray alloc] init];
        for (int i = 0; i < 3; i++) {
            RoomAvatarRenderView *avatarView = [[RoomAvatarRenderView alloc] init];
            [lists addObject:avatarView];
        }
        _avatarViewLists = [lists copy];
    }
    return self;
}

@end
