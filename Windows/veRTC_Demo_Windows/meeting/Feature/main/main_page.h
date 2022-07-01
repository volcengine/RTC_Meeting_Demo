#ifndef MAIN_PAGE_H
#define MAIN_PAGE_H

#include <QWidget>
#include "info_view.h"

namespace Ui {
	class MainPage;
}

class MainPage : public QWidget {
	Q_OBJECT
public:
	enum { kNormalPage, kSpeakerPage };
	explicit MainPage(QWidget* parent = nullptr);
	~MainPage();

	void updateVideoWidget();
	void showWidget(int cnt);
	void init();
	void setListButtonVisiable(bool enabled);
	void setRecVisiable(bool visiable);
	void setInfoVisible(bool visiable);
	void updateInfo();
	void changeViewMode(int mode);
	 int viewMode();
	void froceClose();
	void hideInfo();

signals:
	void sigClose();
protected:
	void resizeEvent(QResizeEvent*) override;
	void moveEvent(QMoveEvent*) override;
	void showEvent(QShowEvent*) override;
	void hideEvent(QHideEvent*) override;
	void closeEvent(QCloseEvent*) override;

private:
	Ui::MainPage* ui;
	InfoView* info_view_ = nullptr;
	int current_page_ = kNormalPage;
	QTimer* main_timer_;
	int64_t tick_count_;
	bool froce_close_ = false;
	bool show_ = false;
	bool info_show_ = false;
	bool expand_on_ = true;
};

#endif  // MAIN_PAGE_H
