#ifndef SIDE_WIDGET_H
#define SIDE_WIDGET_H

#include "meeting/Common/side_list_widget_wrap.h"
#include "meeting/Common/meeting_list_item.h"

#include <QWidget>


namespace Ui {
class SideWidget;
}

class SideWidget : public QWidget {
  Q_OBJECT

 public:
  explicit SideWidget(QWidget *parent = nullptr);
  ~SideWidget();

  void updateData();
  void setEventFilter(QWidget *listener);

  bool eventFilter(QObject *obj, QEvent *e) override;
  void paintEvent(QPaintEvent *) override;

 signals:
  void sigHide();
  void sigMuteAll();

 private:
  QWidget *listener_ = nullptr;
  Ui::SideWidget *ui;
  SideListWidgetWrap<MeetingListItem> *list_;
};

#endif  // SIDE_WIDGET_H
