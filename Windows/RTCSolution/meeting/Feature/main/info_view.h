#pragma once
#include <QWidget>
#include "ui_info_view.h"
class InfoView : public QWidget {
 public:
  InfoView(QWidget* parent = nullptr);
  ~InfoView() = default;

  void updateData();

 protected:
  void paintEvent(QPaintEvent* e)override;
  private:
  Ui::InfoView ui;
};