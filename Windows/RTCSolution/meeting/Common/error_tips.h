#ifndef ERROR_TIPS_H
#define ERROR_TIPS_H

#include <QVBoxLayout>
#include <QWidget>
#include <QLabel>

class ErrorTips : public QWidget {
	Q_OBJECT

public:
	explicit ErrorTips(QWidget* parent = nullptr);
	~ErrorTips();
	void setInfoDataText(const QString& text);

protected:
	void paintEvent(QPaintEvent*) override;

private:
	QVBoxLayout* layout_;
	QLabel* lbl_data;
};

#endif  // ERROR_TIPS_H
