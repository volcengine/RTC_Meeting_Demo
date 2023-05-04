import React, { useEffect, useState } from 'react';
import { Modal, Button } from 'antd';
import styles from './index.less';

type AutoPlayModalProps = {
  handleAutoPlay: () => void;
  autoPlayFailUser: string[];
};

const AutoPlayModal: React.FC<AutoPlayModalProps> = ({
  handleAutoPlay,
  autoPlayFailUser,
}) => {
  const [visible, setVisible] = useState<boolean>(false);

  useEffect(() => {
    setVisible(autoPlayFailUser.length ? true : false);
  }, [autoPlayFailUser]);

  return (
    <>
      <Modal
        className={styles.container}
        footer={null}
        closable={false}
        width={450}
        open={visible}
        style={{ top: '20%', minHeight: 150 }}
      >
        <Button
          className={styles.button}
          block
          size="large"
          onClick={handleAutoPlay}
        >
          自动播放
        </Button>
      </Modal>
    </>
  );
};

export default AutoPlayModal;
