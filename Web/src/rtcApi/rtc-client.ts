import VERTC, {
  VideoRenderMode,
  IRTCEngine,
  RTCStream,
  LocalStreamStats,
  RemoteStreamStats,
  StreamIndex,
  MediaType,
  StreamRemoveReason,
  UserMessageEvent,
  RemoteAudioPropertiesInfo,
  LocalAudioPropertiesInfo,
  ConnectionStateChangeEvent,
} from '@volcengine/rtc';
import { v4 as uuid } from 'uuid';

import { IVolume } from '@/app-interfaces';
import Utils from '@/utils/utils';

export interface IBindEvent {
  handleUserPublishStream: (event: {
    userId: string;
    mediaType: MediaType;
  }) => void;
  handleUserUnPublishStream: (event: {
    userId: string;
    mediaType: MediaType;
    reason: StreamRemoveReason;
  }) => void;

  handleUserPublishScreen: (event: {
    userId: string;
    mediaType: MediaType;
  }) => void;

  handleUserUnPublishScreen: (event: {
    userId: string;
    mediaType: MediaType;
    reason: StreamRemoveReason;
  }) => void;

  handleEventError: (e: any, VERTC: any) => void;
  handleAudioVolumeIndication: (event: RemoteAudioPropertiesInfo[]) => void;
  handleLocalAudioPropertiesReport: (event: LocalAudioPropertiesInfo[]) => void;
  handleLocalStreamState: (stats: LocalStreamStats) => void;
  handleRemoteStreamState: (stats: RemoteStreamStats) => void;
  handleTrackEnd: (params: { kind: string; isScreen: boolean }) => void;
  handleConnectionStateChanged: (event: ConnectionStateChangeEvent) => void;
}

interface RTCConfig {
  appId: string;
  rtsUid: string;
  uid: string;
  rtmToken: string;
  serverUrl: string;
  serverSignature: string;
  bid: string;
  roomId?: string;
}

export default class RtcClient {
  config!: RTCConfig;
  engine!: IRTCEngine;
  handleUserPublishStream!: IBindEvent['handleUserPublishStream'];
  handleUserUnPublishStream!: IBindEvent['handleUserUnPublishStream'];
  handleUserPublishScreen!: IBindEvent['handleUserPublishScreen'];
  handleUserUnPublishScreen!: IBindEvent['handleUserUnPublishScreen'];
  private _videoCaptureDevice?: string;
  private _audioCaptureDevice?: string;
  //   private _loginedRTS: boolean = false;

  constructor() {
    this.setRemoteVideoPlayer = this.setRemoteVideoPlayer.bind(this);
    this.setLocalVideoPlayer = this.setLocalVideoPlayer.bind(this);
    this.createScreenStream = this.createScreenStream.bind(this);
  }
  SDKVERSION = VERTC.getSdkVersion();
  init(props: RTCConfig): void {
    this.config = props;
    this.engine = VERTC.createEngine(this.config.appId);
  }

  setRoomId = (roomId: string) => {
    this.config.roomId = roomId;
  };

  bindEngineEvents({
    handleUserPublishStream,
    handleUserUnPublishStream,
    handleUserPublishScreen,
    handleUserUnPublishScreen,
    handleEventError,
    handleAudioVolumeIndication,
    handleLocalAudioPropertiesReport,
    handleLocalStreamState,
    handleRemoteStreamState,
    handleTrackEnd,
    handleConnectionStateChanged,
  }: IBindEvent): void {
    this.handleUserPublishStream = handleUserPublishStream;
    this.handleUserUnPublishStream = handleUserUnPublishStream;
    this.handleUserPublishScreen = handleUserPublishScreen;
    this.handleUserUnPublishScreen = handleUserUnPublishScreen;

    this.engine.on(VERTC.events.onUserPublishStream, handleUserPublishStream);
    this.engine.on(
      VERTC.events.onUserUnpublishStream,
      handleUserUnPublishStream
    );
    this.engine.on(VERTC.events.onUserPublishScreen, handleUserPublishScreen);
    this.engine.on(
      VERTC.events.onUserUnpublishScreen,
      handleUserUnPublishScreen
    );
    this.engine.on(VERTC.events.onError, (e) => handleEventError(e, VERTC));
    this.engine.on(
      VERTC.events.onRemoteAudioPropertiesReport,
      handleAudioVolumeIndication
    );
    this.engine.on(
      VERTC.events.onLocalAudioPropertiesReport,
      handleLocalAudioPropertiesReport
    );
    this.engine.on(VERTC.events.onLocalStreamStats, handleLocalStreamState);
    this.engine.on(VERTC.events.onRemoteStreamStats, handleRemoteStreamState);
    this.engine.on(VERTC.events.onTrackEnded, handleTrackEnd);
    this.engine.on(
      VERTC.events.onConnectionStateChanged,
      handleConnectionStateChanged
    );
  }
  /**
   * remove the listeners when `createengine`
   */
  removeEventListener(): void {
    this.engine.off(
      VERTC.events.onUserPublishStream,
      this.handleUserPublishStream
    );
    this.engine.off(
      VERTC.events.onUserUnpublishStream,
      this.handleUserUnPublishStream
    );
    this.engine.off(
      VERTC.events.onUserPublishScreen,
      this.handleUserPublishScreen
    );
    this.engine.off(
      VERTC.events.onUserUnpublishScreen,
      this.handleUserUnPublishScreen
    );
  }

  joinWithRTS = async () => {
    await this.engine.login(this.config.rtmToken, this.config.rtsUid);
    await this.engine.setServerParams(
      this.config.serverSignature,
      this.config.serverUrl
    );

    // this._loginedRTS = true;
  };

  sendServerMessage = async <T>(
    eventname: string,
    payload: Record<string, any>,
    roomId?: string
  ): Promise<T> => {
    return new Promise((resolve, reject) => {
      const request_id = `${eventname}:${uuid()}`;

      const content = {
        app_id: this.config.appId,
        room_id: roomId || this.config.roomId,
        user_id: this.config.rtsUid,
        device_id: Utils.getDeviceId(),
        event_name: `vc${eventname
          .split('')
          .map((i, index) => {
            if (index === 0) {
              return i.toUpperCase();
            }
            return i;
          })
          .join('')}`,
        request_id,
        content: JSON.stringify(payload),
      };

      const callback = (e: UserMessageEvent) => {
        const { userId, message } = e;

        if (userId === 'server') {
          try {
            const res = JSON.parse(message);
            if (res.request_id === request_id) {
              this.engine.removeListener(
                VERTC.events.onUserMessageReceivedOutsideRoom,
                callback
              );

              if (res.code === 200) {
                resolve(res.response);
              } else {
                reject(res);
              }
            }
          } catch (e) {
            console.error('sendServerMessage callback error catch', e);
            reject(e);
          }
        }
      };

      this.engine.on(VERTC.events.onUserMessageReceivedOutsideRoom, callback);

      this.engine
        .sendServerMessage(JSON.stringify(content))
        .catch((e: unknown) => {
          console.error('sendServerMessage error', e);
          reject(
            (
              e as {
                message: string;
              }
            ).message
          );
        });
    });
  };

  join(token: string, roomId: string, uid: string): Promise<void> {
    this.config.roomId = roomId;

    return this.engine.joinRoom(
      token,
      roomId,
      {
        userId: uid,
      },
      {
        isAutoPublish: true,
        isAutoSubscribeAudio: true,
        isAutoSubscribeVideo: true,
      }
    );
  }

  // check permission of browser
  checkPermission(): Promise<{
    video: boolean;
    audio: boolean;
  }> {
    return VERTC.enableDevices();
  }
  /**
   * get the devices
   * @returns
   */
  async getDevices(): Promise<{
    audioInputs: MediaDeviceInfo[];
    videoInputs: MediaDeviceInfo[];
    audioPlaybackList: MediaDeviceInfo[];
  }> {
    const devices = await VERTC.enumerateDevices();

    const audioInputs = devices.filter(
      (i) => i.deviceId && i.kind === 'audioinput'
    );
    const videoInputs = devices.filter(
      (i) => i.deviceId && i.kind === 'videoinput'
    );

    this._audioCaptureDevice =
      this._audioCaptureDevice || audioInputs?.[0]?.deviceId;
    this._videoCaptureDevice =
      this._videoCaptureDevice || videoInputs?.[0]?.deviceId;

    return {
      audioInputs,
      videoInputs,
      audioPlaybackList: devices.filter(
        (i) => i.deviceId && i.kind === 'audiooutput'
      ),
    };
  }

  /**
   * @brief 取消共享屏幕音视频流
   * @function destoryScreenStream
   */
  async destoryScreenStream(
    success: () => void,
    fail?: (err: Error) => void
  ): Promise<void> {
    //  停止捕获屏幕流
    this.engine
      .stopScreenCapture()
      .then(() => {
        // this.engine.unpublishScreen(MediaType.AUDIO_AND_VIDEO);
        success && success();
      })
      .catch((err: Error) => fail && fail(err));
  }

  /**
   * @brief 创建本地音视频流
   * @function createLocalStream
   * @param streamOptions 流参数
   * @param isPublish 是否发布 预览则不发布
   */
  async createLocalStream(
    streamOptions: {
      mic: string;
      camera: string;
      audio: boolean;
      video: boolean;
    },
    // isPublish: boolean,
    callback: (param: any) => void
  ): Promise<void> {
    const { mic, camera, audio, video } = streamOptions;
    const devicesStatus = {
      video: 1,
      audio: 1,
    };
    const permissions = await this.checkPermission();
    await this.getDevices();
    if (audio && permissions.audio) {
      this._audioCaptureDevice = mic || this._audioCaptureDevice;
      await this.engine.startAudioCapture(this._audioCaptureDevice);
    } else {
      if (!permissions.audio) devicesStatus['audio'] = 0;
    }
    if (video && permissions.video) {
      this._videoCaptureDevice = camera || this._videoCaptureDevice;
      await this.engine.startVideoCapture(this._videoCaptureDevice);
    } else {
      if (!permissions.video) devicesStatus['video'] = 0;
    }

    // if (isPublish) {
    //   if (permissions.audio) {
    //     this.engine.publishStream(MediaType.AUDIO);
    //   }
    //   if (permissions.video) {
    //     this.engine.publishStream(MediaType.VIDEO);
    //   }
    // }

    callback &&
      callback({
        code: 0,
        msg: '设备获取成功',
        devicesStatus,
      });
  }

  /**
   * @brief 挂载流播放的容器
   * @param type 流类型 0/1
   */
  setLocalVideoPlayer(
    type: StreamIndex,
    renderDom?: string | HTMLElement
  ): void {
    this.engine.setLocalVideoPlayer(type, {
      renderDom: renderDom,
      userId: this.config.uid,
      renderMode: VideoRenderMode.RENDER_MODE_FIT,
    });
  }
  /**
   * @brief 挂载流播放的容器
   * @param type 流类型 0/1
   */
  async setRemoteVideoPlayer(
    type: StreamIndex,
    remoteUserId: string,
    domId: string | HTMLElement
  ): Promise<void> {
    try {
      this.engine.setRemoteVideoPlayer(type, {
        userId: remoteUserId,
        renderDom: domId,
        renderMode: VideoRenderMode.RENDER_MODE_FIT,
      });
    } catch (error) {
      console.error('error', error);
    }
  }

  async changeAudioState(isMicOn: boolean): Promise<void> {
    isMicOn
      ? await this.engine.startAudioCapture(this._audioCaptureDevice)
      : await this.engine.stopAudioCapture();
  }

  async changeVideoState(isVideoOn: boolean): Promise<void> {
    isVideoOn
      ? await this.engine.startVideoCapture(this._videoCaptureDevice)
      : await this.engine.stopVideoCapture();
    this.engine.setLocalVideoMirrorType(1);
  }

  async leave(): Promise<void> {
    await this.engine.leaveRoom();
  }

  createEngine(): void {
    this.engine = VERTC.createEngine(this.config.appId);
  }

  //   unpublish(): Promise<void> {
  //     return this.engine.unpublishStream(MediaType.AUDIO_AND_VIDEO);
  //   }

  /**
   * 切换设备
   */
  switchDevice = async (
    deviceType: 'camera' | 'microphone',
    deviceId: string
  ) => {
    if (deviceType === 'microphone') {
      this._audioCaptureDevice = deviceId;
      await this.engine.setAudioCaptureDevice(deviceId);
    } else {
      this._videoCaptureDevice = deviceId;
      await this.engine.setVideoCaptureDevice(deviceId);
    }
  };

  /**
   * @brief 共享屏幕音视频流
   * @function createScreenStream
   * @param {*} screenConfig
   */
  async createScreenStream(
    success: () => void,
    fail: (err: any) => void
  ): Promise<void> {
    try {
      await this.engine.startScreenCapture({
        enableAudio: true,
      });
      success();
      //   this.engine
      //     .publishScreen(MediaType.AUDIO_AND_VIDEO)
      //     .then(() => {})
      //     .catch((err: Error) => console.error('err', err));
    } catch (e: any) {
      fail(e);
    }
  }
  /**
   * @brief 取消共享屏幕音视频流
   * @function stopScreenStream
   */
  async stopScreenStream(success: () => void): Promise<void> {
    await this.engine.stopScreenCapture();
    // await this.engine.unpublishScreen(MediaType.AUDIO_AND_VIDEO);
    success();
  }
}
