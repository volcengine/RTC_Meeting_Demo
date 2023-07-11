#pragma once
#include <QHBoxLayout>
#include <QWidget>

#include "label_warp.h"
class QLabel;
class QPushButton;
enum class TipsType { kError, kWarning };

class WarningTips : public QWidget {
  Q_OBJECT

 public:
  explicit WarningTips(TipsType type, QWidget* parent = nullptr);
  ~WarningTips() = default;

  void showTips(int timeout);
  static void showTips(const QString& text, TipsType type, QWidget* parent,
                       int timeout);
  void setText(const QString& text);

 protected:
  void paintEvent(QPaintEvent*) override;
  void closeEvent(QCloseEvent*) override;

 private:
  QHBoxLayout* layout_;
  QLabel* lbl_icon_;
  QLabel* lbl_text_;
  LabelWarp* btn_close_;
};
