import React, { useLayoutEffect, useRef, useEffect } from 'react';
import { StreamIndex } from '@volcengine/rtc';
import { injectRTC } from '@/pages/Meeting/configs/config';
interface VideoPlayerProps {
  userId: string;
  rtc: any;
}

const ShareViewWithRTC: React.FC<{
  //   localCaptureSuccess: boolean;
  rtc: any;
  sharingUser: any;
  me: boolean;
}> = ({ rtc, sharingUser, me }) => {
  useEffect(() => {
    // if (localCaptureSuccess) {
    if (me) {
      rtc.setLocalVideoPlayer(StreamIndex.STREAM_INDEX_SCREEN, undefined);
      rtc.setLocalVideoPlayer(StreamIndex.STREAM_INDEX_SCREEN, 'screen-player');
    } else {
      rtc.setRemoteVideoPlayer(
        StreamIndex.STREAM_INDEX_SCREEN,
        sharingUser.user_id,
        undefined
      );
      rtc.setRemoteVideoPlayer(
        StreamIndex.STREAM_INDEX_SCREEN,
        sharingUser.user_id,
        'screen-player'
      );
      //   }
    }

    return () => {
      rtc.setLocalVideoPlayer(StreamIndex.STREAM_INDEX_SCREEN, undefined);

      rtc.setRemoteVideoPlayer(
        StreamIndex.STREAM_INDEX_SCREEN,
        sharingUser.user_id,
        undefined
      );
    };
  }, [me, rtc, sharingUser.user_id]);

  return (
    <div
      id="screen-player"
      style={{
        width: '100%',
        height: '100%',
        background: '#000',
      }}
    ></div>
  );
};

export const LocalPlayerWithRTC: React.FC<{
  localCaptureSuccess: boolean;
  rtc: any;
  renderDom: string;
}> = ({ localCaptureSuccess, rtc, renderDom }) => {
  useEffect(() => {
    if (localCaptureSuccess) {
      rtc.setLocalVideoPlayer(StreamIndex.STREAM_INDEX_MAIN, undefined);
      rtc.setLocalVideoPlayer(StreamIndex.STREAM_INDEX_MAIN, renderDom);
    }

    return () => {
      rtc.setLocalVideoPlayer(StreamIndex.STREAM_INDEX_MAIN, undefined);
    };
  }, [localCaptureSuccess, rtc.setLocalVideoPlayer, renderDom]);
  return (
    <div
      style={{
        width: '100%',
        height: '100%',
        background: '#000',
      }}
      id={renderDom}
    ></div>
  );
};

export const MediaPlayerWithRTC: React.FC<VideoPlayerProps> = ({
  userId,
  rtc,
}) => {
  useLayoutEffect(() => {
    if (rtc.setRemoteVideoPlayer) {
      rtc.setRemoteVideoPlayer(
        StreamIndex.STREAM_INDEX_MAIN,
        userId,
        `remoteStream_${userId}`
      );
    }
    return () => {
      if (rtc.setRemoteVideoPlayer) {
        rtc.setRemoteVideoPlayer(
          StreamIndex.STREAM_INDEX_MAIN,
          userId,
          undefined
        );
      }
    };
  }, [rtc.setRemoteVideoPlayer, userId]);

  return (
    <div
      style={{
        width: '100%',
        height: '100%',
        position: 'relative',
        background: '#000',
      }}
      className="remoteStream"
      id={`remoteStream_${userId}`}
    ></div>
  );
};

export const MediaPlayer = injectRTC(MediaPlayerWithRTC);

export const LocalPlayer = injectRTC(LocalPlayerWithRTC);

export const ShareView = injectRTC(ShareViewWithRTC);
