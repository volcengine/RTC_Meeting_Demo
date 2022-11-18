#pragma once
#include <QLabel>
#include <QStackedWidget>
#include <QWidget>

enum class UserRole { kAudience, kMe, kBroadCast };

class VideoWidget : public QWidget {
public:
    VideoWidget(QWidget* parent = nullptr);
    ~VideoWidget() = default;
    void setUserRole(UserRole role);
    void setUserName(const QString & str);
    void setShare(bool enabled);
    void setHasVideo(bool has_video);
    void* getWinID();
    void setVideoUpdateEnabled(bool enabled);
    void hideVideo();
    void showVideo();
    void setHighLight(bool enabled);
    QPaintEngine* paintEngine() const { return nullptr; }

    UserRole user_role();
    void setUserCount(int cnt,bool fixed_size = false);
private:
    QStackedWidget* stacked_widget_;
    UserRole role_;

class NoVideoWidget : public QWidget {
public:
    NoVideoWidget(QWidget* parent = nullptr);
    ~NoVideoWidget() = default;

    void setUserCount(int cnt, bool fixedSize);
    void setUserName(const QString& str);
    void setShare(bool enabled);
    void setUserRole(UserRole role);
    void setHighLight(bool enabled);
private:
    bool higth_light_ = false;
    int cnt_ = 0;
    bool fixed_size_ = false;
    UserRole user_role_;
    QLabel* lbl_user_logo_;
    QLabel* lbl_share_logo_;
    QLabel* lbl_user_name_;
    QLabel* lbl_user_role_;
};

class HasVideoWidget : public QWidget {
public:
    HasVideoWidget(QWidget* parent = nullptr);
    ~HasVideoWidget() = default;

    void* getVideoWinID();
    void setVideoUpdateEnabled(bool enabled);

    void setUserName(const QString& str);

    void hideVideo();
    void showVideo();

    void setHighLight(bool enabled);
    void setShare(bool enabled);
    void setUserRole(UserRole role);

protected:
    void resizeEvent(QResizeEvent* e) override;
    void showEvent(QShowEvent*) override;
    void moveEvent(QMoveEvent*) override;

private:
    UserRole user_role_;
    QWidget* info_content_;
    QWidget* video_;
    QWidget* info_;
    QLabel* lbl_share_logo_;
    QLabel* lbl_user_name_;
    QLabel* lbl_user_role_;
};

};
