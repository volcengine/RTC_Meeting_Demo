#include "ui_scene_select.h"

class SceneSelectWidget : public QWidget {
	Q_OBJECT
 public:
	explicit SceneSelectWidget(QWidget* parent = nullptr);
	~SceneSelectWidget();
	static SceneSelectWidget& instance();
	void resizeEvent(QResizeEvent* e);
	void closeEvent(QCloseEvent*);
	void enableMask(bool enabled);
	QStackedWidget* getMainStackWidget();

 signals:
	void sigLogOut();
	void enterScene(QString sceneName);

public slots:
	void updateData();

 private:
	void initControls();
	void initConnects();
	void setupMeetingSceneButton();
	void setupMoreSceneButton();

 private:
	bool enabled_controls_;
	Ui::SceneSelectWidget ui;
	QWidget* mask_widget_{nullptr};
};