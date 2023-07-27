import React, { FC, useMemo } from 'react';
import { Dispatch } from '@@/plugin-dva/connect';
import { injectIntl } from 'umi';
import { connect, bindActionCreators } from 'dva';
import { userActions } from '@/models/user';
import { meetingActions } from '@/models/meeting';
import {
  AppState,
  IMeetingState,
  LocalStats,
  RemoteStats,
} from '@/app-interfaces';
import { ConnectedProps } from 'react-redux';
import { WrappedComponentProps } from 'react-intl';
import Utils from '@/utils/utils';
import styles from './index.less';

type StreamStats = {
  accessDelay: number;
  videoSentFrameRate: number;
  videoSentResolutionWidth: number;
  videoSentResolutionHeight: number;
  videoSentBitrate: number;
  audioSentBitrate: number;
  videoReceivedPacketsLost: number;
  audioReceivedPacketsLost: number;
  videoReceivedResolutionHeight: number;
  videoReceivedResolutionWidth: number;
};

function mapStateToProps(state: AppState) {
  return {
    user: state.user,
    settings: state.meetingSettings,
    meeting: state.meeting,
  };
}

function mapDispatchToProps(dispatch: Dispatch) {
  return {
    dispatch,
    ...bindActionCreators({ ...userActions, ...meetingActions }, dispatch),
  };
}

const connector = connect(mapStateToProps, mapDispatchToProps);

type IProps = {
  streamStatses: IMeetingState['streamStatses'];
};

export type StatsProps = ConnectedProps<typeof connector> &
  WrappedComponentProps &
  IProps;

const StreamStats: FC<StatsProps> = ({ settings, streamStatses }) => {
  const local = useMemo(
    () => streamStatses.local,
    [streamStatses.local]
  ) as LocalStats;

  const remoteMediaDetail: Record<string, any> = useMemo(
    () => {
      const streamMap = streamStatses.remoteStreams;

      const resolutionOrder = Object.values(streamMap).sort(
        (a, b) => (a.videoStats.width || 0) * (a.videoStats.height || 0) - (b.videoStats.width || 0) * (b.videoStats.height || 0)
      ).map((item) => { return { width: item.videoStats.width, height: item.videoStats.height }; });
      
      const fpsOrder = Object.values(streamMap).sort(
        (a, b) => (a.videoStats.decoderOutputFrameRate || 0) - (b.videoStats.decoderOutputFrameRate || 0)
      ).map((item) => { return { decoderOutputFrameRate: item.videoStats.decoderOutputFrameRate?.toFixed(1) || 0 }; });

      const average: Record<string, number> = {
        rttVideo: 0,
        rttAudio: 0,
        bitVideo: 0,
        bitAudio: 0,
        lossVideo: 0,
        lossAudio: 0,
      };
      Object.values(streamMap).forEach((info) => {
        average.rttVideo += info.videoStats.e2eDelay;
      });
      Object.keys(average).forEach((key) => {
        average[key] = average[key] / resolutionOrder.length;
      });
      return {
        ...average,
        resolution: resolutionOrder.length ? `${resolutionOrder[0].width || 0}*${resolutionOrder[0].height || 0} - ${resolutionOrder.at(-1)?.width || 0}*${resolutionOrder.at(-1)?.height || 0}` : `0*0 - 0*0`,
        fps: fpsOrder.length ? `${fpsOrder[0].decoderOutputFrameRate} - ${fpsOrder.at(-1)?.decoderOutputFrameRate}` : `0 - 0`,
      };
    }
    , [JSON.stringify(streamStatses.remoteStreams)]
  );

  return settings?.realtimeParam ? (
    <div className={styles['status']}>
      <div>[LOCAL]</div>
      <div>
        RES：
        {`${local?.videoStats.encodedFrameWidth || 0} * ${
          local?.videoStats.encodedFrameHeight || 0
        }`}
      </div>
      <div>FPS：{local?.videoStats.inputFrameRate || 0}</div>
      <div>
        BIT(VIDEO)：{Utils.getThousand(local?.videoStats.sentKBitrate)}kbps
      </div>
      <div>
        BIT(AUDIO)：{Utils.getThousand(local?.audioStats.sentKBitrate)}kbps
      </div>

      {/* Params of remote should be scaled, only show one result. */}
      {
        Object.keys(streamStatses.remoteStreams || {})?.length ?
        <>
          <div style={{ marginTop: 10 }}>[REMOTE]</div>
          <div>RTT(VIDEO): {remoteMediaDetail.rttVideo.toFixed(1)}ms</div>
          <div>RTT(AUDIO): {remoteMediaDetail.rttAudio.toFixed(1)}ms</div>
          <div>CPU: 0% | 0%</div>
          <div>
            BIT(VIDEO): {Utils.getThousand(remoteMediaDetail.bitVideo).toFixed(1)}kbps
          </div>
          <div>
            BIT(AUDIO): {Utils.getThousand(remoteMediaDetail.bitAudio).toFixed(1)}kbps
          </div>
          <div>
            RES: {remoteMediaDetail.resolution}
          </div>
          <div>
            FPS: {remoteMediaDetail.fps}
          </div>
          <div>
            LOSS(VEDIO): {remoteMediaDetail.lossVideo.toFixed(1)}%
          </div>
          <div>
            LOSS(AUDIO)：{remoteMediaDetail.lossAudio.toFixed(1)}%
          </div>
        </> : null
      }
      {/* {Object.keys(remote).map((i) => {
        const remoteStats = remote[i];
        return (
          <>
            <div style={{ marginTop: 10 }}>[REMOTE]{i}</div>
            <div>RTT(VIDEO)：{remoteStats?.videoStats.e2eDelay ?? 0}ms</div>
            <div>RTT(AUDIO)：{remoteStats?.audioStats.e2eDelay ?? 0}ms</div>
            <div>CPU：0%|0%</div>
            <div>
              BIT(VIDEO)：
              {Utils.getThousand(remoteStats?.videoStats.receivedKBitrate)}kbps
            </div>
            <div>
              BIT(AUDIO):{' '}
              {Utils.getThousand(remoteStats?.audioStats.receivedKBitrate)}kbps
            </div>
            <div>
              RES：
              {`${remoteStats?.videoStats.width || 0} * ${
                remoteStats?.videoStats.height || 0
              }`}
            </div>
            <div>
              FPS：{remoteStats?.videoStats.decoderOutputFrameRate || 0}
            </div>
            <div>
              LOSS（VEDIO）：{remoteStats?.videoStats.videoLossRate || 0}%
            </div>
            <div>
              LOSS(AUDIO)：{remoteStats?.audioStats.audioLossRate || 0}%
            </div>
          </>
        );
      })} */}
    </div>
  ) : null;
};

export default connector(injectIntl(StreamStats));
