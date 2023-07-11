// Copyright (c) 2023 Beijing Volcano Engine Technology Ltd.
// SPDX-License-Identifier: MIT

package com.volcengine.vertcdemo.meeting.feature.participant;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.volcengine.vertcdemo.core.AudioVideoConfig;
import com.volcengine.vertcdemo.meeting.core.MeetingDataManager;
import com.volcengine.vertcdemo.meeting.core.MeetingRTCManager;
import com.volcengine.vertcdemo.meeting.bean.MeetingUserInfo;
import com.volcengine.vertcdemo.meeting.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * 参会者人员适配器
 */
public class ParticipantAdapter extends RecyclerView.Adapter<ParticipantAdapter.ParticipantViewHolder> {
    private static final String ON_VOLUME_CHANGED = "on_volume_changed";

    private final List<MeetingUserInfo> mList = new ArrayList<>();
    private final UserOptionCallback mUserOptionCallback;

    private Runnable mAutoRecoverRunnable;

    public ParticipantAdapter(List<MeetingUserInfo> list, UserOptionCallback userOptionCallback) {
        mList.addAll(sortUsers(list));
        mUserOptionCallback = userOptionCallback;
    }

    @NonNull
    @Override
    public ParticipantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_participant, parent, false);
        return new ParticipantViewHolder(view, mUserOptionCallback);
    }

    @Override
    public void onBindViewHolder(@NonNull ParticipantViewHolder holder, int position) {
        MeetingUserInfo entity = mList.get(position);
        bindView(holder, entity);
    }

    @Override
    public void onBindViewHolder(@NonNull ParticipantViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.contains(ON_VOLUME_CHANGED)) {
            bindVolume(holder, mList.get(position));
        } else {
            super.onBindViewHolder(holder, position, payloads);
        }
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    public void setData(List<MeetingUserInfo> list) {
        mList.clear();
        mList.addAll(sortUsers(list));
        notifyDataSetChanged();
    }

    public void onUserJoin(MeetingUserInfo info) {
        if (info == null || TextUtils.isEmpty(info.userId)) {
            return;
        }
        int index = mList.size();
        mList.add(info);
        notifyItemInserted(index);
    }

    public void onUserLeave(MeetingUserInfo info) {
        if (info == null || TextUtils.isEmpty(info.userId)) {
            return;
        }
        String uid = info.userId;
        for (int i = 0; i < mList.size(); i++) {
            if (uid.equals(mList.get(i).userId)) {
                mList.remove(i);
                notifyItemRemoved(i);
                return;
            }
        }
    }

    public void onMicStatusChanged(String uid, boolean status) {
        if (TextUtils.isEmpty(uid)) {
            return;
        }
        MeetingUserInfo info;
        for (int i = 0; i < mList.size(); i++) {
            info = mList.get(i);
            if (info != null && uid.equals(info.userId)) {
                info.isMicOn = status;
                notifyItemChanged(i);
            }
        }
    }

    public void onCameraStatusChanged(String uid, boolean status) {
        if (TextUtils.isEmpty(uid)) {
            return;
        }
        MeetingUserInfo info;
        for (int i = 0; i < mList.size(); i++) {
            info = mList.get(i);
            if (info != null && uid.equals(info.userId)) {
                info.isCameraOn = status;
                notifyItemChanged(i);
            }
        }
    }

    public void onHostChanged(String oldUid, String newUid) {
        MeetingUserInfo info;
        int hostAt = 0;
        for (int i = 0; i < mList.size(); i++) {
            info = mList.get(i);
            if (info != null) {
                if (oldUid != null && oldUid.equals(info.userId)) {
                    info.isHost = false;
                } else if (newUid != null && newUid.equals(info.userId)) {
                    info.isHost = true;
                    hostAt = i;
                }
            }
        }
        if (hostAt < mList.size()) {
            info = mList.remove(hostAt);
            mList.add(0, info);
        }
        notifyDataSetChanged();
    }

    public void onScreenShareChanged(String uid, boolean status) {
        if (TextUtils.isEmpty(uid)) {
            return;
        }
        MeetingUserInfo info;
        for (int i = 0; i < mList.size(); i++) {
            info = mList.get(i);
            if (info != null && uid.equals(info.userId)) {
                info.isSharing = status;
                notifyItemChanged(i);
            }
        }
    }

    public void onVolumeEvent(String uid, int volume) {
        if (TextUtils.isEmpty(uid)) {
            return;
        }
        MeetingUserInfo info;
        for (int i = 0; i < mList.size(); i++) {
            info = mList.get(i);
            if (info != null && uid.equals(info.userId)) {
                info.volume = volume;
                notifyItemChanged(i, ON_VOLUME_CHANGED);
            }
        }
    }

    public void onMuteAllEvent() {
        String hostUid = MeetingDataManager.getHostUid();
        MeetingUserInfo info;
        for (int i = 0; i < mList.size(); i++) {
            info = mList.get(i);
            if (info != null && !TextUtils.equals(hostUid, info.userId)) {
                info.isMicOn = false;
            }
        }
        notifyDataSetChanged();
    }

    private void bindView(ParticipantViewHolder holder, MeetingUserInfo userInfo) {
        holder.bindUserInfo(userInfo);
        String userName = userInfo.userName == null ? " " : userInfo.userName;
        holder.mPrefixTv.setText(userName.substring(0, 1));
        holder.mUidTv.setText(userName);
        holder.mHostTv.setVisibility(userInfo.isHost ? View.VISIBLE : View.GONE);
        holder.mScreenShare.setVisibility(userInfo.isSharing ? View.VISIBLE : View.GONE);
        if (mAutoRecoverRunnable != null) {
            holder.mAudioStatus.removeCallbacks(mAutoRecoverRunnable);
        }

        bindVolume(holder, userInfo);
        if (!userInfo.isMicOn && MeetingDataManager.getUserVolume(userInfo.userId) > AudioVideoConfig.VOLUME_MIN_THRESHOLD) {
            mAutoRecoverRunnable = () -> bindVolume(holder, userInfo);
            holder.mAudioStatus.postDelayed(mAutoRecoverRunnable, MeetingRTCManager.VOLUME_INTERVAL_MS);
        }

        if (userInfo.isCameraOn) {
            holder.mVideoStatus.setImageResource(R.drawable.camera_on);
        } else {
            holder.mVideoStatus.setImageResource(R.drawable.camera_off_red);
        }
    }

    private void bindVolume(ParticipantViewHolder holder, MeetingUserInfo entity) {
        if (!entity.isMicOn) {
            holder.mAudioStatus.setImageResource(R.drawable.mic_off_red);
            holder.mUserNameHighlight.setVisibility(View.GONE);
        } else if (MeetingDataManager.getUserVolume(entity.userId) > AudioVideoConfig.VOLUME_MIN_THRESHOLD) {
            holder.mAudioStatus.setImageResource(R.drawable.mic_active);
            holder.mUserNameHighlight.setVisibility(View.VISIBLE);
        } else {
            holder.mAudioStatus.setImageResource(R.drawable.mic_on);
            holder.mUserNameHighlight.setVisibility(View.GONE);
        }
    }

    private List<MeetingUserInfo> sortUsers(List<MeetingUserInfo> list) {
        List<MeetingUserInfo> result = new ArrayList<>();
        if (list == null || list.isEmpty()) {
            return result;
        }
        String hostUid = MeetingDataManager.getHostUid();
        String selfUid = MeetingDataManager.getSelfId();
        String loudestUid = MeetingDataManager.getLoudestUid();
        MeetingUserInfo host = null;
        MeetingUserInfo mine = null;
        MeetingUserInfo loudest = null;
        List<MeetingUserInfo> cameraOnMicOn = new ArrayList<>();
        List<MeetingUserInfo> cameraOffMicOn = new ArrayList<>();
        List<MeetingUserInfo> cameraOnMicOff = new ArrayList<>();
        List<MeetingUserInfo> cameraOffMicOff = new ArrayList<>();
        for (MeetingUserInfo info : list) {
            if (TextUtils.equals(info.userId, hostUid)) {
                host = info;
            } else if (TextUtils.equals(info.userId, selfUid)) {
                mine = info;
            } else if (TextUtils.equals(info.userId, loudestUid)) {
                loudest = info;
            } else if (info.isMicOn && info.isCameraOn) {
                cameraOnMicOn.add(info);
            } else if (info.isMicOn) {
                cameraOffMicOn.add(info);
            } else if (info.isCameraOn) {
                cameraOnMicOff.add(info);
            } else {
                cameraOffMicOff.add(info);
            }
        }
        if (host != null) {
            result.add(host);
        }
        if (mine != null) {
            result.add(mine);
        }
        if (loudest != null) {
            result.add(loudest);
        }
        result.addAll(sortByDictOrder(cameraOnMicOn));
        result.addAll(sortByDictOrder(cameraOffMicOn));
        result.addAll(sortByDictOrder(cameraOnMicOff));
        result.addAll(sortByDictOrder(cameraOffMicOff));

        return result;
    }

    private List<MeetingUserInfo> sortByDictOrder(List<MeetingUserInfo> list) {
        MeetingUserInfo[] array = list.toArray(new MeetingUserInfo[]{});
        Arrays.sort(array, mComparator);
        return new ArrayList<>(Arrays.asList(array));
    }

    private final Comparator<MeetingUserInfo> mComparator = new Comparator<MeetingUserInfo>() {

        @Override
        public int compare(MeetingUserInfo o1, MeetingUserInfo o2) {
            String uid1 = o1.userUniformName == null ? "" : o1.userUniformName;
            String uid2 = o2.userUniformName == null ? "" : o2.userUniformName;
            return compareString(uid1, uid2);
        }

        private int compareString(String o1, String o2) {
            char[] chars1 = o1.toLowerCase().toCharArray();
            char[] chars2 = o2.toLowerCase().toCharArray();
            int i = 0;
            while (i < chars1.length && i < chars2.length) {
                if (chars1[i] > chars2[i]) {
                    return 1;
                } else if (chars1[i] < chars2[i]) {
                    return -1;
                } else {
                    i++;
                }
            }
            if (i == chars1.length) {
                return -1;
            }
            if (i == chars2.length) {
                return 1;
            }
            return 0;
        }
    };

    static class ParticipantViewHolder extends RecyclerView.ViewHolder {

        private MeetingUserInfo mUserInfo;
        public TextView mPrefixTv;
        public TextView mUidTv;
        public TextView mHostTv;
        public ImageView mScreenShare;
        public View mUserNameHighlight;
        private final ImageView mVideoStatus;
        private final ImageView mAudioStatus;
        private final UserOptionCallback mUserOptionCallback;

        public ParticipantViewHolder(@NonNull View itemView, UserOptionCallback userOptionCallback) {
            super(itemView);
            mPrefixTv = itemView.findViewById(R.id.user_prefix);
            mUidTv = itemView.findViewById(R.id.user_txt_tv);
            mHostTv = itemView.findViewById(R.id.user_host_tv);
            mUserNameHighlight = itemView.findViewById(R.id.user_name_highlight);
            mScreenShare = itemView.findViewById(R.id.user_share_screen_status);
            mVideoStatus = itemView.findViewById(R.id.user_right_iv1);
            mAudioStatus = itemView.findViewById(R.id.user_right_iv2);
            mUserOptionCallback = userOptionCallback;
            itemView.setOnLongClickListener(v -> {
                if (mUserOptionCallback != null) {
                    mUserOptionCallback.onLongClick(mUserInfo);
                }
                return false;
            });
        }

        public void bindUserInfo(MeetingUserInfo userInfo) {
            mUserInfo = userInfo;
        }
    }

    public interface UserOptionCallback {
        void onLongClick(MeetingUserInfo userInfo);
    }
}