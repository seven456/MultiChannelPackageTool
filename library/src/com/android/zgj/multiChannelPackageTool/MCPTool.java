/*
 * Copyright (C) 2014 seven456@gmail.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.zgj.multiChannelPackageTool;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.security.Key;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipFile;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;

/**
 * 多渠道打包工具；<br/>
 * 利用的是Zip文件“可以添加comment（注释）”的数据结构特点，在文件的末尾写入任意数据，而不用重新解压zip文件（apk文件就是zip文件格式）；<br/>
 * 创建时间： 2014-12-16 18:56:29
 * @author zhangguojun
 * @version 1.1
 * @since JDK1.7 Android2.2
 */
public class MCPTool {
	/**
	 * 数据结构体的签名标记
	 */
	private static final String SIG = "MCPT";
	/**
	 * 数据结构的版本号
	 */
	private static final String VERSION_1_1 = "1.1";
	/**
	 * 数据编码格式
	 */
	private static final String CHARSET_NAME = "UTF-8";
	/**
	 * 加密用的IvParameterSpec参数
	 */
	private static final byte[] IV = new byte[] { 1, 3, 1, 4, 5, 2, 0, 1 };

	/**
	 * 写入数据
	 * @param path 文件路径
	 * @param content 写入的内容
	 * @param password 加密密钥
	 * @throws Exception
	 */
	private static void write(File path, String content, String password) throws Exception {
		write(path, content.getBytes(CHARSET_NAME), password);
	}

	/**
	 * 写入数据（如：渠道号）
	 * @param path 文件路径
	 * @param content 写入的内容
	 * @param password 加密密钥
	 * @throws Exception
	 */
	private static void write(File path, byte[] content, String password) throws Exception {
		ZipFile zipFile = new ZipFile(path);
		boolean isIncludeComment = zipFile.getComment() != null;
		zipFile.close();
		if (isIncludeComment) {
			throw new IllegalStateException("Zip comment is exists, Repeated write is not recommended.");
		}
		
		boolean isEncrypt = password != null && password.length() > 0;
		byte[] bytesContent = isEncrypt ? encrypt(password, content) : content;
		byte[] bytesVersion = VERSION_1_1.getBytes(CHARSET_NAME);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		baos.write(bytesContent); // 写入内容；
		baos.write(short2Stream((short) bytesContent.length)); // 写入内容长度；
		baos.write(isEncrypt ? 1 : 0); // 写入是否加密标示；
		baos.write(bytesVersion); // 写入版本号；
		baos.write(short2Stream((short) bytesVersion.length)); // 写入版本号长度；
		baos.write(SIG.getBytes(CHARSET_NAME)); // 写入SIG标记；
		byte[] data = baos.toByteArray();
		baos.close();
		if (data.length > Short.MAX_VALUE) {
			throw new IllegalStateException("Zip comment length > 32767.");
		}
		
		// Zip文件末尾数据结构：{@see java.util.zip.ZipOutputStream.writeEND}
		RandomAccessFile raf = new RandomAccessFile(path, "rw");
		raf.seek(path.length() - 2); // comment长度是short类型
		raf.write(short2Stream((short) data.length)); // 重新写入comment长度，注意Android apk文件使用的是ByteOrder.LITTLE_ENDIAN（小端序）；
		raf.write(data);
		raf.close();
	}
	
	/**
	 * 读取数据
	 * @param path 文件路径
	 * @param password 解密密钥
	 * @return 被该工具写入的数据（如：渠道号）
	 * @throws Exception
	 */
	private static byte[] read(File path, String password) throws Exception {
		byte[] bytesContent = null;
		byte[] bytesMagic = SIG.getBytes(CHARSET_NAME);
		byte[] bytes = new byte[bytesMagic.length];
		RandomAccessFile raf = new RandomAccessFile(path, "r");
		Object[] versions = getVersion(raf);
		long index = (long) versions[0];
		String version = (String) versions[1];
		if (VERSION_1_1.equals(version)) {
			bytes = new byte[1];
			index -= bytes.length;
			readFully(raf, index, bytes); // 读取内容长度；
			boolean isEncrypt = bytes[0] == 1;

			bytes = new byte[2];
			index -= bytes.length;
			readFully(raf, index, bytes); // 读取内容长度；
			int lengthContent = stream2Short(bytes, 0);

			bytesContent = new byte[lengthContent];
			index -= lengthContent;
			readFully(raf, index, bytesContent); // 读取内容；

			if (isEncrypt && password != null && password.length() > 0) {
				bytesContent = decrypt(password, bytesContent);
			}
		}
		raf.close();
		return bytesContent;
	}
	
	/**
	 * 读取数据结构的版本号
	 * @param raf RandomAccessFile
	 * @return 数组对象，[0] randomAccessFile.seek的index，[1] 数据结构的版本号
	 * @throws IOException
	 */
	private static Object[] getVersion(RandomAccessFile raf) throws IOException {
		String version = null;
		byte[] bytesMagic = SIG.getBytes(CHARSET_NAME);
		byte[] bytes = new byte[bytesMagic.length];
		long index = raf.length();
		index -= bytesMagic.length;
		readFully(raf, index, bytes); // 读取SIG标记；
		if (Arrays.equals(bytes, bytesMagic)) {
			bytes = new byte[2];
			index -= bytes.length;
			readFully(raf, index, bytes); // 读取版本号长度；
			int lengthVersion = stream2Short(bytes, 0);
			index -= lengthVersion;
			byte[] bytesVersion = new byte[lengthVersion];
			readFully(raf, index, bytesVersion); // 读取内容；
			version = new String(bytesVersion, CHARSET_NAME);
		}
		return new Object[] { index, version };
	}

	/**
	 * RandomAccessFile seek and readFully
	 * @param raf
	 * @param index
	 * @param buffer
	 * @throws IOException
	 */
	private static void readFully(RandomAccessFile raf, long index, byte[] buffer) throws IOException {
		raf.seek(index);
		raf.readFully(buffer);
	}

	/**
	 * 读取数据（如：渠道号）
	 * @param path 文件路径
	 * @param password 解密密钥
	 * @return 被该工具写入的数据（如：渠道号）
	 */
	public static String readContent(File path, String password) {
		try {
			return new String(read(path, password), CHARSET_NAME);
		} catch (Exception ignore) {
		}
		return null;
	}
	
	/**
	 * Android平台读取渠道号
	 * @param context Android中的android.content.Context对象
	 * @param mcptoolPassword mcptool解密密钥
	 * @param defValue 读取不到时用该值作为默认值
	 * @return
	 */
	public static String getChannelId(Object context, String mcptoolPassword, String defValue) {
		String content = MCPTool.readContent(new File(getPackageCodePath(context)), mcptoolPassword);
		return content == null || content.length() == 0 ? defValue : content;
	}

	/**
	 * 获取已安装apk文件的存储路径（这里使用反射，因为MCPTool项目本身不需要导入Android的运行库）
	 * @param context Android中的Context对象
	 * @return
	 */
	private static String getPackageCodePath(Object context) {
		try {
			return (String) context.getClass().getMethod("getPackageCodePath").invoke(context);
		} catch (Exception ignore) {
		}
		return null;
	}

	/**
	 * 加密
	 * @param password
	 * @param content
	 * @return
	 * @throws Exception
	 */
	private static byte[] encrypt(String password, byte[] content) throws Exception {
		return cipher(Cipher.ENCRYPT_MODE, password, content);
	}

	/**
	 * 解密
	 * @param password
	 * @param content
	 * @return
	 * @throws Exception
	 */
	private static byte[] decrypt(String password, byte[] content) throws Exception {
		return cipher(Cipher.DECRYPT_MODE, password, content);
	}

	/**
	 * 加解密
	 * @param cipherMode
	 * @param password
	 * @param content
	 * @return
	 * @throws Exception
	 */
	private static byte[] cipher(int cipherMode, String password, byte[] content) throws Exception {
		DESKeySpec dks = new DESKeySpec(password.getBytes(CHARSET_NAME));
		SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
		Key secretKey = keyFactory.generateSecret(dks);
		Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
		IvParameterSpec spec = new IvParameterSpec(IV);
		cipher.init(cipherMode, secretKey, spec);
		return cipher.doFinal(content);
	}

	/**
	 * short转换成字节数组（小端序）
	 * @param data
	 * @return
	 */
	private static short stream2Short(byte[] stream, int offset) {
        ByteBuffer buffer = ByteBuffer.allocate(2);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(stream[offset]);
        buffer.put(stream[offset + 1]);
        return buffer.getShort(0);
    }

	/**
	 * 字节数组转换成short（小端序）
	 * @param stream
	 * @param offset
	 * @return
	 */
	private static byte[] short2Stream(short data) {
        ByteBuffer buffer = ByteBuffer.allocate(2);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putShort(data);
        buffer.flip();
        return buffer.array();
    }

	/**
	 * nio高速拷贝文件
	 * @param source
	 * @param target
	 * @return
	 * @throws IOException
	 */
	private static boolean nioTransferCopy(File source, File target) throws IOException {
		FileChannel in = null;
		FileChannel out = null;
		FileInputStream inStream = null;
		FileOutputStream outStream = null;
		try {
			File parent = target.getParentFile();
			if (!parent.exists()) {
				parent.mkdirs();
			}
			inStream = new FileInputStream(source);
			outStream = new FileOutputStream(target);
			in = inStream.getChannel();
			out = outStream.getChannel();
			return in.transferTo(0, in.size(), out) == in.size();
		} finally {
			close(inStream);
			close(in);
			close(outStream);
			close(out);
		}
	}

	/**
	 * 关闭数据流
	 * @param closeable
	 */
	private static void close(Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (IOException ignore) {
			}
		}
	}
	
//	/**
//	 * 简单测试代码段
//	 * @param args
//	 * @throws Exception
//	 */
//	public static void test() throws Exception {
//		String content = "abc";
//		String password = "123456789";
//		System.out.println("content = " + content);
//		String contentE = new String(encrypt(password, content.getBytes(CHARSET_NAME)), CHARSET_NAME);
//		System.out.println("contentE = " + contentE);
//		String contentD = new String(decrypt(password, contentE.getBytes(CHARSET_NAME)), CHARSET_NAME);
//		System.out.println("contentD = " + contentD);
//		
//	}

	/**
	 * jar命令行的入口方法
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
//		写入渠道号
//		args = "-path D:/111.apk -outdir D:/111/ -contents googleplay;m360; -password 12345678".split(" ");
//		查看工具程序版本号
//		args = "-version".split(" ");
//		读取渠道号
//		args = "-path D:/111_m360.apk -password 12345678".split(" ");

		long time = System.currentTimeMillis();
		String cmdPath  = "-path";
		String cmdOutdir  = "-outdir";
		String cmdContents  = "-contents";
		String cmdPassword  = "-password";
		String cmdVersion  = "-version";
		String help = "用法：java -jar MCPTool.jar [" + cmdPath + "] [arg0] [" + cmdOutdir + "] [arg1] [" + cmdContents + "] [arg2] [" + cmdPassword + "] [arg3]"
				+ "\n" + cmdPath + "		APK文件路径"
				+ "\n" + cmdOutdir + "		输出路径（可选），默认输出到APK文件同一级目录"
				+ "\n" + cmdContents + "	写入内容集合，多个内容之间用“;”分割（linux平台请在“;”前加“\\”转义符），如：googleplay;m360; 当没有" + cmdContents + "”参数时输出已有文件中的contents"
				+ "\n" + cmdPassword + "	加密密钥（可选），长度8位以上，如果没有该参数，不加密"
				+ "\n" + cmdVersion + "	显示MCPTool版本号"
				+ "\n例如："
				+ "\n写入：java -jar MCPTool.jar -path D:/test.apk -outdir ./ -contents googleplay;m360; -password 12345678"
				+ "\n读取：java -jar MCPTool.jar -path D:/test.apk -password 12345678";
		
		if (args.length == 0 || args[0] == null || args[0].trim().length() == 0) {
			System.out.println(help);
		} else {
			if (args.length > 0) {
				if (args.length == 1 && cmdVersion.equals(args[0])) {
					System.out.println("version: " + VERSION_1_1);
				} else {
					Map<String, String> argsMap = new LinkedHashMap<String, String>();
					for (int i = 0; i < args.length; i += 2) {
						if (i + 1 < args.length) {
							if (args[i + 1].startsWith("-")) {
								throw new IllegalStateException("args is error, help: \n" + help);
							} else {
								argsMap.put(args[i], args[i + 1]);
							}
						}
					}
					System.out.println("argsMap = " + argsMap);
					File path = argsMap.containsKey(cmdPath) ? new File(argsMap.get(cmdPath)) : null;
					String parent = path == null? null : (path.getParent() == null ? "./" : path.getParent());
					File outdir = parent == null ? null : new File(argsMap.containsKey(cmdOutdir) ? argsMap.get(cmdOutdir) : parent);
					String[] contents = argsMap.containsKey(cmdContents) ? argsMap.get(cmdContents).split(";") : null;
					String password = argsMap.get(cmdPassword);
					if (path != null) {
						System.out.println("path: " + path);
						System.out.println("outdir: " + outdir);
						if (contents != null && contents.length > 0) {
							System.out.println("contents: " + Arrays.toString(contents));
						}
						System.out.println("password: " + password);
						if (contents == null || contents.length == 0) { // 读取数据；
							System.out.println("content: " + readContent(path, password));
						} else { // 写入数据；
							String fileName = path.getName();
							int dot = fileName.lastIndexOf(".");
							String prefix = fileName.substring(0, dot);
							String suffix = fileName.substring(dot);
							for (String content : contents) {
								File target = new File(outdir, prefix + "_" + content + suffix);
								if (nioTransferCopy(path, target)) {
									write(target, content, password);
								}
							}
						}
					}
				}
			}
		}
		System.out.println("time：" + (System.currentTimeMillis() - time));
	}
}