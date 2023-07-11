#include "side_widget.h"

#include <QApplication>
#include <QPainter>
#include <QStyleOption>

#include "meeting/Core/data_mgr.h"
#include "ui_side_widget.h"

SideWidget::SideWidget(QWidget *parent)
    : QWidget(parent), ui(new Ui::SideWidget) {
  ui->setupUi(this);
  ui->content->setLayout(new QHBoxLayout());
  list_ = new SideListWidgetWrap<MeetingListItem>(this);
  ui->content->layout()->addWidget(list_);
  ui->content->setStyleSheet("background:#fff;");
  ui->content->layout()->setContentsMargins(0, 0, 0, 0);
  ui->btn_mute_all->setIcon(QIcon(":img/mute_logo"));
  ui->btn_mute_all->setLayoutDirection(Qt::LeftToRight);
  connect(ui->btn_close, &QPushButton::clicked, this, [=] {
    emit sigHide();
    hide();
  });
  connect(ui->btn_mute_all, &QPushButton::clicked, this,
          [=] { emit sigMuteAll(); });
}

SideWidget::~SideWidget() { delete ui; }

void SideWidget::updateData() {
  auto users = meeting::DataMgr::instance().users();
  if (meeting::DataMgr::instance().local_role() == MeetingRole::kBroadCast) {
    ui->btn_mute_all->show();
  } else {
    ui->btn_mute_all->hide();
  }
  ui->lbl_cnt_person->setText(QString("(%1)").arg(users.size()));
  list_->resetCount(users.size(), [=](MeetingListItem *item, int index) {
    auto &user = users[index];
    item->setVideoState(user.is_camera_on ? MeetingVideoState::kNormal
                                          : MeetingVideoState::kDisalbe);

    if (user.is_mic_on) {
      item->setMicState(user.audio_volume > 5 ? MeetingMicState::kSpeacking
                                              : MeetingMicState::kNormal);
    } else {
      item->setMicState(MeetingMicState::kDisalbe);
    }

    item->setRole(user.is_host ? MeetingRole::kBroadCast
                  : user.user_id == meeting::DataMgr::instance().user_id()
                      ? MeetingRole::kMe
                      : MeetingRole::kAudience);
    item->setShare(user.is_sharing);
    item->setName(user.user_name.c_str());
    item->setUid(user.user_id.c_str());
  });
}

void SideWidget::setEventFilter(QWidget *listener) {
  listener->installEventFilter(this);
  listener_ = listener;
}

bool SideWidget::eventFilter(QObject *obj, QEvent *e) {
  if (listener_ != obj) return false;
  switch (e->type()) {
    case QEvent::Resize:
      setGeometry(listener_->width() - 280, 0, 280, listener_->height());
      break;
    default:
      break;
  }

  return false;
}

void SideWidget::paintEvent(QPaintEvent *e) {
  QStyleOption opt;
  opt.init(this);
  QPainter p(this);
  style()->drawPrimitive(QStyle::PE_Widget, &opt, &p, this);
  QWidget::paintEvent(e);
}
