#ifndef TIPS_DLG_H
#define TIPS_DLG_H

#include <QDialog>

namespace Ui {
class TipsDlg;
}

class TipsDlg : public QDialog
{
    Q_OBJECT

public:
    explicit TipsDlg(QWidget *parent = nullptr);
    ~TipsDlg();
    void setText(const QString & text);

private:
    Ui::TipsDlg *ui;
};

#endif // TIPS_DLG_H
