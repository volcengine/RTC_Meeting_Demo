// 
// Copyright (c) 2023 Beijing Volcano Engine Technology Ltd.
// SPDX-License-Identifier: MIT
// 

#import <UIKit/UIKit.h>
#import "HistoryTableCell.h"

NS_ASSUME_NONNULL_BEGIN

@interface HistoryTableView : UIView

@property (nonatomic, assign) BOOL isDelete;

@property (nonatomic, copy) NSArray *dataLists;

@end

NS_ASSUME_NONNULL_END
