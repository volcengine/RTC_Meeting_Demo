import { UserModelState } from './models/user';
import { MeetingModelState } from './models/meeting';
import { MeetingSettingsState } from './models/meeting-settings';
import { RTCClientControlModelState } from './models/meeting-client';

import { MeetingControlModelState, ImmerReducer } from '@@/plugin-dva/connect';
import { Action } from 'dva-model-creator';
import { EffectsMapObject, SubscriptionsMapObject } from 'dva';
import { AudioStats } from '@/lib/socket-interfaces';
import { RTCStream } from '@volcengine/rtc';
import { ConnectedProps, connector } from '@/pages/Meeting/configs/config';

export interface AppState {
  user: UserModelState;
  meeting: MeetingModelState;
  meetingControl: MeetingControlModelState;
  meetingSettings: MeetingSettingsState;
  rtcClientControl: RTCClientControlModelState;
}

export interface AppModel<S> {
  namespace: string;
  state: S;
  reducers: {
    [K in string]: ImmerReducer<S, Action<any>>;
  };
  effects?: EffectsMapObject;
  subscriptions?: SubscriptionsMapObject;
}

export enum FitType {
  cover,
  contain,
}

type EncoderConfiguration = {
  resolution?: {
    width: number;
    height: number;
  };
  frameRate?: {
    min: number;
    max: number;
  };
  bitrate?: {
    min: number;
    max: number;
  };
};

export type IStreamStats = {
  accessDelay: number;
  audioSentBytes: number;
  audioReceivedBytes: number;
  videoSentResolutionWidth: number;
  videoSentResolutionHeight: number;
  videoSentFrameRate: number;
  videoSentBitrate: number;
  audioSentBitrate: number;
  videoReceivedResolutionWidth: number;
  videoReceivedResolutionHeight: number;
  videoReceivedPacketsLost: number;
  audioReceivedPacketsLost: number;
};

export interface Stream {
  hasAudio: boolean;
  hasVideo: boolean;
  isScreen: boolean;
  userId: string;
  playerComp?: JSX.Element;
  videoStreamDescriptions: {
    framerate: number;
    height: number;
    maxkbps: number;
    rid: string;
    width: number;
  }[];
}

export interface ConnectStatus {
  connected: boolean;
  disconnect: boolean;
}

export type DeviceItems = {
  audioInputs: MediaDeviceInfo[];
  videoInputs: MediaDeviceInfo[];
  audioPlaybackList: MediaDeviceInfo[];
};

export interface IVolume {
  volume: number;
  userId: string;
}

export interface IMeetingState {
  cameraStream: RTCStream | null;
  screenStream: boolean;
  remoteStreams: { [id: string]: RTCStream };
  //   localVolume: number;
  refresh: boolean;
  localSpeaker: IVolume;
  streamStatses: {
    local: any;
    localScreen: any;
    remoteStreams: {
      [key: string]: any;
    };
  };
  autoPlayFailUser: string[];
  playStatus: { [key: string]: { audio: boolean; video: boolean } };
  //   users: any[];
}

export type MeetingProps = ConnectedProps<typeof connector>;

export type LocalStats = {
  audioStats: {
    audioLossRate: number;
    numChannels: number;
    recordSampleRate: number;
    rtt: number;
    sentKBitrate: number;
    statsInterval: number;
  };
  isScreen: boolean;
  videoStats: {
    codecType: string;
    encodedFrameCount: number;
    encodedFrameHeight: number;
    encodedFrameWidth: number;
    encoderOutputFrameRate: number;
    inputFrameRate: number;
    isScreen: boolean;
    rtt: number;
    sentFrameRate: number;
    sentKBitrate: number;
    statsInterval: number;
    videoLossRate: number;
  };
};

export type RemoteStats = {
  audioStats: {
    audioLossRate: number;
    concealedSamples: number;
    concealmentEvents: number;
    e2eDelay: number;
    jitterBufferDelay: number;
    numChannels: number;
    receivedKBitrate: number;
    receivedSampleRate: number;
    recordSampleRate: number;
    statsInterval: number;
  };
  isScreen: boolean;
  userId: string;
  videoStats: {
    decoderOutputFrameRate: number;
    e2eDelay: number;
    height: number;
    isScreen: boolean;
    receivedKBitrate: number;
    statsInterval: number;
    videoLossRate: number;
    width: number;
  };
};
