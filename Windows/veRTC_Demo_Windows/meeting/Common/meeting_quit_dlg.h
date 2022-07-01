#ifndef MEETING_QUIT_DLG_H
#define MEETING_QUIT_DLG_H

#include <QDialog>

namespace Ui {
class MeetingQuitDlg;
}

class MeetingQuitDlg : public QDialog
{
    Q_OBJECT

public:
    explicit MeetingQuitDlg(QWidget *parent = nullptr);
    ~MeetingQuitDlg();
    void initView();
   private:
    Ui::MeetingQuitDlg *ui;
};

#endif // MEETING_QUIT_DLG_H
