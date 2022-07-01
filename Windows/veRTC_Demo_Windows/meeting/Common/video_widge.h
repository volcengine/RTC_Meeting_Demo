#pragma once
#include <QLabel>
#include <QStackedWidget>
#include <QWidget>

enum class UserRole { kAudience, kMe, kBroadCast };

class VideoWidget : public QWidget {
 public:
  VideoWidget(QWidget* parent = nullptr);
  ~VideoWidget() = default;
  void setUserRole(UserRole role);
  void setUserName(const QString & str);
  void setShare(bool enabled);
  void setHasVideo(bool has_video);
  void* getWinID();
  void setVideoUpdateEnabled(bool enabled);
  void hideVideo();
  void showVideo();
  void setHighLight(bool enabled);
  QPaintEngine* paintEngine() const { return nullptr; }

  UserRole user_role();
  void setUserCount(int cnt,bool fixed_size = false);
 private:
  QStackedWidget* stacked_widget_;
  UserRole role_;
};
