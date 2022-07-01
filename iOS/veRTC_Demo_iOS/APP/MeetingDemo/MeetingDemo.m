//
//  MeetingDemo.m
//  AFNetworking
//
//  Created by bytedance on 2022/4/21.
//

#import <Core/NetworkReachabilityManager.h>
#import "MeetingDemo.h"
#import "MeetingLoginViewController.h"
#import "MeetingRTCManager.h"
#import "MeetingDemoConstants.h"

@implementation MeetingDemo

- (void)pushDemoViewControllerBlock:(void (^)(BOOL result))block {
    [super pushDemoViewControllerBlock:block];
    [MeetingRTCManager shareRtc].networkDelegate = [NetworkReachabilityManager sharedManager];
    [[MeetingRTCManager shareRtc] connect:@"meeting"
                               loginToken:[LocalUserComponents userModel].loginToken
                            volcAccountID:AccountID
                                 vodSpace:VodSpace
                                    block:^(BOOL result) {
        if (result) {
            MeetingLoginViewController *next = [[MeetingLoginViewController alloc] init];
            UIViewController *topVC = [DeviceInforTool topViewController];
            [topVC.navigationController pushViewController:next animated:YES];
        } else {
            [[ToastComponents shareToastComponents] showWithMessage:@"连接失败"];
        }
        if (block) {
            block(result);
        }
    }];
}

@end
