#pragma once
#include <functional>
#include <string>

#include "meeting/Core/meeting_model.h"

class MeetingNotify {
 public:
  static MeetingNotify& instance();
  static void init();
  void onUserMicStatusChange(
      std::function<void(const std::string& user_id, bool status)>&& callback);
  void onUserCameraStatusChange(
      std::function<void(const std::string& user_id, bool status)>&& callback);
  void onHostChange(
      std::function<void(const std::string&, const std::string&)>&& callback);
  void onUserJoinMeeting(std::function<void(User)>&& callback);
  void onUserLeaveMeeting(std::function<void(const std::string&)>&& callback);
  void onShareScreenStatusChanged(
      std::function<void(const std::string&, bool)>&& callback);
  void onRecord(std::function<void(int)>&& callback);
  void onMeetingEnd(std::function<void(int)>&& callback);
  void onMuteAll(std::function<void(int)>&& callback);
  void onMuteUser(std::function<void(const std::string&)>&& callback);
  void onAskingMicOn(std::function<void(const std::string&)>&& callback);
  void onAskingCameraOn(std::function<void(const std::string&)>&& callback);
  void onUserKickedOff(std::function<void(int)>&& callback);
  void offAll();
  void onInvalidToken(std::function<void(int)>&& callback);

 private:
  MeetingNotify();
  ~MeetingNotify() = default;

  void _init();

  std::function<void(const std::string& user_id, bool status)>
      onUserMicStatusChange_;
  std::function<void(const std::string& user_id, bool status)>
      onUserCameraStatusChange_;
  std::function<void(const std::string&, const std::string&)> onHostChange_;
  std::function<void(User)> onUserJoinMeeting_;
  std::function<void(const std::string&)> onUserLeaveMeeting_;
  std::function<void(const std::string&, bool)> onShareScreenStatusChanged_;
  std::function<void(int)> onRecord_;
  std::function<void(int)> onMeetingEnd_;
  std::function<void(int)> onMuteAll_;
  std::function<void(const std::string&)> onMuteUser_;
  std::function<void(const std::string&)> onAskingMicOn_;
  std::function<void(const std::string&)> onAskingCameraOn_;
  std::function<void(int)> onInvalidToken_;
  std::function<void(int)> onUserKickedOff_;
};
