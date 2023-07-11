#pragma once

#include <QObject>
#include <QProcess>

#include "core/module_interface.h"

namespace vrd {
class MeetingModule : public QObject, public IModule {
	Q_OBJECT
public:
	static void addThis();

private:
	MeetingModule();
	void turnToSceneSelectWidget();

public:
	~MeetingModule();

public:
	void open() override;
	void close() override;

public:
	void quit(bool error = false);
};

}  // namespace vrd
