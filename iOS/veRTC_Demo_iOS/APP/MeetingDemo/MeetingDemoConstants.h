//
//  MeetingDemoConstants.h
//  Pods
//
//  Created by bytedance on 2022/4/18.
//

#ifndef MeetingDemoConstants_h
#define MeetingDemoConstants_h

#define HomeBundleName @"MeetingDemo"

/*
 火山控制台账户id， 使用会议录制功能时需要。
 https://console.volcengine.com/user/basics/
 */
#define AccountID @""

/*
 点播空间名， 使用会议录制功能时需要。
 https://console.volcengine.com/vod/overview/
 */
#define VodSpace @""

#ifdef DEBUG

// 屏幕共享使用
#define ExtensionName @"vertc.veRTCDemo.ios.MeetingScreenShare"
#define APPGroupId @"group.vertc.veRTCDemo.ios.MeetingScreenShare"

#else
   
// 屏幕共享使用
#define ExtensionName @"rtc.veRTCDemo.ios.MeetingScreenShare"
#define APPGroupId @"group.rtc.veRTCDemo.ios.meetingScreenShare"

#endif

#endif /* MeetingDemoConstants_h */
