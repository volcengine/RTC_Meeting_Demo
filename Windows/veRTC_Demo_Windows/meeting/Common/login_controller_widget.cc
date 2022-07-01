#include "login_controller_widget.h"

#include <QBitmap>
#include <QPainter>
#include <QStyleOption>

#include "Meeting/Common/warning_tips.h"
#include "meeting/Core/data_mgr.h"
#include "ui_login_controller_widget.h"
#include "utils.h"

static const char* kEdtError =
    "background: #FFECE8;"
    "background-image:url(:img/error);"
    "background-position:right;"
    "background-repeat:no-repeat;"
    "background-origin:content;";

static const char* kButtonQss =
    "#%1{"
    "background: #F2F3F5;"
    "border-radius: 16px;"
    "background-image:url(%2);"
    "background-position:center;"
    "background-repeat:no-repeat;"
    "}"

    "#%1:hover{"
    "	background:#E5E6EB;"
    "	background-image:url(%2);"
    "	background-position:center;"
    "	background-repeat:no-repeat;"
    "}"

    "#%1:pressed{"
    "	background:#C9CDD4;"
    "	background-image:url(%2);"
    "	background-position:center;"
    "	background-repeat:no-repeat;"
    "}";

LoginControllerWidget::LoginControllerWidget(QWidget* parent)
    : QWidget(parent), ui(new Ui::LoginControllerWidget) {
	ui->setupUi(this);
	setWindowFlags(Qt::FramelessWindowHint);
	listen_ = parent;
	parent->installEventFilter(this);
	tips_user_ = new ErrorTips(this);
	auto text =
		"1. 26个大写字母 A ~ Z\n"
		"2. 26个小写字母 a ~ z\n"
		"3. 10个数字 0 ~ 9\n"
		"4. 中文\n";
	tips_user_->setInfoDataText(text);
	tips_room_ = new ErrorTips(this);
	utils::setWidgetRoundMask(this, 40, 60);
	initData();
	connect(ui->edt_room_id, &QLineEdit::textChanged, this,
		&LoginControllerWidget::validateRoomId);
	connect(ui->edt_user_id, &QLineEdit::textChanged, this,
		&LoginControllerWidget::validateUserId);
	connect(ui->btn_setting, &QPushButton::clicked, this,
		[=] { emit sigShowSettingPage(); });
	connect(ui->btn_join_room, &QPushButton::clicked, this, [=] {
		meeting::DataMgr::instance().setUserName(ui->edt_user_id->text().toUtf8().constData());
		meeting::DataMgr::instance().setRoomID(ui->edt_room_id->text().toUtf8().constData());
		emit sigJoinRomm();
		});
	connect(ui->btn_mic, &QPushButton::clicked, this,
		[=] { emit sigAudioClicked(); });
	connect(ui->btn_camera, &QPushButton::clicked, this,
		[=] { emit sigVideoClicked(); });
}

LoginControllerWidget::~LoginControllerWidget() { delete ui; }

void LoginControllerWidget::setMicEnable(bool enabled) {
	if (enabled)
		ui->btn_mic->setStyleSheet(
			QString(kButtonQss).arg("btn_mic").arg(":/img/meeting_mic"));
	else
		ui->btn_mic->setStyleSheet(
			QString(kButtonQss).arg("btn_mic").arg(":/img/meeting_mic_close"));
}

void LoginControllerWidget::setCameraEnable(bool enabled) {
	if (enabled)
		ui->btn_camera->setStyleSheet(
			QString(kButtonQss).arg("btn_camera").arg(":/img/meeting_camera"));
	else
		ui->btn_camera->setStyleSheet(QString(kButtonQss)
			.arg("btn_camera")
			.arg(":/img/meeting_camera_close"));
}

bool LoginControllerWidget::eventFilter(QObject* obj, QEvent* e) {
	if (listen_ == obj &&
		(e->type() == QEvent::Move || e->type() == QEvent::Resize)) {
		tips_user_->move(mapToGlobal(ui->edt_user_id->pos()) -
			QPoint(0, tips_user_->height()));
		tips_room_->move(mapToGlobal(ui->edt_room_id->pos()) -
			QPoint(0, tips_room_->height()));
	}
	return false;
}

void LoginControllerWidget::showEvent(QShowEvent*) {
	tips_user_->move(mapToGlobal(ui->edt_user_id->pos()) -
		QPoint(0, tips_user_->height()));
	tips_room_->move(mapToGlobal(ui->edt_room_id->pos()) -
		QPoint(0, tips_room_->height()));
}

void LoginControllerWidget::paintEvent(QPaintEvent* e) {
	QStyleOption opt;
	opt.init(this);
	QPainter p(this);
	style()->drawPrimitive(QStyle::PE_Widget, &opt, &p, this);
	QWidget::paintEvent(e);
}

void LoginControllerWidget::setUserEditError(bool error) {
	if (error) {
		ui->edt_user_id->setStyleSheet(kEdtError);
		tips_user_->show();
	}
	else {
		ui->edt_user_id->setStyleSheet("");
		tips_user_->hide();
	}
}

void LoginControllerWidget::setRoomEditError(bool error) {
	if (error) {
		ui->edt_room_id->setStyleSheet(kEdtError);
		tips_room_->show();
	}
	else {
		ui->edt_room_id->setStyleSheet("");
		tips_room_->hide();
	}
}

void LoginControllerWidget::validateUserId(QString str) {
	if (str.size() > 18) {
		ui->edt_user_id->setText(str.left(18));
		WarningTips::showTips("输入长度超过18位字符。", TipsType::kWarning,
			static_cast<QWidget*>(parent()), 2000);
	}
	auto tmp = ui->edt_user_id->text();
	userIdErrorFlag = false;
	for (auto& ch : tmp) {
		ushort uNum = ch.unicode();
		if (!(ch.isDigit() || ch.isUpper() || ch.isLower() || (uNum >= 0x4E00 && uNum <= 0x9FA5))) {
			userIdErrorFlag = true;
			break;
		}
	}
	setUserEditError(userIdErrorFlag);
	if (!userIdErrorFlag && !roomIdErrorFlag && !ui->edt_room_id->text().isEmpty() &&
		!ui->edt_user_id->text().isEmpty()) {
		ui->btn_join_room->setEnabled(true);
	}
	else {
		ui->btn_join_room->setEnabled(false);
	}
}

void LoginControllerWidget::validateRoomId(QString str) {
	if (str.size() > 18) {
		ui->edt_room_id->setText(str.left(18));
		WarningTips::showTips("输入长度超过18位字符。", TipsType::kWarning,
			static_cast<QWidget*>(parent()), 2000);
	}
	auto tmp = ui->edt_room_id->text();
	roomIdErrorFlag = false;
	for (auto& ch : tmp) {
		if (!(ch.isDigit() || ch.isUpper() || ch.isLower() || ch == '_' ||
			ch == '-' || ch == '@' || ch == '.')) {
			roomIdErrorFlag = true;
			break;
		}
	}
	setRoomEditError(roomIdErrorFlag);
	if (!roomIdErrorFlag && !userIdErrorFlag && !ui->edt_room_id->text().isEmpty() &&
		!ui->edt_user_id->text().isEmpty()) {
		ui->btn_join_room->setEnabled(true);
	}
	else {
		ui->btn_join_room->setEnabled(false);
	}
}

void LoginControllerWidget::initData() {
  setMicEnable(true);
  setCameraEnable(true);
  ui->btn_join_room->setEnabled(false);
  ui->edt_room_id->clear();
  ui->edt_user_id->clear();
}

void LoginControllerWidget::setUserName(const QString& name) {
  ui->edt_user_id->setText(name);
}
