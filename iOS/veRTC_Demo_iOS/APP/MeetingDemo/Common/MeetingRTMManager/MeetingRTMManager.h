//
//  MeetingRTMManager.h
//  SceneRTCDemo
//
//  Created by on 2021/3/16.
//

#import <Foundation/Foundation.h>
#import "MeetingControlUserModel.h"
#import "MeetingControlRoomModel.h"
#import "MeetingControlRecordModel.h"
#import "RoomVideoSession.h"
#import "BaseRTCManager.h"

NS_ASSUME_NONNULL_BEGIN

@interface MeetingRTMManager : BaseRTCManager

#pragma mark - Get meeting data

/*
 * Issue appID
 * @param uid User ID
 * @param roomId Room ID
 * @param block Callback
 */
+ (void)getAppIDWithUid:(NSString *)uid roomId:(NSString *)roomId block:(void (^)(NSString *appID, RTMACKModel *model))block;

/*
 * Join the meeting
 * @param loginModel Login user data
 * @param userLists user lists data
 * @param block Callback
 */
+ (void)joinMeeting:(RoomVideoSession *)loginModel block:(void (^)(NSString *token, NSArray<RoomVideoSession *> *userLists, RTMACKModel *model))block;

/*
 * Leave Meeting
 */
+ (void)leaveMeeting;

/*
 * Turn On Mic
 */
+ (void)turnOnMic;

/*
 * Turn Off Mic
 */
+ (void)turnOffMic;

/*
 * Turn On Camera
 */
+ (void)turnOnCamera;

/*
 * Turn Off Camera
 */
+ (void)turnOffCamera;

/*
 * Get the participant list/participant status
 * @param userId User ID
 * @param block Callback
 */
+ (void)getMeetingUserInfo:(NSString *)userId block:(void (^)(NSArray<RoomVideoSession *> *userLists, RTMACKModel *model))block;

/*
 * Get meeting information
 * @param block Callback
 */
+ (void)getMeetingInfoWithBlock:(void (^)(MeetingControlRoomModel *roomModel, RTMACKModel *model))block;

/*
 * Get historical video download address
 * @param isHolder Holder Video
 * @param block Callback
 */
+ (void)getHistoryVideoRecord:(BOOL)isHolder block:(void (^)(NSArray<MeetingControlRecordModel *> *recordLists, RTMACKModel *model))block;

/*
 * Delete historical video download address
 * @param vid Video vid
 * @param block Callback
 */
+ (void)deleteVideoRecord:(NSString *)vid block:(void (^)(RTMACKModel *model))block;

/*
 * Reconnect
 * @param block Callback
 */
+ (void)reconnectWithBlock:(void (^)(RTMACKModel *model))block;

#pragma mark - Control meeting status

/*
 * Change Host
 * @param userId Host ID to be handed over
 * @param block Callback
 */
+ (void)changeHost:(NSString *)userId block:(void (^)(BOOL result, RTMACKModel *model))block;

/*
 * Mute single/all users
 * @param userId Users who want to mute, do not pass means to mute all users
 * @param block Callback
 */
+ (void)muteUser:(NSString *)userId block:(void (^)(BOOL result, RTMACKModel *model))block;

/*
 * Request to turn on the microphone
 * @param userId ID of the user who requested to turn on the microphone
 * @param block Callback
 */
+ (void)askMicOn:(NSString *)userId block:(void (^)(BOOL result))block;

/*
 * Request to open the camera
 * @param userId ID of the user who requested to turn on the microphone
 * @param block Callback
 */
+ (void)askCameraOn:(NSString *)userId block:(void (^)(BOOL result))block;

/*
 * End Meeting
 */
+ (void)endMeeting;

/*
 * Start Share Screen
 */
+ (void)startShareScreen;

/*
 * End Share Screen
 */
+ (void)endShareScreen;

/*
 * Record Meeting
 * @param uids User ID
 * @param screenId Screen ID
 */
+ (void)recordMeeting:(NSArray *)uids screenId:(NSString *)screenId block:(void(^)(RTMACKModel *ackModel))block;

/*
 * Adjust recording layout
 * @param uids User ID
 * @param screenId Screen ID
 */
+ (void)updateRecordLayout:(NSArray *)uids screenId:(NSString *)screenId;


#pragma mark - Notification message

/*
 * User microphone on/off notification
 * @param block Callback
 */
+ (void)onUserMicStatusChangeWithBlock:(void (^)(NSString *uid, BOOL result))block;

/*
 * User camera on/off notification
 * @param block Callback
 */
+ (void)onUserCameraStatusChangeWithBlock:(void (^)(NSString *uid, BOOL result))block;

/*
 * Host change notice
 * @param block Callback
 */
+ (void)onHostChangeWithBlock:(void (^)(NSString *formerHostUid, NSString *hostUid))block;

/*
 * Host change notice
 * @param block Callback
 */
+ (void)onUserJoinMeetingWithBlock:(void (^)(MeetingControlUserModel *model))block;

/*
 * User leaving the meeting notice
 * @param block Callback
 */
+ (void)onUserLeaveMeetingWithBlock:(void (^)(NSString *uid))block;

/*
 * Notification of user being kicked out
 * @param block Callback
 */
+ (void)onUserKickedOffWithBlock:(void (^)(BOOL result))block;

/*
 * Screen sharing status change notification
 * @param block Callback
 */
+ (void)onShareScreenStatusChangedWithBlock:(void (^)(NSString *uid, BOOL result))block;

/*
 * Meeting recording notice
 * @param block Callback
 */
+ (void)onRecordWithBlock:(void (^)(BOOL result))block;

/*
 * Notice of the end of the meeting
 * @param block Callback
 */
+ (void)onMeetingEndWithBlock:(void (^)(BOOL result))block;

/*
 * User silent notification
 * @param block Callback
 */
+ (void)onMuteUserWithBlock:(void (^)(NSString *uid))block;

/*
 * Mute notifications for all users
 * @param block Callback
 */
+ (void)onMuteAllWithBlock:(void (^)(BOOL result))block;

/*
 * Conference recording completion notice // Phase 2 will be developed
 * @param block Callback
 */
+ (void)onRecordFinishedWithBlock:(void (^)(BOOL result))block;

/*
 * Request to turn on microphone notification
 * @param block Callback
 */
+ (void)onAskingMicOnWithBlock:(void (^)(NSString *uid))block;

/*
 * Request to turn on camera notification
 * @param block Callback
 */
+ (void)onAskingCameraOnWithBlock:(void (^)(NSString *uid))block;

@end

NS_ASSUME_NONNULL_END
