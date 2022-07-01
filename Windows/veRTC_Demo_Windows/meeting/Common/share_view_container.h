#pragma once
#include <QWidget>

#include "core/common_define.h"
#include "flowlayout.h"
#include "meeting/common/share_view_wnd.h"

class ShareViewContainer : public QWidget {
  Q_OBJECT
 public:
  explicit ShareViewContainer(QWidget* parent = nullptr);
  ~ShareViewContainer();

  void addItem(const SnapshotAttr& item, QPixmap&& map);
  void clear();
 signals:
  void sigItemPressed(SnapshotAttr item);

 private:
  FlowLayout* lay_;
  std::vector<std::pair<ShareViewWnd*, SnapshotAttr>> share_wnds_;
};
