视频会议是火山引擎实时音视频提供的一个开源示例项目。本文介绍如何快速跑通该示例项目，体验 RTC 视频会议效果。

## 应用使用说明

使用该工程文件构建应用后，即可使用构建的应用进行视频会议。
你和你的同事必须加入同一个房间，才能进行视频会议。
如果你已经安装过 火山引擎场景化 Demo 安装包，示例项目编译运行前请先卸载原有安装包，否则会提示安装失败。

## 前置条件

- Android Studio （推荐版本 [Chipmunk](https://developer.android.com/studio/releases)）
	
- [Gradle](https://gradle.org/releases/) （版本： gradle-7.4.2-all）
	
- Android 4.4+ 真机
	
- 有效的 [火山引擎开发者账号](https://console.volcengine.com/auth/login)
	

### 操作步骤

### **步骤 1：获取 AppID 和 AppKey**

在火山引擎控制台->[应用管理](https://console.volcengine.com/rtc/listRTC)页面创建应用或使用已创建应用获取 AppID 和 AppAppKey

### **步骤 2：获取 AccessKeyID 和 SecretAccessKey**

在火山引擎控制台-> [密钥管理](https://console.volcengine.com/iam/keymanage/)页面获取 **AccessKeyID 和 SecretAccessKey**

### **步骤 3：获取 AccountID**

在火山引擎控制台 -> 账号管理-> [基本信息](https://console.volcengine.com/user/basics/)页面获取账号 ID。

<img src="https://portal.volccdn.com/obj/volcfe/cloud-universal-doc/upload_0e9889a4754890c020ae85769c6b0c92.png" width="500px" >

### 步骤 4：获取 VodSpace

在火山引擎控制台-> 视频管理 -> [空间管理](https://console.volcengine.com/vod/overview/)页面获取 VodSpace。若不存在空间，请新建空间。
<img src="https://portal.volccdn.com/obj/volcfe/cloud-universal-doc/upload_04f71eb88c25ec26d0d9df2b7277019c.png" width="500px" >

### 步骤 5：构建工程

1. 使用 Android Studio 打开该项目的`RTC_Meeting_Demo-master/Android/veRTC_Demo_Android` 文件夹
	
2. 填写 HEAD_URL。 
    进入 `component/ToolKit` 目录下 `gradle.properties`文件，填写 **HEAD_URL**。

    当前你可以使用 **`https://common.rtc.volcvideo.com/rtc_demo_special/`** 作为测试服务器域名，仅提供跑通测试服务，无法保障正式需求。

    <img src="https://portal.volccdn.com/obj/volcfe/cloud-universal-doc/upload_fbf862c5eba00c62e632f5522f52f6dc.png" width="500px" >

3. **填写 APPID、APPKey、AccessKeyID、SecretAccessKey**

    进入 `component/JoinRTSParamsKit` 目录下 `gradle.properties`文件，使用从火山引擎控制台获取的 APPID、APPKey、AccessKeyID、SecretAccessKey 填写到该文件相应位置。

    <img src="https://portal.volccdn.com/obj/volcfe/cloud-universal-doc/upload_16fb7b7892995ad297c71a49f81b5f86.png" width="500px" >

4. **填写 ACCOUNT_ID 和 VOD_SPACE**

	进入 `solutions/meeting` 目录下 `gradle.properties`文件，填写 **ACCOUNT_ID 和 VOD_SPACE**

    <img src="https://portal.volccdn.com/obj/volcfe/cloud-universal-doc/upload_04e894554cba8b3642f116e23476d90b.png" width="500px" >

### 步骤 6：编译运行

1. 将手机连接到电脑，并在开发者选项中打开调试功能。连接成功后，设备名称会出现在界面右上方。
	<img src="https://portal.volccdn.com/obj/volcfe/cloud-universal-doc/upload_e19552fe9cefd9004323501d9be3f286.png" width="500px" >

2. 选择**Run** -> **Run 'app'** ，开始编译。编译成功后你的 Android 设备上会出现新应用。部分手机会出现二次确认，请选择确认安装。
	<img src="https://portal.volccdn.com/obj/volcfe/cloud-universal-doc/upload_0e6a11707c6d05adac680659bb585a56.png" width="500px" >

运行开始界面如下：
<br>
<img src="https://portal.volccdn.com/obj/volcfe/cloud-universal-doc/upload_ae314e8155698d6a6c2e01200db7d91e.jpg" width="200px" >