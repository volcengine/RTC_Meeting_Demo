#include "speech_widget.h"

#include <QPainter>
#include <QScrollBar>
#include <QStyleOption>
#include <QTimer>
#include <QWheelEvent>

#include "meeting/core/page_manager.h"
#include "ui_speech_widget.h"
SpeechWidget::SpeechWidget(QWidget *parent)
    : QWidget(parent), ui(new Ui::SpeechWidget) {
  ui->setupUi(this);
  ui->video_list_container->setStyleSheet(
      "background:transparent; border:none;");
  ui->scrollArea->setHorizontalScrollBarPolicy(Qt::ScrollBarAlwaysOff);
  ui->scrollArea->setVerticalScrollBarPolicy(Qt::ScrollBarAlwaysOff);
  ui->video_list->setLayout(new QHBoxLayout);
  ui->video_list->layout()->setContentsMargins(0, 0, 0, 0);
  ui->video_list->layout()->setSpacing(16);
  ui->big_view->setLayout(new QHBoxLayout);
  ui->big_view->layout()->setContentsMargins(0, 0, 0, 0);
  ui->big_view->layout()->setSpacing(0);
}

SpeechWidget::~SpeechWidget() { delete ui; }

void SpeechWidget::init() {
  auto list = meeting::PageManager::getVideoList();
  auto lay = ui->video_list->layout();
  for (int i = 0; i < 9; i++) {
    lay->removeWidget(list[i].get());
    list[i]->hide();
  }
  cnt_ = 0;
}

void SpeechWidget::showWidget(int cnt) {
  cnt = std::min(8, cnt);
  int need_width = 160 * cnt + (cnt + 1) * 16;
  if (width() <= need_width) {
    ui->video_list->resize(need_width, height());
  }
  auto list = meeting::PageManager::getVideoList();
  auto lay = ui->video_list->layout();
  for (int i = cnt; i < cnt_; i++) {
    list[i]->hide();
  }

  ui->big_view->layout()->addWidget(
      meeting::PageManager::getScreenVideo().get());
  for (int i = 0; i < cnt; i++) {
    lay->addWidget(list[i].get());
    list[i]->setMaximumSize(160, 92);
    list[i]->setMinimumSize(160, 92);
    list[i]->show();
  }
  cnt_ = cnt;
  ui->video_list->layout()->setAlignment(Qt::AlignCenter);
}

void SpeechWidget::hideList() { ui->video_list_container->hide(); }

void SpeechWidget::showList() { ui->video_list_container->show(); }

void SpeechWidget::wheelEvent(QWheelEvent *e) {
  int numberDegrees = e->angleDelta().y() / 8;
  int numberSteps = numberDegrees;
  ui->scrollArea->horizontalScrollBar()->setValue(
      ui->scrollArea->horizontalScrollBar()->value() - numberSteps);
  e->accept();
}

void SpeechWidget::paintEvent(QPaintEvent *e) {
  QStyleOption opt;
  opt.init(this);
  QPainter p(this);
  style()->drawPrimitive(QStyle::PE_Widget, &opt, &p, this);
  QWidget::paintEvent(e);
}
