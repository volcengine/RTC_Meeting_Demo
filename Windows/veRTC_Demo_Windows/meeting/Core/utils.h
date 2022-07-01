#pragma once
#include <windows.h>
#include <sstream>
#include "meeting_model.h"
#include <QString>
#include <QFont>

namespace utils {

std::string getOsInfo();

QString elideText(const QFont& font, const QString& str, int width);


}  // namespace utils
