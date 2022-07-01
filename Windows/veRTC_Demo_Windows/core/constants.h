#pragma once  

namespace vrd {
    /*
    可以填入提供的测试服务器域名，上线正式时，需要部署自己的服务端并更换为自己的服务端域名
    */
	static const std::string URL = "";

    /*
     RTC 需要
     每个应用的唯一标识符，由 RTC 控制台申请获得，不同的 AppId 生成的实例在 RTC 中进行音视频通话完全独立，无法互通。
     https://console.volcengine.com/rtc/listRTC
    */
    static const std::string APP_ID = "";

    /*
     使用 RTC 时需要，AppKey是用来生产rtc token及鉴权rtc token使用的。
     https://console.volcengine.com/rtc/listRTC
    */
    static const std::string APP_KEY = "";

    /*
     使用 RTS 时需要，用于服务端调用 RTS openapi 时鉴权使用。
     用来确认服务端有这个APPID的所有权 https://console.volcengine.com/iam/keymanage/
    */
    static const std::string VOLC_AK = "";

    /*
     使用 RTS 时需要，用于服务端调用 RTS openapi 时鉴权使用。
     用来确认服务端有这个APPID的所有权 https://console.volcengine.com/iam/keymanage/
    */
    static const std::string VOLC_SK = "";

    /*
     火山控制台账户id， 使用会议录制功能时需要。 https://console.volcengine.com/user/basics/
    */
    static const std::string ACCOUNT_ID = "";

    /*
     点播空间名， 使用会议录制功能时需要。https://console.volcengine.com/vod/overview/
    */
    static const std::string VOD_SPACE = "";
}