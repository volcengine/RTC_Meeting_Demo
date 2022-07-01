#ifndef FEED_BACK_DLG_H
#define FEED_BACK_DLG_H

#include <QDialog>

namespace Ui {
class FeedBackDlg;
}

class FeedBackDlg : public QDialog
{
    Q_OBJECT

public:
    explicit FeedBackDlg(QWidget *parent = nullptr);
    ~FeedBackDlg();

private:
    Ui::FeedBackDlg *ui;
};

#endif // FEED_BACK_DLG_H
