// 
// Copyright (c) 2023 Beijing Volcano Engine Technology Ltd.
// SPDX-License-Identifier: MIT
// 

#import "RoomViewController.h"
#import "SettingViewController.h"
#import "SettingsService.h"
#import "RoomNavView.h"
#import "UIViewController+Orientation.h"
#import "RoomViewController+Listener.h"
#import "RoomViewController+Sort.h"
#import "RoomSpeakerView.h"
#import "RoomParamInfoView.h"
#import "MeetingEndComponent.h"
#import "MeetingScreenComponent.h"
#import "MeetingRTCManager.h"
#import "MeetingRTSManager.h"
#import "NetworkingTool.h"

@interface RoomViewController () <UINavigationControllerDelegate, MeetingRTCManagerDelegate, RoomNavViewDelegate, RoomBottomViewDelegate>
@property (nonatomic, strong) RoomNavView *navView;
@property (nonatomic, strong) RoomParamInfoView *paramInfoView;
@property (nonatomic, strong) UIView *toastView;
@property (nonatomic, strong) UIImageView *recordTipView;
@property (nonatomic, weak) UserListViewController *userListViewController;
@property (nonatomic, strong) RoomMatrixView *videoMatrixView;
@property (nonatomic, strong) RoomSpeakerView *videoSpeakerView;
@property (nonatomic, strong) MeetingEndComponent *endComponent;

@property (nonatomic, strong) NSString *recordID;
@property (nonatomic, strong) NSMutableDictionary <NSString *, NSString *> *currentStreamDic;
@property (nonatomic, assign) BOOL isLandscape;
@property (nonatomic, strong) MeetingScreenComponent *screenComponent;

@property (nonatomic, strong) dispatch_semaphore_t lock;

@end

@implementation RoomViewController

- (instancetype)initWithVideoSession:(RoomVideoSession *)loginModel
                           userLists:(NSArray<RoomVideoSession *> *)userLists {
    self = [super init];
    if (self) {
        [UIApplication sharedApplication].idleTimerDisabled = YES;
        _localVideoSession = loginModel;
        _isLandscape = NO;
        [self.userDataPool removeAllObjects];
        [self.userDataPool addObjectsFromArray:userLists];
    }
    return self;
}

- (void)viewDidLoad {
    [super viewDidLoad];
    self.view.backgroundColor = [UIColor colorFromHexString:@"#1D2129"];
    
    [self addSocketListener];
    [self addOrientationNotice];
    [self createUIComponent];

    [self loadDataWithMeetingContrller:NO];
    [MeetingRTCManager shareRtc].delegate = self;
    [[MeetingRTCManager shareRtc] joinChannelWithRoomVideoSession:_localVideoSession];
    
    __weak __typeof(self) wself = self;
    self.videoSpeakerView.clickOrientationBlock = ^{
        if (!wself.isLandscape) {
            [wself setDeviceInterfaceOrientation:UIDeviceOrientationLandscapeLeft];
        } else {
            [wself setDeviceInterfaceOrientation:UIDeviceOrientationPortrait];
        }
    };
    
    [self statrSort:^(NSMutableArray * _Nonnull userLists) {
        [wself sortEndCallback:userLists];
    }];
    
    //Refresh now render view
    [self updateRenderModeView:[self updateSortListsPromptly]];
}

- (void)viewWillAppear:(BOOL)animated {
    [super viewWillAppear:animated];
    
    [self.navigationController setNavigationBarHidden:YES animated:NO];
    [[MeetingRTCManager shareRtc] updateRtcVideoParams];
    
    if (self.currentRoomModel) {
        if (!self.videoSpeakerView.hidden) {
            [self setAllowAutoRotate:ScreenOrientationLandscapeAndPortrait];
        } else {
            [self setAllowAutoRotate:ScreenOrientationPortrait];
        }
    }
}

- (void)viewDidDisappear:(BOOL)animated {
    [super viewDidDisappear:animated];
    
    [self setAllowAutoRotate:ScreenOrientationPortrait];
}

- (void)orientationDidChang:(BOOL)isLandscape {
    _isLandscape = isLandscape;
    CGFloat navHeight = 44 + [DeviceInforTool getStatusBarHight];
    CGFloat bottomBottom = 0;
    if (isLandscape) {
        navHeight = 0;
        bottomBottom = (128/2 + [DeviceInforTool getVirtualHomeHeight] * 2);
    }
    
    [self.navView mas_updateConstraints:^(MASConstraintMaker *make) {
        make.height.mas_equalTo(navHeight);
    }];
    [self.bottomView mas_updateConstraints:^(MASConstraintMaker *make) {
        make.bottom.equalTo(self.view).offset(bottomBottom);
    }];
    self.videoSpeakerView.isLandscape = isLandscape;
    [self.bottomView dismissMoreView];
    [self updateRecordTipViewTop];
}

#pragma mark - Publish Action

- (void)hangUp:(BOOL)isManual {
    
    
    //Stop sort timer
    [self stopSort];
    //User screen sharing
    if ([self.currentRoomModel.screen_shared_uid isEqualToString:self.localVideoSession.uid]) {
        [MeetingRTSManager endShareScreen];
        [[MeetingRTCManager shareRtc] stopScreenSharing];
    }
    //socket api end/leave Meeting
    if ([self.currentRoomModel.host_id isEqualToString:self.localVideoSession.uid]) {
        [MeetingRTSManager endMeeting];
    } else {
        [MeetingRTSManager leaveMeeting];
    }
    //sdk api end Meeting
    [[MeetingRTCManager shareRtc] leaveChannel];
    //ui
    [self dismissViewControllerAnimated:YES completion:nil];
    if (isManual && self.closeRoomBlock) {
        BOOL isEnableAudio = ([self.bottomView getButtonStatus:RoomBottomStatusMic] == ButtonStatusActive) ? NO : YES;
        BOOL isEnableVideo = ([self.bottomView getButtonStatus:RoomBottomStatusCamera] == ButtonStatusActive) ? NO : YES;
        self.closeRoomBlock(isEnableAudio, isEnableVideo);
    }
}

- (void)updateModeWithStatus:(RoomModeStatus)roomModeStatus {
    if (roomModeStatus == RoomModeStatusGallery) {
        self.videoSpeakerView.hidden = YES;
        self.videoMatrixView.hidden = NO;
        [self setAllowAutoRotate:ScreenOrientationPortrait];
    } else {
        self.videoSpeakerView.localVideoSession = self.localVideoSession;
        self.videoSpeakerView.currentRoomModel = self.currentRoomModel;
        self.videoMatrixView.hidden = YES;
        self.videoSpeakerView.hidden = NO;
        [self setAllowAutoRotate:ScreenOrientationLandscapeAndPortrait];
    }
    [self updateRecordTipViewTop];
}

- (void)updateRecordTipViewTop {
    CGFloat recorderTop = 12;
    if (!self.videoMatrixView.hidden) {
        recorderTop = 12;
    } else if (!self.videoSpeakerView.hidden) {
        if (self.isLandscape) {
            recorderTop = 30;
        } else {
            recorderTop = 104;
        }
    } else {
        recorderTop = 12;
    }
    [self.recordTipView mas_updateConstraints:^(MASConstraintMaker *make) {
        make.top.equalTo(self.navView.mas_bottom).offset(recorderTop);
    }];
}

- (void)updateRecordTipStatusWithHidden:(BOOL)isHidden {
    self.recordTipView.hidden = isHidden;
    [self.bottomView updateButtonStatus:RoomBottomStatusRecord close:!isHidden];
}

- (void)restoreScreenOrientation {
    if (self.isLandscape) {
        [self setDeviceInterfaceOrientation:UIDeviceOrientationPortrait];
    }
}

#pragma mark - MeetingRTCManagerDelegate

- (void)meetingRTCManager:(MeetingRTCManager *)manager onRoomStateChanged:(RTCJoinModel *)joinModel {
    if (joinModel.errorCode == 0) {
        if (joinModel.joinType == 0) {
            // 首次进房成功
            [self getMeetingUserInfo];
        } else {
            // 断网重连成功后，进房成功
            [self meetingReconnect];
        }
    }
}

- (void)rtcManager:(MeetingRTCManager * _Nonnull)rtcManager didStreamAdded:(NSString * _Nullable)streamsUid {
    if (NOEmptyStr(streamsUid)) {
        [self.currentStreamDic setValue:@"1" forKey:streamsUid];
    }
}

- (void)rtcManager:(MeetingRTCManager *_Nonnull)rtcManager didStreamRemoved:(NSString * _Nullable)streamsUid {
    if (NOEmptyStr(streamsUid)) {
        [self.currentStreamDic removeObjectForKey:streamsUid];
    }
}

- (void)rtcManager:(MeetingRTCManager * _Nonnull)rtcManager didScreenStreamAdded:(NSString *_Nullable)screenStreamsUid {
    [self.videoSpeakerView addScreenStreamsUid:screenStreamsUid];
}

- (void)rtcManager:(MeetingRTCManager *_Nonnull)rtcManager didScreenStreamRemoved:(NSString *)screenStreamsUid {
    [self.videoSpeakerView removeScreenStreamsUid:screenStreamsUid];
}

- (void)rtcManager:(MeetingRTCManager *_Nonnull)rtcManager onRemoteVideoSizeChanged:(NSString *)screenStreamsUid {
    
    [self.videoSpeakerView removeScreenStreamsUid:screenStreamsUid];
    
    [self.videoSpeakerView addScreenStreamsUid:screenStreamsUid];
    
}

- (void)meetingRTCManager:(MeetingRTCManager *)meetingRTCManager changeParamInfo:(RoomParamInfoModel *)model {
    self.paramInfoView.paramInfoModel = model;
}

- (void)rtcManager:(MeetingRTCManager *)rtcManager reportAllAudioVolume:(NSDictionary<NSString *, NSNumber *> *)volumeInfo {
    [self updateRenderModeViewUserRankeWithAudioVolume:volumeInfo];
}

#pragma mark - RoomBottomViewDelegate

- (void)roomBottomView:(RoomBottomView *)roomBottomView itemButton:(RoomItemButton *)itemButton didSelectStatus:(RoomBottomStatus)status {
    if (status == RoomBottomStatusMic) {
        [self clickRoomBottomStatusMic:itemButton];
    } else if (status == RoomBottomStatusCamera) {
        [self clickRoomBottomStatusCamera:itemButton];
    } else if (status == RoomBottomStatusScreen) {
        if (itemButton.status == ButtonStatusNone) {
            __weak __typeof(self) wself = self;
            [self.screenComponent start:^{
                [MeetingRTSManager startShareScreen];
                [wself startScreenWithCamera];
            }];
        } else {
            if (![self.currentRoomModel.screen_shared_uid isEqualToString:self.localVideoSession.uid]) {
                [[ToastComponent shareToastComponent] showWithMessage:@"屏幕共享发起失败，请提示前一位参会者结束共享"];
            }
        }
        self.videoSpeakerView.clickCloseBlock = ^{
            [[MeetingRTCManager shareRtc] stopScreenSharing];
        };
    } else if (status == RoomBottomStatusPar) {
        UserListViewController *userListViewController = [[UserListViewController alloc] init];
        userListViewController.localVideoSession = self.localVideoSession;
        userListViewController.isLoginHost = [self.localVideoSession.uid isEqualToString:self.currentRoomModel.host_id] ? YES : NO;
        userListViewController.videoSessions = [self getSortUserLists];
        [self.navigationController pushViewController:userListViewController animated:YES];
        self.userListViewController = userListViewController;
    } else if (status == RoomBottomStatusAudio) {
        BOOL micEarpiece = (itemButton.status == ButtonStatusActive);
        if (micEarpiece) {
            itemButton.status = ButtonStatusNone;
            itemButton.desTitle = @"扬声器";
        } else {
            itemButton.status = ButtonStatusActive;
            itemButton.desTitle = @"听筒";
        }
        [[MeetingRTCManager shareRtc] setEnableSpeakerphone:micEarpiece];
    } else if (status == RoomBottomStatusSet) {
        SettingViewController *settingsVC = [[SettingViewController alloc] init];
        [self.navigationController pushViewController:settingsVC animated:YES];
        __weak typeof(self) weakSelf = self;
        settingsVC.switchValueChangeBlock = ^(BOOL on) {
            weakSelf.paramInfoView.hidden = !on;
        };
    } else if (status == RoomBottomStatusRecord) {
        if (itemButton.status == ButtonStatusNone) {
            if ([self.currentRoomModel.host_id isEqualToString:self.localVideoSession.uid]) {
                //ui
                [self updateRecordTipStatusWithHidden:NO];
            
                //Start record Meeting socket api
                __weak typeof(self) weakSelf = self;
                [MeetingRTSManager recordMeeting:[self getRecordUids]
                                        screenId:self.currentRoomModel.screen_shared_uid
                                           block:^(RTSACKModel * _Nonnull ackModel) {
                    if (ackModel.code == 804) {
                        [weakSelf updateRecordTipStatusWithHidden:YES];
                        [[ToastComponent shareToastComponent] showWithMessage:@"vod配置错误，请检查参数。"];
                    }
                    else if (!ackModel.result) {
                        [weakSelf updateRecordTipStatusWithHidden:YES];
                        [[ToastComponent shareToastComponent] showWithMessage:ackModel.message];
                    }
                }];
            } else {
                [[ToastComponent shareToastComponent] showWithMessage:@"如需录制会议，请提醒主持人开启录制。"];
            }
        }
    } else {
        
    }
}

#pragma mark - RoomNavViewDelegate

- (void)roomNavView:(RoomNavView *)roomNavView didSelectStatus:(RoomNavStatus)status {
    if (status == RoomNavStatusHangeup) {
        [self showEndView];
    } else if (status == RoomNavStatusSwitchCamera) {
        if (self.localVideoSession.isEnableVideo) {
            [[MeetingRTCManager shareRtc] switchCamera];
        }
    }
}

#pragma mark - RoomViewController + Sort

- (void)sortEndCallback:(NSMutableArray * _Nonnull)userLists {
    NSDictionary *subscribeUidDic = [[MeetingRTCManager shareRtc] getSubscribeUidDic];
    //Unsubscribe first
    if (subscribeUidDic.count > 0) {
        for (int i = 0; i < userLists.count; i++) {
            RoomVideoSession *userModel = userLists[i];
            NSString *value = [subscribeUidDic objectForKey:userModel.uid];
            if ([value integerValue] == 1 && i >= MaxAvatarNumber) {
                [[MeetingRTCManager shareRtc] unsubscribe:userModel.uid];
                userModel.isVideoStream = NO;
            }
        }
    }

    for (int i = 0; i < userLists.count; i++) {
        RoomVideoSession *userModel = userLists[i];
        if (![userModel.uid isEqualToString:self.localVideoSession.uid]) {
            //Don't subscribe self
            NSString *isStream = self.currentStreamDic[userModel.uid];
            if (i < MaxAvatarNumber) {
                if ([isStream integerValue] == 1) {
                    //subscribe
                    userModel.isVideoStream = YES;
                    [[MeetingRTCManager shareRtc] subscribeStream:userModel.uid];
                } else {
                    //unsubscribe
                    userModel.isVideoStream = NO;
                    [[MeetingRTCManager shareRtc] unsubscribe:userModel.uid];
                }
            } else {
                
            }
        }
    }
    
    [self updateRenderModeView:userLists];
    
    if ([self.localVideoSession.uid isEqualToString:self.currentRoomModel.host_id]) {
        [self updateRecordLayout:[self getRecordUids]
                        screenId:self.currentRoomModel.screen_shared_uid];
    }
}

- (void)updateRenderModeViewUserRankeWithAudioVolume:(NSDictionary<NSString *, NSNumber *> *)volumeInfo {
    dispatch_semaphore_wait(self.lock, DISPATCH_TIME_FOREVER);
    for (RoomVideoSession *userModel in self.userDataPool) {
        NSNumber *volumeNumber = [volumeInfo objectForKey:userModel.uid];
        if (volumeNumber) {
            userModel.volume = [volumeNumber integerValue];
        } else {
            userModel.volume = 0;
        }
    }
    dispatch_semaphore_signal(self.lock);
}

- (void)updateRenderModeViewWithCameraStatus:(NSString *)uid enableCamera:(BOOL)isEnable {
    dispatch_semaphore_wait(self.lock, DISPATCH_TIME_FOREVER);
    for (RoomVideoSession *userModel in self.userDataPool) {
        if ([userModel.uid isEqualToString:uid]) {
            userModel.isEnableVideo = isEnable;
            break;
        }
    }
    dispatch_semaphore_signal(self.lock);
}

- (void)updateRenderModeViewWithMicStatus:(NSString *)uid enableMic:(BOOL)isEnable {
    dispatch_semaphore_wait(self.lock, DISPATCH_TIME_FOREVER);
    if (IsEmptyStr(uid)) {
        //mute all user
        for (RoomVideoSession *userModel in self.userDataPool) {
            if (![userModel.uid isEqualToString:self.currentRoomModel.host_id]) {
                userModel.isEnableAudio = isEnable;
            }
        }
    } else {
        //mute user
        for (RoomVideoSession *userModel in self.userDataPool) {
            if ([userModel.uid isEqualToString:uid]) {
                userModel.isEnableAudio = isEnable;
                break;
            }
        }
    }
    dispatch_semaphore_signal(self.lock);
}

- (void)updateRenderModeViewWithHost:(NSString *)hostID {
    dispatch_semaphore_wait(self.lock, DISPATCH_TIME_FOREVER);
    for (RoomVideoSession *userModel in self.userDataPool) {
        //Because there is only one host, the status of everyone needs to be updated
        userModel.isHost = ([userModel.uid isEqualToString:hostID]) ? YES : NO;
    }
    self.userListViewController.isLoginHost = [self.localVideoSession.uid isEqualToString:self.currentRoomModel.host_id] ? YES : NO;
    dispatch_semaphore_signal(self.lock);
}

- (void)updateRenderModeViewWithScreenStatus:(NSString *)uid enableScreen:(BOOL)isShare {
    dispatch_semaphore_wait(self.lock, DISPATCH_TIME_FOREVER);
    for (RoomVideoSession *userModel in self.userDataPool) {
        if (isShare) {
            if ([userModel.uid isEqualToString:uid]) {
                userModel.isScreen = YES;
            } else {
                userModel.isScreen = NO;
            }
        } else {
            userModel.isScreen = NO;
        }
    }
    dispatch_semaphore_signal(self.lock);
}

#pragma mark - Network request Action

- (void)loadDataWithMeetingContrller:(BOOL)isUserInfo {
    __weak __typeof(self) wself = self;
    [MeetingRTSManager getMeetingInfoWithBlock:^(MeetingControlRoomModel * _Nonnull roomModel, RTSACKModel * _Nonnull model) {
        if (model.result) {
            wself.currentRoomModel = roomModel;
            BOOL isOpenScreen = (roomModel.screen_shared_uid.length > 0);
            [wself updateModeWithStatus:!isOpenScreen ? RoomModeStatusGallery : RoomModeStatusSpaker];
            [wself updateRecordTipStatusWithHidden:!roomModel.record];
            [wself.bottomView updateButtonStatus:RoomBottomStatusMic close:!wself.localVideoSession.isEnableAudio];
            [wself.bottomView updateButtonStatus:RoomBottomStatusCamera close:!wself.localVideoSession.isEnableVideo];
            [wself.bottomView updateButtonStatus:RoomBottomStatusScreen close:isOpenScreen];
            [wself updateNavTimeWithModel:roomModel];
            if (isUserInfo) {
                [wself getMeetingUserInfo];
            }
        } else {
            AlertActionModel *alertModel = [[AlertActionModel alloc] init];
            alertModel.title = @"确定";
            [[AlertActionManager shareAlertActionManager] showWithMessage:model.message actions:@[alertModel]];
        }
    }];
}

- (void)meetingReconnect {
    __weak __typeof(self) wself = self;
    [MeetingRTSManager reconnectWithBlock:^(RTSACKModel * _Nonnull model) {
        NSString *type = @"";
        if (model.result) {
            type = @"resume";
        } else if (model.code == RTSStatusCodeUserIsInactive ||
                   model.code == RTSStatusCodeRoomDisbanded ||
                   model.code == RTSStatusCodeUserNotFound) {
            type = @"exit";
        } else {
            
        }
        if (type.length > 0) {
            NSDictionary *dic = @{@"type" : type ?: @""};
            [wself meetingControlChange:dic];
        }
    }];
}

- (void)getMeetingUserInfo {
    __weak __typeof(self) wself = self;
    [MeetingRTSManager getMeetingUserInfo:@"" block:^(NSArray<RoomVideoSession *> * _Nonnull userLists, RTSACKModel * _Nonnull model) {
        //Refresh now render view
        dispatch_semaphore_wait(wself.lock, DISPATCH_TIME_FOREVER);
        [wself.userDataPool removeAllObjects];
        [wself.userDataPool addObjectsFromArray:userLists];
        dispatch_semaphore_signal(wself.lock);
        [wself updateRenderModeView:[wself updateSortListsPromptly]];
    }];
}

#pragma mark - Private Action

- (void)meetingControlChange:(NSDictionary *)dic {
    if ([dic isKindOfClass:[NSDictionary class]]) {
        NSString *type = dic[@"type"];
        if ([type isEqualToString:@"resume"]) {
            [self loadDataWithMeetingContrller:YES];
        } else if ([type isEqualToString:@"exit"]) {
            [self hangUp:YES];
        } else {
            
        }
    }
}

- (void)updateNavTimeWithModel:(MeetingControlRoomModel *)roomModel {
    NSInteger time = (roomModel.now - roomModel.created_at) / 1000000000;
    if (time < 0) {
        time = 0;
    }
    self.navView.meetingTime = time;
    self.navView.localVideoSession = self.localVideoSession;
}

- (void)addUser:(RoomVideoSession *)roomUserModel {
    dispatch_semaphore_wait(self.lock, DISPATCH_TIME_FOREVER);
    //重复数据删除
    //Deduplication
    NSInteger index = -1;
    for (int i = 0; i < self.userDataPool.count; i++) {
        RoomVideoSession *userModel = self.userDataPool[i];
        if ([userModel.uid isEqualToString:roomUserModel.uid]) {
            index = i;
            break;
        }
    }
    if (index >= 0) {
        [self.userDataPool replaceObjectAtIndex:index withObject:roomUserModel];
    } else {
        if ([roomUserModel.uid isEqualToString:self.localVideoSession.uid]) {
            [self.userDataPool insertObject:roomUserModel atIndex:0];
        } else {
            [self.userDataPool addObject:roomUserModel];
        }
    }
    dispatch_semaphore_signal(self.lock);
}

- (void)removeUser:(NSString *)uid {
    NSMutableArray *sortUserLists = [self removeSortListsPromptly:uid];
    [self updateRenderModeView:sortUserLists];
}


- (void)updateRenderModeView:(NSArray *)userLists {
    dispatch_async(dispatch_get_main_queue(), ^{
        if (!self.videoMatrixView.hidden) {
            [self.videoMatrixView bindVideoSessions:userLists];
        } else if (!self.videoSpeakerView.hidden) {
            [self.videoSpeakerView bindVideoSessions:userLists];
        } else {
            //error
        }
        self.userListViewController.videoSessions = [userLists mutableCopy];
    });
}

- (void)createUIComponent {
    [self.view addSubview:self.navView];
    [self.view addSubview:self.videoMatrixView];
    [self.view addSubview:self.videoSpeakerView];
    [self.view addSubview:self.paramInfoView];
    [self.view addSubview:self.bottomView];
    [self.view addSubview:self.toastView];
    [self.view addSubview:self.recordTipView];
    
    [self.navView mas_makeConstraints:^(MASConstraintMaker *make) {
        make.top.left.right.equalTo(self.view);
        make.height.mas_equalTo(44 + [DeviceInforTool getStatusBarHight]);
    }];

    [self.videoMatrixView mas_makeConstraints:^(MASConstraintMaker *make) {
        make.top.equalTo(self.navView.mas_bottom);
        make.left.right.equalTo(self.view);
        make.bottom.equalTo(self.bottomView.mas_top);
    }];
    
    [self.videoSpeakerView mas_makeConstraints:^(MASConstraintMaker *make) {
        make.edges.equalTo(self.videoMatrixView);
    }];
    
    [self.bottomView mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.right.equalTo(self.view);
        make.bottom.equalTo(self.view).offset(0);
        make.height.mas_equalTo(128/2 + [DeviceInforTool getVirtualHomeHeight]);
    }];

    [self.paramInfoView mas_makeConstraints:^(MASConstraintMaker *make) {
        make.left.equalTo(self.view);
        make.bottom.equalTo(self.bottomView.mas_top);
        make.width.mas_equalTo(150.f);
        make.height.mas_equalTo(16 * 16 + 10 + 10);
    }];
    
    [self.toastView mas_makeConstraints:^(MASConstraintMaker *make) {
        make.edges.equalTo(self.view);
    }];
    
    [self.recordTipView mas_makeConstraints:^(MASConstraintMaker *make) {
        make.size.mas_equalTo(CGSizeMake(130/2, 60/2));
        make.top.equalTo(self.navView.mas_bottom).offset(12);
        make.right.equalTo(self.view).offset(-12);
    }];
    
    self.paramInfoView.hidden = ![SettingsService getOpenParam];
}

- (void)showEndView {
    [self.endComponent showWithStatus:[self.currentRoomModel.host_id isEqualToString:self.localVideoSession.uid] ? MeetingEndStatusHost : MeetingEndStatusNone];
    __weak __typeof(self) wself = self;
    self.endComponent.clickButtonBlock = ^(MeetingButtonStatus status) {
        if (status == MeetingButtonStatusEnd ||
            status == MeetingButtonStatusLeave) {
            [wself hangUp:YES];
        } else if (status == MeetingButtonStatusCancel) {
            //cancel
        }
        wself.endComponent = nil;
    };
}

- (NSArray<NSString *> *)getRecordUids {
    NSArray *userLists = [self getSortUserLists];
    NSInteger maxLimit = 9;
    if (self.currentRoomModel.screen_shared_uid.length > 0) {
        //There is screen flow
        maxLimit = 8;
    }
    NSInteger currentLen = MIN(userLists.count, maxLimit);
    NSArray *lists = [userLists subarrayWithRange:NSMakeRange(0, currentLen)];
    NSMutableArray *uids = [[NSMutableArray alloc] init];
    for (int i = 0; i < lists.count; i++) {
        RoomVideoSession *userModel = lists[i];
        [uids addObject:userModel.uid];
    }
    return [uids copy];
}

- (void)startScreenWithCamera {
    ButtonStatus cameraStatus = [self.bottomView getButtonStatus:RoomBottomStatusCamera];
    if (cameraStatus == ButtonStatusNone) {
        //Turn on screen sharing, turn off the camera
        self.localVideoSession.isEnableVideo = NO;
        [[MeetingRTCManager shareRtc] enableLocalVideo:NO];
        [MeetingRTSManager turnOffCamera];
        [self.bottomView updateButtonStatus:RoomBottomStatusCamera close:YES];
        self.screenComponent.isTurnOffCamera = YES;
    }
}

- (void)clickRoomBottomStatusMic:(RoomItemButton *)itemButton {
    [SystemAuthority authorizationStatusWithType:AuthorizationTypeAudio block:^(BOOL isAuthorize) {
        if (isAuthorize) {
            itemButton.status = (itemButton.status == ButtonStatusActive) ? ButtonStatusNone : ButtonStatusActive;
            BOOL isEnableAudio = self.localVideoSession.isEnableAudio;
            [[MeetingRTCManager shareRtc] publishStreamAudio:!isEnableAudio];
            [SystemAuthority autoJumpWithAuthorizationStatusWithType:AuthorizationTypeAudio];
            if (itemButton.status == ButtonStatusActive) {
                //off
                [MeetingRTSManager turnOffMic];
                self.localVideoSession.isEnableAudio = NO;
            } else if (itemButton.status == ButtonStatusNone) {
                [MeetingRTSManager turnOnMic];
                self.localVideoSession.isEnableAudio = YES;
            }
        } else {
            [[ToastComponent shareToastComponent] showWithMessage:@"麦克风权限已关闭，请至设备设置页开启"];
        }
    }];
}

- (void)clickRoomBottomStatusCamera:(RoomItemButton *)itemButton {
    [SystemAuthority authorizationStatusWithType:AuthorizationTypeCamera block:^(BOOL isAuthorize) {
        if (isAuthorize) {
            if (self.screenComponent.isSharing) {
                //Screen Sharing
                [[ToastComponent shareToastComponent] showWithMessage:@"结束共享后方可打开摄像头"];
            } else {
                itemButton.status = (itemButton.status == ButtonStatusActive) ? ButtonStatusNone : ButtonStatusActive;
                self.localVideoSession.isEnableVideo = !self.localVideoSession.isEnableVideo;
                [[MeetingRTCManager shareRtc] enableLocalVideo:self.localVideoSession.isEnableVideo];
                [SystemAuthority autoJumpWithAuthorizationStatusWithType:AuthorizationTypeCamera];
                if (itemButton.status == ButtonStatusActive) {
                    //off
                    [MeetingRTSManager turnOffCamera];
                } else if (itemButton.status == ButtonStatusNone) {
                    [MeetingRTSManager turnOnCamera];
                }
            }
        } else {
            [[ToastComponent shareToastComponent] showWithMessage:@"摄像头权限已关闭，请至设备设置页开启"];
        }
    }];
}

- (void)updateRecordLayout:(NSArray<NSString *> *)uids screenId:(NSString *)screenId {
    NSString *temoRecordID = @"";
    for (int i = 0; i < uids.count; i++) {
        temoRecordID = [NSString stringWithFormat:@"%@%@", temoRecordID, uids[i]];
    }
    temoRecordID = [NSString stringWithFormat:@"%@%@", temoRecordID, screenId];
    if (![temoRecordID isEqualToString:self.recordID] && self.currentRoomModel.record) {
        self.recordID = temoRecordID;
        [MeetingRTSManager updateRecordLayout:uids screenId:screenId];
    }
}

- (void)videoSpeakerViewTouchAction {
    [self.bottomView dismissMoreView];
    if (self.isLandscape) {
        [self setDeviceInterfaceOrientation:UIDeviceOrientationPortrait];
    }
}

- (void)videoMatrixViewTouchAction {
    [self.bottomView dismissMoreView];
}

#pragma mark - getter

- (RoomBottomView *)bottomView {
    if (!_bottomView) {
        _bottomView = [[RoomBottomView alloc] init];
        _bottomView.delegate = self;
    }
    return _bottomView;
}

- (RoomNavView *)navView {
    if (!_navView) {
        _navView = [[RoomNavView alloc] init];
        _navView.delegate = self;
    }
    return _navView;
}

- (RoomParamInfoView *)paramInfoView {
    if (!_paramInfoView) {
        _paramInfoView = [[RoomParamInfoView alloc] init];
        _paramInfoView.backgroundColor = [UIColor clearColor];
        _paramInfoView.hidden = YES;
    }
    return _paramInfoView;
}

- (UIImageView *)recordTipView {
    if (!_recordTipView) {
        _recordTipView = [[UIImageView alloc] init];
        _recordTipView.image = [UIImage imageNamed:@"meeting_room_recore" bundleName:HomeBundleName];
        _recordTipView.hidden = YES;
    }
    return _recordTipView;
}

- (RoomMatrixView *)videoMatrixView {
    if (!_videoMatrixView) {
        _videoMatrixView = [[RoomMatrixView alloc] init];
        _videoMatrixView.backgroundColor = [UIColor colorFromHexString:@"#1D2129"];
        
        _videoMatrixView.userInteractionEnabled = YES;
        UITapGestureRecognizer *tap = [[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(videoMatrixViewTouchAction)];
        [_videoMatrixView addGestureRecognizer:tap];
    }
    return _videoMatrixView;
}

- (RoomSpeakerView *)videoSpeakerView {
    if (!_videoSpeakerView) {
        _videoSpeakerView = [[RoomSpeakerView alloc] init];
        _videoSpeakerView.hidden = YES;
        
        _videoSpeakerView.userInteractionEnabled = YES;
        UITapGestureRecognizer *tap = [[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(videoSpeakerViewTouchAction)];
        [_videoSpeakerView addGestureRecognizer:tap];
    }
    return _videoSpeakerView;
}

- (MeetingEndComponent *)endComponent {
    if (!_endComponent) {
        _endComponent = [[MeetingEndComponent alloc] init];
    }
    return _endComponent;
}

- (NSMutableArray<RoomVideoSession *> *)userDataPool {
    if (!_userDataPool) {
        _userDataPool = [[NSMutableArray alloc] init];
    }
    return _userDataPool;
}

- (NSMutableDictionary<NSString *,NSString *> *)currentStreamDic {
    if (!_currentStreamDic) {
        _currentStreamDic = [[NSMutableDictionary alloc] init];
    }
    return _currentStreamDic;
}

- (MeetingScreenComponent *)screenComponent {
    if (!_screenComponent) {
        _screenComponent = [[MeetingScreenComponent alloc] init];
    }
    return _screenComponent;
}

- (UIView *)toastView {
    if (!_toastView) {
        _toastView = [[UIView alloc] init];
        _toastView.backgroundColor = [UIColor clearColor];
        _toastView.userInteractionEnabled = NO;
    }
    return _toastView;
}

- (dispatch_semaphore_t)lock {
    if (_lock == nil) {
        _lock = dispatch_semaphore_create(1);
    }
    return _lock;
}

- (void)dealloc {
    [UIApplication sharedApplication].idleTimerDisabled = NO;
}

@end
