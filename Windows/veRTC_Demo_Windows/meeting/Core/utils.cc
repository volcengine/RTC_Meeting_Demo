#include "meeting/Core/utils.h"

#include <QFontMetrics>

namespace utils {

    std::string getOsInfo() {
  // get os name according to version number
  OSVERSIONINFO osver = {sizeof(OSVERSIONINFO)};
  GetVersionEx(&osver);
  std::string os_name;
  if (osver.dwMajorVersion == 5 && osver.dwMinorVersion == 0)
    os_name = "Windows 2000";
  else if (osver.dwMajorVersion == 5 && osver.dwMinorVersion == 1)
    os_name = "Windows XP";
  else if (osver.dwMajorVersion == 6 && osver.dwMinorVersion == 0)
    os_name = "Windows 2003";
  else if (osver.dwMajorVersion == 5 && osver.dwMinorVersion == 2)
    os_name = "windows vista";
  else if (osver.dwMajorVersion == 6 && osver.dwMinorVersion == 1)
    os_name = "windows 7";
  else if (osver.dwMajorVersion == 6 && osver.dwMinorVersion == 2)
    os_name = "windows 10";

  std::ostringstream res;
  res << os_name << " " << osver.dwMajorVersion << '.' << osver.dwMinorVersion;
  return res.str();
}

QString elideText(const QFont& font, const QString& str, int width) {
  QFontMetrics mf(font);
  QString res = str;
  if (mf.horizontalAdvance(str) > width) {
    res = mf.elidedText(str, Qt::TextElideMode::ElideRight, width);
  }
  return res;
}

}  // namespace utils