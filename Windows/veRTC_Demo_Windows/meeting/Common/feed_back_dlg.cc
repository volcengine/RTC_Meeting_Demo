#include "feed_back_dlg.h"

#include <QJsonDocument>
#include <QJsonObject>

#include "meeting/Core/meeting_rtc_wrap.h"
#include "meeting/Core/utils.h"
#include "ui_feed_back_dlg.h"

FeedBackDlg::FeedBackDlg(QWidget *parent)
    : QDialog(parent), ui(new Ui::FeedBackDlg) {
  ui->setupUi(this);
  setWindowFlags(Qt::FramelessWindowHint);
  connect(ui->btn_close, &QPushButton::clicked, this, [=] { reject(); });
  connect(ui->btn_cancel, &QPushButton::clicked, this, [=] { reject(); });
  connect(ui->txt_feedback, &QTextEdit::textChanged, this, [=] {
    if (ui->txt_feedback->toPlainText().size() > 500) {
      ui->txt_feedback->setText(ui->txt_feedback->toPlainText().left(500));
      ui->txt_feedback->moveCursor(QTextCursor::End);
    }
  });
  connect(ui->btn_commit, &QPushButton::clicked, this, [=] {
    QString str;
    QJsonObject feed;
    int type = 0;
    if (ui->lbl_video->checked()) {
      str += "[视频卡顿]";
      type |=
          bytertc::ProblemFeedbackOption::kProblemFeedbackOptionVideoLagging |
          bytertc::kProblemFeedbackOptionNoVideo;
    }
    if (ui->lbl_share->checked()) {
      str += "[共享内容故障]";
      type = type | bytertc::kProblemFeedbackOptionNoVideo;
    }
    if (ui->lbl_audio->checked()) {
      str += "[音频卡顿]";
      type = type |
             bytertc::ProblemFeedbackOption::kProblemFeedbackOptionAudioLagging;
    }
    if (ui->lbl_other->checked()) {
      str += "[其他问题]";
      type = type |
             bytertc::ProblemFeedbackOption::kProblemFeedbackOptionOtherMessage;
    }
    str += ui->txt_feedback->toPlainText();
    feed["grade"] = 1;
    feed["type"] = type;
    feed["problem_desc"] = str;
    feed["os_version"] = utils::getOsInfo().c_str();
    feed["network_type"] = "UNKNON";
    MeetingRtcEngineWrap::instance().feedBack(
        QJsonDocument(feed).toJson().constData());
    accept();
  });
}

FeedBackDlg::~FeedBackDlg() { delete ui; }
