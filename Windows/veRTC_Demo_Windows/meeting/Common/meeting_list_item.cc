#include "meeting_list_item.h"

#include <iostream>

#include "hint_tips.h"
#include "core/util.h"
#include "meeting/Common/tips_dlg.h"
#include "meeting/Common/warning_tips.h"
#include "meeting/Core/meeting_session.h"
#include "meeting/Core/page_manager.h"
#include "meeting/core/data_mgr.h"
#include "ui_meeting_list_item.h"

static const char *kImgQss =
    "background: none;"
    "background-image:url(:img/%1);"
    "background-position:center;"
    "background-repeat:no-repeat;"
    "background-origin:content;";

MeetingListItem::MeetingListItem(QWidget *parent)
    : QWidget(parent), ui(new Ui::MeetingListItem) {
  setMouseTracking(true);
  ui->setupUi(this);
  ui->role_widget->setStyleSheet("background:none;");
  ui->lbl_role->setStyleSheet("color:#4080FF;"
      "margin-left:2px;"
      "margin-right:2px;"
	"background:#1d2129;"
	"border-radius:2px;");
  tips_ = new HintTips;
  tips_->setHintText("移交主持人");

  connect(ui->btn_camera, &QPushButton::clicked, this, [=] {
    if (meeting::DataMgr::instance().local_role() == MeetingRole::kBroadCast) {
      if (uid_ != meeting::DataMgr::instance().user_id().c_str()) {
        auto dlg = new TipsDlg();
        auto uid = std::string(uid_.toUtf8().constData());
        dlg->setText(QString("是否将支持人移交给: %1").arg(name_));
        if (QDialog::Accepted != dlg->exec()) return;
        vrd::MeetingSession::instance().changeHost(uid, [uid](int code) {
          if (code != 200) {
            return;
          }
        });
      }
    } else {
    }
  });

  connect(ui->btn_mic, &QPushButton::clicked, this, [=] {
    if (meeting::DataMgr::instance().local_role() == MeetingRole::kBroadCast) {
      if (uid_ != meeting::DataMgr::instance().user_id().c_str()) {
        auto uid = std::string(uid_.toUtf8().constData());
        auto name = name_;
        if (mic_state_ == MeetingMicState::kDisalbe) {
          vrd::MeetingSession::instance().askUserMicOn(uid, [=](int code) {
            if (code != 200) {
              WarningTips::showTips(QString("请求打开%1麦克风失败，错误码：%2")
                                        .arg(name)
                                        .arg(code),
                                    TipsType::kWarning,
                                    meeting::PageManager::currentWidget(),
                                    2000);
              return;
            }
          });
        } else {
          vrd::MeetingSession::instance().muteUserMic(uid, [=](int code) {
            if (code != 200) {
              WarningTips::showTips(QString("请求关闭%s麦克风失败，错误码：%d")
                                        .arg(name_)
                                        .arg(code),
                                    TipsType::kWarning,
                                    meeting::PageManager::currentWidget(),
                                    2000);
              return;
            }
            WarningTips::showTips("关闭麦克风已发送", TipsType::kWarning,
                                  meeting::PageManager::currentWidget(), 2000);
          });
        }
      }
    }
  });
}
MeetingListItem::~MeetingListItem() { delete ui; }

void MeetingListItem::setMicState(MeetingMicState state) {
  if (mic_state_ == state) return;
  mic_state_ = state;
  ui->lbl_ico->setStyleSheet("");
  switch (mic_state_) {
    case MeetingMicState::kNormal:
      ui->btn_mic->setStyleSheet(QString(kImgQss).arg("mic_normal_16"));
      break;
    case MeetingMicState::kSpeacking:
      ui->btn_mic->setStyleSheet(QString(kImgQss).arg("mic_speech_16"));
      ui->lbl_ico->setStyleSheet(
          "border:1px solid #4080FF;\n"
          "font-size:14px;\n"
          "	background:#4e5969;\n"
          "	border-radius:14px;\n");
      break;
    case MeetingMicState::kDisalbe:
      ui->btn_mic->setStyleSheet(QString(kImgQss).arg("mic_close_16"));
      break;
    default:
      break;
  }
}

void MeetingListItem::setVideoState(MeetingVideoState state, bool froce) {
  if (video_state_ == state && !froce) return;
  video_state_ = state;
  if (in_ &&
      meeting::DataMgr::instance().local_role() == MeetingRole::kBroadCast &&
      uid_ != meeting::DataMgr::instance().user_id().c_str())
    return;
  switch (video_state_) {
    case MeetingVideoState::kNormal:
      ui->btn_camera->setStyleSheet(QString(kImgQss).arg("video_16"));
      ui->btn_camera->update();
      break;
    case MeetingVideoState::kDisalbe:
      ui->btn_camera->setStyleSheet(QString(kImgQss).arg("video_close_16"));
      ui->btn_camera->update();
      break;
    default:
      break;
  }
}

void MeetingListItem::setName(const QString &str) {
  name_ = str;
  ui->lbl_name->setText(
      util::elideText(ui->lbl_ico->font(), str, 134));
  ui->lbl_ico->setText(name_.left(1).toUpper());
}

void MeetingListItem::setUid(const QString &uid) { uid_ = uid; }

void MeetingListItem::setVolume(int vol) { vol_ = vol; }

void MeetingListItem::setRole(MeetingRole role) {
  if (role_ == role) return;
  role_ = role;
  switch (role) {
    case MeetingRole::kMe:
      ui->lbl_role->setText("我");
      ui->role_widget->show();
      break;
    case MeetingRole::kBroadCast:
      ui->lbl_role->setText("主持人");
      ui->role_widget->show();
      break;
    case MeetingRole::kAudience:
      ui->role_widget->hide();
      break;
    default:
      break;
  }
}

void MeetingListItem::setShare(bool is_share) {
  if (is_share) {
    ui->lbl_share->show();
  } else {
    ui->lbl_share->hide();
  }
}

void MeetingListItem::enterEvent(QEvent *) {
  if (meeting::DataMgr::instance().local_role() == MeetingRole::kBroadCast &&
      uid_ != meeting::DataMgr::instance().user_id().c_str()) {
    ui->btn_camera->setStyleSheet(
        "background:#1d2129;"
        "border-radius:14px;"
        "background-image:url(:img/give_mic);"
        "background-position:center;"
        "background-repeat:no-repeat;");
    tips_->setEventFilter(ui->btn_camera);
    in_ = true;
  }
}

void MeetingListItem::leaveEvent(QEvent *) {
  in_ = false;
  tips_->unSetEventFilter(ui->btn_camera);
  tips_->hide();
  setVideoState(video_state_, true);
}
