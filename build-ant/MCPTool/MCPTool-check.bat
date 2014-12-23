@echo off

echo ***********************************
echo 多渠道打包工具
echo !
echo 功能
echo --------
echo 1. 检查apk中已经写入的渠道号；

echo ?
echo 使用方法
echo --------
echo 1. 将apk文件放到MCPTool目录下
echo 2. 将apk文件拖拽到MCPTool-check.bat上
echo 3. 会在控制台显示渠道号内容
echo ***********************************


rem 拖拽的文件名是命令行的第一个参数
set apkFile=%1
rem cd到命名所在的根目录
cd /d %apkFile%\..


echo .........MCPTool..........
java -jar MCPTool-1.0.jar -path "%apkFile%" -password 12345678
echo.
echo 按任意键退出……
pause>nul
exit