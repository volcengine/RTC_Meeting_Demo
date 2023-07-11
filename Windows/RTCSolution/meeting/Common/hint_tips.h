#ifndef HINT_TIPS_H
#define HINT_TIPS_H

#include <QVBoxLayout>
#include <QWidget>
class QLabel;

class HintTips : public QWidget {
  Q_OBJECT

 public:
  explicit HintTips(QWidget* parent = nullptr);
  ~HintTips();
  void setHintText(const QString& text);
  void setEventFilter(QWidget* w);
  void unSetEventFilter(QWidget*w);

  bool eventFilter(QObject* obj,QEvent* e)override;

 protected:
  void paintEvent(QPaintEvent*) override;

 private:
  QWidget* listener_ = nullptr;
  QString content_;
};

#endif  // ERROR_TIPS_H
