#pragma once
#include "Meeting/Common/login_controller_widget.h"
#include "ui_meeting_login.h"
#include "Core/Component/label_warp.h"

class MeetingLoginWidget : public QWidget {
  Q_OBJECT
 public:
  explicit MeetingLoginWidget(QWidget* parent = nullptr);
  void setName(const QString & name);
  void startTest();
  void setMic(bool on);
  void setCamera(bool on);

signals:
  void sigClose();

 protected:
  void showEvent(QShowEvent*)override;
  void resizeEvent(QResizeEvent*) override;
  void closeEvent(QCloseEvent*)override;

  private:
  void stopTest();
 private:
  bool login_ = false;
  LoginControllerWidget* login_controller_widget_;
  LabelWarp * btn_back;
  Ui::MeetingLoginWidget ui;
};
