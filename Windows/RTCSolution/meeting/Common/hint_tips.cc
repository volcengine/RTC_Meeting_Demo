#include "hint_tips.h"

#include <QEvent>
#include <QGraphicsDropShadowEffect>
#include <QLabel>
#include <QPainter>

HintTips::HintTips(QWidget* parent) : QWidget(parent) {
  setWindowFlags(Qt::Tool | Qt::FramelessWindowHint | Qt::WindowStaysOnTopHint);
  setAttribute(Qt::WA_TranslucentBackground);
  setFixedSize(120, 50);
  QGraphicsDropShadowEffect* effect = new QGraphicsDropShadowEffect(this);
  effect->setOffset(0, 2);
  effect->setColor(QColor(0, 0, 0, 25));
  effect->setBlurRadius(5);
  this->setGraphicsEffect(effect);
}

HintTips::~HintTips() {}

void HintTips::setHintText(const QString& text) { content_ = text; }

void HintTips::setEventFilter(QWidget* w) {
  w->installEventFilter(this);
  listener_ = w;
}

void HintTips::unSetEventFilter(QWidget* w) {
  w->removeEventFilter(this);
  listener_ = nullptr;
}

bool HintTips::eventFilter(QObject* obj, QEvent* e) {
  if (obj != listener_) return false;
  switch (e->type()) {
    case QEvent::Enter: {
      move(listener_->mapToGlobal(QPoint(0, 0)) -
           QPoint(((width() - listener_->width()) / 2), height() + 10));
      show();
    } break;
    case QEvent::Leave:
      hide();
      break;
    default:
      break;
  }

  return false;
}

void HintTips::paintEvent(QPaintEvent*) {
  QPainter p(this);
  p.setPen(Qt::NoPen);
  p.setRenderHint(QPainter::Antialiasing);
  p.setBrush(QBrush(QColor(0x1d, 0x21, 0x29)));
  auto rc = rect();
  rc.setHeight(rc.height() - 10);
  rc.setWidth(rc.width() - 1);
  rc.setX(1);
  p.drawRoundedRect(rc, 4, 4);
  QPolygon triangle;

  triangle.setPoints(3, (rc.width() - 6) / 2, rc.height(), (rc.width() + 6) / 2,
                     rc.height(), rc.width() / 2, rc.height() + 6);
  p.drawPolygon(triangle);
  p.setFont(QFont("Microsoft YaHei", 12));
  p.setPen(QPen(Qt::white));
  p.drawText(rc, content_, QTextOption(Qt::AlignCenter));
}
