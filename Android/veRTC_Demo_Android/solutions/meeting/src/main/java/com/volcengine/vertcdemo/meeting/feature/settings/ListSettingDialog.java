// Copyright (c) 2023 Beijing Volcano Engine Technology Ltd.
// SPDX-License-Identifier: MIT

package com.volcengine.vertcdemo.meeting.feature.settings;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.volcengine.vertcdemo.common.WindowUtils;
import com.volcengine.vertcdemo.meeting.R;

import java.util.ArrayList;
import java.util.List;

public class ListSettingDialog extends AppCompatDialog {
    private final int COLOR_SELECTED = Color.parseColor("#E5E6EB");
    private final int COLOR_UNSELECTED = Color.parseColor("#86909C");
    private final ListDialogListener mDialogListener;
    private final View mView;
    private int mSelectIndex;
    private final List<String> mData = new ArrayList<>();
    private final RecyclerView mRecyclerView;

    public ListSettingDialog(Context context, List<String> data, int defaultIndex, String title, ListSettingDialog.ListDialogListener dialogListener) {
        super(context, R.style.SolutionCommonDialog);
        this.setCancelable(true);
        this.mDialogListener = dialogListener;
        this.mData.add("");
        this.mData.addAll(data);
        this.mData.add("");
        this.mSelectIndex = defaultIndex;
        LayoutInflater inflater = LayoutInflater.from(this.getContext());
        this.mView = inflater.inflate(R.layout.dialog_common_list_setting, (ViewGroup)null);
        this.mRecyclerView = (RecyclerView)this.mView.findViewById(R.id.common_list_setting_rv);
        this.mRecyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        ListSettingAdapter adapter = new ListSettingAdapter(this.mData);
        this.mRecyclerView.setAdapter(adapter);
        this.mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    ((LinearLayoutManager) mRecyclerView.getLayoutManager()).scrollToPositionWithOffset(mSelectIndex, 0);
                }
            }

            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                ListSettingDialog.this.setSelectState(recyclerView);
            }
        });
        this.mView.findViewById(R.id.common_list_setting_confirm).setOnClickListener((v) -> {
            if (this.mDialogListener != null) {
                this.mDialogListener.onConfirmClick(this.mSelectIndex, (String)this.mData.get(this.mSelectIndex + 1));
            }

            this.dismiss();
        });
        this.mView.findViewById(R.id.common_list_setting_cancel).setOnClickListener((v) -> {
            this.dismiss();
        });
        ((TextView)this.mView.findViewById(R.id.common_list_setting_title)).setText(title);
    }

    private void setSelectState(RecyclerView recyclerView) {
        int rvHeight = recyclerView.getHeight();
        if (rvHeight != 0) {
            for(int i = 0; i < recyclerView.getChildCount(); ++i) {
                View view = recyclerView.getChildAt(i);
                TextView content = (TextView)view.findViewById(R.id.item_setting_list_content);
                if (content != null) {
                    int color;
                    if (view.getTop() <= rvHeight / 2 && view.getTop() > rvHeight / 6) {
                        color = this.COLOR_SELECTED;
                        this.mSelectIndex = recyclerView.getLayoutManager().getPosition(view) - 1;
                    } else {
                        color = this.COLOR_UNSELECTED;
                    }

                    content.setTextColor(color);
                }
            }
        }
    }

    public void show() {
        super.show();
        WindowManager.LayoutParams params = this.getWindow().getAttributes();
        params.width = WindowUtils.getScreenWidth(this.getContext());
        params.height = -1;
        this.getWindow().setAttributes(params);
        this.getWindow().setContentView(this.mView, new ViewGroup.LayoutParams(-1, -1));
        ((LinearLayoutManager) this.mRecyclerView.getLayoutManager()).scrollToPositionWithOffset(mSelectIndex, 0);
        mRecyclerView.post(() -> setSelectState(mRecyclerView));
    }

    private static class ListSettingViewHolder extends RecyclerView.ViewHolder {
        public ListSettingViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        public void bind(String s) {
            TextView content = (TextView)this.itemView.findViewById(R.id.item_setting_list_content);
            if (content != null) {
                content.setText(s);
            }
        }
    }

    private static class ListSettingAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final List<String> mData = new ArrayList<>();

        public ListSettingAdapter(List<String> data) {
            this.mData.addAll(data);
        }

        @NonNull
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_item_setting_list, parent, false);
            return new ListSettingDialog.ListSettingViewHolder(view);
        }

        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof ListSettingViewHolder) {
                ((ListSettingViewHolder)holder).bind((String)this.mData.get(position));
            }
        }

        public int getItemCount() {
            return this.mData.size();
        }
    }

    public interface ListDialogListener {
        void onConfirmClick(int var1, String var2);

        void onItemClick(int var1, String var2);
    }
}
