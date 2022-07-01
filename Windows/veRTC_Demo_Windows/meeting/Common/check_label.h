#pragma once
#include <QLabel>

class CheckLabel :public QLabel {
Q_OBJECT

 public:
  explicit CheckLabel(QWidget* parent = nullptr);
  ~CheckLabel() = default;
  
  bool checked() const;
  protected:
  void mousePressEvent(QMouseEvent*) override;

signals:
  void sigChecked(bool is_check);

private:
  bool checked_ = false;
};