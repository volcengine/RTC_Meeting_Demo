#include <QBitmap>
#include <QWidget>
#include "meeting/core/meeting_model.h"

namespace utils {
void setWidgetRoundMask(QWidget* widget, int rx, int ry);

VideoKbpsHint getVideoKpbsHit(int w, int h);
VideoKbpsHint getVideoKpbsHit(const VideoResolution& resolution);
int getIdxFromResolution(const VideoResolution& resolution);

}  // namespace utils