#include "call_exp_dlg.h"

#include "ui_call_exp_dlg.h"

CallExpDlg::CallExpDlg(QWidget *parent)
    : QDialog(parent), ui(new Ui::CallExpDlg) {
  ui->setupUi(this);
  this->setWindowFlags(Qt::FramelessWindowHint);
  connect(ui->btn_close, &QPushButton::clicked, this, [=] { reject(); });
  connect(ui->btn_up, &QPushButton::clicked, this, [=] { reject(); });
  connect(ui->btn_down, &QPushButton::clicked, this, [=] { accept(); });
}

CallExpDlg::~CallExpDlg() { delete ui; }
