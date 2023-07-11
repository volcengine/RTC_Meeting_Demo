#include "data_mgr.h"
namespace meeting {
DataMgr& DataMgr::instance() {
  static DataMgr mgr;
  return mgr;
}

void DataMgr::init() {
  instance().mute_video_ = false;
  instance().mute_audio_ = false;
  instance().record_ = false;
}

}  // namespace meeting
