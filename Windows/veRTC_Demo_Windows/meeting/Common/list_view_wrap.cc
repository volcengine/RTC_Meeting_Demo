#include "list_view_wrap.h"

#include <QApplication>
#include <QBitmap>
#include <QEvent>
#include <QHBoxLayout>
#include <QMouseEvent>
#include <QPainter>
#include <QStyleOption>
#include <iostream>

#include "list_view_wrap.h"
#if DEBUG
#include <iostream>
#endif

Q_DECLARE_METATYPE(HistoryItemData)
Q_DECLARE_METATYPE(ListViewWrap::Mode)

ListViewWrap::ListViewWrap(QWidget *parent) : QWidget(parent) {
  list_view_ = new QListView(this);
  setWindowFlags(Qt::Popup | Qt::FramelessWindowHint |
                 Qt::NoDropShadowWindowHint);
  setAttribute(Qt::WA_TranslucentBackground);
  setLayout(new QHBoxLayout(this));
  setContentsMargins(0, 0, 0, 0);
  layout()->addWidget(list_view_);
  layout()->setContentsMargins(0, 0, 0, 0);
  item_model_ = new QStandardItemModel(this);
  list_view_->setModel(item_model_);
  list_view_->setViewMode(QListView::ViewMode::ListMode);
  list_view_->setItemDelegate(new ListItemDeletegate(this));
  list_view_->setHorizontalScrollBarPolicy(Qt::ScrollBarAlwaysOff);
  list_view_->setHorizontalScrollMode(
      QAbstractItemView::ScrollMode::ScrollPerPixel);
  list_view_->setVerticalScrollMode(
      QAbstractItemView::ScrollMode::ScrollPerPixel);
  list_view_->setVerticalScrollBarPolicy(Qt::ScrollBarAlwaysOff);
  list_view_->setEditTriggers(QAbstractItemView::NoEditTriggers);
  list_view_->setMouseTracking(true);

  connect(list_view_, &QListView::clicked, this, [=](const QModelIndex &index) {
    if (index.isValid()) {
      if (index.data(ListItemDeletegate::kPushButtonClicked).toBool() &&
          button_mode_ == Mode::HasButton) {
        emit sigButtonClicked(
            index.data(ListItemDeletegate::kInfo).value<HistoryItemData>());
      } else {
        emit sigItemClicked(
            index.data(ListItemDeletegate::kInfo).value<HistoryItemData>());
      }

      hide();
    }
  });
  list_view_->setMinimumWidth(320);
  button_mode_ = Mode::NoButton;
}

void ListViewWrap::addItem(const QString &str, const QString &url,
                           const QString &vid) {
  QStandardItem *item = new QStandardItem();
  item->setData(
      QVariant::fromValue<HistoryItemData>(HistoryItemData{str, url, vid}),
      ListItemDeletegate::kInfo);
  if (button_mode_ == Mode::HasButton) {
    item->setData(QVariant::fromValue(button_mode_),
                  ListItemDeletegate::kPushButtonEnabled);
  }
  item_model_->appendRow(item);
}

void ListViewWrap::removeItem(const QString &vid) {
  item_model_->rowCount();
  for (int i = 0; i < item_model_->rowCount(); i++) {
    auto item = item_model_->item(i);
    if (item->data().value<HistoryItemData>().vid == vid)
      item_model_->removeRow(i);
  }
}

void ListViewWrap::setButtonMode(ListViewWrap::Mode mode) {
  button_mode_ = mode;
  for (int i = 0; i < item_model_->rowCount(); i++) {
    item_model_->item(i)->setData(QVariant::fromValue(mode),
                                  ListItemDeletegate::kPushButtonEnabled);
  };
}

void ListViewWrap::clear() { item_model_->clear(); }

void ListViewWrap::focusInEvent(QFocusEvent *) {
#if DEBUG
  std::cout << "focusInEvent" << std::endl;
#endif
}

void ListViewWrap::focusOutEvent(QFocusEvent *) {
#if DEBUG
  std::cout << "focusOutEvent" << std::endl;
#endif
}

void ListViewWrap::paintEvent(QPaintEvent *e) {}

ListItemDeletegate::ListItemDeletegate(QObject *parent)
    : QStyledItemDelegate(parent) {
  bmp_distbin_ = QPixmap(":img/meeting/dustbin");
}

void ListItemDeletegate::paint(QPainter *painter,
                               const QStyleOptionViewItem &option,
                               const QModelIndex &index) const {
  if (index.isValid()) {
    painter->save();
    painter->setRenderHint(QPainter::Antialiasing);
    QVariant variant = index.data(ListItemDeletegate::kInfo);
    HistoryItemData data = variant.value<HistoryItemData>();

    QStyleOptionViewItem viewOption(option);

    QRectF rect;
    rect.setX(option.rect.x());
    rect.setY(option.rect.y());
    rect.setWidth(option.rect.width() - 1);
    rect.setHeight(option.rect.height() - 1);
    painter->setPen(Qt::NoPen);
    if (option.state.testFlag(QStyle::State_Selected) ||
        option.state.testFlag(QStyle::State_MouseOver)) {
      painter->setBrush(QColor(0xF2, 0xF3, 0xF8));
      painter->drawRect(rect);
    } else {
      painter->setBrush(QColor(0xFF, 0xFF, 0xFF));
      painter->drawRect(rect);
    }
    QRect NameRect =
        QRect(rect.left() + 10, rect.top(), rect.width(), rect.height());
    painter->setPen(QPen(Qt::black));
    painter->setFont(QFont("Microsoft YaHei", 12));
    painter->drawText(NameRect, Qt::AlignLeft | Qt::AlignVCenter, data.str);
    if (option.state.testFlag(QStyle::State_Selected) ||
        option.state.testFlag(QStyle::State_MouseOver)) {
      if (index.data(ListItemDeletegate::kPushButtonEnabled)
              .value<ListViewWrap::Mode>() == ListViewWrap::Mode::HasButton) {
        QRect bmpRect =
            QRect(rect.width() - bmp_distbin_.width() - 10 + rect.x(),
                  (rect.height() - bmp_distbin_.height()) / 2 + rect.y(),
                  bmp_distbin_.width(), bmp_distbin_.height());
        painter->drawPixmap(bmpRect, bmp_distbin_);
      }
    }
    painter->restore();
  }
}

QSize ListItemDeletegate::sizeHint(const QStyleOptionViewItem &option,
                                   const QModelIndex &index) const {
  return QSize(160, 32);
}

bool ListItemDeletegate::editorEvent(QEvent *event, QAbstractItemModel *model,
                                     const QStyleOptionViewItem &option,
                                     const QModelIndex &index) {
  if (event->type() == QEvent::MouseMove) {
  }
  if (event->type() == QEvent::MouseButtonPress ||
      event->type() == QEvent::MouseButtonDblClick) {
    QStyleOptionViewItem viewOption(option);
    QRectF rect;
    rect.setX(option.rect.x());
    rect.setY(option.rect.y());
    rect.setWidth(option.rect.width() - 1);
    rect.setHeight(option.rect.height() - 1);
    QRect bmpRect =
        QRect(rect.width() - bmp_distbin_.width() - 10 + rect.x(),
              (rect.height() - bmp_distbin_.height()) / 2 + rect.y(),
              bmp_distbin_.width(), bmp_distbin_.height());
    if (bmpRect.contains(static_cast<QMouseEvent *>(event)->pos()))
      model->setData(index, QVariant::fromValue(true), kPushButtonClicked);
  }
  if (event->type() == QEvent::MouseButtonRelease) {
  }
  return true;
}
