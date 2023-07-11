// 
// Copyright (c) 2023 Beijing Volcano Engine Technology Ltd.
// SPDX-License-Identifier: MIT
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
