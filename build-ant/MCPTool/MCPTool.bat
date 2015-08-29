@echo off

echo ***********************************
echo 多渠道打包工具
echo !
echo 功能
echo --------
echo 1. 不对apk文件解包和签名即可完成渠道打包功能；
echo 2. 读取apk中的渠道号：执行MCPTool-check.bat，Java代码中使用MCPTool.jar中的readContent(path, password)方法读取；

echo ?
echo 使用方法
echo --------
echo 1. 将apk文件放到MCPTool目录下
echo 2. 将apk文件拖拽到MCPTool.bat上
echo 3. 执行完成后会在当前目录下生成各个渠道的apk文件
echo ***********************************


rem 拖拽的文件名是命令行的第一个参数
set apkFile=%1
rem cd到命名所在的根目录
cd /d %apkFile%\..


echo .........MCPTool..........
java -jar MCPTool-1.1.jar -path "%apkFile%" -outdir ./ -contents googleplay;m360;wandoujia;tencent;baidu;taobao;xiaomi; -password 12345678
echo.
echo 按任意键退出……
pause>nul
exit