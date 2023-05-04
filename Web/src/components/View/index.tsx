import React, { useEffect, useState, useCallback } from 'react';
import { Tooltip } from 'antd';
import classnames from 'classnames';

import { v4 as uuid } from 'uuid';
import { useSize } from '@umijs/hooks';
import type { ActiveMeetingUser } from '@/pages/Meeting/components/MeetingViews';
import shareOffIcon from '/assets/images/shareOffIcon.png';
import micVolumeIon from '/assets/images/micWithVolume.png';
import micOffIcon from '/assets/images/micOffIcon.png';
import micOnIcon from '/assets/images/micOnIcon.png';

import styles from './index.less';
type IViewProps = ActiveMeetingUser & {
  avatarOnCamOff?: React.ReactElement;
  sharingId?: string;
  volume: number;
  player: JSX.Element | undefined;
  sharingView: boolean;
};

const View: React.FC<IViewProps> = ({
  me = false,
  speaking,
  is_host: isHost,
  is_camera_on: isCameraOn,
  is_mic_on: isMicOn,
  user_name: username = '',
  avatarOnCamOff = null,
  sharingId = '',
  user_id,
  volume,
  player,
  sharingView,
  room_id,
}) => {
  const [id] = useState<string>(uuid());
  const [layoutSize, containerRef] = useSize<HTMLDivElement>();
  const [avatarSize, updateAvatarSize] = useState(24);

  const handleResize = () => {
    if (layoutSize.width && layoutSize.height) {
      const { width, height } = layoutSize;
      const shortRect = width > height ? height : width;
      const halfShortRect = Math.round(shortRect / 3);
      if (halfShortRect > 150) {
        updateAvatarSize(150);
      } else if (halfShortRect < 24) {
        updateAvatarSize(24);
      } else {
        updateAvatarSize(halfShortRect);
      }
    }
  };

  useEffect(() => {
    if (layoutSize.width && layoutSize.height) {
      handleResize();
    }
  }, [layoutSize]);

  const hasVolume = useCallback(() => {
    let res: boolean;
    if (me && isMicOn && volume > 0) {
      res = true;
    } else {
      res = volume > 0;
    }
    return res;
  }, [volume, isMicOn, me]);

  const render = () => {
    if (!sharingView && !isCameraOn) {
      return (
        <div className={styles.layoutWithoutCamera}>
          <div
            className={styles.avatar}
            style={{
              width: avatarSize,
              height: avatarSize,
              borderRadius: avatarSize / 2,
              lineHeight: `${avatarSize}px`,
              fontSize: avatarSize / 2,
              fontWeight: 600,
              border:
                hasVolume() && speaking
                  ? '1px solid #A0C0FF'
                  : '1px solid transparent',
            }}
          >
            {avatarOnCamOff ? avatarOnCamOff : username[0]}
          </div>

          <div className={styles.username}>
            {room_id && (
              <img
                src={
                  hasVolume() && speaking
                    ? micVolumeIon
                    : isMicOn
                    ? micOnIcon
                    : micOffIcon
                }
                alt={`camera-${hasVolume() ? 'on' : 'off'}`}
                style={{
                  width: 16,
                  height: 16,
                  marginRight: 4,
                }}
              />
            )}
            <Tooltip title={username}>
              {username.length > 10 ? `${username.slice(0, 10)}...` : username}
            </Tooltip>
            {me && <span className={styles.me}>我</span>}
            {isHost && <span className={styles.me}>主持人</span>}
          </div>
        </div>
      );
    }
  };

  return (
    <div
      className={styles.viewContainer}
      ref={containerRef}
      //   style={{ border: hasVolume() && speaking ? '1px solid #A0C0FF' : 'none' }}
    >
      {render()}

      <div
        className={styles.streamContainer}
        id={id}
        style={{
          display: sharingView || isCameraOn ? 'block' : 'none',
          border:
            hasVolume() && speaking
              ? '1px solid #A0C0FF'
              : '1px solid transparent',
        }}
      >
        {player}
        <span className={styles.username2}>
          {sharingId === user_id && (
            <img src={shareOffIcon} style={{ width: 14 }} />
          )}
          &nbsp;
          {room_id && (
            <img
              src={
                hasVolume() && speaking
                  ? micVolumeIon
                  : isMicOn
                  ? micOnIcon
                  : micOffIcon
              }
              alt={`camera-${hasVolume() ? 'on' : 'off'}`}
              style={{
                width: !hasVolume() ? 12 : 16,
                height: 16,
                marginRight: 4,
              }}
            />
          )}
          {username}
          {me && <span className={styles.me}>我</span>}
          {isHost && <span className={styles.me}>主持人</span>}
        </span>
      </div>
    </div>
  );
};

export default View;
