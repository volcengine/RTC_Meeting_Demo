#include "info_view.h"

#include <QPainter>
#include <QVboxLayout>

#include "meeting/Core/data_mgr.h"
InfoView::InfoView(QWidget* parent) : QWidget(parent) {
  this->setWindowFlags(Qt::FramelessWindowHint | Qt::WindowStaysOnTopHint |
                       Qt::Tool);
  setAttribute(Qt::WA_TranslucentBackground);
  ui.setupUi(this);
}

void InfoView::updateData() {
  auto info = meeting::DataMgr::instance().stream_info();
  ui.lbl_local_res->setText(
      QString("%1*%2").arg(info.local_width).arg(info.local_height));
  ui.lbl_local_fps->setText(QString("%1 fps").arg(info.local_fps));
  ui.lbl_local_video_bit->setText(QString("%1 kpbs").arg(info.local_video_bit));
  ui.lbl_local_audio_bit->setText(QString("%1 kpbs").arg(info.local_auido_bit));
  ui.lbl_video_rtt->setText(QString("%1 ms").arg(info.video_rtt));
  ui.lbl_audio_rtt->setText(QString("%1 ms").arg(info.audio_rtt));
  ui.lbl_cpu->setText(QString("%1% | %2%").arg(info.cpu_use).arg(info.cpu_total));
  ui.lbl_remote_video_bit->setText(QString("%1 kbps").arg(info.remote_video_bit));
  ui.lbl_remote_audio_bit->setText(QString("%1 kbps").arg(info.remote_audio_bit));
  ui.lbl_remote_res->setText(QString("%1*%2 - %3*%4")
                                 .arg(info.remote_width_min)
                                 .arg(info.remote_height_min)
                                 .arg(info.remote_width_min)
                                 .arg(info.remote_width_max));
  ui.lbl_remote_fps->setText(QString("%1 - %2 ").arg(info.remote_fps_min).arg(info.remote_fps_max));
  ui.lbl_remote_video_lose->setText(QString("%1%").arg(info.remote_video_lose));
  ui.lbl_remote_audio_lose->setText(QString("%1%").arg(info.remote_audio_lose));
}

void InfoView::paintEvent(QPaintEvent* e) {
  QPainter painter(this);
  painter.setRenderHint(QPainter::Antialiasing);
  painter.setPen(Qt::NoPen);
  painter.setBrush(QBrush(QColor(0x1d, 0x21, 0x29, 255 / 2)));
  painter.drawRoundedRect(rect(), 10, 10);
}
