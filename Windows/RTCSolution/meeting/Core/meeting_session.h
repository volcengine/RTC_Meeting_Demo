#pragma once

#include <memory>
#include <vector>

#include "core/session_base.h"
#include "meeting/Core/meeting_model.h"

namespace vrd {

class MeetingSession {
 public:
  static MeetingSession& instance();

  void setUserId(const std::string& uid);
  void setToken(const std::string& token);
  void setRoomId(const std::string& roomId);
  std::string user_id();

  //----------------------------------interface----------------------------------
  void changeUserName(CSTRING_REF_PARAM name, CallBackFunction&& callback);

  void initSceneConfig(std::function<void(void)>&& callback);
  void exitScene();

  void joinMeeting(const std::string& userName, const std::string& userId,
                   const std::string& roomId, bool micOn, bool cameraOn,
                   std::function<void(int)>&& callback);

  void leaveMeeting(std::function<void(int)>&& callback);

  void toggleMicState(bool on, std::function<void(int)>&& callback);

  void toggleCameraState(bool on, std::function<void(int)>&& callback);

  void getMeetingParticipantsInfo(std::function<void(int code)>&& callback);

  void getMeetingParticipantInfo(const std::string& userId,
                                 std::function<void(int code)> callback);

  void getMeetingInfo(std::function<void(int)> callback);

  int changeHost(const std::string& userId, std::function<void(int)> callback);

  void muteUserMic(const std::string& userId,
                   std::function<void(int)> callback);

  void muteAllUserMic(std::function<void(int)> callback);

  void askUserMicOn(const std::string& userId,
                    std::function<void(int)> callback);

  void askUserCameraOn(const std::string& userId,
                       std::function<void(int)> callback);

  void closeMeeting(std::function<void(int)> callback);

  void startScreenShare(std::function<void(int)> callback);

  void stopScreenShare(std::function<void(int)> callback);

  void startMeetingRecord(const std::vector<std::string>& users,
                          const std::string& screen_uid,
                          std::function<void(int)> callback);

  void updateRecordLayout(const std::vector<std::string>& users,
                          const std::string& screen_uid,
                          std::function<void(int)> callback);

  void getHistoryMeetingRecord(std::function<void(int code)> callback);

  void deleteVideoRecord(const std::string& vid,
                         std::function<void(int)> callback);

  void getAuditState(const std::string& version,
                     std::function<void(int)> callback);

  void userReconnect(std::function<void(int)> callback);

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

  void onInvalidToken(std::function<void(int)>&& callback);

 private:
  MeetingSession();
  ~MeetingSession() = default;

 private:
  std::shared_ptr<SessionBase> base_;
};

}  // namespace vrd