#include <QHBoxLayout>
#include <QResizeEvent>
#include <unordered_map>
#include <iostream>

#include "video_widget.h"

struct VideoWidgetInfo {
  int user_logo_font_size;
  int font_size;
  int user_logo_width;
  int user_logo_height;
  int info_height;
};

static std::unordered_map<int, VideoWidgetInfo> g_video_config = {
    {0, {80, 16, 200, 200, 24}}, {1, {80, 16, 200, 200, 24}},
    {2, {36, 16, 80, 80, 24}},   {3, {36, 16, 80, 80, 24}},
    {4, {36, 16, 80, 80, 24}},   {5, {24, 14, 64, 64, 22}},
    {6, {24, 14, 64, 64, 22}},   {7, {22, 12, 40, 40, 22}},
    {8, {22, 12, 40, 40, 22}},   {9, {22, 12, 40, 40, 22}}};


VideoWidget::HasVideoWidget::HasVideoWidget(QWidget* parent /*= nullptr*/) 
    : QWidget(parent) {
    this->setObjectName("HasVideoWidget");
    video_ = new QWidget(this);
    info_ = new QWidget(this);
    lbl_share_logo_ = new QLabel(info_);
    lbl_user_name_ = new QLabel(info_);
    lbl_user_role_ = new QLabel(info_);
    info_content_ = new QWidget(this);
    info_content_->setLayout(new QHBoxLayout(this));
    info_content_->layout()->addWidget(info_);
    info_content_->setMinimumHeight(20);
    info_content_->setMaximumHeight(20);
    info_->setMinimumHeight(20);
    auto hbox_layout = new QHBoxLayout(this);
    hbox_layout->addWidget(lbl_share_logo_);
    hbox_layout->addWidget(lbl_user_name_);
    hbox_layout->addWidget(lbl_user_role_);
    lbl_user_name_->setVisible(false);
    lbl_user_role_->setVisible(false);
    lbl_share_logo_->setVisible(false);
    info_->setLayout(hbox_layout);
    info_->layout()->setContentsMargins(8, 0, 8, 0);
    info_->layout()->setSpacing(8);
    info_content_->layout()->setSpacing(0);
    info_content_->layout()->setContentsMargins(0, 0, 2, 2);
    info_->setStyleSheet(
        "background:#1d2129;"
        "border-radius:2px;"
        "font-family: \"Microsoft YaHei\";\n"
        "font-size: 12px;\n");
    video_->setStyleSheet("background:#272e3B;");
    lbl_user_role_->setStyleSheet(
        "background:#1D2129; border-radius:2px; padding:2px; color:#4080FF;");
    lbl_share_logo_->setFixedSize(16, 16);
    setShare(false);
}

void* VideoWidget::HasVideoWidget::getVideoWinID() { 
    return reinterpret_cast<void*>(video_->winId()); 
}

void VideoWidget::HasVideoWidget::setVideoUpdateEnabled(bool enabled) {
    video_->setUpdatesEnabled(enabled);
}

void VideoWidget::HasVideoWidget::setUserName(const QString& str) {
    lbl_user_name_->setText(str);
    lbl_user_name_->setVisible(true);
    info_content_->resize(info_content_->layout()->sizeHint());
}

void VideoWidget::HasVideoWidget::hideVideo() { 
    video_->hide(); 
}
void VideoWidget::HasVideoWidget::showVideo() { 
    video_->show(); 
}

void VideoWidget::HasVideoWidget::setHighLight(bool enabled) {
    if (enabled) {
        setStyleSheet("#HasVideoWidget{border:2px solid #4080FF;}");
    }
    else {
        setStyleSheet("#HasVideoWidget{border:none}");
    }
}

void VideoWidget::HasVideoWidget::setShare(bool enabled) {
    lbl_share_logo_->setVisible(enabled);
    lbl_share_logo_->setStyleSheet(
        "background-image:url(:img/meeting_sharing);"
        "background-position:right;"
        "background-repeat:no-repeat;"
        "background-origin:content;");
    info_content_->move(size().width() - info_content_->width() - 2,
        height() - 22);
}

void VideoWidget::HasVideoWidget::setUserRole(UserRole role) {
    user_role_ = role;
    switch (user_role_) {
    case UserRole::kMe:
        lbl_user_role_->setText("我");
        lbl_user_role_->setVisible(true);
        break;
    case UserRole::kBroadCast:
        lbl_user_role_->setText("主持人");
        lbl_user_role_->setVisible(true);
        break;
    case UserRole::kAudience:
        lbl_user_role_->setVisible(false);
        break;
    default:
        break;
    }
    info_content_->resize(info_content_->layout()->sizeHint());
    info_content_->move(size().width() - info_content_->width() - 2,
        height() - 22);
}

void VideoWidget::HasVideoWidget::resizeEvent(QResizeEvent* e) {
    info_content_->move(e->size().width() - info_content_->width() - 2,
        height() - 22);
    video_->setGeometry(2, 2, e->size().width() - 4, e->size().height() - 4);
}

void VideoWidget::HasVideoWidget::showEvent(QShowEvent*) {
    info_content_->move(size().width() - info_content_->width() - 2,
        height() - 22);
}

void VideoWidget::HasVideoWidget::moveEvent(QMoveEvent*) {}

VideoWidget::NoVideoWidget::NoVideoWidget(QWidget* parent /*= nullptr*/) 
    : QWidget(parent) {
    user_role_ = UserRole::kAudience;
    setStyleSheet(
        "background:#272E3B;"
        "font-family: \"Microsoft YaHei\";\n"
        "font-size: 12px;\n");
    lbl_user_logo_ = new QLabel(this);
    lbl_share_logo_ = new QLabel(this);
    lbl_user_name_ = new QLabel(this);
    lbl_user_role_ = new QLabel(this);

    auto p = new QVBoxLayout();
    setLayout(p);
    p->addItem(
        new QSpacerItem(1, 1, QSizePolicy::Expanding, QSizePolicy::Expanding));
    auto logo_layout = new QHBoxLayout();
    logo_layout->setContentsMargins(0, 0, 0, 0);
    logo_layout->setSpacing(0);
    logo_layout->addWidget(lbl_user_logo_);
    p->addItem(logo_layout);
    auto hbox_layout = new QHBoxLayout();
    hbox_layout->addItem(
        new QSpacerItem(1, 1, QSizePolicy::Expanding, QSizePolicy::Expanding));
    hbox_layout->addWidget(lbl_share_logo_);
    hbox_layout->addWidget(lbl_user_name_);
    hbox_layout->addWidget(lbl_user_role_);

    hbox_layout->addItem(
        new QSpacerItem(1, 1, QSizePolicy::Expanding, QSizePolicy::Expanding));
    hbox_layout->setContentsMargins(0, 0, 0, 0);
    hbox_layout->setSpacing(8);
    p->addItem(hbox_layout);
    p->addItem(
        new QSpacerItem(1, 1, QSizePolicy::Expanding, QSizePolicy::Expanding));

    p->setContentsMargins(0, 0, 0, 0);
    p->setSpacing(0);
    lbl_user_name_->setText("test");
    lbl_user_logo_->setFixedSize(40, 40);
    lbl_user_name_->setMinimumHeight(20);
    lbl_user_name_->setMaximumHeight(20);
    lbl_share_logo_->setFixedSize(20, 20);
    lbl_user_role_->setMinimumHeight(20);
    lbl_user_role_->setMaximumHeight(20);

    lbl_user_role_->setStyleSheet(
        "background:#1d2129;border-radius:2px; padding-left:4px; "
        "padding-right:4px; padding-bottom:2px; color:#4080FF;");

    lbl_share_logo_->setVisible(false);
    lbl_user_name_->setVisible(false);
    lbl_user_role_->setVisible(false);

    lbl_user_logo_->setText("B");
    lbl_user_logo_->setAlignment(Qt::AlignCenter);
    lbl_user_logo_->setStyleSheet(
        "border-radius:20px; background:#4E5969;border: 2px solid #4080FF;");
}

void VideoWidget::NoVideoWidget::setUserCount(int cnt, bool fixedSize) {
    if (fixedSize) {
        cnt = 9;
    }
    fixed_size_ = fixedSize;
    if (higth_light_) {
        lbl_user_logo_->setStyleSheet(
            QString(
                "border-radius:%1px;font-family:Microsoft YaHei; font-size:%2px; "
                "background:#4e5969;border:2px solid #4080FF;")
            .arg(g_video_config[cnt].user_logo_width / 2)
            .arg(g_video_config[cnt].user_logo_font_size));
    }
    else {
        lbl_user_logo_->setStyleSheet(
            QString("border-radius:%1px;font-family:Microsoft YaHei; font-size:%2px; "
                "background:#4e5969;")
            .arg(g_video_config[cnt].user_logo_width / 2)
            .arg(g_video_config[cnt].user_logo_font_size));
    }

    lbl_user_logo_->setFixedSize(g_video_config[cnt].user_logo_width,
        g_video_config[cnt].user_logo_height);
    setStyleSheet(QString("background:#272E3B;"
        "font-family: \"Microsoft YaHei\";\n"
        "font-size: %1px;\n")
        .arg(g_video_config[cnt].font_size));

    lbl_user_role_->setMinimumHeight(g_video_config[cnt].info_height);
    lbl_user_role_->setMaximumHeight(g_video_config[cnt].info_height);
    lbl_share_logo_->setMinimumHeight(g_video_config[cnt].info_height);
    lbl_share_logo_->setMaximumHeight(g_video_config[cnt].info_height);
    lbl_user_name_->setMinimumHeight(g_video_config[cnt].info_height);
    lbl_user_name_->setMaximumHeight(g_video_config[cnt].info_height);
    cnt_ = cnt;
}

void VideoWidget::NoVideoWidget::setUserName(const QString& str) {
    lbl_user_name_->setText(str);
    lbl_user_logo_->setText(str.left(1).toUpper());
    lbl_user_name_->setVisible(true);
}

void VideoWidget::NoVideoWidget::setShare(bool enabled) {
    lbl_share_logo_->setVisible(enabled);
    lbl_share_logo_->setStyleSheet(
        "background-image:url(:img/meeting_sharing);"
        "background-position:right;"
        "background-repeat:no-repeat;"
        "background-origin:content;");
}

void VideoWidget::NoVideoWidget::setUserRole(UserRole role) {
    user_role_ = role;
    switch (user_role_) {
    case UserRole::kMe:
        lbl_user_role_->setText("我");
        lbl_user_role_->setVisible(true);
        break;
    case UserRole::kBroadCast:
        lbl_user_role_->setText("主持人");
        lbl_user_role_->setVisible(true);
        break;
    case UserRole::kAudience:
        lbl_user_role_->setVisible(false);
        break;
    default:
        break;
    }
}

void VideoWidget::NoVideoWidget::setHighLight(bool enabled) {
    higth_light_ = enabled;
    setUserCount(cnt_, fixed_size_);
}

VideoWidget::VideoWidget(QWidget* parent)
    : QWidget(parent), role_(UserRole::kAudience) {
  setLayout(new QHBoxLayout());
  layout()->setContentsMargins(0, 0, 0, 0);
  layout()->setSpacing(0);

  stacked_widget_ = new QStackedWidget(this);
  stacked_widget_->addWidget(new HasVideoWidget(this));
  stacked_widget_->addWidget(new NoVideoWidget(this));
  stacked_widget_->setCurrentIndex(0);
  stacked_widget_->setContentsMargins(0, 0, 0, 0);
  setUserRole(role_);
  layout()->addWidget(stacked_widget_);
}

void VideoWidget::setUserRole(UserRole role) {
  static_cast<HasVideoWidget*>(stacked_widget_->widget(0))->setUserRole(role);
  static_cast<NoVideoWidget*>(stacked_widget_->widget(1))->setUserRole(role);
}

void VideoWidget::setUserName(const QString& str) {
  static_cast<HasVideoWidget*>(stacked_widget_->widget(0))->setUserName(str);
  static_cast<NoVideoWidget*>(stacked_widget_->widget(1))->setUserName(str);
}

void VideoWidget::setShare(bool enabled) {
  static_cast<HasVideoWidget*>(stacked_widget_->widget(0))->setShare(enabled);
  static_cast<NoVideoWidget*>(stacked_widget_->widget(1))->setShare(enabled);
}

void VideoWidget::setHasVideo(bool has_video) {
  stacked_widget_->setCurrentIndex(!has_video);
}

void* VideoWidget::getWinID() {
  return static_cast<HasVideoWidget*>(stacked_widget_->widget(0))
      ->getVideoWinID();
}

void VideoWidget::setVideoUpdateEnabled(bool enabled) {
  static_cast<HasVideoWidget*>(stacked_widget_->widget(0))
      ->setVideoUpdateEnabled(enabled);
}

void VideoWidget::hideVideo() {
  static_cast<HasVideoWidget*>(stacked_widget_->widget(0))->hideVideo();
}

void VideoWidget::showVideo() {
  static_cast<HasVideoWidget*>(stacked_widget_->widget(0))->showVideo();
}

void VideoWidget::setHighLight(bool enabled) {
  static_cast<HasVideoWidget*>(stacked_widget_->widget(0))
      ->setHighLight(enabled);
  static_cast<NoVideoWidget*>(stacked_widget_->widget(1))
      ->setHighLight(enabled);
}

UserRole VideoWidget::user_role() { return role_; }

void VideoWidget::setUserCount(int cnt,bool fixedSize) {
  if (cnt > 9) cnt = 9;
  static_cast<NoVideoWidget*>(stacked_widget_->widget(1))
      ->setUserCount(cnt, fixedSize);
}
