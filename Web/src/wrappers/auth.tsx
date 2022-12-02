import React, { useEffect, useState } from 'react';
import { Skeleton, message } from 'antd';
import { Redirect, injectIntl, history } from 'umi';
import { ConnectedProps } from 'react-redux';
import { WrappedComponentProps } from 'react-intl';
import { AppState } from '@/app-interfaces';
import { connect, bindActionCreators } from 'dva';
import { Dispatch } from '@@/plugin-dva/connect';
import { userActions } from '@/models/user';
import Logger from '@/utils/Logger';
import Utils from '@/utils/utils';
import { TOASTS } from '@/config';

export interface AuthProps {
  children: React.ReactNode;
}

export type LoginProps = ConnectedProps<typeof connector> &
  WrappedComponentProps &
  AuthProps;

function mapStateToProps(state: AppState) {
  return {
    currentUser: state.user,
    mc: state.meetingControl.sdk,
    logged: state.user.logged,
    rtc: state.rtcClientControl.rtc,
    user: state.user,
  };
}

function mapDispatchToProps(dispatch: Dispatch) {
  return {
    dispatch,
    ...bindActionCreators(userActions, dispatch),
  };
}

const connector = connect(mapStateToProps, mapDispatchToProps);
const logger = new Logger('auth');

const login_token = Utils.getLoginToken() || '';
const login_name = Utils.getLoginUserName() || '';
const login_userId = Utils.getLoginUserId() || '';
const device_id = Utils.getDeviceId() || '';

const Auth = (props: LoginProps) => {
  const [loading, setLoading] = useState(true);

  const verifyLoginToken = async () => {
    const verifyLoginTokenRes = await props.mc?.verifyLoginToken({
      login_token: login_token,
      device_id,
    });

    if (verifyLoginTokenRes?.code !== 200) {
      if (verifyLoginTokenRes?.code === 450) {
        Utils.removeLoginInfo();
        history.push('/login');
        return;
      }
      return;
    }

    const res = verifyLoginTokenRes.response;

    logger.debug('verifyLoginToken: %o', res);
    props.setUserName(login_name);
    props.setUserId(login_userId);
    props.setAppId(res.app_id);
    props.rtc.init({
      appId: res.app_id,
      rtsUid: login_userId,
      uid: login_userId,
      rtmToken: res.rtm_token,
      serverUrl: res.server_url,
      serverSignature: res.server_signature,
      bid: res.bid,
    });

    props.mc!.rtcClient = props.rtc;

    await props.rtc.joinWithRTS();
    props.setLogged(true);
    setLoading(false);
  };

  useEffect(() => {
    Utils.protocolCheck();

    if (props.logged) {
      setLoading(false);
      return;
    }
    if (!login_token) {
      setLoading(false);
      props.setLogged(false);
      return;
    }
    if (!props.mc) {
      logger.warn('joinMeeting before meeting control init !');
      return;
    }
    verifyLoginToken();

    window.addEventListener('load', () => {
      window.addEventListener('online', () => {
        props.setNetWork(true);
      });
      window.addEventListener('offline', () => {
        props.setNetWork(false);
        message.warning(TOASTS['network_error']);
      });
    });
  }, []);

  if (loading) {
    return <Skeleton />;
  }

  if (props.logged) {
    return <div>{props.children}</div>;
  }

  return <Redirect to="/login" />;
};

export default connector(injectIntl(Auth));
