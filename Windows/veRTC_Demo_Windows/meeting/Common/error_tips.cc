#include "error_tips.h"
#include <QGraphicsDropShadowEffect>
#include <QLabel>
#include <QPainter>

static const char* kMainQss = "#lbl_title{"
    "font-family : 'Microsoft YaHei';"
    "background : transparent;"
    "font-size : 12px;"
    "color : #F53F3F; "
    "}"

    "#lbl_data{"
    "font-family : 'Microsoft YaHei';"
    "background : transparent;"
    "font-size : 12px;"
    "color : #1D2129; "
    "}";

ErrorTips::ErrorTips(QWidget* parent) : QWidget(parent) {
	layout_ = new QVBoxLayout(this);
	setWindowFlags(Qt::Tool | Qt::FramelessWindowHint);
	setAttribute(Qt::WA_TranslucentBackground);
	setLayout(layout_);
	setFixedSize(240, 140);
	this->setStyleSheet(kMainQss);
	auto lbl_title = new QLabel("非法输入，输入规则如下：", this);
	lbl_title->setObjectName("lbl_title");
	layout_->addWidget(lbl_title);
	lbl_data = new QLabel(
		"1. 26个大写字母 A ~ Z\n"
		"2. 26个小写字母 a ~ z\n"
		"3. 10个数字 0 ~ 9\n"
		"4. 下划线\"_\", at符\"@\", 减号\"-\", 点\".\"\n",
		this);
	lbl_data->setObjectName("lbl_data");
	layout_->addWidget(lbl_data);
	setContentsMargins(10, 10, 0, 0);
	QGraphicsDropShadowEffect* effect = new QGraphicsDropShadowEffect(this);
	effect->setOffset(0, 2);
	effect->setColor(QColor(0, 0, 0, 25));
	effect->setBlurRadius(5);
	this->setGraphicsEffect(effect);
}

ErrorTips::~ErrorTips() {}

void ErrorTips::setInfoDataText(const QString& text) {
    if (lbl_data) {
        lbl_data->setText(text);
    }
}

void ErrorTips::paintEvent(QPaintEvent*) {
	QPainter p(this);
	p.setPen(Qt::NoPen);
	p.setRenderHint(QPainter::Antialiasing);
	p.setBrush(QBrush(QColor(0xff, 0xff, 0xff)));
	auto rc = rect();
	rc.setHeight(rc.height() - 10);
	rc.setWidth(rc.width() - 3);
	rc.setX(3);
	p.drawRoundedRect(rc, 4, 4);
	QPolygon triangle;
	QPoint p0((rc.width() - 6) / 2, rc.height());
	QPoint p1((rc.width() + 6) / 2, rc.height());
	QPoint p2(rc.width() / 2, rc.height() + 6);

	triangle.setPoints(3, (rc.width() - 6) / 2, rc.height(), (rc.width() + 6) / 2,
		rc.height(), rc.width() / 2, rc.height() + 6);
	p.drawPolygon(triangle);
}
