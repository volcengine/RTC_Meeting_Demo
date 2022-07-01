#include "utils.h"
#include <QBitmap>
#include <QPainter>
#include <tuple>

namespace utils {
void setWidgetRoundMask(QWidget* widget, int rx, int ry) {
  QBitmap bmp(QSize(widget->width() - 2, widget->height() - 2));
  bmp.fill();
  QPainter p(&bmp);
  p.setRenderHint(QPainter::Antialiasing);
  p.setPen(Qt::NoPen);
  p.setBrush(Qt::black);
  p.drawRoundedRect(QRect(1, 1, bmp.width() - 1, bmp.height() - 1), rx, ry);
  widget->setMask(bmp);
}

static std::vector<std::tuple<VideoResolution, VideoKbpsHint>> g_video_kbps = {
    {{160, 160}, {40, 150}},    {{320, 180}, {80, 350}},
    {{320, 240}, {100, 400}},   {{640, 360}, {200, 1000}},
    {{480, 480}, {200, 1000}},  {{640, 480}, {250, 1000}},
    {{960, 540}, {400, 1600}},  {{1280, 720}, {500, 2000}},
    {{1920, 1080}, {800, 3000}}};

VideoKbpsHint getVideoKpbsHit(int w, int h) {
  VideoKbpsHint hint;
  for (const auto& iter : g_video_kbps) {
    if (w == std::get<0>(iter).width && h == std::get<0>(iter).height) {
      hint.min_kbps = std::get<1>(iter).min_kbps;
      hint.max_kbps = std::get<1>(iter).max_kbps;
      break;
    }
  }

  return hint;
}

VideoKbpsHint getVideoKpbsHit(const VideoResolution& resolution) {
  return getVideoKpbsHit(resolution.width, resolution.height);
}

int getIdxFromResolution(const VideoResolution& resolution) {
  int idx = -1;
  for (const auto& iter : g_video_kbps) {
    idx++;
    if (resolution.width == std::get<0>(iter).width &&
        resolution.height == std::get<0>(iter).height)
      return idx;
  }
  return idx;
}

}  // namespace utils