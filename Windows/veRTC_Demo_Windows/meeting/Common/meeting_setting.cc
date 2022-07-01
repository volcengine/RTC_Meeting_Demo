#include "meeting_setting.h"

#include <QCloseEvent>
#include <QDateTime>
#include <QDesktopServices>
#include <QItemDelegate>
#include <QListView>
#include <QPainter>
#include <QTimer>

#include "meeting/Core/meeting_rtc_wrap.h"
#include "meeting/Core/meeting_session.h"
#include "ui_meeting_setting.h"
#include "utils.h"

static constexpr char* kQss =
    "QListView{"
    "background:#fff;"
    "border:1px solid #E5E6EB;"
    "border-radius : 4px;"
    "padding:5px;"
    "color:#1D2129"
    "}"
    "QListView::item{ "
    "color:#1D2129;"
    " height : 32px;"
    "}"
    "QListView::item:hover{"
    "color:#1D2129;"
    "background:#F2F3F8;"
    "}"
    "QListView::item:selected{"
    "color:#1D2129;"
    "background:#F2F3F8;"
    "}";

Q_DECLARE_METATYPE(VideoResolution)

MeetingSetting::MeetingSetting(QWidget* parent)
    : QDialog(parent), ui(new Ui::MeetingSetting) {
  history_list_view_ = std::make_shared<ListViewWrap>();
  recored_list_view_ = std::make_shared<ListViewWrap>();
  history_list_view_->setStyleSheet(kQss);
  recored_list_view_->setStyleSheet(kQss);
  history_list_view_->hide();
  recored_list_view_->hide();
  recored_list_view_->setButtonMode(ListViewWrap::Mode::HasButton);
  ui->setupUi(this);
  setWindowFlags(Qt::FramelessWindowHint | Qt::Dialog);
  auto set_combobox = [](QComboBox* cmb) {
    auto list_view = new QListView(cmb);
    list_view->setStyleSheet(kQss);
    cmb->setView(list_view);
    cmb->view()->window()->setWindowFlags(Qt::Popup | Qt::FramelessWindowHint |
                                          Qt::NoDropShadowWindowHint);
    cmb->view()->window()->setAttribute(Qt::WA_TranslucentBackground);
  };
  set_combobox(ui->cmb_camera);
  set_combobox(ui->cmb_fps);
  set_combobox(ui->cmb_mic);
  set_combobox(ui->cmb_resolution);
  set_combobox(ui->cmb_screen_fps);
  set_combobox(ui->cmb_screen_resolution);

  auto set_resoultion_data = [](QComboBox* cmb) {
    std::vector<VideoResolution> resolutions{
        {160, 160}, {320, 180}, {320, 240},  {640, 360},  {480, 480},
        {640, 480}, {960, 540}, {1280, 720}, {1920, 1080}};
    for (auto item : resolutions) {
      QVariant var;
      var.setValue(item);
      cmb->addItem(QString("%1*%2").arg(item.width).arg(item.height), var);
    }
  };

  auto set_fps_data = [](QComboBox* cmb) {
    std::vector<int> vec_fps{15, 20, 24};
    for (auto item : vec_fps) {
      QVariant var;
      var.setValue(item);
      cmb->addItem(QString("%1 fps").arg(item).arg(item), var);
    }
  };

  set_resoultion_data(ui->cmb_resolution);
  set_resoultion_data(ui->cmb_screen_resolution);
  set_fps_data(ui->cmb_fps);
  set_fps_data(ui->cmb_screen_fps);
  initConnect();
}

void MeetingSetting::onRecordShow() {
  QPoint p = ui->btn_record->mapToGlobal(ui->btn_record->rect().bottomLeft());
  recored_list_view_->move(p);
  recored_list_view_->show();
  recored_list_view_->setFocus();
}

void MeetingSetting::onHistoryShow() {
  QPoint p = ui->btn_history->mapToGlobal(ui->btn_history->rect().bottomLeft());
  history_list_view_->move(p);
  history_list_view_->show();
  history_list_view_->setFocus();
}

void MeetingSetting::onConfirm() {
  setting_.camera_idx = ui->cmb_camera->currentIndex();
  setting_.mic_idx = ui->cmb_mic->currentIndex();
  meeting::DataMgr::instance().setSetting(setting_);
  code_ = QDialog::Accepted;
  close();
}

void MeetingSetting::onClose() {
  code_ = QDialog::Rejected;
  close();
}

void MeetingSetting::onCancel() {
  code_ = QDialog::Rejected;
  close();
}

int MeetingSetting::getFpsCmbIdxFromFps(int fps) {
  static std::map<int, int> fps_map{{15, 0}, {20, 1}, {24, 2}};
  auto iter = fps_map.find(fps);
  if (iter != fps_map.end()) return fps_map[fps];
  return 0;
}

void MeetingSetting::initConnect() {
  connect(ui->sldr_kbps, &SliderWrap::sigPositonChange, this, [=](qreal real) {
    ui->edt_kbps->setText(QString("%1").arg(int(real)));
    setting_.camera.kbps = int(real);
  });
  connect(ui->sldr_screen_kbps, &SliderWrap::sigPositonChange, this,
          [=](qreal real) {
            ui->edt_screen_kbps->setText(QString("%1").arg(int(real)));
            setting_.screen.kbps = int(real);
          });
  connect(
      ui->cmb_resolution, QOverload<int>::of(&QComboBox::currentIndexChanged),
      this, [=](int idx) {
        qreal v =
            qreal(ui->sldr_kbps->getCurValue() - ui->sldr_kbps->getMinValue()) /
            (ui->sldr_kbps->getMaxValue() - ui->sldr_kbps->getMinValue());
        setting_.camera.resolution =
            ui->cmb_resolution->currentData().value<VideoResolution>();
        setting_.camera.kbpsRange =
            utils::getVideoKpbsHit(setting_.camera.resolution);

        ui->sldr_kbps->setMinValue(setting_.camera.kbpsRange.min_kbps);
        ui->sldr_kbps->setMaxValue(setting_.camera.kbpsRange.max_kbps);
        ui->sldr_kbps->setValue(
            int(v * (qlonglong(setting_.camera.kbpsRange.max_kbps) -
                     setting_.camera.kbpsRange.min_kbps) +
                setting_.camera.kbpsRange.min_kbps));
      });

  connect(
      ui->cmb_screen_resolution,
      QOverload<int>::of(&QComboBox::currentIndexChanged), this, [=](int idx) {
        qreal v = qreal(ui->sldr_screen_kbps->getCurValue() -
                        ui->sldr_screen_kbps->getMinValue()) /
                  (ui->sldr_screen_kbps->getMaxValue() -
                   ui->sldr_screen_kbps->getMinValue());
        setting_.screen.resolution =
            ui->cmb_screen_resolution->currentData().value<VideoResolution>();
        setting_.screen.kbpsRange =
            utils::getVideoKpbsHit(setting_.screen.resolution);
        ui->sldr_screen_kbps->setMinValue(setting_.screen.kbpsRange.min_kbps);
        ui->sldr_screen_kbps->setMaxValue(setting_.screen.kbpsRange.max_kbps);
        ui->sldr_screen_kbps->setValue(
            int(v * (qlonglong(setting_.screen.kbpsRange.max_kbps) -
                     setting_.screen.kbpsRange.min_kbps) +
                setting_.screen.kbpsRange.min_kbps));
      });

  connect(ui->cmb_fps, QOverload<int>::of(&QComboBox::currentIndexChanged),
          this, [=](int idx) {
            setting_.camera.fps = ui->cmb_fps->currentData().toInt();
          });

  connect(ui->cmb_screen_fps,
          QOverload<int>::of(&QComboBox::currentIndexChanged), this,
          [=](int idx) {
            setting_.screen.fps = ui->cmb_screen_fps->currentData().toInt();
          });

  connect(ui->btn_enable_param, &CheckButton::sigChecked, this,
          [=](bool checked) { setting_.enable_show_info = checked; });

  connect(
      recored_list_view_.get(), &ListViewWrap::sigItemClicked, this,
      [=](HistoryItemData data) { QDesktopServices::openUrl(QUrl(data.url)); });

  connect(
      history_list_view_.get(), &ListViewWrap::sigItemClicked, this,
      [=](HistoryItemData data) { QDesktopServices::openUrl(QUrl(data.url)); });

  connect(recored_list_view_.get(), &ListViewWrap::sigButtonClicked, this,
          [=](HistoryItemData data) {
            vrd::MeetingSession::instance().deleteVideoRecord(
                data.vid.toUtf8().constData(),
                [=, shared_this = recored_list_view_](int code) {
                  if (code != 200) {
                    return;
                  }
                  shared_this->removeItem(data.vid);
                });
          });

  QObject::connect(&MeetingRtcEngineWrap::instance(),
      &MeetingRtcEngineWrap::sigUpdateDevices, this, [this](){
		  if (isVisible()) {
              updateData();
		  }
      });
}

void MeetingSetting::initView() {
  setting_ = meeting::DataMgr::instance().setting();
  ui->cmb_fps->setCurrentIndex(getFpsCmbIdxFromFps(setting_.camera.fps));
  ui->sldr_kbps->setMinValue(setting_.camera.kbpsRange.min_kbps);
  ui->sldr_kbps->setMaxValue(setting_.camera.kbpsRange.max_kbps);
  ui->sldr_kbps->setValue(setting_.camera.kbps);
  ui->sldr_screen_kbps->setMinValue(setting_.screen.kbpsRange.min_kbps);
  ui->sldr_screen_kbps->setMaxValue(setting_.screen.kbpsRange.max_kbps);
  ui->sldr_screen_kbps->setValue(setting_.screen.kbps);
  ui->cmb_resolution->setCurrentIndex(
      utils::getIdxFromResolution(setting_.camera.resolution));
  ui->cmb_screen_resolution->setCurrentIndex(
      utils::getIdxFromResolution(setting_.screen.resolution));
  ui->btn_enable_param->setChecked(setting_.enable_show_info);
  updateData();
}

MeetingSetting::~MeetingSetting() { delete ui; }

void MeetingSetting::updateData() {
    ui->cmb_camera->clear();
    ui->cmb_mic->clear();
    std::vector<RtcDevice> audio_input_devices;
    std::vector<RtcDevice> camera_devices;
    MeetingRtcEngineWrap::getAudioInputDevices(audio_input_devices);
    MeetingRtcEngineWrap::getVideoCaptureDevices(camera_devices);
    int idx = 0;
    for (auto& dc : camera_devices) {
		ui->cmb_camera->addItem(dc.name.c_str());
		ui->cmb_camera->setItemData(idx, QVariant::fromValue(idx),
			Qt::UserRole + 1);
		idx++;
    }

    if (camera_devices.empty()) {
        setting_.camera_idx = -1;
    } else {
	    ui->cmb_camera->blockSignals(true);
	    setting_.camera_idx = camera_devices.size() - 1;
	    ui->cmb_camera->setCurrentIndex(setting_.camera_idx);
	    ui->cmb_camera->blockSignals(false);
    }

    if (setting_.camera_idx == -1) {
	    ui->cmb_camera->setCurrentIndex(0);
	    setting_.camera_idx = 0;
    }

    for (auto& dc : audio_input_devices) {
		ui->cmb_mic->addItem(dc.name.c_str());
		ui->cmb_mic->setItemData(idx, QVariant::fromValue(idx), Qt::UserRole + 1);
		idx++;
    }

    if (audio_input_devices.empty()) {
        setting_.mic_idx = -1;
    } else {
	    ui->cmb_mic->blockSignals(true);
        setting_.mic_idx = audio_input_devices.size() - 1;
	    ui->cmb_mic->setCurrentIndex(setting_.mic_idx);
	    ui->cmb_mic->blockSignals(false);
    }
	
    if (setting_.mic_idx == -1) {
		ui->cmb_mic->setCurrentIndex(0);
		setting_.mic_idx = 0;
	}

    vrd::MeetingSession::instance().getHistoryMeetingRecord(
        [=, shared_list = recored_list_view_,
        shared_history = history_list_view_](int code) {
        shared_list->clear();
        shared_history->clear();
        if (code != 200) {
            return;
        }
        auto records = meeting::DataMgr::instance().record_infos();
        for (const auto& record : records) {
            if (record.video_holder) {
            shared_list->addItem(QDateTime::fromMSecsSinceEpoch(
                                        record.meeting_start_time / 1000000)
                                        .toString("yyyy/MM/dd hh:mm:ss"),
                                    record.url.c_str(), record.vid.c_str());
            } else {
            shared_history->addItem(QDateTime::fromMSecsSinceEpoch(
                                        record.meeting_start_time / 1000000)
                                        .toString("yyyy/MM/dd hh:mm:ss"),
                                    record.url.c_str(), record.vid.c_str());
            }
        }
        });
}

void MeetingSetting::showEvent(QShowEvent* e) {}

bool MeetingSetting::eventFilter(QObject* o, QEvent* e) { return false; }

void MeetingSetting::closeEvent(QCloseEvent* e) {
  e->ignore();
  if (code_ == QDialog::Accepted) {
    this->accept();
  } else {
    reject();
  }
}
