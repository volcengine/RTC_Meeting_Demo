#ifndef GLOBAL_BUTTON_BAR_H
#define GLOBAL_BUTTON_BAR_H

#include <QWidget>
#include "push_button_warp.h"

namespace Ui {
class GlobalButtonBar;
}

class GlobalButtonBar : public QWidget {
  Q_OBJECT

 public:
  explicit GlobalButtonBar(QWidget *parent = nullptr);
  ~GlobalButtonBar();
  void init();
  void setForceMic(bool mute);
  void setMic(bool mute);
  void setMoveEnabled(bool enabled);
  void setForceCamera(bool mute);
  void setShareState(PushButtonWarp::PushButtonState state);
  void setUserListState(PushButtonWarp::PushButtonState state);
  void setRecordingState(PushButtonWarp::PushButtonState state);
  void setEventFilter(QWidget *w);
  void unSetEventFilter(QWidget *w);

 protected:
  void mousePressEvent(QMouseEvent *e);
  void mouseMoveEvent(QMouseEvent *e);
  void paintEvent(QPaintEvent *) override;
  bool eventFilter(QObject *watched, QEvent *e) override;
 signals:
  void sigShareButtonClicked(bool is_share);
  void sigShowUserList(bool is_active);
  void sigEndMeeting();
  void sigMeetingSeeting();
  void sigMicEnabled(bool is_enabled);
  void sigCameraEnabled(bool is_enabled);
  void sigShareEnabled(bool is_enabled);
  void sigRecordEnabled(bool is_enalbed);

 private:
  Ui::GlobalButtonBar *ui;
  QWidget *listener_ = nullptr;
  bool move_enabled_ = false;
  QPoint point_;
};

#endif  // GLOBAL_BUTTON_BAR_H
