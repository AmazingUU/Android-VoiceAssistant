# Android-VoiceAssistant
## 简介
仿照Siri，利用[百度语音](http://yuyin.baidu.com/)和[图灵机器人](http://www.tuling123.com/)，实现打电话，聊天等功能

## 功能
1. 给通讯录中联系人打电话
2. 给通讯录中联系人发短信
3. 打开应用
4. 网上查找资料
5. 聊天

## 效果图
<img src="https://github.com/AmazingUU/Android-VoiceAssistant/raw/master/mdPicRes/VoiceAssistant.gif" width = "300" height = "500" alt="VoiceAssisant" align=left /><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br>
### 注：效果图是用华为荣耀2平板录制的，不支持SIM卡，没有打电话和短信的效果。但是我是用三星S3测试的，可以正常实现打电话和发短信的功能。因为三星S3不知道为什么不能用Vysor将手机画面显示在电脑上，所以我只好用华为荣耀2平板录制了，敬请见谅。

## 注意事项
1. 项目里的百度语音的APPId、APPKey、SecretKey和图灵机器人的APIKey只是为了让项目正常运行，实际使用过程中请替换成自己的实际值。
2. 百度语音识别提供的jniLibs里的so库是arm版，没有x86版，所以请在真机运行，模拟器上无法运行

## 后续
后面有时间的话会加入语音唤醒的功能，使用户即使在桌面也可以随时唤出语音助手