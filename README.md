# MultiChannelPackageTool
Android Multi channel package tool （安卓多渠道打包工具）

## 为什么需要这个工具
	1、国内应用市场繁多，上线的apk应用需要知道自己在哪个渠道下载的；
	2、简直是急速啊：5M的apk，1秒种能打300个；
	3、因为是急速，可以做网页下载时动态打渠道包功能（如：分享apk给好友，好友下载打开apk后直接计算准确的分享量）；

## 该工具的原理
	利用的是Zip文件“可以添加comment（摘要）”的数据结构特点，在文件的末尾写入任意数据，而不用重新解压zip文件（apk文件就是zip文件格式）；
	所以该工具不需要对apk文件解压缩和重新签名即可完成多渠道自动打包，高效速度快，无兼容性问题；
	
	另外美团的适配渠道包方案（不同的渠道嵌入不同的SDK）：
	美团Android自动化之旅—适配渠道包（Gradle flavor解决内嵌第三方SDK适配）（http://tech.meituan.com/mt-apk-adaptation.html）

## 与现有多渠道打包工具的对比
	友盟（https://github.com/umeng/umeng-muti-channel-build-tool）
		打包：解压apk文件 -> 替换AndroidManifest.xml中的meta-data -> 压缩apk文件 -> 签名
		读取渠道号：直接通过Android的API读取meta-data
		特点：需要解压缩、压缩、重签名耗费时间较多，重签名会导致apk包在运行时有兼容性问题；
	美团（http://tech.meituan.com/mt-apk-packaging.html）
		打包：解压apk文件 -> META-INF目录下创建一个以渠道号为文件名的空文件 -> 压缩apk文件
		读取渠道号：解压已安装的data/app/<package>.apk -> 读取以渠道号为文件名的空文件的文件名
		特点：比友盟高效点，只有解压缩和压缩，没有签名，兼容性也比较好，但是读取渠道号需要解压缩apk，速度比较慢；
	我自己
		打包：直接写入渠道号到apk文件的末尾
		读取渠道号：直接读取data/app/<package>.apk文件末尾的渠道号
		特点：没有解压缩、压缩、重签名，没有兼容性问题，速度最快；写入的渠道号数据支持加密，安全可靠；

## 除了多渠道打包，我还能做什么？
	由于速度极快，我还可以作为服务器端下载apk时动态写入“特定数据”，用户下载到apk后安装启动，读取“特定数据”完成特定的操作；
	如：加好友功能，下载前写入用户ID，用户下载后启动apk，读取写入的用户ID，完成加好友操作，用户体验大大提升，没有断裂感；
	当然，也可以写入JSON数据，想做什么就做什么；
	

## 如何使用
	1、命令行使用说明：
	用法：java -jar MCPTool.jar [-path] [arg] [-contents] [arg] [-password] [arg]
	-path		APK文件路径
	-outdir		输出路径（可选），默认输出到APK文件同一目录
	-contents	写入内容集合，多个内容之间用“;”分割，如：googleplay;m360; 当没有“-contents”参数时输出已有文件中的content
	-password	加密密钥（可选），长度8位以上，如果没有该参数，不加密
	-version	显示版本号
	例如：
	写入：java -jar MCPTool.jar -path D:/test.apk -outdir ./ -contents googleplay;m360; -password 12345678
	读取：java -jar MCPTool.jar -path D:/test.apk -password 12345678
	
	2、Android代码中读取写入的渠道号：
	导入MCPTool.jar中的MCPTool类，MCPTool.getChannelId(context, mcptoolPassword, defValue)读出写入的渠道号；
	
	3、jenkins、hudson、ant使用说明：
	请看MultiChannelPackageTool\build-ant\MCPTool\build.xml文件；
	
	4、Windows下bat脚本运行说明：
	拖拽文件即可完成多渠道打包：MultiChannelPackageTool\build-ant\MCPTool\MCPTool.bat；
	拖拽文件检查渠道号是否写入成功：MultiChannelPackageTool\build-ant\MCPTool\MCPTool-check.bat；
	上面2个bat文件中有密码，可以自行修改；

## 更新日志
	V1.1
	20150829	更新内容
		修复Android5.0及以上系统不能安装打出的渠道包问题（校验apk文件的comment数据的长度）
	V1.0
	20141216	更新内容
		创建项目

## License

    Copyright (C) 2014 seven456@gmail.com
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
       http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
