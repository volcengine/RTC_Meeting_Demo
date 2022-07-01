//
//  RoomViewController+Sort.h
//  veRTC_Demo
//
//  Created by bytedance on 2021/6/8.
//  Copyright © 2021 . All rights reserved.
//

#import "RoomViewController.h"

NS_ASSUME_NONNULL_BEGIN

@interface RoomViewController (Sort)

- (void)statrSort:(void (^)(NSMutableArray *userLists))block;

- (void)stopSort;

- (NSMutableArray *)removeSortListsPromptly:(NSString *)uid;

- (NSMutableArray *)updateSortListsPromptly;

- (NSMutableArray<RoomVideoSession *> *)getSortUserLists;

@end

NS_ASSUME_NONNULL_END
