// 
// Copyright (c) 2023 Beijing Volcano Engine Technology Ltd.
// SPDX-License-Identifier: MIT
// 

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface MeetingControlRoomModel : NSObject

@property (nonatomic, copy) NSString *room_id;

@property (nonatomic, copy) NSString *host_id;

@property (nonatomic, assign) BOOL record;

@property (nonatomic, copy) NSString *screen_shared_uid;

@property (nonatomic, assign) NSInteger created_at;

@property (nonatomic, assign) NSInteger now;

@end

NS_ASSUME_NONNULL_END
