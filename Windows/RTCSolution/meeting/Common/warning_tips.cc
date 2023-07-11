#include "warning_tips.h"

#include <QCloseEvent>
#include <QGuiApplication>
#include <QLabel>
#include <QPainter>
#include <QPushButton>
#include <QScreen>
#include <QTimer>

WarningTips::WarningTips(TipsType type, QWidget* parent) : QWidget(parent) {
	setWindowFlags(Qt::FramelessWindowHint);
	layout_ = new QHBoxLayout(this);
	setLayout(layout_);
	lbl_icon_ = new QLabel(this);
	lbl_icon_->setFixedSize(20, 20);
	if (type == TipsType::kError) {
		lbl_icon_->setStyleSheet(
			"background:transparent; border-image: url(:/img/error);");
	}
	else {
		lbl_icon_->setStyleSheet(
			"background:transparent; border-image: url(:/img/warning);");
	}
	layout_->addWidget(lbl_icon_);
	lbl_text_ = new QLabel(this);
	lbl_text_->setStyleSheet(
		"font-family:'Microsoft YaHei'; font-size: 12px; color:black; "
		"margin-bottom: 1px;background:transparent;");
	layout_->addWidget(lbl_text_);
	btn_close_ = new LabelWarp(this);
	btn_close_->setStyleSheet(
		"background:transparent;border-image: url(:/img/meeting_close); "
		"background-position:center;background-repeat:no-repeat;");
	layout_->addWidget(btn_close_);
	btn_close_->setMinimumSize(12, 10);
	connect(btn_close_, &LabelWarp::sigPressed, this, &QWidget::close);
}

void WarningTips::showTips(int timeout) {
	QTimer::singleShot(timeout, this, [=] {
		close();
		deleteLater();
		});
}

void WarningTips::showTips(const QString& text, TipsType type, QWidget* parent,
                           int timeout) {
	auto toast = new WarningTips(type, parent);
	toast->setWindowFlags(toast->windowFlags());
	toast->setText(text);
	toast->adjustSize();
	if (parent) {
		toast->move(QPoint((parent->width() - toast->width()) / 2, 60));
	}
	else {
		QScreen* screen = QGuiApplication::primaryScreen();
		toast->move((screen->size().width() - toast->width()) / 2,
			screen->size().height() / 5);
	}
	toast->show();
	toast->showTips(timeout);
}

void WarningTips::setText(const QString& text) { lbl_text_->setText(text); }

void WarningTips::paintEvent(QPaintEvent*) {
	QPainter painter(this);
	painter.setPen(Qt::NoPen);
	painter.setRenderHint(QPainter::RenderHint::Antialiasing);
	painter.setBrush(QBrush(QColor(0xff, 0xff, 0xff)));
	painter.drawRoundedRect(rect(), 6, 8);
}

void WarningTips::closeEvent(QCloseEvent* e) {
	e->ignore();
	hide();
}
