package com.volcengine.vertcdemo.bean;

import com.google.gson.annotations.SerializedName;
import com.ss.video.rtc.demo.basic_module.utils.GsonUtils;

import org.json.JSONException;
import org.json.JSONObject;

public class MeetingBroadcast {
    @SerializedName("status")
    public boolean status;
    @SerializedName("user_id")
    public String user_id; // user's id
    @SerializedName("user_name")
    public String user_name; // user's name
    @SerializedName("user_uniform_name")
    public String user_uniform_name; // user's uniform name
    @SerializedName("room_id")
    public String room_id; // meeting's id
    @SerializedName("host_id")
    public String host_id; // host's id
    @SerializedName("former_host_id")
    public String former_host_id; // former host's id
    @SerializedName("user_status")
    public int user_status;
    @SerializedName("created_at")
    public long created_at; // UTC/GMT timestamp when meeting created
    @SerializedName("raise_hands_at")
    public long raise_hands_at;
    @SerializedName("is_host")
    public boolean is_host; // whether user is host
    @SerializedName("is_sharing")
    public boolean is_sharing; // whether somebody is sharing screen
    @SerializedName("is_mic_on")
    public boolean is_mic_on;  // whether local microphone is on
    @SerializedName("is_camera_on")
    public boolean is_camera_on; // whether local camera is on
    @SerializedName("host_info")
    public MeetingUserInfo host_info;

    public static MeetingBroadcast getBroadcast(Object originResp) {
        if (originResp instanceof JSONObject) {
            JSONObject resp = (JSONObject) originResp;
            String response;
            try {
                response = resp.getString("data");
                if (!JSONObject.NULL.equals(response)) {
                    return GsonUtils.gson().fromJson(response, MeetingBroadcast.class);
                } else {
                    return null;
                }
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            return null;
        }
    }
}