#pragma once
#include <QCoreApplication>
#include <QEvent>
#include <QThread>

namespace meeting {
class ForwardEvent : public QEvent {
public:
	ForwardEvent(std::function<void(void)>&& task)
		: QEvent(User), task_(std::move(task)) {}
	void execTask() {
		if (task_) task_();
	}
	static void PostEvent(QObject* obj, std::function<void()>&& task) {
		ForwardEvent* event = new ForwardEvent(std::move(task));
		QCoreApplication::postEvent(obj, event);
	}
	std::function<void(void)> task_;
};

class Worker : public QThread {
	Q_OBJECT

public:
	static Worker& instance();
	void customEvent(QEvent* e) override {
		if (e->type() == QEvent::User) {
			auto user_event = static_cast<ForwardEvent*>(e);
			user_event->execTask();
		}
	}

private:
	explicit Worker() {
		moveToThread(this);
		start();
	}
	~Worker() {
		quit();
		wait();
	}
};
}  // namespace meeting
