//
//  RoomScreenView.h
//  quickstart
//
//  Created by on 2021/3/25.
//  
//

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

@interface RoomScreenView : UIView

@property (nonatomic, copy) void (^clickCloseBlock) (void);

@end

NS_ASSUME_NONNULL_END
