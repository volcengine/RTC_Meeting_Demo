# iOS

视频会议是火山引擎实时音视频提供的一个开源示例项目。本文介绍如何快速跑通该示例项目，体验 RTC 视频会议效果。

## 应用使用说明

使用该工程文件构建应用后，即可使用构建的应用进行视频会议。
你和你的同事必须加入同一个房间，才能进行视频会议。

## 前置条件

- [Xcode](https://developer.apple.com/download/all/?q=Xcode) 12.0+
	

- iOS 12.0+ 真机
	

- 有效的 [AppleID](http://appleid.apple.com/)
	

- 有效的 [火山引擎开发者账号](https://console.volcengine.com/auth/login)
	

- [CocoaPods](https://guides.cocoapods.org/using/getting-started.html#getting-started) 1.10.0+
	

## 操作步骤

### **步骤 1：获取 AppID 和 AppKey**

在火山引擎控制台->[应用管理](https://console.volcengine.com/rtc/listRTC)页面创建应用或使用已创建应用获取 AppID 和 AppAppKey

### **步骤 2：获取 AccessKeyID 和 SecretAccessKey**

在火山引擎控制台-> [密钥管理](https://console.volcengine.com/iam/keymanage/)页面获取 **AccessKeyID 和 SecretAccessKey**

### **步骤 3：获取 AccountID**

在火山引擎控制台 -> 账号管理-> [基本信息](https://console.volcengine.com/user/basics/)页面获取账号 ID。

<img src="https://lf3-volc-editor.volccdn.com/obj/volcfe/sop-public/upload_bccec2d06673c1e0b425561618108028" width="500px" >


### 步骤 4：获取 VodSpace

在火山引擎控制台-> 视频管理 -> [空间管理](https://console.volcengine.com/vod/overview/)页面获取 VodSpace。若不存在空间，请新建空间。

<img src="https://lf6-volc-editor.volccdn.com/obj/volcfe/sop-public/upload_3dbb81aea0c91db339b16a3a03b22ede" width="500px" >

### 步骤 5：构建工程

1. 打开终端窗口，进入 `RTC_Meeting_Demo-master/iOS/veRTC_Demo_iOS` 根目录
	

<img src="https://lf3-volc-editor.volccdn.com/obj/volcfe/sop-public/upload_860ae385c4def9728b90a56b17b7f360" width="500px" >

2. 执行 `pod install` 命令构建工程
	

<img src="https://lf6-volc-editor.volccdn.com/obj/volcfe/sop-public/upload_af72e1bade067796c1c1bc0a5ac06870" width="500px" >

3. 进入 `RTC_Meeting_Demo-master/iOS/veRTC_Demo_iOS` 根目录，使用 Xcode 打开 `veRTC_Demo.xcworkspace`
	

<img src="https://lf3-volc-editor.volccdn.com/obj/volcfe/sop-public/upload_99450c7c3bd9312bab41f0d07ab29d6b" width="500px" >

4. 在 Xcode 中打开 `Pods/Development Pods/Core/BuildConfig.h` 文件
	

<img src="https://lf3-volc-editor.volccdn.com/obj/volcfe/sop-public/upload_bfffdcae640c9c5924270104df37051f" width="500px" >

5. 填写 **LoginUrl**
	

当前你可以使用 **`https://common.rtc.volcvideo.com/rtc_demo_special/login`** 作为测试服务器域名，仅提供跑通测试服务，无法保障正式需求。

<img src="https://lf3-volc-editor.volccdn.com/obj/volcfe/sop-public/upload_75ac2501be0ab326df13b3c4bfd622cd" width="500px" >

6. **填写 APPID、APPKey、AccessKeyID 和 SecretAccessKey**
	

使用在控制台获取的 **APPID、APPKey、AccessKeyID 和 SecretAccessKey** 填写到 `BuildConfig.h`文件的对应位置**。** 

<img src="https://lf6-volc-editor.volccdn.com/obj/volcfe/sop-public/upload_2a4652ef807f5be0d8a56ebaee7f5889" width="500px" >

7. **填写 ACCOUNT\_ID 和 VOD\_SPACE**
	

使用在控制台获取的 **ACCOUNT\_ID 和 VOD\_SPACE** 填写到 `Pods/Development Pods/MeetingDemo/MeetingDemoConstants.h` 文件的对应位置。

<img src="https://lf6-volc-editor.volccdn.com/obj/volcfe/sop-public/upload_f2b45aed8f678ea624c80418c0e073b5" width="500px" >

### **步骤 6：配置开发者证书**

#### 不使用屏幕共享功能

1. 将手机连接到电脑，在 `iOS Device` 选项中勾选您的 iOS 设备。
	

<img src="https://lf6-volc-editor.volccdn.com/obj/volcfe/sop-public/upload_0272ae27c61c37b6100e8777ff5d9bd8" width="500px" >

2. 登录 Apple ID。
	

2.1 选择 Xcode 页面左上角 **Xcode** > **Preferences**，或通过快捷键 **Command** + **,**  打开 Preferences。
2.2 选择 **Accounts**，点击左下部 **+**，选择 Apple ID 进行账号登录。

<img src="https://lf6-volc-editor.volccdn.com/obj/volcfe/sop-public/upload_f3be9f246513f73bf526da3b5992b603" width="500px" >

3. 单击 Xcode 左侧导航栏中的 `VeRTC_Demo` 项目，单击 `TARGETS` 下的 `MeetingScreenShare` 项目，右键选择 `delete`。
	

<img src="https://lf6-volc-editor.volccdn.com/obj/volcfe/sop-public/upload_6be58d290d465588f1e081eb1550818c" width="500px" >

4. 单击 `TARGETS` 下的 `VeRTC_Demo` 项目，单击 `Build Setting`,删除`Signing`下 `Code Signing Entitlements` 的值。
	

<img src="https://lf3-volc-editor.volccdn.com/obj/volcfe/sop-public/upload_3c91c0c64e6d71b8b334621a763cfec0" width="500px" >

5. 配置开发者证书。
	

3.1 单击 Xcode 左侧导航栏中的 `VeRTC_Demo` 项目，单击 `TARGETS` 下的 `VeRTC_Demo` 项目，选择 **Signing & Capabilities** > **Automatically manage signing** 自动生成证书

<img src="https://lf6-volc-editor.volccdn.com/obj/volcfe/sop-public/upload_008590ac8d8df73204cb963ce0fdf094" width="500px" >

3.2 在 **Team** 中选择 Personal Team。

<img src="https://lf3-volc-editor.volccdn.com/obj/volcfe/sop-public/upload_77b6915e42a5a40fe0b626f5ebd33c25" width="500px" >

3.3 **修改 Bundle** **Identifier****。** 

默认的 `vertc.veRTCDemo.ios` 已被注册， 将其修改为其他 Bundle ID，格式为 `vertc.xxx`。

<img src="https://lf3-volc-editor.volccdn.com/obj/volcfe/sop-public/upload_017f231a9f68d4c84ff129add5d70651" width="500px" >

#### 使用屏幕共享功能

1. 将手机连接到电脑，在 `iOS Device` 选项中勾选您的 iOS 设备。
	

<img src="https://lf6-volc-editor.volccdn.com/obj/volcfe/sop-public/upload_c67a3128ca93eb0baf69a60066db7c3c" width="500px" >

2. 配置 App Groups，获取屏幕共享 Extension 对应的 `Bundle Identifier`和`App Groups Identifiers`。
	

关于如何创建和配置 App Groups，请参看[如何创建和配置 App Group](https://www.volcengine.com/docs/6348/125721)。

3. 更新 Constants 信息。
	

打开 `Pods/Development Pods/MeetingDemo/MeetingDemoConstants.h` 文件，使用获取的`Bundle Identifier`和`App Groups Identifiers`值填写到对应位置。

<img src="https://lf3-volc-editor.volccdn.com/obj/volcfe/sop-public/upload_c12912813c5c282a0a1d3984ad451e9c" width="500px" >

4. 更新`entitlements` 信息。
	

打开 `veRTC_Demo/MeetingScreenShare/veRTC_Demo.entitlements` 文件，使用获取的`App Groups Identifiers` 值填写到对应位置。

<img src="https://lf3-volc-editor.volccdn.com/obj/volcfe/sop-public/upload_4ce3863da3b629fb28f19e6e97d572b7" width="500px" >

5. 配置开发者证书。
	

3.1 单击 Xcode 左侧导航栏中的 `VeRTC_Demo` 项目，单击 `TARGETS` 下的 `VeRTC_Demo` 项目，选择 **Signing & Capabilities** > **Automatically manage signing** 自动生成证书

<img src="https://lf6-volc-editor.volccdn.com/obj/volcfe/sop-public/upload_8a00a4aeb5e67519e0a0407116c6efc8" width="500px" >

3.2 在 **Team** 中选择 Personal Team。

<img src="https://lf6-volc-editor.volccdn.com/obj/volcfe/sop-public/upload_c7f2ada7839bafd595211cad265f5f1b" width="500px" >

3.3 **修改 Bundle** **Identifier****。** 
默认的 `vertc.veRTCDemo.ios` 已被注册， 将其修改为其他 Bundle ID，格式为 `vertc.xxx`。

<img src="https://lf3-volc-editor.volccdn.com/obj/volcfe/sop-public/upload_d2121b695c59953144f693d61500489e" width="500px" >

3.4 单击单击 Xcode 左侧导航栏中的 `VeRTC_Demo` 项目，单击 `TARGETS` 下的 `MeetingScreenShare` 项目，更新签名

<img src="https://lf3-volc-editor.volccdn.com/obj/volcfe/sop-public/upload_e3ccf8bb29343b438279d7761915a6b5" width="500px" >

### **步骤 7：编译运行**

选择 **Product** > **Run**， 开始编译。编译成功后你的 iOS 设备上会出现新应用。若为免费苹果账号，需先在`设置->通用-> VPN与设备管理 -> 描述文件与设备管理`中信任开发者 APP。

<img src="https://lf6-volc-editor.volccdn.com/obj/volcfe/sop-public/upload_d9e8f88fe1c32508b2f4857bc7f4464f" width="500px" >

运行开始界面如下：

<img src="https://lf6-volc-editor.volccdn.com/obj/volcfe/sop-public/upload_42ab289bfa8036085559230f9c252c53" width="200px" >
