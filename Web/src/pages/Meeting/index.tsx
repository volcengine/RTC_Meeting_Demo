import React, { Component } from 'react';
import { message } from 'antd';
import { history } from 'umi';
import Utils from '@/utils/utils';
import { meetingInitialState } from '@/models/meeting';
import { UsersDrawer, LeavingConfirm, ControlBar, Header } from './components';
import { MeetingProps } from '@/app-interfaces';
import MeetingEvent from '@/pages/Meeting/components/MeetEvent';
import { injectProps } from '@/pages/Meeting/configs/config';

import styles from './index.less';

/**
 * @param drawerVisible 用户列表抽屉是否可见
 * @param exitVisible 退出会议弹窗是否可见
 */
const initState = {
  drawerVisible: false,
  exitVisible: false,
};

type MeetingState = {
  drawerVisible: boolean;
  exitVisible: boolean;
};

class Meeting extends Component<MeetingProps, MeetingState> {
  constructor(props: MeetingProps) {
    super(props);

    window.__meeting = this;
    this.state = initState;
    this.unMount = this.unMount.bind(this);
  }

  // deviceLib = new DeviceController(this.props);

  get roomId(): string {
    return (
      this.props.currentUser.roomId ||
      (Utils.getQueryString('roomId') as string)
    );
  }

  componentDidMount = (): void => {
    Utils.checkMediaState();
    window.addEventListener('beforeunload', this.unMount);
  };

  componentWillUnmount() {
    this.unMount();
    window.removeEventListener('beforeunload', this.unMount);
  }

  componentDidUpdate(prevProps: MeetingProps, preState: MeetingState): void {
    if (
      this.props.mc &&
      prevProps.currentUser.isHost !== this.props.currentUser.isHost
    ) {
      this.props.mc.isHost = this.props.currentUser.isHost;
    }
  }

  unMount() {
    const props = this.props;
    props.rtc.leave();
    props.rtc.removeEventListener();
    props.mc?.removeEvent();
    // this.deviceLib?.stopShare(false);

    props.setMeetingInfo(meetingInitialState.meetingInfo);
    props.setMeetingUsers([]);
    props.setViewMode(meetingInitialState.viewMode);
    // props.setMeetingStatus(meetingInitialState.status);
    props.setSpeakCollapse(false);
    props.setLocalCaptureSuccess(false);
    props.setLocalAudioVideoCaptureSuccess(false);
    props.setMeetingIsSharing(false);
    props.setIsSharing(false);
    props.setIsHost(false);
  }

  end = async (): Promise<void> => {
    // window.open(`/?roomId=${this.roomId}`, '_self');
    await this.props.rtc.engine.stopAudioCapture();
    await this.props.rtc.engine.stopVideoCapture();

    history.push(`/?roomId=${this.roomId}`);
  };

  // //结束当前通话
  leavingMeeting = (): void => {
    const { props } = this;
    props.setMeetingStatus('end');
    props.rtc.leave();
    props.mc?.leaveMeeting();
    if (props.meeting.meetingInfo.screen_shared_uid) {
      props.mc?.endShareScreen();
    }
    props.rtc.removeEventListener();
    // this.setState({ leaving: false });
    this.end();
  };

  // //结束全部通话
  endMeeting = (): void => {
    try {
      this.props.mc?.endMeeting();
      // !fix :结束通话会报record not found
      // this.setState({ leaving: false });
    } catch (error) {
      message.error({ content: `${error}` });
    }
  };

  changeSpeakCollapse() {
    const {
      meeting: { speakCollapse },
    } = this.props;
    this.props?.setSpeakCollapse(!speakCollapse);
  }

  render() {
    const { props, state } = this;

    return (
      <div className={styles.container}>
        <Header
          changeSpeakCollapse={this.changeSpeakCollapse.bind(this)}
          meeting={props.meeting}
          username={props.currentUser.name || 'unknown'}
          meetingCreateAt={props.meeting.meetingInfo.created_at}
          now={props.meeting.meetingInfo.now}
          roomId={
            props.currentUser.roomId ||
            props.meeting.meetingInfo.room_id ||
            'unknown'
          }
        />

        <MeetingEvent leavingMeeting={this.leavingMeeting} end={this.end} />

        <ControlBar
          openUsersDrawer={() => this.setState({ drawerVisible: true })}
          leaveMeeting={() => this.setState({ exitVisible: true })}
        />

        <UsersDrawer
          visible={state.drawerVisible}
          closeUserDrawer={() => this.setState({ drawerVisible: false })}
        />

        <LeavingConfirm
          visible={state.exitVisible}
          isHost={props.currentUser.isHost}
          cancel={() => {
            this.setState({ exitVisible: false });
          }}
          leaveMeeting={this.leavingMeeting}
          endMeeting={this.endMeeting}
        />
      </div>
    );
  }
}

export default injectProps(Meeting);
