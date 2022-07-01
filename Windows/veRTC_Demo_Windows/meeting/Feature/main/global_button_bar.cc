#include "global_button_bar.h"

#include <QMouseEvent>
#include <QPainter>
#include <QStyleOption>
#include <iostream>
#include <unordered_map>

#include "meeting/Common/warning_tips.h"
#include "meeting/Core/data_mgr.h"
#include "meeting/Core/meeting_rtc_wrap.h"
#include "meeting/Core/meeting_session.h"
#include "meeting/Core/page_manager.h"
#include "meeting/common/hint_tips.h"
#include "ui_global_button_bar.h"

static void helper_set_event(QWidget *w, const QString &str) {
  auto p = new HintTips();
  p->setEventFilter(w);
  p->setHintText(str);
}

static constexpr char *kImgQss =
    "border:none;"
    "background:%2;"
    "background-image:url(%1);"
    "background-position:center;"
    "background-repeat:no-repeat;";

static std::unordered_map<int, QString> camera_qss_map = {
    {PushButtonWarp::kNormal,
     QString(kImgQss).arg(":/img/meeting_big_cam").arg("none")},
    {PushButtonWarp::kHover,
     QString(kImgQss).arg(":/img/meeting_big_cam").arg("#272E3B")},
    {PushButtonWarp::kPress,
     QString(kImgQss).arg(":/img/meeting_big_cam").arg("#101319")},
    {PushButtonWarp::kSelect,
     QString(kImgQss).arg(":/img/meeting_big_cam_close").arg("none")},
    {PushButtonWarp::kSelect | PushButtonWarp::kHover,
     QString(kImgQss).arg(":/img/meeting_big_cam_close").arg("#272E3B")},
    {PushButtonWarp::kSelect | PushButtonWarp::kPress,
     QString(kImgQss).arg(":/img/meeting_big_cam_close").arg("#101319")}};

static std::unordered_map<int, QString> mic_qss_map = {
    {PushButtonWarp::kNormal,
     QString(kImgQss).arg(":/img/meeting_big_mic").arg("none")},
    {PushButtonWarp::kHover,
     QString(kImgQss).arg(":/img/meeting_big_mic").arg("#272E3B")},
    {PushButtonWarp::kPress,
     QString(kImgQss).arg(":/img/meeting_big_mic").arg("#101319")},
    {PushButtonWarp::kSelect,
     QString(kImgQss).arg(":/img/meeting_big_mic_close").arg("none")},
    {PushButtonWarp::kSelect | PushButtonWarp::kHover,
     QString(kImgQss).arg(":/img/meeting_big_mic_close").arg("#272E3B")},
    {PushButtonWarp::kSelect | PushButtonWarp::kPress,
     QString(kImgQss).arg(":/img/meeting_big_mic_close").arg("#101319")}};

static std::unordered_map<int, QString> record_qss_map = {
    {PushButtonWarp::kNormal,
     QString(kImgQss).arg(":/img/meeting_record").arg("none")},
    {PushButtonWarp::kHover,
     QString(kImgQss).arg(":/img/meeting_record").arg("#272E3B")},
    {PushButtonWarp::kPress,
     QString(kImgQss).arg(":/img/meeting_record").arg("#101319")},
    {PushButtonWarp::kSelect,
     QString(kImgQss).arg(":/img/meeting_recording").arg("none")},
    {PushButtonWarp::kSelect | PushButtonWarp::kHover,
     QString(kImgQss).arg(":/img/meeting_recording").arg("#272E3B")},
    {PushButtonWarp::kSelect | PushButtonWarp::kPress,
     QString(kImgQss).arg(":/img/meeting_recording").arg("#101319")}};

static std::unordered_map<int, QString> setting_qss_map = {
    {PushButtonWarp::kNormal,
     QString(kImgQss).arg(":/img/meeting_big_setting").arg("none")},
    {PushButtonWarp::kHover,
     QString(kImgQss).arg(":/img/meeting_big_setting").arg("#272E3B")},
    {PushButtonWarp::kPress,
     QString(kImgQss).arg(":/img/meeting_big_setting").arg("#101319")}};

static std::unordered_map<int, QString> share_qss_map = {
    {PushButtonWarp::kNormal,
     QString(kImgQss).arg(":/img/meeting_share").arg("none")},
    {PushButtonWarp::kHover,
     QString(kImgQss).arg(":/img/meeting_share").arg("#272E3B")},
    {PushButtonWarp::kPress,
     QString(kImgQss).arg(":/img/meeting_share").arg("#101319")},
    {PushButtonWarp::kSelect,
     QString(kImgQss).arg(":/img/meeting_big_sharing").arg("none")},
    {PushButtonWarp::kSelect | PushButtonWarp::kHover,
     QString(kImgQss).arg(":/img/meeting_big_sharing").arg("#272E3B")},
    {PushButtonWarp::kSelect | PushButtonWarp::kPress,
     QString(kImgQss).arg(":/img/meeting_big_sharing").arg("#101319")}};

static std::unordered_map<int, QString> user_list_qss_map = {
    {PushButtonWarp::kNormal,
     QString(kImgQss).arg(":/img/meeting_user_list").arg("none")},
    {PushButtonWarp::kHover,
     QString(kImgQss).arg(":/img/meeting_user_list").arg("#272E3B")},
    {PushButtonWarp::kPress,
     QString(kImgQss).arg(":/img/meeting_user_list").arg("#101319")},
    {PushButtonWarp::kSelect,
     QString(kImgQss).arg(":/img/meeting_user_list_select").arg("none")},
    {PushButtonWarp::kSelect | PushButtonWarp::kHover,
     QString(kImgQss).arg(":/img/meeting_user_list_select").arg("#272E3B")},
    {PushButtonWarp::kSelect | PushButtonWarp::kPress,
     QString(kImgQss).arg(":/img/meeting_user_list_select").arg("#101319")}};

static std::unordered_map<int, QString> hang_up_qss_map = {
    {PushButtonWarp::kNormal,
     QString(kImgQss).arg(":/img/hang_up").arg("none")},
    {PushButtonWarp::kHover,
     QString(kImgQss).arg(":/img/hang_up").arg("#272E3B")},
    {PushButtonWarp::kPress,
     QString(kImgQss).arg(":/img/hang_up").arg("#101319")}};

GlobalButtonBar::GlobalButtonBar(QWidget *parent)
    : QWidget(parent), ui(new Ui::GlobalButtonBar) {
    ui->setupUi(this);
    this->setWindowFlags(Qt::Tool | Qt::WindowStaysOnTopHint |
                        Qt::FramelessWindowHint);
    this->setAttribute(Qt::WA_TranslucentBackground);
    ui->content_widget->setAttribute(Qt::WA_TranslucentBackground);
    helper_set_event(ui->btn_camera, "摄像头");
    helper_set_event(ui->btn_mic, "麦克风");
    helper_set_event(ui->btn_share, "屏幕共享");
    helper_set_event(ui->btn_record, "开启录制");
    helper_set_event(ui->btn_user_list, "参会人列表");
    helper_set_event(ui->btn_setting, "设置");
    helper_set_event(ui->btn_hang, "结束会议");
    ui->btn_camera->setStateMapQss(camera_qss_map);
    ui->btn_hang->setStateMapQss(hang_up_qss_map);
    ui->btn_mic->setStateMapQss(mic_qss_map);
    ui->btn_record->setStateMapQss(record_qss_map);
    ui->btn_setting->setStateMapQss(setting_qss_map);
    ui->btn_share->setStateMapQss(share_qss_map);
    ui->btn_user_list->setStateMapQss(user_list_qss_map);

    connect(ui->btn_share, &QPushButton::clicked, this, [=] {
		if (ui->btn_share->state() == PushButtonWarp::kNormal) {
			ui->btn_share->setState(PushButtonWarp::kSelect);
			emit sigShareButtonClicked(true);
		}
		else {
			auto cur_share_uid =
				meeting::DataMgr::instance().room().screen_shared_uid;
			if (!cur_share_uid.empty() &&
				cur_share_uid != meeting::DataMgr::instance().user_id()) {
				WarningTips::showTips("屏幕共享发起失败，请提示前一位参会者结束共享",
					TipsType::kWarning,
					meeting::PageManager::currentWidget(), 2000);
				return;
			}
			ui->btn_share->setState(PushButtonWarp::kNormal);
			emit sigShareButtonClicked(false);
		}
    });
    connect(ui->btn_user_list, &QPushButton::clicked, this, [=] {
		if (meeting::DataMgr::instance().room().screen_shared_uid ==
			meeting::DataMgr::instance().user_id()) {
			WarningTips::showTips("停止屏幕共享后可查看参会者列表",
				TipsType::kWarning, nullptr, 2000);
			return;
		}
		if (ui->btn_user_list->state() == PushButtonWarp::kNormal) {
			ui->btn_user_list->setState(PushButtonWarp::kSelect);
			emit sigShowUserList(true);
		}
		else {
			ui->btn_user_list->setState(PushButtonWarp::kNormal);
			emit sigShowUserList(false);
		}
    });
    connect(ui->btn_hang, &QPushButton::clicked, this,
            [=] { emit sigEndMeeting(); });

    connect(ui->btn_setting, &QPushButton::clicked, this, [=] {
        if (meeting::DataMgr::instance().room().screen_shared_uid ==
            meeting::DataMgr::instance().user_id()) {
            WarningTips::showTips("停止屏幕共享后可进入会议设置页",
                                TipsType::kWarning, nullptr, 2000);
            return;
        }
        emit sigMeetingSeeting();
    });

    connect(ui->btn_mic, &QPushButton::clicked, this, [=] {
		auto mute = meeting::DataMgr::instance().mute_audio();
		if (mute) {
			if (!MeetingRtcEngineWrap::audioRecordDevicesTest()) {
				WarningTips::showTips("麦克风权限已关闭，请至设备设置页开启", TipsType::kWarning,
					nullptr, 2000);
				return;
			}
		}
		vrd::MeetingSession::instance().toggleMicState(mute, [=](int code) {
			if (code != 200) {
				WarningTips::showTips(QString("麦克风%1失败，请检查设备")
					.arg(!mute ? "打开" : "关闭")
					.arg(code),
					TipsType::kWarning,
					meeting::PageManager::currentWidget(), 2000);
				return;
			}
			MeetingRtcEngineWrap::muteLocalAudio(!mute);
			MeetingRtcEngineWrap::enableLocalAudio(mute);
			meeting::DataMgr::instance().setMuteAudio(!mute);
			ui->btn_mic->setState(mute ? PushButtonWarp::kNormal
				: PushButtonWarp::kSelect);
			emit sigMicEnabled(mute);
			});
    });

    connect(ui->btn_camera, &QPushButton::clicked, this, [=] {
	    auto mute = meeting::DataMgr::instance().mute_video();
	    if (mute) {
		    std::vector<RtcDevice> devices;
		    MeetingRtcEngineWrap::getVideoCaptureDevices(devices);
		    if (devices.empty()) {
			    WarningTips::showTips("摄像头权限已关闭，请至设备设置页开启", TipsType::kWarning,
				    meeting::PageManager::currentWidget(), 2000);
			    return;
		    }
	    }
	    vrd::MeetingSession::instance().toggleCameraState(mute, [=](int code) {
		    if (code != 200) {
			    WarningTips::showTips(QString("摄像头%1失败, 请检查设备")
				    .arg(!mute ? "打开" : "关闭")
				    .arg(code),
				    TipsType::kWarning,
				    meeting::PageManager::currentWidget(), 2000);
			    return;
		    }

		    MeetingRtcEngineWrap::muteLocalVideo(!mute);
		    MeetingRtcEngineWrap::enableLocalVideo(mute);
		    meeting::DataMgr::instance().setMuteVideo(!mute);
		    ui->btn_camera->setState(mute ? PushButtonWarp::kNormal
			    : PushButtonWarp::kSelect);
		    mute ? MeetingRtcEngineWrap::startPreview() : MeetingRtcEngineWrap::stopPreview();
		    emit sigCameraEnabled(mute);
		    });
	    });

    connect(ui->btn_record, &QPushButton::clicked, this, [=] {
        if (ui->btn_record->state() == PushButtonWarp::kNormal) {
            if (meeting::DataMgr::instance().local_role() != MeetingRole::kBroadCast) {
			    WarningTips::showTips(QString("如需录制会议，请提醒主持人开启录制"),
				    TipsType::kWarning,
				    meeting::PageManager::currentWidget(), 2000);
			    return;
            }

            auto users = meeting::DataMgr::instance().users();
            std::vector<std::string> uids(users.size());
            std::transform(users.begin(), users.end(), uids.begin(),
                            [=](const User &u) { return u.user_id; });
            vrd::MeetingSession::instance().startMeetingRecord(
                uids, meeting::DataMgr::instance().room().screen_shared_uid,
                [=](int code) {
                if (code != 200) {
                    WarningTips::showTips(
                        QString("录制失败 err:%1").arg(code), TipsType::kWarning,
                        meeting::PageManager::currentWidget(), 2000);
                    return;
                }
                ui->btn_record->setState(PushButtonWarp::kSelect);
                emit sigRecordEnabled(true);
                });
            WarningTips::showTips(QString("会议录制开始"), TipsType::kWarning,
                                meeting::PageManager::currentWidget(), 2000);
            meeting::DataMgr::instance().setRecordUsers(std::move(uids));
            meeting::DataMgr::instance().setRecordScreen(
                std::move(meeting::DataMgr::instance().room().screen_shared_uid));
        } else {
            // ui->btn_record->setState(PushButtonWarp::kNormal);
            // emit sigRecordEnabled(false);
        }
    });
}

GlobalButtonBar::~GlobalButtonBar() { delete ui; }

void GlobalButtonBar::init() {
  ui->btn_camera->setState(!meeting::DataMgr::instance().mute_video()
                               ? PushButtonWarp::kNormal
                               : PushButtonWarp::kSelect);

  ui->btn_mic->setState(!meeting::DataMgr::instance().mute_audio()
                            ? PushButtonWarp::kNormal
                            : PushButtonWarp::kSelect);
  ui->btn_hang->setState(PushButtonWarp::kNormal);
  ui->btn_record->setState(meeting::DataMgr::instance().room().is_recording
                               ? PushButtonWarp::kSelect
                               : PushButtonWarp::kNormal);
  ui->btn_setting->setState(PushButtonWarp::kNormal);
  ui->btn_share->setState(PushButtonWarp::kNormal);
  ui->btn_user_list->setState(PushButtonWarp::kNormal);
}

void GlobalButtonBar::setForceMic(bool mute) {
  meeting::DataMgr::instance().setMuteAudio(!mute);
  emit ui->btn_mic->clicked();
}

void GlobalButtonBar::setMic(bool mute) {
  meeting::DataMgr::instance().setMuteAudio(mute);
  MeetingRtcEngineWrap::muteLocalAudio(mute);
  MeetingRtcEngineWrap::enableLocalAudio(!mute);
  ui->btn_mic->setState(!mute ? PushButtonWarp::kNormal
                             : PushButtonWarp::kSelect);
}

void GlobalButtonBar::setMoveEnabled(bool enabled) { 
    move_enabled_ = enabled; 
}

void GlobalButtonBar::setForceCamera(bool mute) {
  meeting::DataMgr::instance().setMuteVideo(!mute);
  emit ui->btn_camera->clicked();
}

void GlobalButtonBar::setShareState(PushButtonWarp::PushButtonState state) {
  ui->btn_share->setState(state);
}

void GlobalButtonBar::setUserListState(PushButtonWarp::PushButtonState state) {
  ui->btn_user_list->setState(state);
}

void GlobalButtonBar::setRecordingState(PushButtonWarp::PushButtonState state) {
  ui->btn_record->setState(state);
}

void GlobalButtonBar::setEventFilter(QWidget *w) {
  w->installEventFilter(this);
  listener_ = w;
}

void GlobalButtonBar::unSetEventFilter(QWidget *w) {
  w->removeEventFilter(this);
}

void GlobalButtonBar::mousePressEvent(QMouseEvent *e) {
  if (e->button() == Qt::LeftButton) {
    point_ = e->pos();
  }
}

void GlobalButtonBar::mouseMoveEvent(QMouseEvent *e) {
  QPoint newLeftPos;
  newLeftPos = e->globalPos() - point_;
  if (move_enabled_) this->move(newLeftPos);
}

void GlobalButtonBar::paintEvent(QPaintEvent *e) {
  QPainter p(this);
  p.setRenderHint(QPainter::Antialiasing);
  p.setBrush(QBrush(QColor(0x1d, 0x21, 0x29)));
  p.setPen(Qt::NoPen);
  p.drawRoundedRect(rect(), 30, 30);
}

bool GlobalButtonBar::eventFilter(QObject *watched, QEvent *e) {
  auto w = static_cast<QWidget *>(watched);
  if (listener_ != w) return false;
  switch (e->type()) {
    case QEvent::Show:
      show();
    case QEvent::Resize:
    case QEvent::Move: {
      QPoint p = w->pos() + QPoint((w->frameGeometry().width() - width()) / 2,
                                   w->frameGeometry().height() - height() - 8);
      this->move(p);
    } break;
    case QEvent::Hide:
      hide();
      break;
  }
  return false;
}
