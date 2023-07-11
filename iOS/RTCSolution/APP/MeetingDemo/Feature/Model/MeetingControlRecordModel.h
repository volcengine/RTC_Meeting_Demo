// 
// Copyright (c) 2023 Beijing Volcano Engine Technology Ltd.
// SPDX-License-Identifier: MIT
// 

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface MeetingControlRecordModel : NSObject

@property (nonatomic, copy) NSString *room_id;

@property (nonatomic, copy) NSString *download_url;

@property (nonatomic, assign) NSInteger created_at;

@property (nonatomic, copy) NSString *vid;

@property (nonatomic, assign) BOOL video_holder;

@end

NS_ASSUME_NONNULL_END
