#ifndef MEETING_LIST_ITEM_H
#define MEETING_LIST_ITEM_H

#include <QString>
#include <QWidget>

#include "meeting/core/meeting_model.h"
class HintTips;
namespace Ui {
class MeetingListItem;
}

enum class MeetingMicState { kUnknown, kNormal, kSpeacking, kDisalbe };
enum class MeetingVideoState { kUnknown, kNormal, kDisalbe };

class MeetingListItem : public QWidget {
  Q_OBJECT

 public:
  explicit MeetingListItem(QWidget *parent = nullptr);
  ~MeetingListItem();
  void setMicState(MeetingMicState state);
  void setVideoState(MeetingVideoState state, bool froce = false);
  void setName(const QString &str);
  void setUid(const QString &uid);
  void setVolume(int vol);
  void setRole(MeetingRole role);
  void setShare(bool is_share);

 protected:
  void enterEvent(QEvent *) override;
  void leaveEvent(QEvent *) override;

 signals:

 private:
  bool in_ = false;
  int vol_;
  MeetingMicState mic_state_ = MeetingMicState::kUnknown;
  MeetingVideoState video_state_ = MeetingVideoState::kUnknown;
  MeetingRole role_;
  QString name_;
  QString uid_;
  Ui::MeetingListItem *ui;
  HintTips *tips_;
};

#endif  // MEETING_LIST_ITEM_H
