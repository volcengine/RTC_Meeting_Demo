#include "tips_dlg.h"

#include "ui_tips_dlg.h"

TipsDlg::TipsDlg(QWidget *parent) : QDialog(parent), ui(new Ui::TipsDlg) {
  this->setWindowFlags(Qt::FramelessWindowHint);
  ui->setupUi(this);
  ui->lbl_info->setStyleSheet("background:url(:img/meeting/info);background-position:center;"
    "background-repeat:no-repeat;");

  connect(ui->btn_ok, &QPushButton::clicked, this, [=]() { accept();
      });
  connect(ui->btn_cancel, &QPushButton::clicked, this, [=]() { reject();
      });

}

TipsDlg::~TipsDlg() { delete ui; }

void TipsDlg::setText(const QString &text) { ui->lbl_text->setText(text); }
