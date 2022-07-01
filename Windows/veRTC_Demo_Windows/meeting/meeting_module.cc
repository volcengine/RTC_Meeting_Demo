#include "core/application.h"
#include "core/navigator_interface.h"
#include "meeting_module.h"
#include "feature/data_mgr.h"
#include "meeting/Core/meeting_notify.h"
#include "meeting/Core/meeting_rtc_wrap.h"
#include "meeting/Core/meeting_session.h"
#include "meeting/Core/page_manager.h"
#include "meeting/Core/worker.h"
#include "scene_select_widget.h"

#include<QDebug>

namespace vrd {
void MeetingModule::addThis() {
  Application::getSingleton()
      .getComponent(VRD_UTIL_GET_COMPONENT_PARAM(vrd::INavigator))
      ->add("meeting",
            std::shared_ptr<IModule>((IModule*)(new MeetingModule())));
}

MeetingModule::MeetingModule() {
	meeting::PageManager::init();
	connect(&meeting::PageManager::instance(),
		&meeting::PageManager::sigReturnMainPage, this, [=] {
			MeetingRtcEngineWrap::unInit();
			turnToSceneSelectWidget();
		});
}

void MeetingModule::turnToSceneSelectWidget() {
	VRD_FUNC_GET_COMPONET(vrd::INavigator)->go("scene_select");
	SceneSelectWidget::instance().show();
}

MeetingModule::~MeetingModule() {}

void MeetingModule::open() {
	meeting::Worker::instance();
	meeting::DataMgr::init();
	meeting::DataMgr::instance().setUserName(vrd::DataMgr::instance().user_name());
	meeting::DataMgr::instance().setUserID(vrd::DataMgr::instance().user_id());

	vrd::MeetingSession::instance().initSceneConfig([]() {
		MeetingRtcEngineWrap::init();
		meeting::PageManager::showLogin();
		meeting::PageManager::showTips();
	});
	MeetingNotify::init();
}

void MeetingModule::close() {
	vrd::MeetingSession::instance().exitScene();
}

void MeetingModule::quit(bool) {
	MeetingRtcEngineWrap::unInit();
	turnToSceneSelectWidget();
}

}  // namespace vrd
