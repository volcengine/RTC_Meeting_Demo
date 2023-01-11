package com.volcengine.vertcdemo;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.ss.video.rtc.demo.basic_module.utils.SafeToast;
import com.volcengine.vertcdemo.core.SolutionDataManager;
import com.volcengine.vertcdemo.core.eventbus.RTSLogoutEvent;
import com.volcengine.vertcdemo.core.eventbus.SolutionDemoEventManager;
import com.volcengine.vertcdemo.core.eventbus.TokenExpiredEvent;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class MainFragment extends Fragment {
    private static final int REQUEST_CODE_PERMISSIONS = 8800;

    private View mTabScenes;
    private View mTabProfile;

    private static final String TAG_SCENES = "fragment_tag_scenes";
    private static final String TAG_PROFILE = "fragment_tag_profile";

    private Fragment mFragmentScenes;
    private Fragment mFragmentProfile;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String token = SolutionDataManager.ins().getToken();
        if (TextUtils.isEmpty(token)) {
            startActivity(new Intent(Actions.LOGIN));
        }

        requestPermissions(new String[]{
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        }, REQUEST_CODE_PERMISSIONS);

        mTabScenes = view.findViewById(R.id.tab_scenes);
        mTabScenes.setOnClickListener(v -> switchMainLayout(true));

        mTabProfile = view.findViewById(R.id.tab_profile);
        mTabProfile.setOnClickListener(v -> switchMainLayout(false));

        // region 恢复或者创建 Tab 的 Fragment
        final FragmentManager fragmentManager = getChildFragmentManager();
        Fragment tabScene = fragmentManager.findFragmentByTag(TAG_SCENES);
        if (tabScene == null) {
            tabScene = new SceneEntryFragment();
            fragmentManager
                    .beginTransaction()
                    .add(R.id.tab_content, tabScene, TAG_SCENES)
                    .commit();
        }

        mFragmentScenes = tabScene;

        Fragment tabProfile = fragmentManager.findFragmentByTag(TAG_PROFILE);
        if (tabProfile == null) {
            tabProfile = new ProfileFragment();
            fragmentManager
                    .beginTransaction()
                    .add(R.id.tab_content, tabProfile, TAG_PROFILE)
                    .commit();
        }
        mFragmentProfile = tabProfile;
        // endregion

        switchMainLayout(true);

        SolutionDemoEventManager.register(this);

        SolutionDataManager.ins().setAppVersionName(BuildConfig.VERSION_NAME);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        SolutionDemoEventManager.unregister(this);
    }


    private void switchMainLayout(boolean isEntrance) {
        mTabScenes.setSelected(isEntrance);
        mTabProfile.setSelected(!isEntrance);

        if (isEntrance) {
            getChildFragmentManager()
                    .beginTransaction()
                    .hide(mFragmentProfile)
                    .show(mFragmentScenes)
                    .commit();
        } else {
            getChildFragmentManager()
                    .beginTransaction()
                    .hide(mFragmentScenes)
                    .show(mFragmentProfile)
                    .commit();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTokenExpiredEvent(TokenExpiredEvent event) {
        switchMainLayout(true);
        startActivity(new Intent(Actions.LOGIN));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRTSLogoutEvent(RTSLogoutEvent event) {
        SafeToast.show(R.string.same_account_logut);
        startActivity(new Intent(Actions.LOGIN));
    }
}
