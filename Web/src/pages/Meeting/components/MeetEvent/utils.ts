import { IVolume } from '@/app-interfaces';

/**
 * 合并数组，并保持上一次相对位置
 * @param oldList
 * @param newList
 * @returns
 */
export const getReleativeSortedList = (
  oldList: IVolume[],
  newList: IVolume[]
) => {
  const oldIdList = oldList.map((item) => item.userId);
  const newIdList = newList.map((item) => item.userId);

  const keepSortedList = Array.from(new Set([...oldIdList, ...newIdList]));

  return keepSortedList.reduce((pre, cur) => {
    const finded = newList.find((item) => item.userId === cur);

    if (finded) {
      pre.push(finded);
    }

    return pre;
  }, [] as IVolume[]);
};
