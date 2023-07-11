#include "info_dlg.h"
#include "ui_info_dlg.h"

InfoDlg::InfoDlg(QWidget *parent) :
    QDialog(parent),
    ui(new Ui::InfoDlg)
{
    ui->setupUi(this);
}

InfoDlg::~InfoDlg()
{
    delete ui;
}
