#pragma once
#include <qobject.h>

#include <memory>
#include "meeting/Feature/login/meeting_login.h"
#include "meeting/common/meeting_setting.h"
#include "meeting/common/share_screen_widget.h"
#include "meeting/common/side_widget.h"
#include "meeting/Common/video_widget.h"
#include "meeting/feature/main/global_button_bar.h"
#include "meeting/feature/main/main_page.h"

namespace meeting {
    static constexpr int kMaxShowWidgetNum = 9;

class PageManager : public QObject {
    Q_OBJECT

public:
    PageManager() = default;
    ~PageManager() = default;

    static PageManager& instance();
    static void init();
    static void showLogin(QWidget* parent = nullptr);
    static void showTips(QWidget* parent = nullptr);
    static void showSetting(QWidget* parent = nullptr);
    static void showShareWidget(QWidget* parent = nullptr);
    static void showUserListWidget(QWidget* parent = nullptr);
    static void hideUserListWidget();

    static int showCallExpDlg(QWidget* parent = nullptr);
    static void setLocalVideoWidget(const User& user, int idx, int cnt);
    static void setRemoteVideoWidget(const User& user, int idx, int cnt);
    static void setRemoteScrrenVideoWidget(const User& user);

    static void initRoom();
    static void showRoom();
    static QWidget* currentWidget();
    static void showGlobalBar();
    static void hideGlobalBar();
    static void hideRoom();
    static void setGlobalBarEvent(QWidget* listener);
    static void unSetGlobalBarEvent(QWidget* listener);
    static std::vector<std::shared_ptr<VideoWidget>> getVideoList();
    static std::shared_ptr<VideoWidget> getCurrentVideo();
    static std::shared_ptr<VideoWidget> getScreenVideo();
    static void updateData();
    static void handleCacheUsers();
    static void sortUsers(std::vector<User>& users);
    static void meetingNotify();
    static void stopScreen();

protected:
    void customEvent(QEvent*) override;

signals:
    void sigReturnMainPage();

private:
    std::unique_ptr<MeetingLoginWidget> meeting_login_widget_;
    std::unique_ptr<MeetingSetting> setting_page_;
    std::unique_ptr<ShareScreenWidget> share_widget_;
    std::unique_ptr<MainPage> main_page_;
    std::unique_ptr<SideWidget> user_list_;
    std::unique_ptr<GlobalButtonBar> button_bar_;
    std::vector<MeetingUserInfo> infos_;
    std::vector<std::shared_ptr<VideoWidget>> videos_;
    std::shared_ptr<VideoWidget> screen_widget_;
    QWidget* current_widget_ = nullptr;
    bool updating = false;
};

}  // namespace meeting
