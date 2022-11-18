//
//  MeetingScreenComponent.h
//  quickstart
//
//  Created by on 2021/4/6.
//  
//

#import <Foundation/Foundation.h>

@interface MeetingScreenComponent : NSObject

@property (nonatomic, assign, readonly) BOOL isSharing;

@property (nonatomic, assign) BOOL isTurnOffCamera;

- (void)start:(void (^)(void))block;

- (void)stop;

@end
