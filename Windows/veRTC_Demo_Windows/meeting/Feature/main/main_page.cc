#include "main_page.h"

#include <QCloseEvent>
#include <QDateTime>
#include <QTimer>
#include <algorithm>
#include <iostream>

#include "meeting/Core/meeting_rtc_wrap.h"
#include "meeting/common/meeting_quit_dlg.h"
#include "meeting/Common/video_widget.h"
#include "meeting/core/page_manager.h"
#include "meeting/feature/normal/normal_widget.h"
#include "meeting/feature/speech/speech_widget.h"
#include "ui_main_page.h"

static constexpr char *kQss =
    "#btn_visible_list{"
    " color : #86909C;"
    "border : none;"
    "background : transparent;"
    "padding-left : 16px;"
    "background-image : url(%1);"
    "background-position : left;"
    "background-repeat : no-repeat;"
    "}";

MainPage::MainPage(QWidget *parent) : QWidget(parent), ui(new Ui::MainPage) {
  ui->setupUi(this);
  resize(QSize(960, 700));
  tick_count_ = 0;
  ui->stackedWidget->addWidget(new NormalWidget(this));
  ui->stackedWidget->addWidget(new SpeechWidget(this));
  ui->stackedWidget->setContentsMargins(0, 0, 0, 0);

  info_view_ = new InfoView;
  main_timer_ = new QTimer(this);
  connect(main_timer_, &QTimer::timeout, this, [=] {
    tick_count_++;
    auto time =
        QString::asprintf("%02lld:%02lld", tick_count_ / 60, tick_count_ % 60);
    ui->lbl_time->setText(time);
  });

  connect(ui->btn_visible_list, &QPushButton::clicked, this, [=]() {
    if (expand_on_) {
      ui->btn_visible_list->setStyleSheet(
          QString(kQss).arg(":img/meeting/down"));
      ui->btn_visible_list->setText("展开列表");
      static_cast<SpeechWidget *>(ui->stackedWidget->widget(current_page_))
          ->hideList();
    } else {
      ui->btn_visible_list->setStyleSheet(QString(kQss).arg(":img/meeting/up"));
      ui->btn_visible_list->setText("隐藏列表");
      static_cast<SpeechWidget *>(ui->stackedWidget->widget(current_page_))
          ->showList();
    }
    expand_on_ = !expand_on_;
  });
}

MainPage::~MainPage() {
  delete info_view_;
  delete ui;
}

void MainPage::updateVideoWidget() {
	auto vec = meeting::DataMgr::instance().users();
	auto streams = meeting::DataMgr::instance().sub_streams();
	std::vector<std::string> tmp;
	for (size_t i = 0; i < std::min(vec.size(), static_cast<size_t>(9u)); i++) {
		if (vec[i].user_id == meeting::DataMgr::instance().user_id()) {
			meeting::PageManager::setLocalVideoWidget(vec[i], i, vec.size());
		}
		else {
			tmp.push_back(vec[i].user_id);
			SubscribeConfig config;
			config.is_screen =
				meeting::DataMgr::instance().record_screen() == vec[i].user_id;
			config.sub_video = true;
			MeetingRtcEngineWrap::subscribeVideoStream(vec[i].user_id, config);
			auto it = std::find_if(streams.begin(), streams.end(),
				[&](const std::string& user_id) {
					return user_id == vec[i].user_id;
				});
			if (it == streams.end()) {
				MeetingRtcEngineWrap::unSubscribeVideoStream(vec[i].user_id, false);
			}
			meeting::PageManager::setRemoteVideoWidget(vec[i], i, vec.size());
		}
	}
	meeting::DataMgr::instance().setSubStreams(std::move(tmp));
	showWidget(vec.size());
}

void MainPage::showWidget(int cnt) {
  if (current_page_ == kNormalPage) {
    static_cast<NormalWidget *>(ui->stackedWidget->widget(current_page_))
        ->showWidget(cnt);
  } else {
    static_cast<SpeechWidget *>(ui->stackedWidget->widget(current_page_))
        ->showWidget(cnt);
  }
}

void MainPage::init() {
    tick_count_ = QDateTime::currentMSecsSinceEpoch() * 1000000 -
        meeting::DataMgr::instance().room().meeting_start_time;
    tick_count_ /= 1000000000;
    ui->lbl_user->setText(meeting::DataMgr::instance().user_name().c_str());
    ui->lbl_user_logo->setText(ui->lbl_user->text().left(1).toUpper());
    ui->lbl_room_id->setText(meeting::DataMgr::instance().room_id().c_str());
    ui->lbl_time->setText("00:00");
    main_timer_->start(1000);
    setRecVisiable(meeting::DataMgr::instance().room().is_recording);
    static_cast<SpeechWidget*>(ui->stackedWidget->widget(kSpeakerPage))->init();
    static_cast<NormalWidget*>(ui->stackedWidget->widget(kNormalPage))->init();
    if (meeting::DataMgr::instance().share_screen()) {
        changeViewMode(MainPage::kSpeakerPage);
    } else {
        changeViewMode(MainPage::kNormalPage);
    }

    froce_close_ = false;
    expand_on_ = true;
    ui->btn_visible_list->setStyleSheet(QString(kQss).arg(":img/meeting/up"));
    ui->btn_visible_list->setText("隐藏列表");

    info_view_->updateData();
}

void MainPage::setListButtonVisiable(bool enabled) {
  if (enabled) {
    ui->btn_visible_list->show();
  } else {
    ui->btn_visible_list->hide();
  }
}

void MainPage::setInfoVisible(bool visiable) {
  if (visiable) {
    if (show_) info_view_->show();
    info_show_ = true;
  } else {
    info_view_->hide();
    info_show_ = false;
  }
}

void MainPage::updateInfo() { info_view_->updateData(); }

void MainPage::changeViewMode(int mode) {
  ui->stackedWidget->setCurrentIndex(mode);
  if (mode == kNormalPage) {
    setListButtonVisiable(false);
  } else {
    setListButtonVisiable(true);
  }

  current_page_ = mode;
  updateVideoWidget();
}

int MainPage::viewMode() { return current_page_; }

void MainPage::froceClose() {
  froce_close_ = true;
  close();
}

void MainPage::hideInfo() { info_view_->hide(); }

void MainPage::setRecVisiable(bool visiable) {
  if (visiable) {
    ui->lbl_rec->show();
    ui->lbl_rec_logo->show();
  } else {
    ui->lbl_rec_logo->hide();
    ui->lbl_rec->hide();
  }
}

void MainPage::resizeEvent(QResizeEvent *) {
  int h = height();
  auto position =
      this->pos() + QPoint(0, frameGeometry().height() - info_view_->height());
  info_view_->move(position);
}

void MainPage::moveEvent(QMoveEvent *) {
  auto position =
      this->pos() + QPoint(0, frameGeometry().height() - info_view_->height());
  info_view_->move(position);
}

void MainPage::showEvent(QShowEvent *) {
  show_ = true;
  if (info_show_) info_view_->show();
}

void MainPage::hideEvent(QHideEvent *) {
  show_ = false;
  info_view_->hide();
}

void MainPage::closeEvent(QCloseEvent *e) {
  if (!froce_close_ &&
      meeting ::PageManager::showCallExpDlg(this) == QDialog::Rejected) {
    e->ignore();
    return;
  }
  main_timer_->stop();
  emit sigClose();
}
