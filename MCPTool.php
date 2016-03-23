<?php
/**
 * 安卓多渠道打包工具  PHP实现给ZIP文件(apk)添加comment（摘要）
 * @author lum (belm@vip.qq.com)
 * @since 2016-03-23
 */

    if ($argc != 3) {
        echo 'example: php MCPTool.php xx.apk comment';echo PHP_EOL;exit;
    }
    $path    = trim($argv[1]);
    $comment = trim($argv[2]);

    echo MCPTool::write($path, $comment);echo PHP_EOL;exit;

    class MCPTool
    {
        /**
         * zip写入comment
         * @param  [string] $path    [apk文件路径]
         * @param  [string] $comment [zip摘要信息]
         * @return [string]          [description]
         */
        static public function write($path, $comment){

            if (!file_exists($path)) {
                return 'file '.$path.' not exist';
            }

            $zip = new ZipArchive;
            $ret = $zip->open($path);

            if ($ret !== true) {
                return 'zip open failed:'.$ret;
            }

            if (!empty($zip->comment)) {
                return $zip->comment;
            }

            $fp =  fopen($path , 'r+'); //这里注意不要使用rw
            //ZIP摘要结构 参考http://pingguohe.net/2016/03/21/Dynimac-write-infomation-into-apk.html
            $seek =  fseek($fp, -2, SEEK_END); //文件指针移到末尾2个字节 zip摘要长度信息占2个字节
            $data = pack("v", strlen($comment)); //always 16 bit, little endian byte order

            $ret1 = fwrite($fp, $data);//返回写入的字符数

            if ($ret1 != 2) {
                return 'write length error';
            }

            $ret2 = fwrite($fp, $comment);

            if ($ret2 === false) {
                return 'write comment error';
            }

            fclose($fp);

            return 'write success, try again to get comment';
        }
    }

 ?>
