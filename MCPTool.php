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

            $len = strlen($comment);
            $data = pack("v", $len + strlen($len)); //always 16 bit, little endian byte order
            $ret1 = fwrite($fp, $data);//返回写入的字符数

            if ($ret1 != 2) {
                return 'write length error';
            }

            $ret2 = fwrite($fp, $comment);
            $ret3 = fwrite($fp, pack("v", $len));

            if ($ret2 === false || $ret3 === false) {
                return 'write comment error';
            }

            fclose($fp);

            return 'write success, try again to get comment';
        }
    }

/*
    //以下为JAVA代码

    //android读取php写入zip的comment

    public static String readApk(Context context) throws IOException {

        String path = context.getPackageCodePath();

        System.out.println("path=" + path);

        File file = new File(path);

        byte[] bytes = null;
        try {
            RandomAccessFile accessFile = new RandomAccessFile(file, "r");
            long index = accessFile.length();

            bytes = new byte[2];
            index = index - bytes.length;
            accessFile.seek(index);
            accessFile.readFully(bytes);

            int contentLength = stream2Short(bytes, 0);

            System.out.println("content length=" + contentLength);

            if (contentLength == 0){
                return null;
            }

            bytes = new byte[contentLength];
            index = index - bytes.length;
            accessFile.seek(index);
            accessFile.readFully(bytes);

            return new String(bytes, "utf-8");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static short stream2Short(byte[] stream, int offset) {
        ByteBuffer buffer = ByteBuffer.allocate(2);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(stream[offset]);
        buffer.put(stream[offset + 1]);
        return buffer.getShort(0);
    }
 */
 ?>
