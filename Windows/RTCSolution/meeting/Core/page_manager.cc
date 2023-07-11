#include "page_manager.h"

#include <QMessageBox>
#include <QTimer>
#include <chrono>
#include <iostream>
#include <type_traits>
#include <QDebug>
#include <QJsonObject>
#include <QJsonDocument>
#include <QJsonArray>

#include "meeting/Common/tips_dlg.h"
#include "meeting/Common/warning_tips.h"
#include "meeting/Core/meeting_notify.h"
#include "meeting/Core/meeting_rtc_wrap.h"
#include "meeting/Core/meeting_session.h"
#include "meeting/Core/worker.h"
#include "meeting/common/meeting_quit_dlg.h"

namespace meeting {
PageManager& PageManager::instance() {
  static PageManager mgr;
  return mgr;
}

void PageManager::init() {
	instance().button_bar_ =
		std::unique_ptr<GlobalButtonBar>(new GlobalButtonBar);
	instance().meeting_login_widget_ =
		std::unique_ptr<MeetingLoginWidget>(new MeetingLoginWidget);

	QObject::connect(instance().meeting_login_widget_.get(),
		&MeetingLoginWidget::sigClose,
		[=] { emit instance().sigReturnMainPage(); });

    QObject::connect(&MeetingRtcEngineWrap::instance(),
                    &MeetingRtcEngineWrap::sigUpdateAudio, []() {
                        meeting::DataMgr::instance().ref_cache_users().push_back(
                            [on = !meeting::DataMgr::instance().mute_audio()](
                                std::vector<User>& users) {
                            auto iter = std::find_if(
                                users.begin(), users.end(),
                                [](const User& user) {
                                    return user.user_id ==
                                        meeting::DataMgr::instance().user_id();
                                });
                            if (iter == users.end()) {
                                return;
                            }
                            iter->is_mic_on = on;
                            });

                        instance().button_bar_->setForceMic(
                            meeting::DataMgr::instance().mute_audio());

                        instance().meeting_login_widget_->setMic(
                            meeting::DataMgr::instance().mute_audio());
                    });

    QObject::connect(&MeetingRtcEngineWrap::instance(),
                    &MeetingRtcEngineWrap::sigUpdateVideo, []() {
                        meeting::DataMgr::instance().ref_cache_users().push_back(
                            [on = !meeting::DataMgr::instance().mute_video()](
                                std::vector<User>& users) {
                            auto iter = std::find_if(
                                users.begin(), users.end(),
                                [](const User& user) {
                                    return user.user_id ==
                                        meeting::DataMgr::instance().user_id();
                                });
                            if (iter == users.end()) {
                                return;
                            }
                            iter->is_camera_on = on;
                            });
                        instance().button_bar_->setForceCamera(
                            meeting::DataMgr::instance().mute_video());
                        instance().meeting_login_widget_->setCamera(
                            meeting::DataMgr::instance().mute_video());
                    });

	QObject::connect(
		&MeetingRtcEngineWrap::instance(),
		&MeetingRtcEngineWrap::sigOnAudioVolumeUpdate, [=]() {
			auto speackers = meeting::DataMgr::instance().volumes();
			auto users = meeting::DataMgr::instance().users();
			for (auto& speacker : speackers) {
				auto iter = std::find_if(users.begin(), users.end(),
					[speacker](const User& user) {
						return user.user_id == speacker.uid;
					});
				if (iter != users.end()) {
					iter->audio_volume = speacker.volume;
				}
			}
			if (speackers.size() > 0 && speackers[0].volume > 5)
				DataMgr::instance().setHighLight(speackers[0].uid);
			else
				DataMgr::instance().setHighLight("");
            handleCacheUsers();
		});

	QObject::connect(&MeetingRtcEngineWrap::instance(),
		&MeetingRtcEngineWrap::sigOnRoomStateChanged,
        [=](std::string room_id, std::string uid, int state, std::string extra_info) {
			if (room_id == meeting::DataMgr::instance().room().room_id
				&& uid == meeting::DataMgr::instance().user_id()) {
				auto infoArray = QByteArray(extra_info.data(), static_cast<int>(extra_info.size()));
				auto infoJsonObj = QJsonDocument::fromJson(infoArray).object();
				auto joinType = infoJsonObj["join_type"].toInt();
				if (state == 0 && joinType == 1) {
					vrd::MeetingSession::instance().userReconnect([=](int code) {
						if (code == 422 || code == 419 || code == 404) {
							instance().main_page_->froceClose();
							hideUserListWidget();
						}
					});
				}
			}
        });

    QObject::connect(&MeetingRtcEngineWrap::instance(),
        &MeetingRtcEngineWrap::sigForceExitRoom,
        [=]() {
            instance().main_page_->froceClose();
            instance().meeting_login_widget_->close();
            hideUserListWidget();
        });

    instance().main_page_ = std::unique_ptr<MainPage>(new MainPage);
	QObject::connect(instance().main_page_.get(), &MainPage::sigClose, [=] {
		MeetingNotify::instance().offAll();
		meeting::PageManager::instance().button_bar_->hide();
		if (meeting::DataMgr::instance().room().screen_shared_uid ==
			meeting::DataMgr::instance().user_id()) {
			meeting::DataMgr::instance().setShareScreen(false);
			MeetingRtcEngineWrap::instance().stopScreenCapture();
		}
		meeting::DataMgr::instance().setRecord(false);
		meeting::DataMgr::instance().setUsers(std::vector<User>());
		auto& cache = meeting::DataMgr::instance().ref_cache_users();
		cache.clear();
		MeetingRtcEngineWrap::instance().logout();
		showLogin();
		});

	QObject::connect(&MeetingRtcEngineWrap::instance(),
		&MeetingRtcEngineWrap::sigUpdateInfo,
		[=] { instance().main_page_->updateInfo(); });

	instance().screen_widget_ = std::make_shared<VideoWidget>();
	instance().videos_.resize(9);
	for (int i = 0; i < kMaxShowWidgetNum; i++) {
		instance().videos_[i] = std::make_shared<VideoWidget>();
	}
	instance().user_list_ = std::unique_ptr<SideWidget>(new SideWidget);

	instance().user_list_->setEventFilter(instance().main_page_.get());

	QObject::connect(instance().user_list_.get(), &SideWidget::sigMuteAll, [] {
		vrd::MeetingSession::instance().muteAllUserMic([](int code) {
			if (code != 200) {
				WarningTips::showTips(QString("全体静音err:%1")
					.arg(code),
					TipsType::kError, currentWidget(), 2000);

				return;
			}
			WarningTips::showTips(QString("全体静音已发送"), TipsType::kWarning,
				currentWidget(), 2000);
			auto& cache = meeting::DataMgr::instance().ref_cache_users();
			cache.push_back([=](std::vector<User>& users) {
				for (auto iter = users.begin(); iter != users.end(); iter++)
					if (!iter->is_host) iter->is_mic_on = false;
				});
			});
		});

	instance().button_bar_->connect(instance().button_bar_.get(),
		&GlobalButtonBar::sigShareButtonClicked,
		[=](bool is_share) {
			if (is_share) {
				showShareWidget();
			}
			else {
				stopScreen();
			}
		});

	instance().button_bar_->connect(instance().button_bar_.get(),
		&GlobalButtonBar::sigShowUserList,
		[=](bool is_active) {
			if (is_active)
				showUserListWidget();
			else
				hideUserListWidget();
		});

	instance().button_bar_->connect(
		instance().button_bar_.get(), &GlobalButtonBar::sigRecordEnabled,
		[=](bool is_active) {
			instance().main_page_->setRecVisiable(is_active);
		});

	instance().button_bar_->connect(
		instance().button_bar_.get(), &GlobalButtonBar::sigCameraEnabled,
		[=](bool is_enabled) { getCurrentVideo()->setHasVideo(is_enabled); });

	instance().button_bar_->connect(instance().button_bar_.get(),
		&GlobalButtonBar::sigEndMeeting,
		[=] {
			instance().main_page_->close();
            hideUserListWidget();
		});

	instance().button_bar_->connect(instance().button_bar_.get(),
		&GlobalButtonBar::sigMeetingSeeting,
		[=] { showSetting(); });

	instance().user_list_->connect(
		instance().user_list_.get(), &SideWidget::sigHide, [=] {
			instance().button_bar_->setUserListState(PushButtonWarp::kNormal);
		});
}

void PageManager::showLogin(QWidget* parent) {
	instance().meeting_login_widget_->show();
	instance().current_widget_ = instance().meeting_login_widget_.get();
	if (!meeting::DataMgr::instance().mute_video()) {
		instance().meeting_login_widget_->startTest();
	}
}

void PageManager::showTips(QWidget* parent) {
    QTimer::singleShot(100, []() {
        QMessageBox::information(nullptr, "meeting tips",
            "本产品仅用于功能体验，单次会议时长不超过二十分钟",
            QMessageBox::Ok);
        });
}

void PageManager::showSetting(QWidget* parent) {
    auto dlg = std::unique_ptr<MeetingSetting>(new MeetingSetting(parent));
    dlg->initView();
    if (dlg->exec() == QDialog::Accepted) {
        auto setting = meeting::DataMgr::instance().setting();
        MeetingRtcEngineWrap::setAudioInputDevice(setting.mic_idx);
        MeetingRtcEngineWrap::setVideoCaptureDevice(setting.camera_idx);
        MeetingRtcEngineWrap::setVideoProfiles(setting.camera);
        MeetingRtcEngineWrap::setScreenProfiles(setting.screen);
        instance().main_page_->setInfoVisible(setting.enable_show_info);
    }
}

void PageManager::showShareWidget(QWidget* parent) {
    auto dlg = std::unique_ptr<ShareScreenWidget>(new ShareScreenWidget(parent));
    if (dlg->exec() == QDialog::Accepted) {
        meeting::DataMgr::instance().setShareScreen(true);
        unSetGlobalBarEvent(instance().main_page_.get());
        hideRoom();
        instance().button_bar_->setMoveEnabled(true);
    } else {
        if (!meeting::DataMgr::instance().share_screen()) {
            instance().button_bar_->setShareState(PushButtonWarp::kNormal);
        }
    }
}

void PageManager::showUserListWidget(QWidget* parent) {
	instance().user_list_->setParent(instance().main_page_.get());
	instance().user_list_->show();
}

void PageManager::hideUserListWidget() { 
    instance().user_list_->hide(); 
}

int PageManager::showCallExpDlg(QWidget* parent) {
    auto quit_dlg = std::unique_ptr<MeetingQuitDlg>(new MeetingQuitDlg());
    if (parent) {
        quit_dlg->move(QPoint(
            parent->pos().x() + (parent->width() - quit_dlg->width()) / 2,
            parent->pos().y() + ((parent->height() - quit_dlg->height()) / 2)));
    }
    return quit_dlg->exec();
}

void PageManager::setLocalVideoWidget(const User& user, int idx, int cnt) {
	MeetingRtcEngineWrap::setupLocalView(
		meeting::PageManager::getVideoList()[idx]->getWinID(),
		bytertc::RenderMode::kRenderModeHidden, "local");

	auto isLocalCameraOn = !meeting::DataMgr::instance().mute_video();
	if (isLocalCameraOn) {
		MeetingRtcEngineWrap::startPreview();
	}

	meeting::PageManager::getVideoList()[idx]->setUserName(
		user.user_name.c_str());
	meeting::PageManager::getVideoList()[idx]->setUserCount(
		cnt, instance().main_page_->viewMode() == MainPage::kSpeakerPage);
	meeting::PageManager::getVideoList()[idx]->setUserRole(
		user.is_host ? UserRole::kBroadCast : UserRole::kMe);
	meeting::PageManager::getVideoList()[idx]->setHighLight(meeting::DataMgr::instance().high_light() == user.user_id);

	meeting::PageManager::getVideoList()[idx]->setShare(user.is_sharing);
	meeting::PageManager::getVideoList()[idx]->setHasVideo(isLocalCameraOn);
	meeting::PageManager::getVideoList()[idx]->setVideoUpdateEnabled(!isLocalCameraOn);
}

void PageManager::setRemoteVideoWidget(const User& user, int idx, int cnt) {
	MeetingRtcEngineWrap::setupRemoteView(
		meeting::PageManager::getVideoList()[idx]->getWinID(),
		bytertc::RenderMode::kRenderModeHidden, user.user_id);
	meeting::PageManager::getVideoList()[idx]->setUserName(
		user.user_name.c_str());
	meeting::PageManager::getVideoList()[idx]->setUserCount(
		cnt, instance().main_page_->viewMode() == MainPage::kSpeakerPage);
	meeting::PageManager::getVideoList()[idx]->setUserRole(
		user.is_host ? UserRole::kBroadCast : UserRole::kAudience);
	meeting::PageManager::getVideoList()[idx]->setShare(user.is_sharing);
	meeting::PageManager::getVideoList()[idx]->setHasVideo(user.is_camera_on);
	if (meeting::DataMgr::instance().high_light() == user.user_id) {
		meeting::PageManager::getVideoList()[idx]->setHighLight(true);
	}
	else {
		meeting::PageManager::getVideoList()[idx]->setHighLight(false);
	}

	if (user.is_camera_on) {
		meeting::PageManager::getVideoList()[idx]->setVideoUpdateEnabled(false);
	}
	else {
		meeting::PageManager::getVideoList()[idx]->setVideoUpdateEnabled(true);
	}
}

void PageManager::setRemoteScrrenVideoWidget(const User& user) {
	auto& ins = instance();
	auto video = getScreenVideo();
	SubscribeConfig config;
	config.is_screen = true;
	config.sub_video = true;
	MeetingRtcEngineWrap::subscribeVideoStream(user.user_id, config, 1);
	MeetingRtcEngineWrap::setRemoteScreenView(user.user_id, video->getWinID());
	video->setUserCount(0, true);
	video->setUserName(user.user_name.c_str());
	video->setUserRole(user.is_host ? UserRole::kBroadCast : UserRole::kAudience);
	video->setShare(user.is_sharing);
	video->setHasVideo(user.is_sharing);
	if (user.is_sharing) {
		video->setVideoUpdateEnabled(false);
	}
	else {
		video->setVideoUpdateEnabled(true);
	}
}

void PageManager::initRoom() {
    meeting::DataMgr::instance().setConnect(true);
    meetingNotify();
    instance().button_bar_->init();
    instance().main_page_->init();
    showRoom();
    updateData();
}

void PageManager::showRoom() {
	setGlobalBarEvent(instance().main_page_.get());
	instance().main_page_->show();
	instance().current_widget_ = instance().main_page_.get();
	if (!meeting::DataMgr::instance().room().screen_shared_uid.empty() &&
		meeting::DataMgr::instance().share_screen()) {
		auto users = meeting::DataMgr::instance().users();
		auto iter = std::find_if(users.begin(), users.end(), [](const User& user) {
			return user.user_id ==
				meeting::DataMgr::instance().room().screen_shared_uid;
			});
		if (iter != users.end()) {
			setRemoteScrrenVideoWidget(*iter);
			instance().button_bar_->setShareState(PushButtonWarp::kSelect);
		}
	}
}

QWidget* PageManager::currentWidget() { return instance().current_widget_; }

void PageManager::showGlobalBar() { instance().button_bar_->show(); }

void PageManager::hideGlobalBar() { instance().button_bar_->hide(); }

void PageManager::hideRoom() { instance().main_page_->hide(); }

void PageManager::setGlobalBarEvent(QWidget* listener) {
    instance().button_bar_->setEventFilter(listener);
}

void PageManager::unSetGlobalBarEvent(QWidget* listener) {
    instance().button_bar_->unSetEventFilter(listener);
}

std::vector<std::shared_ptr<VideoWidget>> PageManager::getVideoList() {
  return instance().videos_;
}

std::shared_ptr<VideoWidget> PageManager::getCurrentVideo() {
  int i = 0;
  for (auto& user : meeting::DataMgr::instance().users()) {
    if (user.user_id == meeting::DataMgr::instance().user_id()) {
      return instance().videos_[i];
    }
    i++;
  }
  return std::shared_ptr<VideoWidget>();
}

std::shared_ptr<VideoWidget> PageManager::getScreenVideo() {
  return instance().screen_widget_;
}

void PageManager::updateData() {
  if (instance().updating) return;
  instance().updating = true;
  instance().main_page_->updateVideoWidget();
  instance().user_list_->updateData();
  if (meeting::DataMgr::instance().local_role() == MeetingRole::kBroadCast &&
      meeting::DataMgr::instance().record()) {
    auto old_usrs = meeting::DataMgr::instance().record_users();
    auto old_screen_id = meeting::DataMgr::instance().record_screen();
    auto users = meeting::DataMgr::instance().users();
    bool update = false;
    if (old_screen_id ==
        meeting::DataMgr::instance().room().screen_shared_uid) {
      if (old_usrs.size() != users.size()) {
        update = true;
      } else {
        for (auto& uid : old_usrs) {
          auto it = std::find_if(
              users.begin(), users.end(),
              [uid](const User& usr) { return uid == usr.user_id; });
          if (it == users.end()) {
            update = true;
            break;
          }
        }
      }
    } else {
      update = true;
    }
    if (update) {
      std::vector<std::string> uids(users.size());
      std::transform(users.begin(), users.end(), uids.begin(),
                     [](const User& usr) { return usr.user_id; });
      vrd::MeetingSession::instance().updateRecordLayout(
          uids, meeting::DataMgr::instance().room().screen_shared_uid,
          [](int code) {
            if (code != 200) {
              WarningTips::showTips(QString("更新录制布局失败er"
                                            "r:%1")
                                        .arg(code),
                                    TipsType::kWarning, currentWidget(), 2000);
            }
          });
    }
  }
  instance().updating = false;
}

void PageManager::handleCacheUsers() {
    auto users = meeting::DataMgr::instance().users();
    for (auto& process : meeting::DataMgr::instance().ref_cache_users()) {
        process(users);
    }
    meeting::DataMgr::instance().ref_cache_users().clear();
    meeting::DataMgr::instance().setUsers(users);
    meeting::ForwardEvent::PostEvent(
        &meeting::Worker::instance(), [users]() mutable {
            sortUsers(users);
            meeting::ForwardEvent::PostEvent(
                &PageManager::instance(), [users] {
                    meeting::DataMgr::instance().setUsers(users);
                    updateData();
            });
        });
}

void PageManager::sortUsers(std::vector<User>& users) {
  std::chrono::system_clock::time_point begin =
      std::chrono::system_clock::now();
  auto find_host = std::find_if(users.begin(), users.end(),
                                [](const User& user) { return user.is_host; });
  if (find_host != users.end()) {
    std::swap(*find_host, users[0]);
    find_host = users.begin();
  }

  auto find_me = std::find_if(users.begin(), users.end(), [](const User& user) {
    return user.user_id == meeting::DataMgr::instance().user_id();
  });

  if (find_me != users.end() && find_host != users.end()) {
    if (find_me != find_host) {
      std::swap(*find_me, users[1]);
      find_me = users.begin() + 1;
    }
  } else {
    return;
  }

  std::sort(find_me + 1, users.end(), [](const User& l, const User& r) {
    if (l.is_mic_on && r.is_mic_on) {  // both have audio
      if ((l.is_camera_on || l.is_sharing) !=
          (r.is_camera_on || r.is_sharing)) {
        return l.audio_volume == r.audio_volume
                   ? l.is_camera_on || l.is_sharing
                   : l.audio_volume > r.audio_volume;
      } else {
        return l.audio_volume == r.audio_volume
                   ? l.user_uniform_name < r.user_uniform_name
                   : l.audio_volume > r.audio_volume;
      }
    } else if (l.is_mic_on || r.is_mic_on) {  // one has audio
      return l.is_mic_on;
    } else {  // Neither has audio
      if ((l.is_camera_on || l.is_sharing) !=
          (r.is_camera_on || r.is_sharing)) {
        return (l.is_camera_on || l.is_sharing);
      } else {
        return l.user_uniform_name < r.user_uniform_name;
      }
    }
  });
}

void PageManager::meetingNotify() {
  MeetingNotify::instance().onUserCameraStatusChange(
      [](const std::string& user_id, bool status) {
        auto& cache = meeting::DataMgr::instance().ref_cache_users();
        cache.push_back([=](std::vector<User>& users) {
          auto iter = std::find_if(
              users.begin(), users.end(),
              [user_id](const User& user) { return user.user_id == user_id; });
          if (iter != users.end()) iter->is_camera_on = status;
        });
      });

  MeetingNotify::instance().onUserMicStatusChange(
      [](const std::string& user_id, bool status) {
        auto& cache = meeting::DataMgr::instance().ref_cache_users();
        cache.push_back([=](std::vector<User>& users) {
          auto iter = std::find_if(
              users.begin(), users.end(),
              [user_id](const User& user) { return user.user_id == user_id; });
          if (iter != users.end()) iter->is_mic_on = status;
        });
      });

  MeetingNotify::instance().onUserJoinMeeting([](User user) {
    auto& cache = meeting::DataMgr::instance().ref_cache_users();
    cache.push_back([user](std::vector<User>& users) {
      auto iter = std::find_if(
          users.begin(), users.end(),
          [user](const User& usr) { return usr.user_id == user.user_id; });
      if (iter == users.end()) {
        users.push_back(user);
      }
    });
  });

  MeetingNotify::instance().onUserLeaveMeeting([](const std::string& user_id) {
    auto& cache = meeting::DataMgr::instance().ref_cache_users();
    cache.push_back([user_id](std::vector<User>& users) {
      auto iter = std::find_if(
          users.begin(), users.end(),
          [user_id](const User& user) { return user.user_id == user_id; });
      if (iter != users.end()) {
        if (iter->is_sharing && meeting::DataMgr::instance().room().screen_shared_uid == user_id)
        {
            PageManager::instance().main_page_->changeViewMode(MainPage::kNormalPage);
            auto r = meeting::DataMgr::instance().room();
            r.screen_shared_uid = "";
            meeting::DataMgr::instance().setRoom(std::move(r));
            setRemoteScrrenVideoWidget(*iter);
            meeting::DataMgr::instance().setShareScreen(false);
            instance().button_bar_->setShareState(PushButtonWarp::kNormal);
            MeetingRtcEngineWrap::unSubscribeVideoStream(user_id, false);
        }
        users.erase(iter);
      }
    });
    handleCacheUsers();
  });

  MeetingNotify::instance().onMeetingEnd([](int) {
    MeetingNotify::instance().offAll();
    instance().main_page_->froceClose();
    meeting::PageManager::instance().button_bar_->hide();
    if (meeting::DataMgr::instance().room().screen_shared_uid ==
        meeting::DataMgr::instance().user_id()) {
      meeting::DataMgr::instance().setShareScreen(false);
      MeetingRtcEngineWrap::instance().stopScreenCapture();
    }
    meeting::DataMgr::instance().setRecord(false);
    meeting::DataMgr::instance().ref_cache_users().clear();
    meeting::DataMgr::instance().ref_users().clear();
  });

  MeetingNotify::instance().onHostChange([](const std::string& from_uid,
                                            const std::string& user_id) {
    auto& cache = meeting::DataMgr::instance().ref_cache_users();
    cache.push_back([=](std::vector<User>& users) {
      if (user_id == meeting::DataMgr::instance().user_id()) {
        WarningTips::showTips(QString("您已经被指定为新的主持人！"),
                              TipsType::kWarning, currentWidget(), 2000);
      }
      for (size_t i = 0; i < users.size(); i++) {
        if (users[i].user_id == from_uid) {
          users[i].is_host = false;
          if (from_uid == meeting::DataMgr::instance().user_id()) {
            meeting::DataMgr::instance().setLocalRole(MeetingRole::kAudience);
            instance().getVideoList()[i]->setUserRole(UserRole::kMe);
          } else
            instance().getVideoList()[i]->setUserRole(UserRole::kAudience);
        }
        if (users[i].user_id == user_id) {
          users[i].is_host = true;
          instance().getVideoList()[i]->setUserRole(UserRole::kBroadCast);
          if (user_id == meeting::DataMgr::instance().user_id()) {
            meeting::DataMgr::instance().setLocalRole(MeetingRole::kBroadCast);
          }
        }
      }
    });
  });

  MeetingNotify::instance().onShareScreenStatusChanged(
      [](const std::string& uid, bool status) {
          auto& cache = meeting::DataMgr::instance().ref_cache_users();
          cache.push_back([=](std::vector<User>& users) {
              if (uid == meeting::DataMgr::instance().user_id()) return;
              for (size_t i = 0; i < users.size(); i++) {
                  if (users[i].user_id == uid) {
                      PageManager::instance().main_page_->changeViewMode(
                          status ? MainPage::kSpeakerPage : MainPage::kNormalPage);
                      users[i].is_sharing = status;
                      auto r = meeting::DataMgr::instance().room();
                      r.screen_shared_uid = status ? uid : "";
                      meeting::DataMgr::instance().setRoom(std::move(r));
                      setRemoteScrrenVideoWidget(users[i]);
                      meeting::DataMgr::instance().setShareScreen(status);
                      if (status) {
                          instance().button_bar_->setShareState(PushButtonWarp::kSelect);
                      }
                      else {
                          instance().button_bar_->setShareState(PushButtonWarp::kNormal);
                          MeetingRtcEngineWrap::unSubscribeVideoStream(uid, false);
                      }
                      break;
                  }
              }
              });
      });

  MeetingNotify::instance().onMuteAll([](int) {
    auto& cache = meeting::DataMgr::instance().ref_cache_users();
    cache.push_back([=](std::vector<User>& users) {
      if (meeting::DataMgr::instance().local_role() !=
          MeetingRole::kBroadCast && !meeting::DataMgr::instance().mute_audio()) {
        WarningTips::showTips(QString("你已被主持人静音"), TipsType::kWarning,
                              currentWidget(), 2000);
        for (auto iter = users.begin(); iter != users.end(); iter++)
          if (!iter->is_host) iter->is_mic_on = false;
        meeting::PageManager::instance().button_bar_->setMic(true);
      }
    });
  });

  MeetingNotify::instance().onMuteUser([](const std::string& user_id) {
    auto& cache = meeting::DataMgr::instance().ref_cache_users();
    cache.push_back([=](std::vector<User>& users) {
      if (user_id == meeting::DataMgr::instance().user_id() 
          && !meeting::DataMgr::instance().mute_audio()) {
        WarningTips::showTips(QString("你已被主持人静音"), TipsType::kWarning,
                              currentWidget(), 2000);
        auto iter = std::find_if(
            users.begin(), users.end(),
            [user_id](const User& usr) { return user_id == usr.user_id; });
        if (iter != users.end()) {
          if (iter->is_mic_on) {
            iter->is_mic_on = false;
            meeting::PageManager::instance().button_bar_->setForceMic(true);
          }
        }
      }
    });
  });

  MeetingNotify::instance().onRecord([](int) {
    meeting::DataMgr::instance().setRecord(true);
    instance().button_bar_->setRecordingState(PushButtonWarp::kSelect);
    instance().main_page_->setRecVisiable(true);
  });

  MeetingNotify::instance().onAskingMicOn([](const std::string& uid) {
    if (uid == meeting::DataMgr::instance().user_id()) {
      auto p = std::unique_ptr<TipsDlg>(new TipsDlg);
      p->setText("主持人邀请你打开麦克风");
      auto res = p->exec();
      if (res == QDialog::Accepted) {
        meeting::PageManager::instance().button_bar_->setForceMic(false);
      }
    }
  });

  MeetingNotify::instance().onUserKickedOff([](int) {
    MeetingNotify::instance().offAll();
    instance().main_page_->froceClose();
    meeting::PageManager::instance().button_bar_->hide();
    if (meeting::DataMgr::instance().room().screen_shared_uid ==
        meeting::DataMgr::instance().user_id()) {
      meeting::DataMgr::instance().setShareScreen(false);
      MeetingRtcEngineWrap::instance().stopScreenCapture();
    }
    meeting::DataMgr::instance().setRecord(false);
    meeting::DataMgr::instance().ref_cache_users().clear();
    meeting::DataMgr::instance().ref_users().clear();
    QTimer::singleShot(100, [] {
      WarningTips::showTips(QString("相同ID用户已登录，您已被强制下线！"),
                            TipsType::kWarning, nullptr, 2000);
    });
  });

  MeetingNotify::instance().onInvalidToken([](int) {

  });
}

void PageManager::stopScreen() {
  meeting::DataMgr::instance().setShareScreen(false);
  meeting::DataMgr::instance().ref_room().screen_shared_uid = "";
  instance().button_bar_->setMoveEnabled(false);
  showRoom();
  if (0 == MeetingRtcEngineWrap::instance().stopScreenCapture()) {
    vrd::MeetingSession::instance().stopScreenShare([](int code) {
      if (code != 200) {
        WarningTips::showTips(QString("关闭屏幕共享失败er"
                                      "r:%1")
                                  .arg(code),
                              TipsType::kError, currentWidget(), 2000);
        return;
      }
    });
  }
}

void PageManager::customEvent(QEvent* e) {
  if (e->type() == QEvent::User) {
    auto user_event = static_cast<meeting::ForwardEvent*>(e);
    user_event->execTask();
  }
}

}  // namespace meeting
