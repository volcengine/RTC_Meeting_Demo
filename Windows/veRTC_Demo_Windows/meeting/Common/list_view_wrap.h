#pragma once
#include <QListView>
#include <QPixMap>
#include <QPushButton>
#include <QStandardItemModel>
#include <QStyledItemDelegate>

struct HistoryItemData {
  QString str;
  QString url;
  QString vid;
};

class ListItemDeletegate : public QStyledItemDelegate {
 public:
  enum {
    kInfo = 1 + Qt::UserRole,
    kPushButtonEnabled,
    kPushButtonClicked,
    kPushButtonRect
  };

 public:
  ListItemDeletegate(QObject *parent);
  ~ListItemDeletegate() = default;
  void paint(QPainter *painter, const QStyleOptionViewItem &option,
             const QModelIndex &index) const;

  bool editorEvent(QEvent *event, QAbstractItemModel *model,
                   const QStyleOptionViewItem &option,
                   const QModelIndex &index);

  QSize sizeHint(const QStyleOptionViewItem &option,
                 const QModelIndex &index) const;

 private:
  QPixmap bmp_distbin_;
};

class ListViewWrap : public QWidget {
  Q_OBJECT

 public:
  enum class Mode {
      NoButton,
      HasButton
  };
 public:
  explicit ListViewWrap(QWidget *parent = nullptr);
  ~ListViewWrap() = default;
  void addItem(const QString &str, const QString &url, const QString & vid);
  void removeItem(const QString & vid);
  void setButtonMode(Mode mode);
  void clear();
 signals:
  void sigItemClicked(HistoryItemData data);
  void sigButtonClicked(HistoryItemData data);

 protected:
  void focusInEvent(QFocusEvent *) override;
  void focusOutEvent(QFocusEvent *) override;
  void paintEvent(QPaintEvent *) override;

 private:
  Mode button_mode_;
  QListView *list_view_ = nullptr;
  QStandardItemModel *item_model_ = nullptr;
};
