#ifndef INFO_DLG_H
#define INFO_DLG_H

#include <QDialog>

namespace Ui {
class InfoDlg;
}

class InfoDlg : public QDialog
{
    Q_OBJECT

public:
    explicit InfoDlg(QWidget *parent = nullptr);
    ~InfoDlg();

private:
    Ui::InfoDlg *ui;
};

#endif // INFO_DLG_H
