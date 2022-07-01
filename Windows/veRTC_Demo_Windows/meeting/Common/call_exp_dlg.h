#ifndef CALL_EXP_DLG_H
#define CALL_EXP_DLG_H

#include <QDialog>

namespace Ui {
class CallExpDlg;
}

class CallExpDlg : public QDialog
{
    Q_OBJECT

public:
    explicit CallExpDlg(QWidget *parent = nullptr);
    ~CallExpDlg();

private:
    Ui::CallExpDlg *ui;
};

#endif // CALL_EXP_DLG_H
