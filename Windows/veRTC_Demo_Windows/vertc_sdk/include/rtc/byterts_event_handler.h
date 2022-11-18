/*
 * Copyright (c) 2020 The VolcEngineRTC project authors. All Rights Reserved.
 * @brief VolcEngineRTC Event Handler Lite
*/

#pragma once

#ifndef BYTE_RTS_EVENT_HANDLER_H__
#define BYTE_RTS_EVENT_HANDLER_H__

#include "bytertc_common_defines.h"

namespace bytertc {
/** 
 * @type callback
 * @brief 实时信令事件回调接口
 */
class IRTSEventHandler {
public:

    /**
     * @hidden
     */
    virtual ~IRTSEventHandler() {
    }

    /** 
     * @type callback
     * @region 引擎管理
     * @brief SDK 与信令服务器连接状态改变回调。连接状态改变时触发。
     * @param [in] state 当前 SDK 与信令服务器的连接状态，详见 ConnectionState{@link #ConnectionState}
     */
    virtual void onConnectionStateChanged(bytertc::ConnectionState state) {
    }

    /** 
     * @type callback
     * @region 引擎管理
     * @brief SDK 当前网络连接类型改变回调。当 SDK 的当前网络连接类型发生改变时回调该事件。
     * @param [in] type  <br>
     *        SDK 当前的网络连接类型，详见：NetworkType{@link #NetworkType} 。
     */
    virtual void onNetworkTypeChanged(bytertc::NetworkType type) {
    }

    /** 
     * @type callback
     * @region 引擎管理
     * @brief 周期性（2s）地发出回调，报告当前cpu与memory使用率
     * @param [in] stats 返回包含当前系统状态信息的结构体，详见 SysStats{@link #SysStats}
     */
    virtual void onSysStats(const bytertc::SysStats& stats) {
    }

    /** 
     * @type callback
     * @region 引擎管理
     * @brief 创建房间失败回调。
     * @param room_id 房间 ID。
     * @param error_code 创建房间错误码，具体原因参看 ErrorCode{@link #ErrorCode}。
     */
    virtual void onCreateRoomStateChanged(const char* room_id, int error_code) {
        (void)room_id;
        (void)error_code;
    }

    /** 
     * @type callback
     * @region 实时消息通信
     * @brief 登录结果回调
     * @param [in] uid  <br>
     *        登录用户 ID
     * @param [in] error_code  <br>
     *        登录结果  <br>
     *        详见 LoginErrorCode{@link #LoginErrorCode}。
     * @param [in] elapsed  <br>
     *        从调用 login{@link #IRTS#login} 接口开始到返回结果所用时长  <br>
     *        单位为 ms。
     * @notes 调用 login{@link #IRTS#login} 后，会收到此回调。
     */
    virtual void onLoginResult(const char* uid, int error_code, int elapsed) {
        (void)uid;
        (void)error_code;
        (void)elapsed;
    }
    /** 
     * @type callback
     * @region 实时消息通信
     * @brief 登出结果回调
     * @notes 调用 logout{@link #IRTS#logout} 后，会收到此回调。
     */
    virtual void onLogout() {
    }
    /** 
     * @type callback
     * @region 实时消息通信
     * @brief 设置应用服务器参数的返回结果
     * @param [in] error <br>
     *        设置结果  <br>
     *        + 返回 200，设置成功  <br>
     *        + 返回其他，设置失败
     * @notes 调用 setServerParams{@link #IRTS#setServerParams} 后，会收到此回调。
     */
    virtual void onServerParamsSetResult(int error) {
        (void)error;
    }
    /** 
     * @type callback
     * @region 实时消息通信
     * @brief 查询对端或本端用户登录状态的返回结果
     * @param [in] peer_user_id  <br>
     *        需要查询的用户 ID
     * @param [in] status  <br>
     *        查询的用户登录状态  <br>
     *        详见 USER_ONLINE_STATUS{@link #USER_ONLINE_STATUS}.
     * @notes 必须先调用 getPeerOnlineStatus{@link #IRTS#getPeerOnlineStatus}，才能收到此回调。
     */
    virtual void onGetPeerOnlineStatus(const char* peer_user_id, int status) {
        (void)peer_user_id;
        (void)status;
    }
    /** 
     * @type callback
     * @region 实时消息通信
     * @brief 收到房间外用户调用 sendMessage{@link #IRTS#sendMessage} 发来的文本消息时，会收到此回调
     * @param [in] uid  <br>
     *        消息发送者 ID
     * @param [in] message  <br>
     *        收到的文本消息内容
     */
    virtual void onMessageReceived(const char* uid, const char* message) {
        (void)uid;
        (void)message;
    }
    /** 
     * @type callback
     * @region 实时消息通信
     * @brief 收到房间外用户调用 sendBinaryMessage{@link #IRTS#sendBinaryMessage} 发来的二进制消息时，会收到此回调
     * @param [in] uid  <br>
     *        消息发送者 ID
     * @param [in] size  <br>
     *        二进制消息长度
     * @param [in] message  <br>
     *        收到的二进制消息内容
     */
    virtual void onBinaryMessageReceived(const char* uid, int size, const uint8_t* message) {
        (void)uid;
        (void)size;
        (void)message;
    }
    /** 
     * @type callback
     * @region 实时消息通信
     * @brief 给房间外指定的用户发送消息的回调
     * @param [in] msgid  <br>
     *        本条消息的 ID  <br>
     *        所有的 P2P 和 P2Server 消息共用一个 ID 序列。
     * @param [in] error  <br>
     *        消息发送结果  <br>
     *        详见 UserMessageSendResult{@link #UserMessageSendResult}。
     * @notes 当调用 sendMessage{@link #IRTS#sendMessage} 或 sendBinaryMessage{@link #IRTS#sendBinaryMessage} 发送消息后，会收到此回调。
     */
    virtual void onMessageSendResult(int64_t msgid, int error) {
        (void)msgid;
        (void)error;
    }

    /** 
     * @type callback
     * @region 实时消息通信
     * @brief 给应用服务器发送消息的回调
     * @param [in] msgid 本条消息的 ID  <br>
     *        所有的 P2P 和 P2Server 消息共用一个 ID 序列。
     * @param [in] error 消息发送结果，详见 UserMessageSendResult{@link #UserMessageSendResult}。
     * @param [in] message 应用服务器收到 HTTP 请求后，在 ACK 中返回的信息。
     * @notes 当调用 sendServerMessage{@link #IRTS#sendServerMessage} 或 sendServerBinaryMessage{@link #IRTS#sendServerBinaryMessage} 发送消息后，会收到此回调。
     */
    virtual void onServerMessageSendResult(int64_t msgid, int error, const bytertc::ServerACKMsg& msg) {
        (void)msgid;
        (void)error;
        (void)msg;
    }
};

} // namespace bytertc

#endif // BYTE_RTS_EVENT_HANDLER_H__
