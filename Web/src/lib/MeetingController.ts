import { EventEmitter } from 'eventemitter3';
import { HOST, TOASTS, BOEHOST, isProd } from '@/config';
import Utils from '@/utils/utils';
import Logger from '@/utils/Logger';
import type { MeetingInfo, MeetingUser } from '@/models/meeting';
import RtcClient from '@/rtcApi/rtc-client';

import type {
  GetAppIDResponse,
  JoinMeetingPayload,
  JoinMeetingResponse,
  UserPayload,
  GetAppIDPayload,
  RecordMeetingPayload,
  GetVerifyCodePayload,
  VerifyLoginSms,
  VerifyLoginRes,
  HistoryVideoRecord,
} from './socket-interfaces';

const logger = new Logger('MettingController');

const BaseHost = isProd ? HOST : BOEHOST;

class MettingController extends EventEmitter {
  private _isHost: boolean;
  private rtc!: RtcClient;
  private eventListenNames: string[] = [
    'onUserMicStatusChange',
    'onUserCameraStatusChange',
    'onHostChange',
    'onUserJoinMeeting',
    'onUserLeaveMeeting',
    'onShareScreenStatusChanged',
    'onRecord',
    'onMeetingEnd',
    'onMuteAll',
    'onMuteUser',
    'onAskingMicOn',
    'onUserKickedOff',
  ];

  constructor() {
    super();
    logger.debug('MettingController constructor()');

    this._isHost = false;
    window.__mc = this;
  }

  set rtcClient(rtc: RtcClient) {
    this.rtc = rtc;
  }

  // 获取验证码
  public getPhoneVerifyCode(payload: GetVerifyCodePayload): Promise<{
    code: number;
    message: string;
  }> {
    return fetch(`${BaseHost}/login`, {
      method: 'POST',
      body: JSON.stringify({
        event_name: 'sendLoginSms',
        content: JSON.stringify(payload),
      }),
    }).then((res) => res.json());
  }

  // 验证验证码
  public verifyLoginSms(payload: VerifyLoginSms): Promise<{
    code: number;
    message: string;
    response: VerifyLoginRes;
  }> {
    return fetch(`${BaseHost}/login`, {
      method: 'POST',
      body: JSON.stringify({
        event_name: 'verifyLoginSms',
        content: JSON.stringify(payload),
      }),
    }).then((res) => res.json());
  }

  // 获取rts token
  public verifyLoginToken(payload: GetAppIDPayload): Promise<{
    code: number;
    message: string;
    response: GetAppIDResponse;
  }> {
    return fetch(`${BaseHost}/login`, {
      method: 'POST',
      body: JSON.stringify({
        event_name: 'joinRTS',
        device_id: payload.device_id,
        content: JSON.stringify({
          login_token: payload.login_token,
          scenes_name: 'meeting',
        }),
      }),
    }).then((res) => res.json());
  }

  // TODO: need to add rts response status code and error message

  // !fn: 暂时没有该功能
  public getHistoryVideoRecord(): Promise<HistoryVideoRecord[]> {
    return this.rtc.sendServerMessage('getHistoryVideoRecord', {
      login_token: Utils.getLoginToken(),
    });
  }

  // !fn: 暂时没有该功能
  public deleteVideoRecord(payload: { vid: string }): Promise<null> {
    return this.rtc.sendServerMessage('deleteVideoRecord', {
      ...payload,
      login_token: Utils.getLoginToken(),
    });
  }

  public joinMeeting(p: JoinMeetingPayload): Promise<JoinMeetingResponse> {
    return this.rtc.sendServerMessage('joinMeeting', p, p.room_id);
  }

  public leaveMeeting(): Promise<null> {
    return this.rtc.sendServerMessage('leaveMeeting', {
      login_token: Utils.getLoginToken(),
    });
  }

  public turnOnMic(): Promise<null> {
    return this.rtc.sendServerMessage('turnOnMic', {
      login_token: Utils.getLoginToken(),
    });
  }

  public turnOffMic(): Promise<null> {
    return this.rtc.sendServerMessage('turnOffMic', {
      login_token: Utils.getLoginToken(),
    });
  }

  public turnOnCamera(): Promise<null> {
    return this.rtc.sendServerMessage('turnOnCamera', {
      login_token: Utils.getLoginToken(),
    });
  }

  public turnOffCamera(): Promise<null> {
    return this.rtc.sendServerMessage('turnOffCamera', {
      login_token: Utils.getLoginToken(),
    });
  }

  public getMeetingUserInfo(payload: UserPayload): Promise<MeetingUser[]> {
    return this.rtc.sendServerMessage('getMeetingUserInfo', {
      ...payload,
      login_token: Utils.getLoginToken(),
    });
  }

  public getMeetingInfo(): Promise<MeetingInfo> {
    return this.rtc.sendServerMessage('getMeetingInfo', {
      login_token: Utils.getLoginToken(),
    });
  }

  public startShareScreen(): Promise<null> {
    return this.rtc.sendServerMessage('startShareScreen', {
      login_token: Utils.getLoginToken(),
    });
  }

  public endShareScreen(): Promise<null> {
    return this.rtc.sendServerMessage('endShareScreen', {
      login_token: Utils.getLoginToken(),
    });
  }

  public changeHost(payload: UserPayload): Promise<null> {
    this._assertNotHost();
    return this.rtc.sendServerMessage('changeHost', {
      ...payload,
      login_token: Utils.getLoginToken(),
    });
  }

  public muteUser(payload: UserPayload): Promise<null> {
    this._assertNotHost();
    return this.rtc.sendServerMessage('muteUser', {
      ...payload,
      login_token: Utils.getLoginToken(),
    });
  }

  public askMicOn(payload: UserPayload): Promise<null> {
    this._assertNotHost();
    return this.rtc.sendServerMessage('askMicOn', {
      ...payload,
      login_token: Utils.getLoginToken(),
    });
  }

  public askCameraOn(payload: UserPayload): Promise<null> {
    this._assertNotHost();
    return this.rtc.sendServerMessage('askCameraOn', {
      ...payload,
      login_token: Utils.getLoginToken(),
    });
  }

  public async endMeeting(): Promise<any> {
    this._assertNotHost();
    const res = await this.rtc.sendServerMessage('endMeeting', {
      login_token: Utils.getLoginToken(),
    });
    return res;
  }

  public recordMeeting(payload: RecordMeetingPayload): Promise<null> {
    try {
      this._assertNotHost();
    } catch (error) {
      throw TOASTS['record'];
    }

    return this.rtc.sendServerMessage('recordMeeting', {
      ...payload,
      login_token: Utils.getLoginToken(),
    });
  }

  public updateRecordLayout(payload: RecordMeetingPayload): Promise<null> {
    this._assertNotHost();

    return this.rtc.sendServerMessage('updateRecordLayout', {
      ...payload,
      login_token: Utils.getLoginToken(),
    });
  }

  public reconnect(): Promise<{ code: number }> {
    return this.rtc.sendServerMessage('updateRecordLayout', {
      login_token: Utils.getLoginToken(),
    });
  }

  public removeEvent(): void {
    [...this.eventListenNames].forEach((type) => {
      this.removeListener(type);
    });
  }

  private _assertNotHost() {
    if (!this._isHost) {
      throw new Error('Permission Denied');
    }
  }

  set isHost(v: boolean) {
    this._isHost = v;
  }
}

export default MettingController;
