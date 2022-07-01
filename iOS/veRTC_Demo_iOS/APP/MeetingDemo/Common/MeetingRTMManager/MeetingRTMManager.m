//
//  MeetingRTMManager.m
//  SceneRTCDemo
//
//  Created by bytedance on 2021/3/16.
//

#import "MeetingRTMManager.h"
#import "MeetingRTCManager.h"
#import "Core.h"

@implementation MeetingRTMManager

#pragma mark - Get meeting data

+ (void)getAppIDWithUid:(NSString *)uid roomId:(NSString *)roomId block:(void (^)(NSString *appID, RTMACKModel *model))block {
    NSDictionary *dic = @{};
    if (uid && roomId && uid.length > 0 && roomId.length > 0) {
        dic = @{@"user_id" : uid,
                @"room_id" : roomId};
    }
    dic = [PublicParameterCompoments addTokenToParams:dic];
    [[MeetingRTCManager shareRtc] emitWithAck:@"vcGetAppID"
                                         with:dic
                                        block:^(RTMACKModel * _Nonnull ackModel) {
        NSString *appid = @"";
        if ([ackModel.response isKindOfClass:[NSDictionary class]]) {
            appid = ackModel.response[@"app_id"];
        }
        dispatch_async(dispatch_get_main_queue(), ^{
            if (block) {
                block(appid, ackModel);
            }
        });
    }];
}

+ (void)joinMeeting:(RoomVideoSession *)loginModel block:(void (^)(NSString *token, NSArray<RoomVideoSession *> *userLists, RTMACKModel *model))block {
    NSMutableDictionary *dic = [[NSMutableDictionary alloc] init];
    [dic setValue:loginModel.appid forKey:@"app_id"];
    [dic setValue:loginModel.uid forKey:@"user_id"];
    [dic setValue:loginModel.name forKey:@"user_name"];
    [dic setValue:loginModel.roomId forKey:@"room_id"];
    BOOL isEnableAudio = loginModel.isEnableAudio;
    [dic setValue:@(isEnableAudio) forKey:@"mic"];
    [dic setValue:@(loginModel.isEnableVideo) forKey:@"camera"];
    
    NSDictionary *dicData = [PublicParameterCompoments addTokenToParams:[dic copy]];
    
    [[MeetingRTCManager shareRtc] emitWithAck:@"vcJoinMeeting"
                                         with:dicData
                                        block:^(RTMACKModel * _Nonnull ackModel) {
        NSString *token = @"";
        NSMutableArray *userLists = [[NSMutableArray alloc] init];
        if ([ackModel.response isKindOfClass:[NSDictionary class]]) {
            token = ackModel.response[@"token"];
            NSArray *lists = ackModel.response[@"users"];
            if ([lists isKindOfClass:[NSArray class]]) {
                for (int i = 0; i < lists.count; i++) {
                    MeetingControlUserModel *meetingUserModel = [MeetingControlUserModel yy_modelWithJSON:lists[i]];
                    RoomVideoSession *videoSession = [RoomVideoSession roomVideoSessionToMeetingControlUserModel:meetingUserModel];
                    [userLists addObject:videoSession];
                }
                BaseUserModel *localUser = [LocalUserComponents userModel];
                localUser.name = loginModel.name;
                [LocalUserComponents updateLocalUserModel:localUser];
            }
        }
        dispatch_async(dispatch_get_main_queue(), ^{
            if (block) {
                block(token, [userLists copy], ackModel);
            }
        });
    }];
}

+ (void)leaveMeeting {
    NSDictionary *dic = [PublicParameterCompoments addTokenToParams:nil];
    [[MeetingRTCManager shareRtc] emitWithAck:@"vcLeaveMeeting"
                                         with:dic
                                        block:nil];
}

+ (void)turnOnMic {
    NSDictionary *dic = [PublicParameterCompoments addTokenToParams:nil];
    [[MeetingRTCManager shareRtc] emitWithAck:@"vcTurnOnMic"
                                         with:dic
                                        block:nil];
}

+ (void)turnOffMic {
    NSDictionary *dic = [PublicParameterCompoments addTokenToParams:nil];
    [[MeetingRTCManager shareRtc] emitWithAck:@"vcTurnOffMic"
                                         with:dic
                                        block:nil];
}

+ (void)turnOnCamera {
    NSDictionary *dic = [PublicParameterCompoments addTokenToParams:nil];
    [[MeetingRTCManager shareRtc] emitWithAck:@"vcTurnOnCamera"
                                         with:dic
                                        block:nil];
}

+ (void)turnOffCamera {
    NSDictionary *dic = [PublicParameterCompoments addTokenToParams:nil];
    [[MeetingRTCManager shareRtc] emitWithAck:@"vcTurnOffCamera"
                                         with:dic
                                        block:nil];
}

+ (void)getMeetingUserInfo:(NSString *)userId block:(void (^)(NSArray<RoomVideoSession *> *userLists, RTMACKModel *model))block {
    NSDictionary *dic = [PublicParameterCompoments addTokenToParams:nil];
    if (NOEmptyStr(userId)) {
        NSMutableDictionary *mutableDictionary = [dic mutableCopy];
        [mutableDictionary setValue:userId forKey:@"user_id"];
        dic = [mutableDictionary copy];
    }
    
    [[MeetingRTCManager shareRtc] emitWithAck:@"vcGetMeetingUserInfo"
                                         with:dic
                                        block:^(RTMACKModel * _Nonnull ackModel) {
        NSMutableArray *modelLsts = [[NSMutableArray alloc] init];
        NSArray *data = (NSArray *)ackModel.response;
        if (data && [data isKindOfClass:[NSArray class]]) {
            for (int i = 0; i < data.count; i++) {
                MeetingControlUserModel *controlUserModel = [MeetingControlUserModel yy_modelWithJSON:data[i]];
                RoomVideoSession *userModel = [RoomVideoSession roomVideoSessionToMeetingControlUserModel:controlUserModel];
                [modelLsts addObject:userModel];
            }
        }
        dispatch_async(dispatch_get_main_queue(), ^{
            if (block) {
                block([modelLsts copy], ackModel);
            }
        });
    }];

}

+ (void)getMeetingInfoWithBlock:(void (^)(MeetingControlRoomModel *roomModel, RTMACKModel *model))block {
    NSDictionary *dic = [PublicParameterCompoments addTokenToParams:nil];
    
    [[MeetingRTCManager shareRtc] emitWithAck:@"vcGetMeetingInfo"
                                         with:dic
                                        block:^(RTMACKModel * _Nonnull ackModel) {
        MeetingControlRoomModel *roomModel = [MeetingControlRoomModel yy_modelWithJSON:ackModel.response];
        dispatch_async(dispatch_get_main_queue(), ^{
            if (block) {
                block(roomModel, ackModel);
            }
        });
    }];
}

+ (void)getHistoryVideoRecord:(BOOL)isHolder block:(void (^)(NSArray<MeetingControlRecordModel *> *recordLists, RTMACKModel *model))block {
    NSDictionary *dic = [PublicParameterCompoments addTokenToParams:nil];
    
    [[MeetingRTCManager shareRtc] emitWithAck:@"vcGetHistoryVideoRecord"
                                         with:dic
                                        block:^(RTMACKModel * _Nonnull ackModel) {
        NSMutableArray *modelLsts = [[NSMutableArray alloc] init];
        NSArray *data = (NSArray *)ackModel.response;
        if (data && [data isKindOfClass:[NSArray class]]) {
            for (int i = 0; i < data.count; i++) {
                MeetingControlRecordModel *model = [MeetingControlRecordModel yy_modelWithJSON:data[i]];
                if (isHolder == model.video_holder) {
                    [modelLsts addObject:model];
                }
            }
        }
        dispatch_async(dispatch_get_main_queue(), ^{
            if (block) {
                block([modelLsts copy], ackModel);
            }
        });
    }];
}

+ (void)deleteVideoRecord:(NSString *)vid block:(void (^)(RTMACKModel *model))block {
    NSDictionary *dic = @{@"vid" : vid};
    dic = [PublicParameterCompoments addTokenToParams:dic];
    
    [[MeetingRTCManager shareRtc] emitWithAck:@"vcDeleteVideoRecord"
                                         with:dic
                                        block:^(RTMACKModel * _Nonnull ackModel) {
        dispatch_async(dispatch_get_main_queue(), ^{
            if (block) {
                block(ackModel);
            }
        });
    }];
}

+ (void)reconnectWithBlock:(void (^)(RTMACKModel *model))block {
    NSDictionary *dic = [PublicParameterCompoments addTokenToParams:nil];
    
    [[MeetingRTCManager shareRtc] emitWithAck:@"vcReconnect"
                                         with:dic
                                        block:^(RTMACKModel * _Nonnull ackModel) {
        dispatch_async(dispatch_get_main_queue(), ^{
            if (block) {
                block(ackModel);
            }
        });
    }];
}


#pragma mark - Control meeting status

+ (void)changeHost:(NSString *)userId block:(void (^)(BOOL result, RTMACKModel *model))block {
    NSDictionary *dic = [PublicParameterCompoments addTokenToParams:nil];
    if (NOEmptyStr(userId)) {
        NSMutableDictionary *mutableDictionary = [dic mutableCopy];
        [mutableDictionary setValue:userId forKey:@"user_id"];
        dic = [mutableDictionary copy];
    }
    
    [[MeetingRTCManager shareRtc] emitWithAck:@"vcChangeHost"
                                         with:dic
                                        block:^(RTMACKModel * _Nonnull ackModel) {
        dispatch_async(dispatch_get_main_queue(), ^{
            if (block) {
                block(NO, ackModel);
            }
        });
    }];
}

+ (void)muteUser:(NSString *)userId block:(void (^)(BOOL result, RTMACKModel *model))block {
    NSDictionary *dic = [PublicParameterCompoments addTokenToParams:nil];
    NSMutableDictionary *mutableDictionary = [dic mutableCopy];
    [mutableDictionary setValue:userId forKey:@"user_id"];
    dic = [mutableDictionary copy];
    
    [[MeetingRTCManager shareRtc] emitWithAck:@"vcMuteUser"
                                         with:dic
                                        block:^(RTMACKModel * _Nonnull ackModel) {
        dispatch_async(dispatch_get_main_queue(), ^{
            if (block) {
                block(NO, ackModel);
            }
        });
    }];
}

+ (void)askMicOn:(NSString *)userId block:(void (^)(BOOL result))block {
    NSDictionary *dic = [PublicParameterCompoments addTokenToParams:nil];
    if (NOEmptyStr(userId)) {
        NSMutableDictionary *mutableDictionary = [dic mutableCopy];
        [mutableDictionary setValue:userId forKey:@"user_id"];
        dic = [mutableDictionary copy];
    }

    [[MeetingRTCManager shareRtc] emitWithAck:@"vcAskMicOn"
                                         with:dic
                                        block:^(RTMACKModel *_Nonnull ackModel) {
                                          if (block) {
                                              block(NO);
                                          }
                                        }];
}

+ (void)askCameraOn:(NSString *)userId block:(void (^)(BOOL result))block {
    NSDictionary *dic = [PublicParameterCompoments addTokenToParams:nil];
    if (NOEmptyStr(userId)) {
        NSMutableDictionary *mutableDictionary = [dic mutableCopy];
        [mutableDictionary setValue:userId forKey:@"user_id"];
        dic = [mutableDictionary copy];
    }
    
    [[MeetingRTCManager shareRtc] emitWithAck:@"vcAskCameraOn" with:dic block:^(RTMACKModel * _Nonnull ackModel) {
        if (block) {
            block(NO);
        }
    }];
}

+ (void)endMeeting {
    NSDictionary *dic = [PublicParameterCompoments addTokenToParams:nil];
    [[MeetingRTCManager shareRtc] emitWithAck:@"vcEndMeeting" with:dic block:nil];
}

+ (void)startShareScreen {
    NSDictionary *dic = [PublicParameterCompoments addTokenToParams:nil];
    [[MeetingRTCManager shareRtc] emitWithAck:@"vcStartShareScreen" with:dic block:nil];
}

+ (void)endShareScreen {
    NSDictionary *dic = [PublicParameterCompoments addTokenToParams:nil];
    [[MeetingRTCManager shareRtc] emitWithAck:@"vcEndShareScreen" with:dic block:nil];
}

+ (void)recordMeeting:(NSArray *)uids screenId:(NSString *)screenId block:(nonnull void (^)(RTMACKModel * _Nonnull))block {
    NSMutableDictionary *dic = [[NSMutableDictionary alloc] init];
    if (uids && [uids isKindOfClass:[NSArray class]]) {
        [dic setValue:uids forKey:@"users"];
    }
    if (screenId.length > 0) {
        [dic setValue:screenId forKey:@"screen_uid"];
    }
    NSDictionary *dicData = [PublicParameterCompoments addTokenToParams:[dic copy]];
    [[MeetingRTCManager shareRtc] emitWithAck:@"vcRecordMeeting" with:dicData block:^(RTMACKModel * _Nonnull ackModel) {
        if (block) {
            block(ackModel);
        }
    }];
}

+ (void)updateRecordLayout:(NSArray *)uids screenId:(NSString *)screenId {
    NSMutableDictionary *dic = [[NSMutableDictionary alloc] init];
    if (uids && [uids isKindOfClass:[NSArray class]]) {
        [dic setValue:uids forKey:@"users"];
    }
    if (screenId.length > 0) {
        [dic setValue:screenId forKey:@"screen_uid"];
    }
    NSDictionary *dicData = [PublicParameterCompoments addTokenToParams:[dic copy]];
    [[MeetingRTCManager shareRtc] emitWithAck:@"vcUpdateRecordLayout" with:dicData block:nil];
}


#pragma mark - Notification message

+ (void)onUserMicStatusChangeWithBlock:(void (^)(NSString *uid, BOOL result))block {
    [[MeetingRTCManager shareRtc] onSceneListener:@"onUserMicStatusChange" block:^(RTMNoticeModel * _Nonnull noticeModel) {
        NSString *uid = @"";
        BOOL status = NO;
        if (noticeModel.data && [noticeModel.data isKindOfClass:[NSDictionary class]]) {
            uid = noticeModel.data[@"user_id"];
            status = [noticeModel.data[@"status"] boolValue];
        }
        if (block) {
            block(uid, status);
        }
    }];
}

+ (void)onUserCameraStatusChangeWithBlock:(void (^)(NSString *uid, BOOL result))block {
    [[MeetingRTCManager shareRtc] onSceneListener:@"onUserCameraStatusChange" block:^(RTMNoticeModel * _Nonnull noticeModel) {
        NSString *uid = @"";
        BOOL status = NO;
        if (noticeModel.data && [noticeModel.data isKindOfClass:[NSDictionary class]]) {
            uid = noticeModel.data[@"user_id"];
            status = [noticeModel.data[@"status"] boolValue];
        }
        if (block) {
            block(uid, status);
        }
    }];
}

+ (void)onHostChangeWithBlock:(void (^)(NSString *formerHostUid, NSString *hostUid))block {
    [[MeetingRTCManager shareRtc] onSceneListener:@"onHostChange" block:^(RTMNoticeModel * _Nonnull noticeModel) {
        NSString *aboveUid = @"";
        NSString *hostUid = @"";
        if (noticeModel.data && [noticeModel.data isKindOfClass:[NSDictionary class]]) {
            aboveUid = noticeModel.data[@"former_host_id"];
            hostUid = noticeModel.data[@"host_id"];
        }
        if (block) {
            block(aboveUid, hostUid);
        }
    }];
}

+ (void)onUserJoinMeetingWithBlock:(void (^)(MeetingControlUserModel *model))block {
    [[MeetingRTCManager shareRtc] onSceneListener:@"onUserJoinMeeting" block:^(RTMNoticeModel * _Nonnull noticeModel) {
        MeetingControlUserModel *model = [MeetingControlUserModel yy_modelWithJSON:noticeModel.data];
        if (block) {
            block(model);
        }
    }];
}

+ (void)onUserLeaveMeetingWithBlock:(void (^)(NSString *uid))block {
    [[MeetingRTCManager shareRtc] onSceneListener:@"onUserLeaveMeeting" block:^(RTMNoticeModel * _Nonnull noticeModel) {
        NSString *uid = @"";
        
        if (noticeModel.data && [noticeModel.data isKindOfClass:[NSDictionary class]]) {
            uid = noticeModel.data[@"user_id"];
        }
        if (block) {
            block(uid);
        }
    }];
}

+ (void)onUserKickedOffWithBlock:(void (^)(BOOL result))block {
    [[MeetingRTCManager shareRtc] onSceneListener:@"onUserKickedOff" block:^(RTMNoticeModel * _Nonnull noticeModel) {
        if (block) {
            block(YES);
        }
    }];
}

+ (void)onShareScreenStatusChangedWithBlock:(void (^)(NSString *uid, BOOL result))block {
    [[MeetingRTCManager shareRtc] onSceneListener:@"onShareScreenStatusChanged" block:^(RTMNoticeModel * _Nonnull noticeModel) {
    
        NSString *uid = @"";
        BOOL status = NO;
        
        if (noticeModel.data && [noticeModel.data isKindOfClass:[NSDictionary class]]) {
            uid = noticeModel.data[@"user_id"];
            status = [noticeModel.data[@"status"] boolValue];
        }
        if (block) {
            block(uid ,status);
        }
    }];
}

+ (void)onRecordWithBlock:(void (^)(BOOL result))block {
    [[MeetingRTCManager shareRtc] onSceneListener:@"onRecord" block:^(RTMNoticeModel * _Nonnull noticeModel) {

        if (block) {
            block(YES);
        }
    }];
}

+ (void)onMeetingEndWithBlock:(void (^)(BOOL result))block {
    [[MeetingRTCManager shareRtc] onSceneListener:@"onMeetingEnd" block:^(RTMNoticeModel * _Nonnull noticeModel) {
    
        if (block) {
            block(YES);
        }
    }];
}

+ (void)onMuteAllWithBlock:(void (^)(BOOL result))block {
    [[MeetingRTCManager shareRtc] onSceneListener:@"onMuteAll" block:^(RTMNoticeModel * _Nonnull noticeModel) {
        dispatch_async(dispatch_get_main_queue(), ^{
            if (block) {
                block(YES);
            }
        });
    }];
}

+ (void)onMuteUserWithBlock:(void (^)(NSString *uid))block {
    [[MeetingRTCManager shareRtc] onSceneListener:@"onMuteUser" block:^(RTMNoticeModel * _Nonnull noticeModel) {
    
        NSString *uid = @"";
        
        if (noticeModel.data && [noticeModel.data isKindOfClass:[NSDictionary class]]) {
            uid = noticeModel.data[@"user_id"];
        }
        dispatch_async(dispatch_get_main_queue(), ^{
            if (block) {
                block(uid);
            }
        });
    }];
}

+ (void)onRecordFinishedWithBlock:(void (^)(BOOL result))block {
    [[MeetingRTCManager shareRtc] onSceneListener:@"onRecordFinished" block:^(RTMNoticeModel * _Nonnull noticeModel) {
        if (block) {
            block(NO);
        }
    }];
}

+ (void)onAskingMicOnWithBlock:(void (^)(NSString *uid))block {
    [[MeetingRTCManager shareRtc] onSceneListener:@"onAskingMicOn" block:^(RTMNoticeModel * _Nonnull noticeModel) {
        NSString *uid = @"";
        
        if (noticeModel.data && [noticeModel.data isKindOfClass:[NSDictionary class]]) {
            uid = noticeModel.data[@"user_id"];
        }
        if (block) {
            block(uid);
        }
    }];
}

+ (void)onAskingCameraOnWithBlock:(void (^)(NSString *uid))block {
    [[MeetingRTCManager shareRtc] onSceneListener:@"onAskingCameraOn" block:^(RTMNoticeModel * _Nonnull noticeModel) {
        if (block) {
            block(nil);
        }
    }];
}

@end
