#pragma once
#include <QObject>
#include <QThread>

#include "core/rtc_engine_wrap.h"
#include "meeting/Core/meeting_model.h"

class MeetingRtcEngineWrap : public QObject {
  Q_OBJECT

public:
	static MeetingRtcEngineWrap& instance();
	static int init();
	static int unInit();
	static int setupLocalView(void* view, bytertc::RenderMode mode,
		const std::string& uid);
	static int setupRemoteView(void* view, bytertc::RenderMode mode,
		const std::string& uid);
	static int startPreview();
	static int stopPreview();

	static int subscribeVideoStream(const std::string& user_id,
		const SubscribeConfig& info, int index = 0);
	static int unSubscribeVideoStream(const std::string& user_id, bool screen);

	static int enableLocalAudio(bool enable);
	static int enableLocalVideo(bool enable);

	static int muteLocalAudio(bool bMute);
	static int muteLocalVideo(bool bMute);
	static int setLocalMirrorMode(bool isMirrored);

	static int login(const std::string& roomid, const std::string& uid,
		const std::string& token);
	static int logout();

	static int setVideoProfiles(const VideoConfiger& vc);
	static int setScreenProfiles(const VideoConfiger& vc);

	static int getAudioInputDevices(std::vector<RtcDevice>&);
	static int setAudioInputDevice(int index);
	static int getAudioInputDevice(std::string& guid);

	static int setAudioVolumeIndicate(int indicate);

	static int getAudioOutputDevices(std::vector<RtcDevice>&);
	static int setAudioOutputDevice(int index);
	static int getAudioOutputDevice(std::string& guid);

	static int getVideoCaptureDevices(std::vector<RtcDevice>&);
	static int setVideoCaptureDevice(int index);
	static int getVideoCaptureDevice(std::string& guid);

	static int setRemoteScreenView(const std::string& uid, void* view);
	static int startScreenCapture(void* source_id,
		const std::vector<void*>& excluded);
	static int startScreenCaptureByWindowId(void* window_id);
	static int stopScreenCapture();

	static int getShareList(std::vector<SnapshotAttr>& list);
	static QPixmap getThumbnail(SnapshotAttr::SnapshotType type, void* source_id,
		int max_width, int max_height);
	static bool audioRecordDevicesTest();
 public:
	 void onVideoStateChanged(std::string device_id,
		 bytertc::MediaDeviceState device_state, bytertc::MediaDeviceError error);
	 void onAudioStateChanged(std::string device_id,
		 bytertc::MediaDeviceState device_state, bytertc::MediaDeviceError error);

signals:
	void sigOnRoomStateChanged(std::string room_id, std::string uid, int state, std::string extra_info);
	void sigUpdateAudio();
	void sigUpdateVideo();
	void sigUpdateDevices();
	void sigOnAudioVolumeUpdate();
	void sigUpdateInfo();
	void sigForceExitRoom();

 protected:
	 MeetingRtcEngineWrap();
	 ~MeetingRtcEngineWrap();
};
