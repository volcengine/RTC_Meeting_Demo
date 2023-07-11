// 
// Copyright (c) 2023 Beijing Volcano Engine Technology Ltd.
// SPDX-License-Identifier: MIT
// 

#import "MeetingDemo.h"
#import "JoinRTSParams.h"
#import "MeetingLoginViewController.h"
#import "MeetingRTCManager.h"
#import "MeetingDemoConstants.h"

@implementation MeetingDemo

- (void)pushDemoViewControllerBlock:(void (^)(BOOL result))block {
    [super pushDemoViewControllerBlock:block];
    
    JoinRTSInputModel *inputModel = [[JoinRTSInputModel alloc] init];
    inputModel.scenesName = @"meeting";
    inputModel.loginToken = [LocalUserComponent userModel].loginToken;
    inputModel.volcAccountID = AccountID;
    inputModel.vodSpace = VodSpace;
    __weak __typeof(self) wself = self;
    [JoinRTSParams getJoinRTSParams:inputModel
                             block:^(JoinRTSParamsModel * _Nonnull model) {
        [wself joinRTS:model block:block];
    }];
}

- (void)joinRTS:(JoinRTSParamsModel * _Nonnull)model
          block:(void (^)(BOOL result))block{
    if (!model) {
        [[ToastComponent shareToastComponent] showWithMessage:@"连接失败"];
        if (block) {
            block(NO);
        }
        return;
    }
    // Connect RTS
    [[MeetingRTCManager shareRtc] connect:model.appId
                                 RTSToken:model.RTSToken
                                serverUrl:model.serverUrl
                                serverSig:model.serverSignature
                                      bid:model.bid
                                    block:^(BOOL result) {
        if (result) {
            MeetingLoginViewController *next = [[MeetingLoginViewController alloc] init];
            UIViewController *topVC = [DeviceInforTool topViewController];
            [topVC.navigationController pushViewController:next animated:YES];
        } else {
            [[ToastComponent shareToastComponent] showWithMessage:@"连接失败"];
        }
        if (block) {
            block(result);
        }
    }];
}

@end
