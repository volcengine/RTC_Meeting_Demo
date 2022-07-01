//
//  HistoryTableCell.h
//  quickstart
//
//  Created by bytedance on 2021/3/23.
//  Copyright © 2021 . All rights reserved.
//

#import <UIKit/UIKit.h>
#import "MeetingControlRecordModel.h"

NS_ASSUME_NONNULL_BEGIN

@interface HistoryTableCell : UITableViewCell

@property (nonatomic, copy) MeetingControlRecordModel *model;

@end

NS_ASSUME_NONNULL_END
