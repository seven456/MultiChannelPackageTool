MultiChannelPackageTool
=======================

Multi channel package tool 多渠道打包工具

为什么需要这个工具
------
国内应用市场百花争鸣，各自都占有自己的一席之地，这给开发者带来了麻烦；
想在多个应用市场发布应用，应用的后台服务器需要知道自己是从哪个渠道下载的；
中国的国情，都懂的；

该工具的原理
------
1、利用的是Zip文件的数据结构特点，在文件的末尾写入任意数据，都不会影响Zip文件的正确显示和解包（apk文件就是zip文件格式）；
2、所以该工具不需要对apk文件解包和重新签名即可完成多渠道自动打包，高效速度快，无兼容性问题；
3、比其他工具对apk解包再替换AndroidManifest.xml文件中的meta-data数据要高效安全得多（如：https://github.com/umeng/umeng-muti-channel-build-tool）；


如何使用
------
用法：java -jar MCPTool.jar [-path] [arg] [-contents] [arg] [-password] [arg]
-path		APK文件路径
-outdir		输出路径（可选），默认输出到APK文件同一目录
-contents	写入内容集合，多个内容之间用“;”分割，如：googleplay;m360; 当没有“-contents”参数时输出已有文件中的content
-password	加密密钥（可选），产度8位以上，如果没有该参数，不加密
-version	显示版本号
例如：
写入：java -jar MCPTool.jar -path D:/test.apk -outdir ./ -contents googleplay;m360; -password 12345678
读取：java -jar MCPTool.jar -path D:/test.apk -password 12345678

更新日志
------
V1.0 
20141216 创建项目