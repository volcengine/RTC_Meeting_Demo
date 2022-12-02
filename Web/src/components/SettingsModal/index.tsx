import React, { FC, useState, useEffect, useMemo, useCallback } from 'react';
import { Modal, Form, Select, Switch, Slider, Row, Col } from 'antd';
import { connect, bindActionCreators } from 'dva';
import { injectIntl } from 'umi';
import { ConnectedProps } from 'react-redux';
import { WrappedComponentProps } from 'react-intl';
import styles from './index.less';
import { RESOLUTIOIN_LIST, FRAMERATE, BITRATEMAP } from '@/config';
import { AppState, DeviceItems } from '@/app-interfaces';
import { Dispatch } from '@@/plugin-dva/connect';
import { meetingSettingsActions } from '@/models/meeting-settings';
import { HistoryVideoRecord } from '@/lib/socket-interfaces';
import deleteIcon from '/assets/images/deleteIcon.png';
import moment from 'moment';
import Logger from '@/utils/Logger';
import Utils from '@/utils/utils';
import VERTC, { DeviceInfo } from '@volcengine/rtc';

function mapStateToProps(state: AppState) {
  return {
    user: state.user,
    mc: state.meetingControl.sdk,
    settings: state.meetingSettings,
    rtc: state.rtcClientControl.rtc,
  };
}

function mapDispatchToProps(dispatch: Dispatch) {
  return {
    dispatch,
    ...bindActionCreators(meetingSettingsActions, dispatch),
  };
}

const connector = connect(mapStateToProps, mapDispatchToProps);

export type SettingsModalProps = ConnectedProps<typeof connector> &
  WrappedComponentProps & { visible: boolean; close: () => void };

const commonCol = {
  labelCol: { span: 8 },
  wrapperCol: { span: 10 },
};

const SettingsModal: FC<SettingsModalProps> = (props) => {
  const [form] = Form.useForm();
  const [videoList, setVideoList] = useState<HistoryVideoRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [devices, setDevices] = useState<DeviceItems>();

  const {
    visible,
    close,
    setStreamSettings,
    setScreenStreamSettings,
    setMic,
    setCamera,
    setRealtimeParam,
    mc,
    settings,
    user,
    rtc: { engine },
  } = props;

  const initialValues = useMemo(() => {
    const {
      streamSettings: { bitrate: BPS, frameRate: FPS, resolution },
      screenStreamSettings: {
        bitrate: shareBPS,
        frameRate: shareFPS,
        resolution: shareResolution,
      },
      mic,
      camera,
      realtimeParam,
    } = settings;

    return {
      resolution: `${resolution.width} * ${resolution.height}`,
      shareResolution: `${shareResolution.width} * ${shareResolution.height}`,
      FPS: FPS.max,
      shareFPS: shareFPS.max,
      BPS: BPS.max,
      shareBPS: shareBPS.max,
      mic: mic || devices?.audioInputs[0]?.deviceId || '',
      camera: camera || devices?.videoInputs[0]?.deviceId || '',
      realtimeParam,
    };
  }, [settings, devices]);

  useEffect(() => {
    form.setFieldsValue({
      ...initialValues,
    });
  }, [form, initialValues]);

  const resolution = Form.useWatch('resolution', form);
  const shareResolution = Form.useWatch('shareResolution', form);

  useEffect(() => {
    const bps = form.getFieldValue('BPS');
    const range = BITRATEMAP[resolution || initialValues.resolution];
    if (bps < range[0]) {
      form.setFieldValue('BPS', range[0]);
    }

    if (bps > range[1]) {
      form.setFieldValue('BPS', range[1]);
    }
  }, [resolution]);

  useEffect(() => {
    const bps = form.getFieldValue('shareBPS');
    const range = BITRATEMAP[shareResolution || initialValues.shareResolution];
    if (bps < range[0]) {
      form.setFieldValue('shareBPS', range[0]);
    }

    if (bps > range[1]) {
      form.setFieldValue('shareBPS', range[1]);
    }
  }, [shareResolution]);

  const formatStreamSettings = (
    res: string,
    fps: number,
    bps: number,
    bpsMin: number
  ) => {
    return {
      resolution: {
        width: parseInt(res.split(' * ')[0]),
        height: parseInt(res.split(' * ')[1]),
      },
      frameRate: { min: 10, max: fps },
      bitrate: {
        min: bpsMin,
        max: bps,
      },
    };
  };

  const onOk = async () => {
    const data = form.getFieldsValue(true);
    const streamConfigs = formatStreamSettings(
      data.resolution,
      data.FPS,
      data.BPS,
      250
    );
    const screenConfigs = formatStreamSettings(
      data.shareResolution,
      data.shareFPS,
      data.shareBPS,
      800
    );

    const isolation = ['width', 'height', 'max', 'min'];
    const streamDiff = Utils.diff(
      settings.streamSettings,
      streamConfigs,
      isolation
    );
    const screenDiff = Utils.diff(
      settings.screenStreamSettings,
      screenConfigs,
      isolation
    );

    const caseToDo = async (type: string) => {
      if (type === 'stream') {
        console.log(streamConfigs);
        await engine.setVideoCaptureConfig({
          frameRate: streamConfigs.frameRate.max,
          ...streamConfigs.resolution,
        });
        await engine.setVideoEncoderConfig({
          frameRate: streamConfigs.frameRate.max,
          ...streamConfigs.resolution,
          maxKbps: streamConfigs.bitrate.max,
        });
      } else {
        await engine.setScreenEncoderConfig({
          frameRate: screenConfigs.frameRate.max,
          ...screenConfigs.resolution,
          maxKbps: screenConfigs.bitrate.max,
        });
      }
    };

    //TODO 如果有变化, 则做对应的处理
    if (Object.keys(streamDiff).length) {
      await caseToDo('stream');
    }

    if (Object.keys(screenDiff).length) {
      await caseToDo('screen');
    }

    if (settings.mic !== data.mic) {
      props.rtc.switchDevice('microphone', data.mic);
      //   await engine.setAudioCaptureDevice(data.mic);
    }
    if (settings.camera !== data.camera) {
      props.rtc.switchDevice('camera', data.camera);

      //   await engine.setVideoCaptureDevice(data.camera);
      engine.setLocalVideoMirrorType(1);
    }

    //TODO: 保存配置
    setStreamSettings(streamConfigs);
    setScreenStreamSettings(screenConfigs);
    setMic(data.mic);
    setCamera(data.camera);
    setRealtimeParam(data.realtimeParam);
    close();
  };

  const getHistoryVideoRecord = useCallback(() => {
    mc?.getHistoryVideoRecord().then((res) => {
      setVideoList(res);
      setLoading(false);
    });
  }, [mc]);

  const devicesEmu = async () => {
    const devices = await props.rtc.getDevices();
    setDevices(devices);
    return devices;
  };

  useEffect(() => {
    devicesEmu();
    // getHistoryVideoRecord();

    const handleVideoDeviceStateChanged = async (e: DeviceInfo) => {
      const devices = await devicesEmu();
      let deviceId = e.mediaDeviceInfo.deviceId;

      if (e.deviceState === 'inactive') {
        deviceId = devices.videoInputs?.[0].deviceId || '';
      }
      setCamera(deviceId);
      props.rtc.switchDevice('camera', deviceId);
    };

    props.rtc.engine.on(
      VERTC.events.onVideoDeviceStateChanged,
      handleVideoDeviceStateChanged
    );

    const handleAudioDeviceStateChanged = async (e: DeviceInfo) => {
      const devices = await devicesEmu();
      if (e.mediaDeviceInfo.kind === 'audioinput') {
        let deviceId = e.mediaDeviceInfo.deviceId;
        if (e.deviceState === 'inactive') {
          deviceId = devices.audioInputs?.[0].deviceId || '';
        }
        setMic(deviceId);
        props.rtc.switchDevice('microphone', deviceId);
      }
    };

    props.rtc.engine.on(
      VERTC.events.onAudioDeviceStateChanged,
      handleAudioDeviceStateChanged
    );

    return () => {
      props.rtc.engine.off(
        VERTC.events.onVideoDeviceStateChanged,
        handleVideoDeviceStateChanged
      );
      props.rtc.engine.off(
        VERTC.events.onAudioDeviceStateChanged,
        handleAudioDeviceStateChanged
      );
    };
  }, []);

  useEffect(() => {
    if (visible) {
      devicesEmu();
    }
  }, [visible]);

  const handleClose = () => {
    form.setFieldsValue(initialValues);
    close();
  };

  const myVideoList = useMemo(() => {
    return videoList?.filter((item) => item.video_holder);
  }, [videoList]);

  return (
    <Modal
      title="会议设置"
      open={visible}
      width={788}
      className={styles['settings-modal']}
      onCancel={handleClose}
      onOk={onOk}
      destroyOnClose
      bodyStyle={{
        minHeight: 400,
      }}
    >
      <Form form={form} labelCol={{ span: 4 }} initialValues={initialValues}>
        <Row>
          <Col span={12}>
            <Form.Item label="分辨率" name="resolution" {...commonCol}>
              <Select>
                {RESOLUTIOIN_LIST.map((item) => (
                  <Select.Option key={item.text} value={item.text}>
                    {item.text}
                  </Select.Option>
                ))}
              </Select>
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item
              label="屏幕共享分辨率"
              name="shareResolution"
              {...commonCol}
            >
              <Select>
                {RESOLUTIOIN_LIST.map((item) => (
                  <Select.Option key={item.text} value={item.text}>
                    {item.text}
                  </Select.Option>
                ))}
              </Select>
            </Form.Item>
          </Col>
        </Row>
        <Row>
          <Col span={12}>
            <Form.Item label="帧率" name="FPS" {...commonCol}>
              <Select>
                {FRAMERATE.map((item) => (
                  <Select.Option key={item} value={item}>
                    {`${item} fps`}
                  </Select.Option>
                ))}
              </Select>
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item label="屏幕共享帧率" name="shareFPS" {...commonCol}>
              <Select>
                {FRAMERATE.map((item) => (
                  <Select.Option key={item} value={item}>
                    {`${item} fps`}
                  </Select.Option>
                ))}
              </Select>
            </Form.Item>
          </Col>
        </Row>
        <Row>
          <Col span={12}>
            <Form.Item label="码率" {...commonCol} wrapperCol={{ span: 16 }}>
              <div style={{ display: 'flex', alignItems: 'center' }}>
                <div
                  style={{
                    display: 'flex',
                    width: 160,
                    justifyContent: 'space-between',
                  }}
                >
                  <Form.Item
                    noStyle
                    shouldUpdate={(prevValues, currentValues) =>
                      prevValues.resolution !== currentValues.resolution
                    }
                  >
                    {() => {
                      const range =
                        BITRATEMAP[resolution || initialValues.resolution];
                      return (
                        <Form.Item noStyle name="BPS">
                          <Slider
                            min={range[0]}
                            max={range[1]}
                            style={{ width: 82 }}
                            tooltip={{
                              open: false,
                            }}
                          />
                        </Form.Item>
                      );
                    }}
                  </Form.Item>
                  <Form.Item
                    noStyle
                    shouldUpdate={(prevValues, currentValues) =>
                      prevValues.BPS !== currentValues.BPS
                    }
                  >
                    {() => (
                      <div className={styles['slider-number']}>
                        {form.getFieldValue('BPS')}
                      </div>
                    )}
                  </Form.Item>
                </div>
                <div style={{ marginLeft: 8 }}>kbps</div>
              </div>
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item
              label="屏幕共享码率"
              {...commonCol}
              wrapperCol={{ span: 16 }}
            >
              <div style={{ display: 'flex', alignItems: 'center' }}>
                <div
                  style={{
                    display: 'flex',
                    width: 160,
                    justifyContent: 'space-between',
                  }}
                >
                  <Form.Item
                    noStyle
                    shouldUpdate={(prevValues, currentValues) =>
                      prevValues.shareResolution !==
                      currentValues.shareResolution
                    }
                  >
                    {() => {
                      const res = form.getFieldValue(
                        'shareResolution'
                      ) as string;
                      const range =
                        BITRATEMAP[
                          shareResolution || initialValues.shareResolution
                        ];

                      return (
                        <Form.Item noStyle name="shareBPS">
                          <Slider
                            min={range[0]}
                            max={range[1]}
                            style={{ width: 82 }}
                            tooltip={{
                              open: false,
                            }}
                          />
                        </Form.Item>
                      );
                    }}
                  </Form.Item>
                  <Form.Item
                    noStyle
                    shouldUpdate={(prevValues, currentValues) =>
                      prevValues.shareBPS !== currentValues.shareBPS
                    }
                  >
                    {() => (
                      <div className={styles['slider-number']}>
                        {form.getFieldValue('shareBPS')}
                      </div>
                    )}
                  </Form.Item>
                </div>
                <div style={{ marginLeft: 8 }}>kbps</div>
              </div>
            </Form.Item>
          </Col>
        </Row>
        <Form.Item label="麦克风" name="mic" wrapperCol={{ span: 5 }}>
          <Select dropdownMatchSelectWidth={false} disabled={!user?.isMicOn}>
            {devices?.audioInputs.map((item) => (
              <Select.Option value={item.deviceId} key={item.deviceId}>
                {item.label}
              </Select.Option>
            ))}
          </Select>
        </Form.Item>
        <Form.Item label="摄像头" name="camera" wrapperCol={{ span: 5 }}>
          <Select dropdownMatchSelectWidth={false} disabled={!user?.isCameraOn}>
            {devices?.videoInputs.map((item) => (
              <Select.Option value={item.deviceId} key={item.deviceId}>
                {item.label}
              </Select.Option>
            ))}
          </Select>
        </Form.Item>
        <Form.Item
          label="查看历史会议"
          name="history"
          wrapperCol={{ span: 10 }}
        >
          <Select
            placeholder="选择历史会议点击链接查看"
            loading={loading}
            onSelect={(url: string) => window.open(url, '_blank')}
          >
            {videoList
              ?.filter((i) => !i.video_holder)
              .map((item) => {
                return (
                  <Select.Option
                    key={item.created_at}
                    value={item.download_url}
                  >
                    {moment(item.created_at / 1000000).format(
                      'YYYY-MM-DD HH:mm:ss'
                    )}
                  </Select.Option>
                );
              })}
          </Select>
        </Form.Item>
        <Form.Item label="我的云录制" name="record" wrapperCol={{ span: 10 }}>
          <Select
            placeholder="会议录制者有权在此处查看和删除录像"
            loading={loading}
            onSelect={(url: string) => window.open(url, '_blank')}
          >
            {myVideoList?.map((item) => (
              <Select.Option
                key={item.created_at}
                value={item.download_url}
                className={styles['my-video-list']}
              >
                <div className={styles['my-video-list-item']}>
                  <div>
                    {moment(item.created_at / 1000000).format(
                      'YYYY-MM-DD HH:mm:ss'
                    )}
                  </div>
                  <img
                    src={deleteIcon}
                    width={12}
                    height={12}
                    onClick={(e) => {
                      e.stopPropagation();
                      // ! 暂时不支持删除
                      //   setLoading(true);
                      //   mc?.deleteVideoRecord({ vid: item.vid })
                      //     .then(() => {
                      //       notification.success({ message: '删除成功' });
                      //     })
                      //     .then(() => getHistoryVideoRecord())
                      //     .catch((err) => {
                      //       notification.error({
                      //         message: '删除录制记录失败',
                      //         description: err,
                      //       });
                      //       setLoading(false);
                      //     });
                    }}
                  />
                </div>
              </Select.Option>
            ))}
          </Select>
        </Form.Item>
        <Form.Item
          label="实时视频参数"
          name="realtimeParam"
          valuePropName="checked"
        >
          <Switch />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default connector(injectIntl(SettingsModal));
