#include "share_screen_widget.h"

#include "core/rtc_engine_wrap.h"
#include "feature/defination.h"
#include "meeting/Common/warning_tips.h"
#include "meeting/Core/meeting_rtc_wrap.h"
#include "meeting/Core/meeting_session.h"
#include "meeting/Core/page_manager.h"
#include "meeting/common/share_view_wnd.h"
#include "ui_share_screen_widget.h"

ShareScreenWidget::ShareScreenWidget(QWidget* parent)
    : QDialog(parent), ui(new Ui::ShareScreenWidget) {
  ui->setupUi(this);
  setWindowFlags(Qt::FramelessWindowHint);
  ui->screen_views->setMinimumWidth(width());
  ui->window_views->setMinimumWidth(width());
  ui->scrollArea->setHorizontalScrollBarPolicy(Qt::ScrollBarAlwaysOff);
  updateData();
  connect(ui->btn_close, &QPushButton::clicked, this, [=] { this->reject(); });
  connect(ui->screen_views, &ShareViewContainer::sigItemPressed, this,
          [=](SnapshotAttr attr) {
            vrd::MeetingSession::instance().startScreenShare([=](int code) {
              if (code != 200) {
                WarningTips::showTips(
                    QString("屏幕共享失败err:%1").arg(code), TipsType::kWarning,
                    meeting::PageManager::currentWidget(), 2000);
                return;
              }
              auto r = meeting::DataMgr::instance().room();
              r.screen_shared_uid = meeting::DataMgr::instance().user_id();
              meeting::DataMgr::instance().setRoom(std::move(r));
              std::vector<void*> excluded;
              MeetingRtcEngineWrap::instance().startScreenCapture(
                  attr.source_id, excluded);
            });
            this->accept();
          });

  connect(ui->window_views, &ShareViewContainer::sigItemPressed, this,
          [=](SnapshotAttr attr) {
            vrd::MeetingSession::instance().startScreenShare([=](int code) {
              if (code != 200) {
                WarningTips::showTips(
                    QString("屏幕共享失败err:%1").arg(code), TipsType::kWarning,
                    meeting::PageManager::currentWidget(), 2000);
                return;
              }
              auto r = meeting::DataMgr::instance().room();
              r.screen_shared_uid = meeting::DataMgr::instance().user_id();
              meeting::DataMgr::instance().setRoom(std::move(r));
              MeetingRtcEngineWrap::instance().startScreenCaptureByWindowId(
                  attr.source_id);
            });
            this->accept();
          });
}

ShareScreenWidget::~ShareScreenWidget() { delete ui; }

void ShareScreenWidget::updateData() {
  std::vector<SnapshotAttr> vec;
  MeetingRtcEngineWrap::getShareList(vec);
  ui->screen_views->clear();
  ui->window_views->clear();
  for (auto& attr : vec) {
    if (attr.type == SnapshotAttr::kScreen) {
      ui->screen_views->addItem(attr,
                                std::move(MeetingRtcEngineWrap::getThumbnail(
                                    attr.type, attr.source_id, 160, 90)));
    } else {
      ui->window_views->addItem(attr,
                                std::move(MeetingRtcEngineWrap::getThumbnail(
                                    attr.type, attr.source_id, 160, 90)));
    }
  }
}
