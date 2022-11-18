#include "meeting_rtc_wrap.h"

#include <algorithm>

#include "feature/defination.h"
#include "meeting/Common/warning_tips.h"
#include "meeting/Core/data_mgr.h"
#include "meeting/Core/page_manager.h"

MeetingRtcEngineWrap& MeetingRtcEngineWrap::instance() {
  static MeetingRtcEngineWrap engine;
  return engine;
}

int MeetingRtcEngineWrap::init() {
	auto& engine_wrap = instance();
	int ret = RtcEngineWrap::instance().setEnv(bytertc::kEnvProduct);
	RtcEngineWrap::instance().initDevices();

	enableLocalAudio(true);
	enableLocalVideo(true);
	setAudioVolumeIndicate(1000);

	QObject::connect(&RtcEngineWrap::instance(), &RtcEngineWrap::sigOnRoomStateChanged,
		&instance(), &MeetingRtcEngineWrap::sigOnRoomStateChanged);

	QObject::connect(
		&RtcEngineWrap::instance(), &RtcEngineWrap::sigOnLocalAudioStateChanged,
		&engine_wrap,
		[=](bytertc::LocalAudioStreamState state,
			bytertc::LocalAudioStreamError error) {
				if (state == bytertc::kLocalAudioStreamStateFailed &&
					(error == bytertc::kLocalAudioStreamErrorDeviceNoPermission ||
						bytertc::kLocalAudioStreamErrorRecordFailure)) {
					WarningTips::showTips("麦克风打开失败，请检查设备",
						TipsType::kWarning,
						meeting::PageManager::currentWidget(), 2000);

					if (!meeting::DataMgr::instance().mute_audio()) {
						meeting::DataMgr::instance().setMuteAudio(true);
						MeetingRtcEngineWrap::muteLocalAudio(true);
						emit instance().sigUpdateAudio();
					}
				}
		});

	QObject::connect(
		&RtcEngineWrap::instance(), &RtcEngineWrap::sigOnLocalVideoStateChanged,
		&engine_wrap,
		[=](bytertc::StreamIndex idx, bytertc::LocalVideoStreamState state,
			bytertc::LocalVideoStreamError error) {
				if (state == bytertc::kLocalVideoStreamStateFailed &&
					(error == bytertc::kLocalVideoStreamErrorDeviceNoPermission ||
						error == bytertc::kLocalVideoStreamErrorCaptureFailure ||
						error == bytertc::kLocalVideoStreamErrorDeviceBusy)) {
					if (!meeting::DataMgr::instance().mute_video()) {
						meeting::DataMgr::instance().setMuteVideo(true);
						MeetingRtcEngineWrap::muteLocalVideo(true);
						WarningTips::showTips("摄像头打开失败，请检查设备",
							TipsType::kWarning,
							meeting::PageManager::currentWidget(), 2000);
						emit instance().sigUpdateVideo();
					}
				}
		});

    QObject::connect(
        &RtcEngineWrap::instance(), &RtcEngineWrap::sigOnVideoDeviceStateChanged,
        &engine_wrap,
        [=](std::string device_id, bytertc::RTCVideoDeviceType type,
            bytertc::MediaDeviceState state, bytertc::MediaDeviceError error) {
                if (type == bytertc::kRTCVideoDeviceTypeCaptureDevice) {
                    instance().onVideoStateChanged(device_id, state, error);
                }
        });

    QObject::connect(
        &RtcEngineWrap::instance(), &RtcEngineWrap::sigOnAudioDeviceStateChanged,
        &engine_wrap,
        [=](std::string device_id, bytertc::RTCAudioDeviceType type,
            bytertc::MediaDeviceState state, bytertc::MediaDeviceError error) {
                if (type == bytertc::kRTCAudioDeviceTypeCaptureDevice) {
                    instance().onAudioStateChanged(device_id, state, error);
                }
        });

	QObject::connect(
		&RtcEngineWrap::instance(), &RtcEngineWrap::sigOnRemoteAudioVolumeIndication,
		&engine_wrap,
		[=](std::vector<AudioVolumeInfoWrap> speakers, int totalVolume) {
			std::sort(
				speakers.begin(), speakers.end(),
				[](const AudioVolumeInfoWrap& l, const AudioVolumeInfoWrap& r) {
					return l.volume > r.volume;
				});
			meeting::DataMgr::instance().setVolumes(std::move(speakers));
			emit instance().sigOnAudioVolumeUpdate();
		});

	QObject::connect(&RtcEngineWrap::instance(), &RtcEngineWrap::sigOnLocalStreamStats,
		&engine_wrap, [=](bytertc::LocalStreamStats stats) {
			StreamInfo info =
				meeting::DataMgr::instance().stream_info();
			info.local_auido_bit = stats.audio_stats.send_kbitrate;

			info.local_video_bit = stats.video_stats.sent_kbitrate;
			info.local_fps = stats.video_stats.sent_frame_rate;
			info.local_width = stats.video_stats.encoded_frame_width;
			info.local_height = stats.video_stats.encoded_frame_height;

			meeting::DataMgr::instance().setStreamInfo(info);
			emit instance().sigUpdateInfo();
		});

	QObject::connect(
		&RtcEngineWrap::instance(), &RtcEngineWrap::sigOnRemoteStreamStats,
		&engine_wrap, [=](RemoteStreamStatsWrap stats) {
			StreamInfo info = meeting::DataMgr::instance().stream_info();
			info.remote_video_bit = stats.video_stats.received_kbitrate;
			info.remote_audio_bit = stats.audio_stats.received_kbitrate;
			info.remote_audio_lose = stats.audio_stats.audio_loss_rate * 100;
			info.remote_video_lose = stats.video_stats.video_loss_rate * 100;
			info.video_rtt = stats.video_stats.rtt;
			info.audio_rtt = stats.audio_stats.rtt;

			if (info.remote_fps_min >
				stats.video_stats.renderer_output_frame_rate &&
				stats.video_stats.renderer_output_frame_rate > 0 ||
				info.remote_fps_min == 0) {
				info.remote_fps_min = stats.video_stats.renderer_output_frame_rate;
			}

			if (info.remote_fps_max <
				stats.video_stats.renderer_output_frame_rate) {
				info.remote_fps_max = stats.video_stats.renderer_output_frame_rate;
			}

			if (stats.video_stats.width < info.remote_width_min &&
				stats.video_stats.height < info.remote_height_min &&
				stats.video_stats.width > 0 && stats.video_stats.height > 0 ||
				info.remote_width_min == 0 || info.remote_height_min == 0) {
				info.remote_width_min = stats.video_stats.width;
				info.remote_height_min = stats.video_stats.height;
			}

			if (stats.video_stats.width > info.remote_width_max &&
				stats.video_stats.height > info.remote_height_max) {
				info.remote_width_max = stats.video_stats.width;
				info.remote_height_max = stats.video_stats.height;
			}

			meeting::DataMgr::instance().setStreamInfo(info);
			emit instance().sigUpdateInfo();
		});

	QObject::connect(&RtcEngineWrap::instance(), &RtcEngineWrap::sigOnSysStats,
		&engine_wrap, [=](bytertc::SysStats stats) {
			StreamInfo info =
				meeting::DataMgr::instance().stream_info();
			info.cpu_total = stats.cpu_total_usage * 100;
			info.cpu_use = stats.cpu_app_usage * 100;
			meeting::DataMgr::instance().setStreamInfo(info);
		});

	return ret;
}

int MeetingRtcEngineWrap::unInit() {
	QObject::disconnect(&RtcEngineWrap::instance(), nullptr, &instance(), nullptr);
	RtcEngineWrap::instance().resetDevices();
	return 0;
}

int MeetingRtcEngineWrap::setupLocalView(void* view, bytertc::RenderMode mode,
                                         const std::string& uid) {
  auto& engine_wrap = instance();
  return RtcEngineWrap::instance().setLocalVideoCanvas(
      uid, bytertc::StreamIndex::kStreamIndexMain, mode, view);
}

int MeetingRtcEngineWrap::setupRemoteView(void* view, bytertc::RenderMode mode,
                                          const std::string& uid) {
  auto& engine_wrap = instance();
  return RtcEngineWrap::instance().setRemoteVideoCanvas(
      uid, bytertc::StreamIndex::kStreamIndexMain, mode, view);
}

int MeetingRtcEngineWrap::startPreview() {
	auto& engine_wrap = instance();
	return RtcEngineWrap::instance().startPreview();
}

int MeetingRtcEngineWrap::stopPreview() {
  auto& engine_wrap = instance();
  return RtcEngineWrap::instance().stopPreview();
}

int MeetingRtcEngineWrap::subscribeVideoStream(const std::string& user_id,
                                               const SubscribeConfig& info,
                                               int index) {
  auto& engine_wrap = instance();
  bytertc::SubscribeConfig config;
  config.is_screen = info.is_screen;
  config.sub_video = info.sub_video;
  config.sub_audio = true;
  config.video_index = index;
  RtcEngineWrap::instance().subscribeVideoStream(user_id, config);
  return 0;
}

int MeetingRtcEngineWrap::unSubscribeVideoStream(const std::string& user_id,
                                                 bool screen) {
  return RtcEngineWrap::instance().unSubscribeVideoStream(user_id, screen);
}

int MeetingRtcEngineWrap::enableLocalAudio(bool enable) {
  return RtcEngineWrap::instance().enableLocalAudio(enable);
}

int MeetingRtcEngineWrap::enableLocalVideo(bool enable) {
  return RtcEngineWrap::instance().enableLocalVideo(enable);
}

int MeetingRtcEngineWrap::muteLocalAudio(bool bMute) {
  auto& engine_wrap = instance();
  return RtcEngineWrap::instance().muteLocalAudio(bMute);
}

int MeetingRtcEngineWrap::muteLocalVideo(bool bMute) {
  auto& engine_wrap = instance();
  return RtcEngineWrap::instance().muteLocalVideo(bMute);
}

int MeetingRtcEngineWrap::setLocalMirrorMode(bool isMirrored) {
    auto type = isMirrored ? bytertc::MirrorType::kMirrorTypeRenderAndEncoder
        : bytertc::MirrorType::kMirrorTypeNone;
    return RtcEngineWrap::instance().setLocalPreviewMirrorMode(type);
}

int MeetingRtcEngineWrap::login(const std::string& roomid,
                                const std::string& uid,
                                const std::string& token) {
    auto& engine_wrap = instance();

    bytertc::UserInfo user = { uid.c_str(), nullptr };
    return RtcEngineWrap::instance().joinRoom(
        token, roomid, user,
        bytertc::RoomProfileType::kRoomProfileTypeLiveBroadcasting);
}

int MeetingRtcEngineWrap::logout() {
    auto& engine_wrap = instance();
    return RtcEngineWrap::instance().leaveRoom();
}

int MeetingRtcEngineWrap::setVideoProfiles(const VideoConfiger& vc) {
    auto& engine_wrap = instance();
    bytertc::VideoEncoderConfig config;
    config.width = vc.resolution.width;
    config.height = vc.resolution.height;
    config.frameRate = vc.fps;
    config.maxBitrate = vc.kbpsRange.max_kbps;
    return RtcEngineWrap::instance().setVideoProfiles(config);
}

int MeetingRtcEngineWrap::setScreenProfiles(const VideoConfiger& vc) {
  bytertc::VideoEncoderConfig config;
  config.frameRate = vc.fps;
  config.height = vc.resolution.height;
  config.width = vc.resolution.width;
  config.maxBitrate = vc.kbpsRange.max_kbps;
  return RtcEngineWrap::instance().setScreenProfiles(config);
}

int MeetingRtcEngineWrap::getAudioInputDevices(
    std::vector<RtcDevice>& devices) {
  return RtcEngineWrap::instance().getAudioInputDevices(devices);
}

int MeetingRtcEngineWrap::setAudioInputDevice(int index) {
  return RtcEngineWrap::instance().setAudioInputDevice(index);
}

int MeetingRtcEngineWrap::getAudioInputDevice(std::string& guid) {
  return RtcEngineWrap::instance().getAudioInputDevice(guid);
}

int MeetingRtcEngineWrap::setAudioVolumeIndicate(int indicate) {
  return RtcEngineWrap::instance().setAudioVolumeIndicate(indicate);
}

int MeetingRtcEngineWrap::getAudioOutputDevices(
    std::vector<RtcDevice>& devices) {
  return RtcEngineWrap::instance().getAudioOutputDevices(devices);
}

int MeetingRtcEngineWrap::setAudioOutputDevice(int index) {
  return RtcEngineWrap::instance().setAudioOutputDevice(index);
}

int MeetingRtcEngineWrap::getAudioOutputDevice(std::string& guid) {
  return RtcEngineWrap::instance().getAudioOutputDevice(guid);
}

int MeetingRtcEngineWrap::getVideoCaptureDevices(
    std::vector<RtcDevice>& devices) {
  return RtcEngineWrap::instance().getVideoCaptureDevices(devices);
}

int MeetingRtcEngineWrap::setVideoCaptureDevice(int index) {
  return RtcEngineWrap::instance().setVideoCaptureDevice(index);
}

int MeetingRtcEngineWrap::getVideoCaptureDevice(std::string& guid) {
  return RtcEngineWrap::instance().getVideoCaptureDevice(guid);
}

int MeetingRtcEngineWrap::setRemoteScreenView(const std::string& uid,
                                              void* view) {
  return RtcEngineWrap::instance().setRemoteVideoCanvas(
      uid, bytertc::StreamIndex::kStreamIndexScreen,
      bytertc::RenderMode::kRenderModeFit, view);
}

int MeetingRtcEngineWrap::startScreenCapture(
    void* source_id, const std::vector<void*>& excluded) {
  return RtcEngineWrap::instance().startScreenCapture(source_id, excluded);
}

int MeetingRtcEngineWrap::startScreenCaptureByWindowId(void* window_id) {
  return RtcEngineWrap::instance().startScreenCaptureByWindowId(window_id);
}

int MeetingRtcEngineWrap::stopScreenCapture() {
  return RtcEngineWrap::instance().stopScreenCapture();
}

int MeetingRtcEngineWrap::getShareList(std::vector<SnapshotAttr>& list) {
  return RtcEngineWrap::instance().getShareList(list);
}

QPixmap MeetingRtcEngineWrap::getThumbnail(SnapshotAttr::SnapshotType type,
                                           void* source_id, int max_width,
                                           int max_height) {
  return RtcEngineWrap::instance().getThumbnail(type, source_id, max_width,
                                         max_height);
}

bool MeetingRtcEngineWrap::audioRecordDevicesTest() {
  return RtcEngineWrap::instance().audioReocrdDeviceTest();
}

int MeetingRtcEngineWrap::feedBack(const std::string& str) { return 0; }

void MeetingRtcEngineWrap::onVideoStateChanged(std::string device_id,
    bytertc::MediaDeviceState device_state, bytertc::MediaDeviceError error) {
	emit instance().sigUpdateDevices();
	std::vector<RtcDevice> devices;
	MeetingRtcEngineWrap::getVideoCaptureDevices(devices);
	if ((devices.empty() || error == bytertc::kMediaDeviceErrorDeviceNoPermission)
		&& !meeting::DataMgr::instance().mute_video()) {
        if (error == bytertc::kMediaDeviceErrorDeviceNoPermission) {
            WarningTips::showTips("摄像头打开失败，请检查设备", TipsType::kWarning,
                meeting::PageManager::currentWidget(), 2000);
        }
		meeting::DataMgr::instance().setMuteVideo(true);
		MeetingRtcEngineWrap::muteLocalVideo(true);
		emit instance().sigUpdateVideo();
	} else {
		MeetingRtcEngineWrap::muteLocalVideo(
			meeting::DataMgr::instance().mute_video());
	}

	switch (device_state) {
		case bytertc::kMediaDeviceStateAdded:  // Insert a new device
			WarningTips::showTips("有新录像设备插入", TipsType::kWarning,
				meeting::PageManager::currentWidget(), 2000);
			break;
		case bytertc::kMediaDeviceStateRemoved:  // Pull out the device
			WarningTips::showTips("录像设备被拔出", TipsType::kWarning,
				meeting::PageManager::currentWidget(), 2000);
			MeetingRtcEngineWrap::setVideoCaptureDevice(RtcEngineWrap::instance().getCurrentVideoCaptureDeviceIndex());
			break;
		default:
			break;
	}
}

void MeetingRtcEngineWrap::onAudioStateChanged(std::string device_id, 
    bytertc::MediaDeviceState device_state, bytertc::MediaDeviceError error) {
	emit instance().sigUpdateDevices();
	std::vector<RtcDevice> devices;
	MeetingRtcEngineWrap::getAudioInputDevices(devices);
	if ((devices.empty() || device_state == bytertc::kMediaDeviceStateRuntimeError)
		&& !meeting::DataMgr::instance().mute_audio()) {
		meeting::DataMgr::instance().setMuteAudio(true);
		MeetingRtcEngineWrap::muteLocalAudio(true);
		emit instance().sigUpdateAudio();
	} else {
	MeetingRtcEngineWrap::muteLocalAudio(
		meeting::DataMgr::instance().mute_audio());
	}

	switch (device_state) {
	case bytertc::kMediaDeviceStateAdded:  // Insert a new device
		WarningTips::showTips("有新麦克风设备插入", TipsType::kWarning,
							meeting::PageManager::currentWidget(), 2000);
		break;
	case bytertc::kMediaDeviceStateRemoved:  // Pull out the device
		WarningTips::showTips("麦克风设备被拔出", TipsType::kWarning,
							meeting::PageManager::currentWidget(), 2000);
		break;
	default:
		break;
	}
}

MeetingRtcEngineWrap::MeetingRtcEngineWrap() : QObject(nullptr) {}

MeetingRtcEngineWrap::~MeetingRtcEngineWrap() {}
