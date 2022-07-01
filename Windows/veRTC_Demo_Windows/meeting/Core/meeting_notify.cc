#include "meeting_notify.h"

#include "meeting/Core/meeting_session.h"

MeetingNotify& MeetingNotify::instance() {
  static MeetingNotify notify;
  return notify;
}

void MeetingNotify::init() { instance()._init(); }

MeetingNotify::MeetingNotify() {}

void MeetingNotify::_init() {
    onUserMicStatusChange_;
    vrd::MeetingSession::instance().onUserMicStatusChange(
        [=](const std::string& uid, bool status) {
            if (onUserMicStatusChange_) onUserMicStatusChange_(uid, status);
        });
    vrd::MeetingSession::instance().onAskingCameraOn(
        [=](const std::string& uid) { onAskingCameraOn_(uid); });
    vrd::MeetingSession::instance().onUserCameraStatusChange(
        [=](const std::string& uid, bool status) {
            if (onUserCameraStatusChange_) onUserCameraStatusChange_(uid, status);
        });

    vrd::MeetingSession::instance().onHostChange(
        [=](const std::string& from_uid, const std::string& uid) {
            if (onHostChange_) onHostChange_(from_uid, uid);
        });

    vrd::MeetingSession::instance().onUserJoinMeeting([=](User usr) {
        if (onUserJoinMeeting_) onUserJoinMeeting_(usr);
    });

    vrd::MeetingSession::instance().onUserLeaveMeeting(
        [=](const std::string& uid) {
            if (onUserLeaveMeeting_) onUserLeaveMeeting_(uid);
        });

    vrd::MeetingSession::instance().onShareScreenStatusChanged(
        [=](const std::string& uid, bool status) {
            if (onShareScreenStatusChanged_)
                onShareScreenStatusChanged_(uid, status);
        });

    vrd::MeetingSession::instance().onRecord([=](int code) {
        if (onRecord_) onRecord_(code);
    });

    vrd::MeetingSession::instance().onMeetingEnd([=](int code) {
        if (onMeetingEnd_) onMeetingEnd_(code);
    });

    vrd::MeetingSession::instance().onMuteAll([=](int code) {
        if (onMuteAll_) onMuteAll_(code);
    });

    vrd::MeetingSession::instance().onMuteUser([=](const std::string& uid) {
        if (onMuteUser_) onMuteUser_(uid);
    });
    vrd::MeetingSession::instance().onAskingMicOn([=](const std::string& uid) {
        if (onAskingMicOn_) onAskingMicOn_(uid);
    });

    vrd::MeetingSession::instance().onAskingCameraOn([=](const std::string& uid) {
        if (onAskingCameraOn_) onAskingCameraOn_(uid);
    });

    vrd::MeetingSession::instance().onUserKickedOff([=](int code) {
        if (onUserKickedOff_) onUserKickedOff_(code);
    });

    vrd::MeetingSession::instance().onInvalidToken([=](int code) {
        if (onInvalidToken_) onInvalidToken_(code);
    });
}

void MeetingNotify::onUserMicStatusChange(
    std::function<void(const std::string& user_id, bool status)>&& callback) {
    onUserMicStatusChange_ = std::move(callback);
}
void MeetingNotify::onUserCameraStatusChange(
    std::function<void(const std::string& user_id, bool status)>&& callback)
{
  onUserCameraStatusChange_ = std::move(callback);
}
void MeetingNotify::onHostChange(
    std::function<void(const std::string&, const std::string&)>&& callback) {
  onHostChange_ = std::move(callback);
}
void MeetingNotify::onUserJoinMeeting(std::function<void(User)>&& callback) {
  onUserJoinMeeting_ = std::move(callback);
}

void MeetingNotify::onUserLeaveMeeting(
    std::function<void(const std::string&)>&& callback) {
  onUserLeaveMeeting_ = std::move(callback);
}

void MeetingNotify::onShareScreenStatusChanged(
    std::function<void(const std::string&, bool)>&& callback) {
  onShareScreenStatusChanged_ = std::move(callback);
}

void MeetingNotify::onRecord(std::function<void(int)>&& callback) {
  onRecord_ = std::move(callback);
}

void MeetingNotify::onMeetingEnd(std::function<void(int)>&& callback) {
  onMeetingEnd_ = std::move(callback);
}

void MeetingNotify::onMuteAll(std::function<void(int)>&& callback) {
  onMuteAll_ = std::move(callback);
}

void MeetingNotify::onMuteUser(
    std::function<void(const std::string&)>&& callback) {
  onMuteUser_ = std::move(callback);
}

void MeetingNotify::onAskingMicOn(
    std::function<void(const std::string&)>&& callback) {
  onAskingMicOn_ = std::move(callback);
}

void MeetingNotify::onAskingCameraOn(
    std::function<void(const std::string&)>&& callback) {
  onAskingCameraOn_ = std::move(callback);
}

void MeetingNotify::onUserKickedOff(std::function<void(int)>&& callback) {
  onUserKickedOff_ = std::move(callback);
}

void MeetingNotify::offAll() {
  onUserMicStatusChange_ = nullptr;
  onUserCameraStatusChange_ = nullptr;
  onHostChange_ = nullptr;
  onUserJoinMeeting_ = nullptr;
  onUserLeaveMeeting_ = nullptr;
  onShareScreenStatusChanged_ = nullptr;
  onRecord_ = nullptr;
  onMeetingEnd_ = nullptr;
  onMuteAll_ = nullptr;
  onMuteUser_ = nullptr;
  onAskingMicOn_ = nullptr;
  onAskingCameraOn_ = nullptr;
  onUserKickedOff_ = nullptr;
  //onWebsocketConnected_ = nullptr;
  //onWebsocketConnecting_ = nullptr;
  onInvalidToken_ = nullptr;
}

void MeetingNotify::onInvalidToken(std::function<void(int)>&& callback) {
  onInvalidToken_ = std::move(callback);
}
