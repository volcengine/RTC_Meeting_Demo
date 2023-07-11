#ifndef NORMAL_WIDGET_H
#define NORMAL_WIDGET_H

#include <meeting/core/meeting_model.h>

#include <QWidget>
namespace Ui {
class NormalWidget;
}

class NormalWidget : public QWidget {
  Q_OBJECT

 public:
  explicit NormalWidget(QWidget *parent = nullptr);
  ~NormalWidget();

  void updateData();
  void showWidget(int cnt);
  void init();
 protected:
  void paintEvent(QPaintEvent *);

 private:
  Ui::NormalWidget *ui;
  int cnt_ = 0;
};

#endif  // NORMAL_WIDGET_H
