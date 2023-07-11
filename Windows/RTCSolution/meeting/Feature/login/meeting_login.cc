#include "meeting_login.h"

#include <Windows.h>
#include <QTimer>

#include "Meeting/Common/meeting_setting.h"
#include "Meeting/Common/utils.h"
#include "core/component/toast.h"
#include "meeting/Common/warning_tips.h"
#include "meeting/Core/meeting_rtc_wrap.h"
#include "meeting/Core/meeting_session.h"
#include "meeting/core/page_manager.h"

MeetingLoginWidget::MeetingLoginWidget(QWidget* parent) : QWidget(parent) {
  ui.setupUi(this);
  resize(QSize(960,700));
  btn_back = new LabelWarp(this);
  btn_back->move(32, 66);
  btn_back->setFixedSize(48, 48);
  btn_back->setStyleSheet(
      "border:none;background-image:url(:img/"
      "back);background-position:center;background-repeat:no-repeat;");
  utils::setWidgetRoundMask(btn_back, 48, 48);
  btn_back->show();
  login_controller_widget_ = new LoginControllerWidget(this);
  login_controller_widget_->show();

  connect(btn_back, &LabelWarp::sigPressed, this, &QWidget::close);

  connect(login_controller_widget_, &LoginControllerWidget::sigShowSettingPage,
          this, [=] { meeting::PageManager::showSetting(this); });

    connect(login_controller_widget_, &LoginControllerWidget::sigJoinRomm, this,
            [=] {
            if (login_)return;
            login_ = true;
            vrd::MeetingSession::instance().changeUserName(
                meeting::DataMgr::instance().user_name(), [=](int code) {
                    if (code != 200) {
                        if (code == 430) {
                            WarningTips::showTips(
                                "输入内容包含敏感词，请重新输入", TipsType::kWarning,
                                meeting::PageManager::currentWidget(), 2000);
                        }
                        else if (code == 500) {
                            WarningTips::showTips(
                                "系统繁忙，请稍后重试", TipsType::kWarning,
                                meeting::PageManager::currentWidget(), 2000);
                        }
                        else {
                            WarningTips::showTips(
                                "修改昵称失败, 请换个昵称重试", TipsType::kWarning,
                                meeting::PageManager::currentWidget(), 2000);
                        }
                    }
                    login_ = false;
                    return;
                });
            vrd::MeetingSession::instance().joinMeeting(
                meeting::DataMgr::instance().user_name(),
                meeting::DataMgr::instance().user_id(),
                meeting::DataMgr::instance().room_id(),
                !meeting::DataMgr::instance().mute_audio(),
                !meeting::DataMgr::instance().mute_video(), [=](int code) {
                    if (code == 200) {
                        if (meeting::DataMgr::instance().room().host_uid ==
                            meeting::DataMgr::instance().user_id()) {
                            meeting::DataMgr::instance().setLocalRole(
                                MeetingRole::kBroadCast);
                        }
                        else {
                            meeting::DataMgr::instance().setLocalRole(
                                MeetingRole::kAudience);
                        }
                        vrd::MeetingSession::instance().setRoomId(meeting::DataMgr::instance().room_id());
                        auto users = meeting::DataMgr::instance().users();
                        auto iter = std::find_if(
                            users.begin(), users.end(),
                            [](const User& usr) { return usr.is_sharing; });
                        meeting::DataMgr::instance().setShareScreen(iter !=
                            users.end());
                        setDefaultProfiles();
                        MeetingRtcEngineWrap::login(
                            meeting::DataMgr::instance().room_id(),
                            meeting::DataMgr::instance().user_id(),
                            meeting::DataMgr::instance().token());

                        stopTest();
                        QTimer::singleShot(100, [=] { hide(); });
                        meeting::PageManager::initRoom();

                        MeetingRtcEngineWrap::muteLocalVideo(meeting::DataMgr::instance().mute_video());
                        MeetingRtcEngineWrap::enableLocalVideo(!meeting::DataMgr::instance().mute_video());
                        MeetingRtcEngineWrap::muteLocalAudio(meeting::DataMgr::instance().mute_audio());
                        MeetingRtcEngineWrap::enableLocalAudio(!meeting::DataMgr::instance().mute_audio());

                        login_ = false;
                    }
                });
        });

  connect(login_controller_widget_, &LoginControllerWidget::sigAudioClicked,
          this, [=] {
            if (!MeetingRtcEngineWrap::audioRecordDevicesTest()) {
                WarningTips::showTips("麦克风权限已关闭，请至设备设置页开启", TipsType::kWarning,
                    nullptr, 2000);
              return;
            }
            bool enabled = !meeting::DataMgr::instance().mute_audio();
            if (!MeetingRtcEngineWrap::muteLocalAudio(enabled)) {
              login_controller_widget_->setMicEnable(!enabled);
              meeting::DataMgr::instance().setMuteAudio(enabled);
            }
          });

  connect(login_controller_widget_, &LoginControllerWidget::sigVideoClicked,
	  this, [=] {
		  std::vector<RtcDevice> devices;
		  MeetingRtcEngineWrap::getVideoCaptureDevices(devices);
		  if (devices.empty()) {
			  Toast::showTip("摄像头权限已关闭，请至设备设置页开启",
				  meeting::PageManager::currentWidget());
			  return;
		  }
		  bool enabled = !meeting::DataMgr::instance().mute_video();
		  if (!MeetingRtcEngineWrap::muteLocalVideo(enabled)) {
			  login_controller_widget_->setCameraEnable(!enabled);
			  if (enabled) {
				  stopTest();
			  }
			  else {
				  startTest();
			  }
			  meeting::DataMgr::instance().setMuteVideo(enabled);
		  }
	  });
}

void MeetingLoginWidget::setName(const QString& name) {
	login_controller_widget_->setUserName(name);
	meeting::DataMgr::instance().setUserName(name.toUtf8().constData());
}

void MeetingLoginWidget::startTest() {
	MeetingRtcEngineWrap::startPreview();
	MeetingRtcEngineWrap::setupLocalView(
		reinterpret_cast<void*>(ui.login_page->winId()),
		bytertc::RenderMode::kRenderModeHidden, "local");
	QTimer::singleShot(100, this,
		[=] { ui.login_page->setUpdatesEnabled(false); });
}

void MeetingLoginWidget::setMic(bool on) {
	login_controller_widget_->setMicEnable(on);
}

void MeetingLoginWidget::setCamera(bool on) {
	login_controller_widget_->setCameraEnable(on);
}

void MeetingLoginWidget::showEvent(QShowEvent*) {
	ui.lbl_sdk_ver->setText(QString("SDK版本v") + RtcEngineWrap::getSDKVersion().c_str());
	setName(meeting::DataMgr::instance().user_name().c_str());
	login_controller_widget_->setMicEnable(
		!meeting::DataMgr::instance().mute_audio());
	login_controller_widget_->setCameraEnable(
		!meeting::DataMgr::instance().mute_video());
}

void MeetingLoginWidget::resizeEvent(QResizeEvent*) {
	login_controller_widget_->move(QPoint(
		(width() - login_controller_widget_->width()) / 2, height() - 180));
}

void MeetingLoginWidget::closeEvent(QCloseEvent*) {
	login_controller_widget_->initData();
	stopTest();
	meeting::DataMgr::instance().setSetting(MeetingSettingModel());
	emit sigClose();
}

void MeetingLoginWidget::stopTest() {
	MeetingRtcEngineWrap::stopPreview();
	MeetingRtcEngineWrap::setupLocalView(
		nullptr, bytertc::RenderMode::kRenderModeHidden, "local");
	ui.login_page->setUpdatesEnabled(true);
}

void MeetingLoginWidget::setDefaultProfiles() {
    auto setting = meeting::DataMgr::instance().setting();
    MeetingRtcEngineWrap::setVideoProfiles(setting.camera);
    MeetingRtcEngineWrap::setScreenProfiles(setting.screen);
    MeetingRtcEngineWrap::setLocalMirrorMode(bytertc::MirrorType::kMirrorTypeRenderAndEncoder);
}
