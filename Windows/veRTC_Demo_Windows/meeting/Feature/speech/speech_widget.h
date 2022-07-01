#ifndef SPEECH_WIDGET_H
#define SPEECH_WIDGET_H

#include <QWidget>
class QHBoxLayout;
class QSpacerItem;
namespace Ui {
class SpeechWidget;
}

class SpeechWidget : public QWidget {
  Q_OBJECT

 public:
  explicit SpeechWidget(QWidget *parent = nullptr);
  ~SpeechWidget();
  void init();
  void showWidget(int cnt);
  void hideList();
  void showList();
  void wheelEvent(QWheelEvent *) override;

 protected:
  void paintEvent(QPaintEvent *) override;

 private:
  Ui::SpeechWidget *ui;
  int cnt_ = 0;
};

#endif  // SPEECH_WIDGET_H
