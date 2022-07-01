#ifndef MEETING_SEETING_H
#define MEETING_SEETING_H

#include <QDialog>

#include "list_view_wrap.h"
#include "meeting/core/data_mgr.h"

namespace Ui {
class MeetingSetting;
}

class MeetingSetting : public QDialog {
  Q_OBJECT

 public:
  explicit MeetingSetting(QWidget* parent = nullptr);
  ~MeetingSetting();
  void updateData();

  void initView();

 protected:
  void showEvent(QShowEvent* e) override;
  bool eventFilter(QObject* o, QEvent* e) override;
  void closeEvent(QCloseEvent *e)override;

 public slots:
  void onRecordShow();
  void onHistoryShow();
  void onConfirm();
  void onClose();
  void onCancel();

 private:
  int getFpsCmbIdxFromFps(int fps);
  void initConnect();

 private:
  QDialog::DialogCode code_;
  MeetingSettingModel setting_;
  Ui::MeetingSetting* ui;
  std::shared_ptr<ListViewWrap> history_list_view_;
  std::shared_ptr<ListViewWrap> recored_list_view_;
};

#endif  // MEETING_SEETING_H
