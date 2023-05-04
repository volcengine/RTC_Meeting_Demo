import React, { useEffect, useRef } from 'react';

import styles from './index.less';

interface IGalleryViewProps {
  views: React.ReactNode[];
}

const getRowCount = (total: number) => {
  if (total >= 7) {
    return 3;
  }
  if (total >= 3) {
    return 2;
  }
  return 1;
};

const getColCount = (total: number) => {
  if (total >= 5) {
    return 3;
  }
  if (total >= 2) {
    return 2;
  }
  return 1;
};

const setSize = (ele: HTMLElement, width: number, height: number) => {
  ele.style.minWidth = `${width}px`;
  ele.style.minHeight = `${height}px`;
  ele.style.maxWidth = `${width}px`;
  ele.style.maxHeight = `${height}px`;
};

const FLEX_GAP = 8;

const GalleryView: React.FC<IGalleryViewProps> = ({ views }) => {
  const layoutRef = useRef<HTMLDivElement>(null);
  const playersRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const resize = () => {
      const layoutDom = layoutRef.current!;

      if (!layoutDom.offsetHeight) {
        return;
      }

      const Height = layoutDom.offsetHeight;
      const Width = layoutDom.offsetWidth;

      const rows = getRowCount(views.length) || 1;
      const cols = getColCount(views.length) || 1;

      const w = (Width - (cols - 1) * FLEX_GAP) / cols;

      const h = (Height - (rows - 1) * FLEX_GAP) / rows;
      // 默认宽度有剩余可以放下16 / 9  的视频区
      let eleHeight = h;
      let eleWidth = (h / 9) * 16;
      if (w / h < 16 / 9) {
        eleWidth = w;
        eleHeight = (w / 16) * 9;
      }
      Array.prototype.forEach.call(playersRef.current?.children, (ele) => {
        setSize(ele as HTMLDivElement, eleWidth, eleHeight);
      });
    };

    window.addEventListener('resize', resize);

    if (layoutRef.current) {
      resize();
    }

    return () => {
      window.removeEventListener('resize', resize);
    };
  }, [views]);

  return (
    <div
      className={styles.playerLayout}
      style={{
        width: '100%',
        height: '100%',
      }}
      ref={layoutRef}
    >
      <div className={styles.playerContainer} ref={playersRef}>
        {views}
      </div>
    </div>
  );
};

export default GalleryView;
