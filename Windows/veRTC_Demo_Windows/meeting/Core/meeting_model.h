#pragma once
#include <unordered_map>
#include <vector>

struct VideoResolution {
  int width = 160;
  int height = 160;
};

struct VideoKbpsHint {
  int min_kbps = 40;
  int max_kbps = 150;
};

struct VideoConfiger {
  VideoResolution resolution;
  VideoKbpsHint kbpsRange;
  int fps = 15;
  int kbps = 60;
};

struct MeetingSettingModel {
  VideoConfiger camera{{640, 480}, {250, 1000}, 15, 500};
  VideoConfiger screen{{1920, 1080}, {800, 3000}, 15, 1500};
  int camera_idx = -1;
  int mic_idx = -1;
  bool enable_show_info = false;
};

struct MeetingUserInfo {
  std::string uid;
};

enum class MeetingRole {
  kMe,
  kBroadCast,
  kAudience,
};


struct User {
  std::string user_id;    // user ID
  std::string user_name;  // user ID
  std::string user_uniform_name;
  int64_t created_at;       // UTC/GMT join meeting time
  bool is_host;             // Host or not
  bool is_sharing;          // shared screen or not
  bool is_mic_on;           // microphone on or not
  bool is_camera_on;        // camera on or not
  int audio_volume = 0;
};



struct MeetingRoom {
  std::string room_id;            // rom ID
  std::string host_uid;           // host ID
  std::string screen_shared_uid;  // screen sharer ID
  bool is_recording = false;      // recording on or not
  int64_t meeting_start_time;     // UTC/GMT, meeting start time
  int64_t now;                    // UTC/GMT current time
};

struct TokenInfo {
  std::string user_id;  // user ID
  std::string room_id;  // room ID
  std::string token;    // meeting token
  std::string app_id;   // app ID
};


struct RecordInfo {
  std::string room_id;
  std::string url;  // Conference recording download address
  std::string vid;
  bool video_holder = false;
  int64_t meeting_start_time;  // UTC/GMT meeting start time
};

struct AuditState {
  std::string url;
  bool state;
};


struct StreamInfo {
  int local_width;
  int local_height;
  int local_fps;
  int local_video_bit;
  int local_auido_bit;
  int video_rtt;
  int audio_rtt;
  int cpu_total;
  int cpu_use;
  int remote_video_bit;
  int remote_audio_bit;
  int remote_width_min;
  int remote_height_min;
  int remote_width_max;
  int remote_height_max;
  int remote_fps_min;
  int remote_fps_max;
  int remote_video_lose;
  int remote_audio_lose;
};

