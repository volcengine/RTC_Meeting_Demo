#include "normal_widget.h"

#include <QPainter>
#include <QStyleOption>
#include <QTimer>

#include "meeting/core/page_manager.h"
#include "ui_normal_widget.h"

NormalWidget::NormalWidget(QWidget *parent)
    : QWidget(parent), ui(new Ui::NormalWidget) {
  ui->setupUi(this);
  auto lay = new QGridLayout(this);
  setLayout(lay);
}

NormalWidget::~NormalWidget() { 
    delete ui; 
}

void NormalWidget::updateData() {}

void NormalWidget::showWidget(int cnt) {
  cnt = std::min(cnt,9);
  auto lay = static_cast<QGridLayout *>(layout());
  auto list = meeting::PageManager::getVideoList();
  for (auto w : list) {
    if (w) {
      w->setMaximumSize(9999, 9999);
    }
  }
  for (int i = cnt; i < cnt_; i++) {
    list[i]->hide();
    list[i]->setUpdatesEnabled(false);
    lay->removeWidget(list[i].get());
  }
  lay->setContentsMargins(24, 0, 24, 60);
  lay->setHorizontalSpacing(24);
  lay->setVerticalSpacing(24);
  switch (cnt) {
    case 1:
      lay->setHorizontalSpacing(0);
      lay->setVerticalSpacing(0);
      lay->setContentsMargins(0, 0, 0, 0);
      lay->addWidget(list[0].get(), 0, 0);
      list[0]->show();
      break;
    case 2:
      lay->setContentsMargins(24, 150, 24, 200);
      lay->addWidget(list[0].get(), 0, 0);
      lay->addWidget(list[1].get(), 0, 1);
      list[0]->show();
      list[1]->show();
      break;
    case 3:
    case 4:
      for (int i = 0; i < cnt; i++) {
        lay->addWidget(list[i].get(), i / 2, i % 2);
        list[i]->show();
      }
      break;
    case 5:
    case 6:
    case 7:
    case 8:
    case 9:
    default:
      for (int i = 0; i < cnt; i++) {
        lay->addWidget(list[i].get(), i / 3, i % 3);
        list[i]->show();
      }
      break;
  }
  for (auto w : list) {
    if (w) {
      w->setUpdatesEnabled(true);
    }
  }
  cnt_ = cnt;
}

void NormalWidget::init() {
  auto list = meeting::PageManager::getVideoList();
  auto lay = static_cast<QGridLayout *>(layout());
  for (int i = 0; i < 9; i++) {
    lay->removeWidget(list[i].get());
    list[i]->hide();
  }
  cnt_ = 0;
}

void NormalWidget::paintEvent(QPaintEvent *e) {
  QStyleOption opt;
  opt.init(this);
  QPainter p(this);
  style()->drawPrimitive(QStyle::PE_Widget, &opt, &p, this);
  QWidget::paintEvent(e);
}
