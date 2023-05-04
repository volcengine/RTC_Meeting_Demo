import { message, Skeleton, Modal } from 'antd';
import { TOASTS } from '@/config';

import type {
  UserStatusChangePayload,
  JoinMeetingResponse,
} from '@/lib/socket-interfaces';
import { MeetingUser, ViewMode } from '@/models/meeting';
import {
  showOpenMicConfirm,
  sendMutedInfo,
} from '@/pages/Meeting/components/MessageTips';
import React from 'react';
import DeviceController from '@/lib/DeviceController';
import { injectProps } from '@/pages/Meeting/configs/config';
import Utils from '@/utils/utils';
import { history } from 'umi';
import { IMeetingState, IVolume, MeetingProps } from '@/app-interfaces';
import VERTC, {
  RTCStream,
  RemoteStreamStats,
  LocalStreamStats,
  MediaType,
  StreamIndex,
  UserMessageEvent,
  RemoteAudioPropertiesInfo,
  LocalAudioPropertiesInfo,
  ConnectionStateChangeEvent,
  ConnectionState,
  AutoPlayFailedEvent,
  PlayerEvent,
} from '@volcengine/rtc';
import {
  MediaPlayer,
  MeetingViews,
  StreamStats,
  AutoPlayModal,
} from '../index';
import { getReleativeSortedList } from './utils';

interface IEvent {
  end: () => void;
  leavingMeeting: () => void;
}

type IProps = MeetingProps & IEvent;

/**
 * @param drawerVisible 用户列表抽屉是否可见
 * @param exitVisible 退出会议弹窗是否可见
 */
const initState = {
  cameraStream: null,
  screenStream: false,
  remoteStreams: {},
  //   audioLevels: [],
  //   localVolume: 0,
  refresh: false,
  localSpeaker: {
    userId: '',
    volume: 0,
  },
  streamStatses: {
    local: {},
    localScreen: undefined,
    remoteStreams: {},
  },
  autoPlayFailUser: [],
  playStatus: {},
  //   users: [],
};

class MeetingEvent extends React.Component<IProps, IMeetingState> {
  constructor(props: IProps) {
    super(props);

    window.__meeting = this;

    this.state = initState;

    if (!props.rtc?.engine) {
      props.rtc?.createEngine();
    }
  }

  deviceLib = new DeviceController(this.props);
  messageArray: UserMessageEvent[] = [];
  messageWait = false;

  get roomId(): string {
    return (
      this.props.currentUser.roomId ||
      (Utils.getQueryString('roomId') as string)
    );
  }

  handleMeetingStatus = () => {
    if (document.hidden) {
      this.props.setMeetingStatus('hidden');
    }
  };

  componentDidMount() {
    if (!this.props.mc) {
      return;
    }

    if (this.state.refresh) {
      setTimeout(() => {
        this.joinMeeting();
        this.setState({
          refresh: false,
        });
      }, 500);
    } else {
      this.joinMeeting();
    }

    // add listeners

    document.addEventListener('visibilitychange', this.handleMeetingStatus);
    this.addRTCEngineEventListener();
  }

  componentWillUnmount(): void {
    document.removeEventListener('visibilitychange', this.handleMeetingStatus);
    this.removeRTCEngineEventListener();
  }

  addRTCEngineEventListener = () => {
    this.props.rtc?.engine.on(
      VERTC.events.onUserPublishStream,
      this.handleUserPublishStream
    );
    this.props.rtc?.engine.on(
      VERTC.events.onUserUnpublishStream,
      this.handleUserUnPublishStream
    );
    this.props.rtc?.engine.on(
      VERTC.events.onUserPublishScreen,
      this.handleUserPublishScreen
    );
    this.props.rtc?.engine.on(
      VERTC.events.onUserUnpublishScreen,
      this.handleUserUnPublishScreen
    );

    this.props.rtc?.engine.on(VERTC.events.onError, (e) =>
      this.handleEventError(e, VERTC)
    );

    this.props.rtc?.engine.on(
      VERTC.events.onRemoteAudioPropertiesReport,
      this.handleAudioVolumeIndication
    );
    this.props.rtc?.engine.on(
      VERTC.events.onLocalAudioPropertiesReport,
      this.handleLocalAudioPropertiesReport
    );

    this.props.rtc?.engine.on(
      VERTC.events.onLocalStreamStats,
      this.handleLocalStreamState
    );
    this.props.rtc?.engine.on(
      VERTC.events.onRemoteStreamStats,
      this.handleRemoteStreamState
    );

    this.props.rtc?.engine.on(VERTC.events.onTrackEnded, this.handleTrackEnd);

    // rts
    this.props.rtc.engine.on(
      VERTC.events.onRoomMessageReceived,
      this.handleRoomMessageReceived
    );
    this.props.rtc.engine.on(
      VERTC.events.onUserMessageReceivedOutsideRoom,
      this.handleRoomMessageReceived
    );
    this.props.rtc.engine.on(
      VERTC.events.onUserMessageReceived,
      this.handleRoomMessageReceived
    );

    this.props.rtc.engine.on(
      VERTC.events.onConnectionStateChanged,
      this.handleReconnect
    );

    this.props.rtc.engine.on(
      VERTC.events.onAutoplayFailed,
      this.handleAutoPlayFail
    );

    this.props.rtc.engine.on(
      VERTC.events.onPlayerEvent,
      this.handlePlayerEvent
    );
  };

  removeRTCEngineEventListener = () => {
    this.props.rtc?.engine.off(
      VERTC.events.onUserPublishStream,
      this.handleUserPublishStream
    );
    this.props.rtc?.engine.off(
      VERTC.events.onUserUnpublishStream,
      this.handleUserUnPublishStream
    );
    this.props.rtc?.engine.off(
      VERTC.events.onUserPublishScreen,
      this.handleUserPublishScreen
    );
    this.props.rtc?.engine.off(
      VERTC.events.onUserUnpublishScreen,
      this.handleUserUnPublishScreen
    );

    this.props.rtc?.engine.off(VERTC.events.onError, (e) =>
      this.handleEventError(e, VERTC)
    );

    this.props.rtc?.engine.off(
      VERTC.events.onRemoteAudioPropertiesReport,
      this.handleAudioVolumeIndication
    );
    this.props.rtc?.engine.off(
      VERTC.events.onLocalAudioPropertiesReport,
      this.handleLocalAudioPropertiesReport
    );

    this.props.rtc?.engine.off(
      VERTC.events.onLocalStreamStats,
      this.handleLocalStreamState
    );
    this.props.rtc?.engine.off(
      VERTC.events.onRemoteStreamStats,
      this.handleRemoteStreamState
    );

    this.props.rtc?.engine.off(VERTC.events.onTrackEnded, this.handleTrackEnd);

    // rts
    this.props.rtc.engine.off(
      VERTC.events.onRoomMessageReceived,
      this.handleRoomMessageReceived
    );
    this.props.rtc.engine.off(
      VERTC.events.onUserMessageReceivedOutsideRoom,
      this.handleRoomMessageReceived
    );
    this.props.rtc.engine.off(
      VERTC.events.onUserMessageReceived,
      this.handleRoomMessageReceived
    );

    this.props.rtc.engine.off(
      VERTC.events.onConnectionStateChanged,
      this.handleReconnect
    );

    this.props.rtc.engine.off(
      VERTC.events.onAutoplayFailed,
      this.handleAutoPlayFail
    );

    this.props.rtc.engine.off(
      VERTC.events.onPlayerEvent,
      this.handlePlayerEvent
    );
  };

  changeUserStatus = (
    status: 'is_mic_on' | 'is_camera_on' | 'is_sharing',
    payload: UserStatusChangePayload
    // meetingUsers: MeetingUser[]
  ) => {
    const users = this.props.meeting.meetingUsers.map((user) => {
      if (user.user_id === payload.user_id) {
        return {
          ...user,
          [status]: payload.status,
        };
      }
      return user;
    });

    this.props.setMeetingUsers(users);
  };

  messageHandler = (e: UserMessageEvent) => {
    try {
      this.messageArray.push(e);
      if (this.messageWait) {
        return;
      }

      this.messageWait = true;
      setTimeout(() => {
        this.messageArray = this.messageArray.sort((a, b) => {
          const clone_a = { ...a };
          const clone_b = { ...b };
          return (
            JSON.parse(clone_a.message).timestamp -
            JSON.parse(clone_b.message).timestamp
          );
        });
        this.messageArray?.forEach((e) => {
          this.handleRoomMessageReceived(e, true);
        });
        this.messageArray = [];
        this.messageWait = false;
      }, 200);
    } catch (error) {
      console.log('error', error);
    }
  };

  handleRoomMessageReceived = (e: UserMessageEvent, force = false) => {
    this.messageHandler(e);

    if (!force) {
      return;
    }

    const { userId, message: msg } = e;
    if (userId !== 'server') {
      return;
    }

    try {
      const res = JSON.parse(msg);

      if (res.message_type === 'return') {
        return;
      }

      console.log('handleRoomMessageReceived', res);

      const payload = res.data;

      if (res.event === 'onUserMicStatusChange') {
        this.changeUserStatus('is_mic_on', payload);
      } else if (res.event === 'onUserCameraStatusChange') {
        this.changeUserStatus('is_camera_on', payload);
      } else if (res.event === 'onHostChange') {
        const users = this.props.meeting.meetingUsers.map((user) => {
          const ret = { ...user };
          if (ret.user_id === payload.host_id) {
            ret.is_host = true;
          } else {
            ret.is_host = false;
          }
          return ret;
        });

        this.props.setMeetingUsers(users);
        const isHost = payload.host_id === this.props.currentUser.userId;
        if (isHost) {
          message.info('你已成为主持人');
        }
        this.props.setIsHost(isHost);
        this.props.setMeetingInfo({
          ...this.props.meeting.meetingInfo,
          host_id: payload.host_id,
        });
      } else if (res.event === 'onUserJoinMeeting') {
        const _curUsers = this.props.meeting.meetingUsers;
        if (_curUsers.some((user) => user.user_id === payload.user_id)) {
          console.error('onUserJoinMeeting 该用户 %s 已存在', payload.user_id);
        } else {
          this.props.setMeetingUsers([..._curUsers, payload]);
        }
      } else if (res.event === 'onUserLeaveMeeting') {
        const _curUsers = this.props.meeting.meetingUsers;

        const curMeetingUsers = _curUsers.filter(
          (user) => user.user_id !== payload.user_id
        );

        this.props.setMeetingUsers(curMeetingUsers);

        this.checkAutoPlayFailUser(payload.user_id);
      } else if (res.event === 'onShareScreenStatusChanged') {
        if (payload.status) {
          this.props.setViewMode(ViewMode.SpeakerView);
          this.props.setMeetingInfo({
            ...this.props.meeting.meetingInfo,
            screen_shared_uid: payload.user_id,
          });
          this.props.setMeetingIsSharing(true);
        } else {
          this.props.setViewMode(ViewMode.GalleryView);
          this.props.setMeetingInfo({
            ...this.props.meeting.meetingInfo,
            screen_shared_uid: '',
          });
          this.props.setMeetingIsSharing(false);
        }
        this.changeUserStatus('is_sharing', payload);
      } else if (res.event === 'onRecord') {
        this.props.setMeetingInfo({
          ...this.props.meeting.meetingInfo,
          record: true,
        });
      } else if (res.event === 'onMeetingEnd') {
        this.props.setMeetingStatus('end');
        // this.props.rtc?.unpublish().then(() => {
        this.props.leavingMeeting();
        // });
      } else if (res.event === 'onMuteAll') {
        //user who is not host will receive muted message
        if (!this.props.currentUser.isHost) {
          if (this.props.currentUser.isMicOn) {
            this.deviceLib.changeAudioState(false);
            sendMutedInfo();
          }
        }
        //重新设置会议人员的状态
        if (this.props.currentUser?.userId) {
          this.props.mc
            ?.getMeetingUserInfo({
              user_id: this.props.currentUser?.userId,
            })
            .then((res: any) => {
              this.props.setMeetingUsers(res);
            });
        }
      } else if (res.event === 'onMuteUser') {
        sendMutedInfo();
        this.deviceLib.changeAudioState(false);
      } else if (res.event === 'onAskingMicOn') {
        showOpenMicConfirm(() => this.deviceLib.changeAudioState(true));
      } else if (res.event === 'onUserKickedOff') {
        message.warning(TOASTS['tick']);
        this.props.end();
      } else {
        console.error(res.event);
        return;
      }

      return;
    } catch (e) {
      console.error(e);
    }
  };

  handleUserPublishStream = (event: {
    userId: string;
    mediaType: MediaType;
  }) => {
    const { userId, mediaType } = event;

    const rtcStream = {
      userId,
      isScreen: false,
      hasVideo:
        mediaType === MediaType.VIDEO ||
        mediaType === MediaType.AUDIO_AND_VIDEO,
      hasAudio:
        mediaType === MediaType.AUDIO ||
        mediaType === MediaType.AUDIO_AND_VIDEO,
    };

    (rtcStream as RTCStream & { playerComp: any }).playerComp = (
      <MediaPlayer userId={userId} />
    );

    const _s = {
      ...this.state.remoteStreams,
      [userId]: rtcStream,
    };

    this.setState({
      remoteStreams: {
        ..._s,
      },
    });
  };

  handleUserUnPublishStream = (event: {
    userId: string;
    mediaType: MediaType;
  }) => {
    const { userId, mediaType } = event;

    if (
      mediaType === MediaType.AUDIO_AND_VIDEO ||
      mediaType === MediaType.VIDEO
    ) {
      const remoteStreams = this.state.remoteStreams;
      if (remoteStreams[userId]) {
        delete remoteStreams[userId];
      }
      this.setState({
        remoteStreams: { ...remoteStreams },
      });
    }
  };

  handleUserPublishScreen = (event: {
    userId: string;
    mediaType: MediaType;
  }) => {
    this.props.rtc.engine.subscribeScreen(event.userId, event.mediaType);
    this.props.setMeetingIsSharing(true);
  };

  handleUserUnPublishScreen = (event: {
    userId: string;
    mediaType: MediaType;
  }) => {
    this.props.rtc.engine.unsubscribeScreen(event.userId, event.mediaType);
    this.props.setMeetingIsSharing(false);
  };

  handleEventError = (e: any, VERTC: any) => {
    if (e.errorCode === VERTC.ErrorCode.DUPLICATE_LOGIN) {
      message.error('你的账号被其他人顶下线了');
      //leaveRoom();
    }
  };

  handleLocalAudioPropertiesReport = (event: LocalAudioPropertiesInfo[]) => {
    const meetInfo = this.props.meeting.meetingInfo;
    this.props.setMeetingInfo({
      ...meetInfo,
      localSpeaker: {
        userId: this.props.currentUser.userId!,
        volume: event[0]?.audioPropertiesInfo.linearVolume || 0,
      },
    });
  };

  handleAudioVolumeIndication = (event: RemoteAudioPropertiesInfo[]) => {
    const otherSpeakers: IVolume[] = [];
    const meetInfo = this.props.meeting.meetingInfo;
    const host_id = meetInfo.host_id;

    const lastSortList = this.props.meeting.meetingInfo.volumeSortList;

    for (const speaker of event) {
      if (speaker.streamKey.userId !== this.props.currentUser.userId) {
        if (speaker.streamKey.streamIndex === StreamIndex.STREAM_INDEX_MAIN) {
          otherSpeakers.push({
            userId: speaker.streamKey.userId,
            volume: speaker.audioPropertiesInfo.linearVolume,
          });
        }
      }
    }

    const keepSortedList = getReleativeSortedList(lastSortList, otherSpeakers);

    const sortList = keepSortedList.sort((a: IVolume, b: IVolume) => {
      if (a.userId === host_id) {
        // a before b
        return -1;
      }
      if (b.userId === host_id) {
        //b before a
        return 1;
      }
      return b.volume - a.volume;
    });
    this.props.setMeetingInfo({ ...meetInfo, volumeSortList: sortList });
  };

  handleLocalStreamState = (stats: LocalStreamStats) => {
    const { streamStatses } = this.state;
    if (stats.isScreen) {
      streamStatses.localScreen = stats;
    } else {
      streamStatses.local = stats;
    }
    this.setState({
      streamStatses,
    });
  };

  handleRemoteStreamState = (stats: RemoteStreamStats) => {
    if (!stats || !stats.userId) return;
    const { streamStatses } = this.state;
    const key = `${stats.userId}-${stats.isScreen ? 'screen' : 'video'}`;
    streamStatses.remoteStreams[key] = stats;
    this.setState({
      streamStatses,
    });
  };

  handleTrackEnd = (event: { kind: string; isScreen: boolean }) => {
    const { kind, isScreen } = event;
    if (isScreen) {
      this.deviceLib.stopShare(false);
    } else {
      if (kind === 'video') {
        if (this.props.currentUser.isCameraOn) {
          this.deviceLib.changeVideoState(false);
        }
      }
    }
  };

  _handleLeave = async () => {
    this.props.setMeetingStatus('end');
    await this.props.rtc.leave();
    this.props.rtc.removeEventListener();

    await this.props.rtc.engine.stopAudioCapture();
    await this.props.rtc.engine.stopVideoCapture();
    await this.props.rtc.engine.stopScreenCapture();
    this.props.rtc.engine.unpublishScreen(MediaType.AUDIO_AND_VIDEO);

    history.push(`/?roomId=${this.roomId}`);
  };

  handleReconnect = async (e: ConnectionStateChangeEvent) => {
    if (e.state === ConnectionState.CONNECTION_STATE_RECONNECTED) {
      const res = await this.props.mc?.reconnect().catch((err) => {
        console.log('reconnect err:', err);

        if (err.code === 404) {
          message.error('断线时间过长，无法重连');
          this._handleLeave();
        }
      });

      console.log('reconnect res:', res);

      if (res?.code === 418) {
        message.error('断线时间过长，无法重连');
        this._handleLeave();
      }

      if (res?.code === 422) {
        message.error('会议已经结束');
        this._handleLeave();
      }
    }
  };

  handleAutoPlayFail = async (event: AutoPlayFailedEvent) => {
    const { userId, kind } = event;
    let playUser = this.state.playStatus[userId] || {};
    playUser = { ...playUser, [kind]: false };
    this.state.playStatus[userId] = playUser;

    this.setState({
      playStatus: { ...this.state.playStatus },
    });

    this.addFailUser(event.userId);
  };

  addFailUser(userId: string) {
    const { autoPlayFailUser } = this.state;
    const index = autoPlayFailUser.findIndex((item) => item === userId);
    if (index === -1) {
      autoPlayFailUser.push(userId);
    }
    this.setState({
      autoPlayFailUser: [...autoPlayFailUser],
    });
  }

  playerFail(params: { type: 'audio' | 'video'; userId: string }) {
    const { type, userId } = params;

    let playUser = this.state.playStatus[userId] || {};
    playUser = { ...playUser, [type]: false };

    const { audio, video } = playUser;

    if (audio === false || video === false) {
      this.addFailUser(userId);
    }

    return playUser;
  }

  handlePlayerEvent = (event: PlayerEvent) => {
    const { userId, rawEvent, type } = event;
    let playUser = this.state.playStatus[userId] || {};
    if (rawEvent.type === 'playing') {
      playUser = { ...playUser, [type]: true };
      const { audio, video } = playUser;
      if (audio !== false && video !== false) {
        this.checkAutoPlayFailUser(userId);
      }
    } else if (rawEvent.type === 'pause') {
      playUser = this.playerFail({ type, userId });
    }

    this.setState({
      playStatus: { ...this.state.playStatus, [userId]: playUser },
    });
  };

  handleAutoPlay = () => {
    const users: string[] = this.state.autoPlayFailUser;
    if (users && users.length) {
      users.forEach((user) => {
        this.props.rtc?.engine.play(user);
      });
    }
    this.setState({
      autoPlayFailUser: [],
    });
  };

  checkAutoPlayFailUser(userId: string) {
    const autoPlayFailUser = this.state.autoPlayFailUser.filter(
      (item) => item !== userId
    );
    this.setState({
      autoPlayFailUser: [...autoPlayFailUser],
    });
  }

  joinMeeting = () => {
    const {
      currentUser,
      setMeetingInfo,
      setMeetingUsers,
      setIsHost,
      setViewMode,
      settings,
      meeting: { meetingInfo },
    } = this.props;

    const userId = Utils.getLoginUserId();
    this.props.setUserId(userId);

    //TODO增加兜底
    if (!this.props.currentUser.appId) {
      return;
    }
    const rtc = this.props.rtc;

    const param = {
      currentUser,
      settings,
    };

    this.props.mc
      ?.joinMeeting({
        app_id: rtc.config.appId,
        user_id: userId,
        user_name: Utils.getLoginUserName(),
        room_id: this.roomId,
        mic: currentUser.isMicOn,
        camera: currentUser.isCameraOn,
        login_token: Utils.getLoginToken(),
      })
      .then((res?: JoinMeetingResponse) => {
        if (!res) {
          return;
        }
        const { info, users } = res;
        const isHost = info.host_id === Utils.getLoginUserId();
        if (isHost) {
          setTimeout(() => {
            this.props.mc
              ?.getMeetingUserInfo({
                user_id: userId,
              })
              .then((res: any) => {
                this.props.setMeetingUsers(res);
              });
          }, 1000);
        } else {
          const _users = this.props.meeting.meetingUsers;
          const curMeetingUsers = [..._users, ...users];
          setMeetingUsers(curMeetingUsers);
        }

        setMeetingInfo({ ...meetingInfo, ...info });

        if (info.screen_shared_uid) {
          setViewMode(ViewMode.SpeakerView);
          this.props.setMeetingIsSharing(true);
        }
        setIsHost(isHost);

        rtc.join(res.token, this.roomId, userId).then(() => {
          if (this.props.meeting.localAudioVideoCaptureSuccess) {
            // rtc.engine.publishStream(MediaType.AUDIO_AND_VIDEO);
            this.props.setLocalCaptureSuccess(true);
          } else {
            this.deviceLib.openCamera(
              param,
              () => {
                this.props.setLocalCaptureSuccess(true);
              }
              //   true
            );
          }
          // 监听音量变化
          this.props.rtc?.engine.enableAudioPropertiesReport({
            interval: 2000,
          });
        });
      })
      .catch((e: any) => {
        message.error(`Join meeting failed: ${e?.message || 'unknown'}`);
        console.error(e);
        if (
          e?.message === 'login token expired' ||
          e?.message === 'login first'
        ) {
          Utils.removeLoginInfo();
          history.push('/login');
        } else {
          this.props.end();
        }
      });
  };

  render() {
    const { state, props } = this;

    if (state.refresh) {
      return <Skeleton />;
    }

    return (
      <>
        <MeetingViews
          currentUser={props.currentUser}
          meeting={props.meeting}
          cameraStream={state.cameraStream}
          screenStream={state.screenStream}
          remoteStreams={state.remoteStreams}
        />
        <StreamStats streamStatses={state.streamStatses} />
        <AutoPlayModal
          handleAutoPlay={this.handleAutoPlay}
          autoPlayFailUser={state.autoPlayFailUser}
        />
      </>
    );
  }
}

export default injectProps(MeetingEvent);
