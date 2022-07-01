#include "meeting/Core/meeting_session.h"
#include "core/Application.h"
#include "feature/data_mgr.h"
#include "meeting/Core/data_mgr.h"
#include "logger.h"

#include <QJsonObject>
#include <QJsonArray>
#include <QJsonDocument>
#include <QStringList>
#include <QDebug>

namespace vrd {
MeetingSession& MeetingSession::instance() {
	static MeetingSession ms;
	return ms;
}
void MeetingSession::setUserId(const std::string& uid) {
	base_->setUserId(uid);
}

void MeetingSession::setToken(const std::string& token) {
	base_->setToken(token);
}

void MeetingSession::setRoomId(const std::string& roomId) {
	base_->setRoomId(roomId);
}

std::string MeetingSession::user_id() { return base_->_userId(); }

void MeetingSession::changeUserName(CSTRING_REF_PARAM name,
                                    CallBackFunction&& callback) {
  base_->changeUserName(name, std::move(callback));
}

void MeetingSession::initSceneConfig(std::function<void(void)>&& callback) {
	base_->connectRTS("meeting", [this, callback](){
		auto res_info = vrd::DataMgr::instance().rts_info();
		meeting::DataMgr::instance().setAppID(res_info.app_id);
		if (callback) {
			callback();
		}
	});
}

void MeetingSession::exitScene() {
	base_->disconnectRTS();
}

void MeetingSession::joinMeeting(const std::string& userName,
                                 const std::string& userId,
                                 const std::string& roomId, bool micOn,
                                 bool cameraOn,
                                 std::function<void(int)>&& callback) {
	QJsonObject req;
	req["login_token"] = QString::fromStdString(base_->_token());
	req["app_id"] = QString::fromStdString(meeting::DataMgr::instance().app_id());
	req["user_id"] = QString::fromStdString(userId);
	req["room_id"] = QString::fromStdString(roomId);
    req["user_name"] = QString::fromStdString(userName);
    req["mic"] = micOn;
    req["camera"] = cameraOn;

	base_->_emitMessage("vcJoinMeeting", req, [=](const QJsonObject& rsp) {
		qDebug() << "sendServerMessage eventName: vcJoinMeeting, response: " << rsp;
		int code = rsp["code"].toInt();
		if (code != 200) {
			callback(code);
			return;
		}
		MeetingRoom r;
		auto& response = rsp["response"].toObject();
		auto& info = response["info"].toObject();
		qDebug()<< "vcJoinMeeting info"<< info;
		r.host_uid = std::string(info["host_id"].toString().toUtf8());
		r.room_id = std::string(info["room_id"].toString().toUtf8());
		r.is_recording = info["record"].toBool();
		r.screen_shared_uid = std::string(info["screen_shared_uid"].toString().toUtf8());
		r.now = info["now"].toDouble();
		r.meeting_start_time = info["created_at"].toDouble();
		meeting::DataMgr::instance().setRoom(std::move(r));
		auto token = std::string(response["token"].toString().toUtf8());
		meeting::DataMgr::instance().setToken(token);

		std::vector<User> users;
		auto& user_list = response["users"];
		for (const auto& userItem : user_list.toArray()) {
			User u;
			auto user = userItem.toObject();
			u.is_camera_on = user["is_camera_on"].toBool();
			u.is_host = user["is_host"].toBool();
			u.is_mic_on = user["is_mic_on"].toBool();
			u.is_sharing = user["is_sharing"].toBool();
			u.created_at = user["created_at"].toDouble();
			u.user_id = std::string(user["user_id"].toString().toUtf8());
			u.user_name = std::string(user["user_name"].toString().toUtf8());
			u.user_uniform_name = std::string(user["user_uniform_name"].toString().toUtf8());
			users.push_back(std::move(u));
		}
		meeting::DataMgr::instance().setUsers(std::move(users));

		callback(code);
		});
}

void MeetingSession::leaveMeeting(std::function<void(int)>&& callback) {
	QJsonObject req;
	req["login_token"] = QString::fromStdString(base_->_token());
	base_->_emitMessage("vcLeaveMeeting", req, [=](const QJsonObject& rsp) {
		int code = rsp["code"].toInt();
		if (code != 200) {
			callback(code);
			return;
		}
		callback(code);
	});
}

void MeetingSession::toggleMicState(bool on,
                                    std::function<void(int)>&& callback) {
	QJsonObject req;
	req["login_token"] = QString::fromStdString(base_->_token());
	base_->_emitMessage(on ? "vcTurnOnMic" : "vcTurnOffMic", req,
		[=](const QJsonObject& rsp) {
			int code = rsp["code"].toInt();
			if (code != 200) {
				callback(code);
				return;
			}
			callback(code);
		});
}

void MeetingSession::toggleCameraState(bool on,
                                       std::function<void(int)>&& callback) {
	QJsonObject req;
	req["login_token"] = QString::fromStdString(base_->_token());
	base_->_emitMessage(on ? "vcTurnOnCamera" : "vcTurnOffCamera", req,
		[=](const QJsonObject& rsp) {
			int code = rsp["code"].toInt();
			callback(code);
		});
}

void MeetingSession::getMeetingParticipantsInfo(
    std::function<void(int code)>&& callback) {
	QJsonObject req;
	req["login_token"] = QString::fromStdString(base_->_token());
	base_->_emitMessage("vcGetMeetingUserInfo", req,
		[=](const QJsonObject& rsp) {
			int code = rsp["code"].toInt();
			if (code != 200) {
				callback(code);
				return;
			}
			callback(code);
		});
}

void MeetingSession::getMeetingParticipantInfo(
    const std::string& userId, std::function<void(int code)> callback) {
	QJsonObject req;
	req["login_token"] = QString::fromStdString(base_->_token());
	req["user_id"] = QString::fromStdString(userId);

	base_->_emitMessage("vcGetMeetingUserInfo", req,
		[=](const QJsonObject& rsp) {
			int code = rsp["code"].toInt();
			if (code != 200) {
				callback(code);
				return;
			}
			callback(code);
		});
}

void MeetingSession::getMeetingInfo(std::function<void(int)> callback) {
	QJsonObject req;
	req["login_token"] = QString::fromStdString(base_->_token());
	base_->_emitMessage("vcGetMeetingInfo", req,
		[=](const QJsonObject& rsp) {
			int code = rsp["code"].toInt();
			if (code != 200) {
				callback(code);
				return;
			}
			callback(code);
		});
}

int MeetingSession::changeHost(const std::string& userId,
                               std::function<void(int)> callback) {
	QJsonObject req;
	req["login_token"] = QString::fromStdString(base_->_token());
	req["user_id"] = QString::fromStdString(userId);
	base_->_emitMessage("vcChangeHost", req,
		[=](const QJsonObject& rsp) {
			int code = rsp["code"].toInt();
			if (code != 200) {
				callback(code);
				return;
			}
			callback(code);
		});
  return 0;
}

void MeetingSession::muteUserMic(const std::string& userId,
                                 std::function<void(int)> callback) {
	QJsonObject req;
	req["login_token"] = QString::fromStdString(base_->_token());
	req["user_id"] = QString::fromStdString(userId);
	base_->_emitMessage("vcMuteUser", req,
		[=](const QJsonObject& rsp) {
			int code = rsp["code"].toInt();
			if (code != 200) {
				callback(code);
				return;
			}
			callback(code);
		});
}

void MeetingSession::muteAllUserMic(std::function<void(int)> callback) {
	QJsonObject req;
	req["login_token"] = QString::fromStdString(base_->_token());
	base_->_emitMessage("vcMuteUser", req,
		[=](const QJsonObject& rsp) {
			int code = rsp["code"].toInt();
			if (code != 200) {
				callback(code);
				return;
			}
			callback(code);
		});
}

void MeetingSession::askUserMicOn(const std::string& userId,
                                  std::function<void(int)> callback) {
	QJsonObject req;
	req["login_token"] = QString::fromStdString(base_->_token());
	req["user_id"] = QString::fromStdString(userId);
	base_->_emitMessage("vcAskMicOn", req,
		[=](const QJsonObject& rsp) {
			int code = rsp["code"].toInt();
			if (code != 200) {
				callback(code);
				return;
			}
			callback(code);
		});
}

void MeetingSession::askUserCameraOn(const std::string& userId,
                                     std::function<void(int)> callback) {
	QJsonObject req;
	req["login_token"] = QString::fromStdString(base_->_token());
	req["user_id"] = QString::fromStdString(userId);
	base_->_emitMessage("vcAskCameraOn", req,
		[=](const QJsonObject& rsp) {
			int code = rsp["code"].toInt();
			if (code != 200) {
				callback(code);
				return;
			}
			callback(code);
		});
}

void MeetingSession::closeMeeting(std::function<void(int)> callback) {
	QJsonObject req;
	req["login_token"] = QString::fromStdString(base_->_token());
	base_->_emitMessage("vcEndMeeting", req,
		[=](const QJsonObject& rsp) {
			int code = rsp["code"].toInt();
			if (code != 200) {
				callback(code);
				return;
			}
			callback(code);
		});
}

void MeetingSession::startScreenShare(std::function<void(int)> callback) {
	QJsonObject req;
	req["login_token"] = QString::fromStdString(base_->_token());
	base_->_emitMessage("vcStartShareScreen", req,
		[=](const QJsonObject& rsp) {
			int code = rsp["code"].toInt();
			if (code != 200) {
				callback(code);
				return;
			}
			callback(code);
		});
}

void MeetingSession::stopScreenShare(std::function<void(int)> callback) {
	QJsonObject req;
	req["login_token"] = QString::fromStdString(base_->_token());
	base_->_emitMessage("vcEndShareScreen", req,
		[=](const QJsonObject& rsp) {
			int code = rsp["code"].toInt();
			if (code != 200) {
				callback(code);
				return;
			}
			callback(code);
		});
}

void MeetingSession::startMeetingRecord(const std::vector<std::string>& users,
                                        const std::string& screen_uid,
                                        std::function<void(int)> callback) {
	QJsonObject req;
	req["login_token"] = QString::fromStdString(base_->_token());
	QJsonArray users_msg;
	for (auto user : users) {
		QString str = QString::fromStdString(user);
		users_msg.push_back(str);
	}

	req["users"] = users_msg;
	qDebug() <<" MeetingSession::startMeetingRecord:: "<<req["users"];
	if (!screen_uid.empty()) {
		req["screen_uid"] = QString::fromStdString(screen_uid);
	}

	base_->_emitMessage("vcRecordMeeting", req, [=](const QJsonObject& rsp) {
		int code = rsp["code"].toInt();
		if (code != 200) {
			callback(code);
			return;
		}
		callback(code);
	});
}

void MeetingSession::updateRecordLayout(const std::vector<std::string>& users,
                                        const std::string& screen_uid,
                                        std::function<void(int)> callback) {
	QJsonObject req;
	req["login_token"] = QString::fromStdString(base_->_token());
	QJsonArray users_msg;
	for (const auto& user : users) {
		auto str = QString::fromStdString(user);
		users_msg.push_back(str);
	}
	req["users"] = users_msg;
	if (!screen_uid.empty()) {
		req["screen_uid"] = QString::fromStdString(screen_uid);
	}

	base_->_emitMessage("vcUpdateRecordLayout", req,
		[=](const QJsonObject& rsp) {
			int code = rsp["code"].toInt();
			if (code != 200) {
				callback(code);
				return;
			}
			callback(code);
		});
}

void MeetingSession::getHistoryMeetingRecord(
    std::function<void(int code)> callback) {
	QJsonObject req;
	req["login_token"] = QString::fromStdString(base_->_token());
	base_->_emitMessage(
		"vcGetHistoryVideoRecord", req, [=](const QJsonObject& rsp) {
			int code = rsp["code"].toInt();
			if (code != 200) {
				callback(code);
				return;
			}
			if (!rsp["response"].isNull()) {
				std::vector<RecordInfo> records;
				auto response = rsp["response"].toArray();
				for (auto& item : response) {
					auto r = item.toObject();
					RecordInfo record;
					record.room_id = std::string(r["room_id"].toString().toUtf8());
					record.url = std::string(r["download_url"].toString().toUtf8());
					record.vid = std::string(r["vid"].toString().toUtf8());
					record.video_holder = r["video_holder"].toBool();
					record.meeting_start_time = r["created_at"].toDouble();
					records.push_back(std::move(record));
				}
				meeting::DataMgr::instance().setRecordInfos(std::move(records));
			}
			callback(code);
		});
}

void MeetingSession::deleteVideoRecord(const std::string& vid,
                                       std::function<void(int)> callback) {
	QJsonObject req;
	req["login_token"] = QString::fromStdString(base_->_token());
	req["vid"] = QString::fromStdString(vid);
	base_->_emitMessage("vcDeleteVideoRecord", req,
		[=](const QJsonObject& rsp) {
			int code = rsp["code"].toInt();
			if (code != 200) {
				callback(code);
				return;
			}
			callback(code);
		});
}

void MeetingSession::getAuditState(const std::string& version,
                                   std::function<void(int)> callback) {}

void MeetingSession::userReconnect(std::function<void(int)> callback) {
	QJsonObject req;
	req["login_token"] = QString::fromStdString(base_->_token());
	base_->_emitMessage("vcReconnect", req, [=](const QJsonObject& rsp) {
		int code = rsp["code"].toInt();
		callback(code);
	});

}

// notify
void MeetingSession::onUserMicStatusChange(
    std::function<void(const std::string& user_id, bool status)>&& callback) {
	base_->_onNotify("onUserMicStatusChange", [=](const QJsonObject& rsp) {
		auto user_id = std::string(rsp["user_id"].toString().toUtf8());
		auto status = rsp["status"].toBool();
		callback(user_id, status);
	});
}

void MeetingSession::onUserCameraStatusChange(
    std::function<void(const std::string& user_id, bool status)>&& callback) {
	base_->_onNotify("onUserCameraStatusChange", [=](const QJsonObject& data) {
		if (!data.isEmpty()) {
			auto user_id = std::string(data["user_id"].toString().toUtf8());
			auto status = data["status"].toBool();

			if (callback) {
				callback(user_id, status);
			}
		}
	});
}

void MeetingSession::onHostChange(
    std::function<void(const std::string&, const std::string&)>&& callback) {
	base_->_onNotify("onHostChange", [=](const QJsonObject& data) {
		if (!data.isEmpty()) {
			auto former_host_id = std::string(data["former_host_id"].toString().toUtf8());
            auto host_id = std::string(data["host_id"].toString().toUtf8());
			if (callback) {
				callback(former_host_id, host_id);
			}
		}
	});
}

void MeetingSession::onUserJoinMeeting(std::function<void(User)>&& callback) {
	base_->_onNotify("onUserJoinMeeting", [=](const QJsonObject& data) {
		if (!data.isEmpty()) {
			User user;
			user.user_id = std::string(data["user_id"].toString().toUtf8());
			user.user_name = std::string(data["user_name"].toString().toUtf8());
			user.created_at = data["created_at"].toDouble();
			user.is_host = data["is_host"].toBool();
			user.is_sharing = data["is_sharing"].toBool();
			user.is_mic_on = data["is_mic_on"].toBool();
			user.is_camera_on = data["is_camera_on"].toBool();
			user.user_uniform_name = std::string(data["user_uniform_name"].toString().toUtf8());

			if (callback) {
				callback(user);
			}
		}
    });
}

void MeetingSession::onUserLeaveMeeting(
    std::function<void(const std::string&)>&& callback) {
	base_->_onNotify("onUserLeaveMeeting", [=](const QJsonObject& data) {
		if (!data.isEmpty()) {
			auto user_id = std::string(data["user_id"].toString().toUtf8());
			if (callback) {
				callback(user_id);
			}
		}
	});
}

void MeetingSession::onShareScreenStatusChanged(
    std::function<void(const std::string&, bool)>&& callback) {
  base_->_onNotify(
      "onShareScreenStatusChanged", [=](const QJsonObject& data) {
		  if (!data.isEmpty()) {
			  auto user_id = std::string(data["user_id"].toString().toUtf8());
			  auto status = data["status"].toBool();
			  auto msg = "recv MeetingSession::onShareScreenStatusChanged user id: " 
                  + user_id + "status: " + std::to_string(status);
			  qInfo(msg.c_str());

			  if (callback) {
				  callback(user_id, status);
			  }
		  }
      });
}

void MeetingSession::onRecord(std::function<void(int)>&& callback) {
	base_->_onNotify("onRecord", [=](const QJsonObject& data) {
        if (callback) {
		    callback(200);
        }
	});
}

void MeetingSession::onMeetingEnd(std::function<void(int)>&& callback) {
	base_->_onNotify("onMeetingEnd", [=](const QJsonObject& data) {
		if (callback) {
			callback(200);
		}
	});
}

void MeetingSession::onMuteAll(std::function<void(int)>&& callback) {
	base_->_onNotify("onMuteAll", [=](const QJsonObject& data) {
		if (callback) {
			callback(200);
		}
	});
}

void MeetingSession::onMuteUser(
    std::function<void(const std::string&)>&& callback) {
	base_->_onNotify("onMuteUser", [=](const QJsonObject& data) {
		if (!data.isEmpty()) {
			auto user_id = std::string(data["user_id"].toString().toUtf8());
			if (callback) {
				callback(user_id);
			}
		}
	});
}

void MeetingSession::onAskingMicOn(
    std::function<void(const std::string&)>&& callback) {
	base_->_onNotify("onAskingMicOn", [=](const QJsonObject& data) {
		if (!data.isEmpty()) {
			auto user_id = std::string(data["user_id"].toString().toUtf8());
			if (callback) {
				callback(user_id);
			}
		}
	});
}

void MeetingSession::onAskingCameraOn(
    std::function<void(const std::string&)>&& callback) {
	base_->_onNotify("onAskingCameraOn", [=](const QJsonObject& data) {
		if (!data.isEmpty()) {
			auto user_id = std::string(data["user_id"].toString().toUtf8());
			if (callback) {
				callback(user_id);
			}
		}
	});
}

void MeetingSession::onUserKickedOff(std::function<void(int)>&& callback) {
	base_->_onNotify("onUserKickedOff", [=](const QJsonObject& data) {
		if (callback) {
			callback(200);
		}
	});
}

void MeetingSession::onInvalidToken(std::function<void(int)>&& callback) {
	base_->_onNotify("onInvalidToken", [=](const QJsonObject& data) {
		if (callback) {
			callback(200);
		}
	});
}

MeetingSession::MeetingSession() {
  base_ = vrd::Application::getSingleton().getComponent(
      VRD_UTIL_GET_COMPONENT_PARAM(vrd::SessionBase));
}

}  // namespace vrd
