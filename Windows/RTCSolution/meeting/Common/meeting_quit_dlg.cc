#include "meeting_quit_dlg.h"

#include "meeting/Core/data_mgr.h"
#include "meeting/Core/meeting_session.h"
#include "ui_meeting_quit_dlg.h"

MeetingQuitDlg::MeetingQuitDlg(QWidget *parent)
    : QDialog(parent), ui(new Ui::MeetingQuitDlg) {
  ui->setupUi(this);
  setWindowFlags(Qt::FramelessWindowHint);
  connect(ui->btn_cancel, &QPushButton::clicked, this, [=] { reject(); });
  connect(ui->btn_leave, &QPushButton::clicked, this, [=] {
    vrd::MeetingSession::instance().leaveMeeting([=](int code) { accept(); });
  });
  connect(ui->btn_end, &QPushButton::clicked, this, [=] {
    vrd::MeetingSession::instance().closeMeeting([=](int code) { accept(); });
  });
  initView();
}

MeetingQuitDlg::~MeetingQuitDlg() { delete ui; }

void MeetingQuitDlg::initView() {
  if (meeting::DataMgr::instance().local_role() == MeetingRole::kBroadCast) {
    ui->btn_leave->setEnabled(false);
  } else {
    ui->btn_end->hide();
    ui->btn_leave->setStyleSheet(
        "background:#f53f3f; color:#fff; border-radius:2px;");
    ui->lbl_info->setText("请再次确认是否要离开会议？");
  }
}
