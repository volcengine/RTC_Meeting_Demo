#ifndef SHARE_SCREEN_WIDGET_H
#define SHARE_SCREEN_WIDGET_H

#include <QDialog>

namespace Ui {
class ShareScreenWidget;
}

class ShareScreenWidget : public QDialog
{
    Q_OBJECT

public:
    explicit ShareScreenWidget(QWidget *parent = nullptr);
    ~ShareScreenWidget();

    void updateData();
    bool canStartSharing();

private:
    Ui::ShareScreenWidget *ui;
};

#endif // SHARE_SCREEN_WIDGET_H
