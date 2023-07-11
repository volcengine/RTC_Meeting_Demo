#pragma once
#include <algorithm>
#include <functional>
#include <mutex>

#include "core/rtc_engine_wrap.h"
#include "meeting_model.h"

namespace meeting {

#define PROPRETY(CLASS, MEMBER, UPPER_MEMBER)                         \
 private:                                                             \
  CLASS MEMBER##_;                                                    \
                                                                      \
 public:                                                              \
  CLASS MEMBER() const { return MEMBER##_; }                          \
  CLASS& ref_##MEMBER() { return MEMBER##_; }                         \
  void set##UPPER_MEMBER(const CLASS& MEMBER) { MEMBER##_ = MEMBER; } \
  void set##UPPER_MEMBER(CLASS&& MEMBER) { MEMBER##_ = std::move(MEMBER); }

using CacheUpdateType = std::vector<std::function<void(std::vector<User>&)>>;

class DataMgr {
 public:
  static DataMgr& instance();
  static void init();

  PROPRETY(StreamInfo, stream_info, StreamInfo)
  PROPRETY(MeetingSettingModel, setting, Setting)
  PROPRETY(MeetingRole, local_role, LocalRole)
  PROPRETY(std::vector<MeetingUserInfo>, user_info, MeetingUserInfo)
  PROPRETY(std::string, user_name, UserName)
  PROPRETY(bool, mute_audio, MuteAudio)
  PROPRETY(bool, mute_video, MuteVideo)
  PROPRETY(bool, share_screen, ShareScreen) //for local share state and remote share state
  PROPRETY(bool, record, Record)
  PROPRETY(bool, connect,Connect)

  PROPRETY(std::string, high_light, HighLight)

  PROPRETY(std::string, app_id, AppID)
  PROPRETY(std::string, user_id, UserID)
  PROPRETY(std::string, room_id, RoomID)

  PROPRETY(MeetingRoom, room, Room)
  PROPRETY(std::vector<User>, users, Users)
  PROPRETY(std::string, token, Token)
  PROPRETY(std::vector<AudioVolumeInfoWrap>, volumes, Volumes)
  PROPRETY(std::vector<RecordInfo>, record_infos, RecordInfos)
  PROPRETY(std::vector<std::string>, record_users, RecordUsers)
  PROPRETY(std::string, record_screen, RecordScreen)

  PROPRETY(CacheUpdateType, cache_users, CacheUsers)
  PROPRETY(std::vector<std::string>, sub_streams, SubStreams)

 protected:
  DataMgr() = default;
  ~DataMgr() = default;

 public:
  mutable std::mutex _mutex;
};

#undef PROPRETY

}  // namespace meeting