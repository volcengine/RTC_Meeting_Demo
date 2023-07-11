#include "worker.h"
namespace meeting {
Worker& Worker::instance() {
  static Worker w;
  return w;
}

}  // namespace meeting
