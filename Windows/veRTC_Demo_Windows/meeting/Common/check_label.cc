#include "check_label.h"

static constexpr char* kNoraml =
    "background:#f2f3f8;"
    "border-radius:2px;"
    "color:#1d2129;";

static constexpr char* kSelected =
    "background:#E8F3FF;"
    "border-radius:2px;"
    "color:#165dFF;";

CheckLabel::CheckLabel(QWidget* parent) : QLabel(parent) {
  setStyleSheet(kNoraml);
}

bool CheckLabel::checked() const { return checked_; }

void CheckLabel::mousePressEvent(QMouseEvent*) {
  checked_ = !checked_;
  if (checked_) {
    setStyleSheet(kSelected);
  } else {
    setStyleSheet(kNoraml);
  }
  emit sigChecked(checked_);
  update();
}
