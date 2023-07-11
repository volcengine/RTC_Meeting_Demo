#ifndef CONTROLLER_WIDGET_H
#define CONTROLLER_WIDGET_H

#include <QWidget>

#include "error_tips.h"
namespace Ui {
class LoginControllerWidget;
}

class LoginControllerWidget : public QWidget {
	Q_OBJECT

public:
	explicit LoginControllerWidget(QWidget* parent = nullptr);
	~LoginControllerWidget();
	void initData();
	void setUserName(const QString& name);

public:
	void setMicEnable(bool enabled);
	void setCameraEnable(bool enabled);

signals:
	void sigShowSettingPage();
	void sigJoinRomm();
	void sigAudioClicked();
	void sigVideoClicked();

protected:
	bool eventFilter(QObject* obj, QEvent* e) override;
	void showEvent(QShowEvent*) override;
	void paintEvent(QPaintEvent*) override;

private:
	void setUserEditError(bool error);
	void setRoomEditError(bool error);
	void validateUserId(QString str);
	void validateRoomId(QString str);

private:
	bool userIdErrorFlag{ false };
	bool roomIdErrorFlag{ false };
	ErrorTips* tips_user_;
	ErrorTips* tips_room_;
	QWidget* listen_;
	Ui::LoginControllerWidget* ui;
};

#endif  // CONTROLLER_WIDGET_H
