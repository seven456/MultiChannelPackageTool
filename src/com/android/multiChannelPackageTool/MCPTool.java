/*
 * 创建日期：2014年12月16日 下午18:56:29
 */
package com.android.multiChannelPackageTool;

import java.io.BufferedOutputStream;
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

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;

/**
 * 多渠道打包工具；
 * @author zhangguojun
 * @version 1.0
 * @since JDK1.6
 */
public class MCPTool {
	/**
	 * 数据结构体的魔数标记，类似于头标记
	 */
	private static final String MAGIC = "MCPT";
	/**
	 * 数据结构的版本号
	 */
	private static final String VERSION_10 = "1.0";
	/**
	 * 数据编码格式
	 */
	private static final String CHARSET_NAME = "UTF-8";
	/**
	 * 加密用的IvParameterSpec参数
	 */
	private static final byte[] IV = new byte[] { 1, 3, 1, 4, 5, 2, 0, 1 };

	/**
	 * 读取数据结构的版本号
	 * @param raf RandomAccessFile
	 * @return 数组对象，[0] randomAccessFile.seek的index，[1] 数据结构的版本号
	 * @throws IOException
	 */
	private static Object[] getVersion(RandomAccessFile raf) throws IOException {
		String version = null;
		byte[] bytesMagic = MAGIC.getBytes(CHARSET_NAME);
		byte[] bytes = new byte[bytesMagic.length];
		long index = raf.length();
		index -= bytesMagic.length;
		readFully(raf, index, bytes); // 读取Magic标记；
		if (Arrays.equals(bytes, bytesMagic)) {
			bytes = new byte[4];
			index -= bytes.length;
			readFully(raf, index, bytes); // 读取版本号长度；
			int lengthVersion = streamToInt(bytes, 0);
			index -= lengthVersion;
			byte[] bytesVersion = new byte[lengthVersion];
			readFully(raf, index, bytesVersion); // 读取内容；
			version = new String(bytesVersion, CHARSET_NAME);
		}
		return new Object[] { index, version };
	}

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
		RandomAccessFile raf = new RandomAccessFile(path, "r");
		String version = (String) getVersion(raf)[1];
		raf.close();
		if (version != null) {
			throw new IllegalStateException("Data is exists, Repeated write is not recommended.");
		}
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(path, true));
		boolean isEncrypt = password != null && password.length() > 0;
		byte[] bytesContent = isEncrypt ? encrypt(password, content) : content;
		byte[] bytesVersion = VERSION_10.getBytes(CHARSET_NAME);
		bos.write(bytesContent); // 写入内容；
		bos.write(intToStream(bytesContent.length)); // 写入内容长度；
		bos.write(isEncrypt ? 1 : 0); // 写入是否加密标示；
		bos.write(bytesVersion); // 写入版本号；
		bos.write(intToStream(bytesVersion.length)); // 写入版本号长度；
		bos.write(MAGIC.getBytes(CHARSET_NAME)); // 写入Magic标记；
		bos.flush();
		bos.close();
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
	 * 读取数据
	 * @param path 文件路径
	 * @param password 解密密钥
	 * @return 被该工具写入的数据（如：渠道号）
	 * @throws Exception
	 */
	private static byte[] read(File path, String password) throws Exception {
		byte[] bytesContent = null;
		byte[] bytesMagic = MAGIC.getBytes(CHARSET_NAME);
		byte[] bytes = new byte[bytesMagic.length];
		RandomAccessFile raf = new RandomAccessFile(path, "r");
		Object[] versions = getVersion(raf);
		long index = (long) versions[0];
		String version = (String) versions[1];
		if (VERSION_10.equals(version)) {
			bytes = new byte[1];
			index -= bytes.length;
			readFully(raf, index, bytes); // 读取内容长度；
			boolean isEncrypt = bytes[0] == 1;

			bytes = new byte[4];
			index -= bytes.length;
			readFully(raf, index, bytes); // 读取内容长度；
			int lengthContent = streamToInt(bytes, 0);

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
	 * int转换成字节数组（小端序）
	 * @param data
	 * @return
	 */
	private static byte[] intToStream(int data) {
		ByteBuffer buffer = ByteBuffer.allocate(4);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(data);
		buffer.flip();
		return buffer.array();
	}

	/**
	 * 字节数组转换成int（小端序）
	 * @param stream
	 * @param offset
	 * @return
	 */
	private static int streamToInt(byte[] stream, int offset) {
		ByteBuffer buffer = ByteBuffer.allocate(4);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.put(stream[offset]);
		buffer.put(stream[offset + 1]);
		buffer.put(stream[offset + 2]);
		buffer.put(stream[offset + 3]);
		return buffer.getInt(0);
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
//		args = "-path D:/111.zip -outdir D:/ -contents googleplay;m360; -password 12345678".split(" ");
//		args = "-version".split(" ");
//		args = "-path D:/111_m360.zip -password 12345678".split(" ");
		
		String cmdPath  = "-path";
		String cmdOutdir  = "-outdir";
		String cmdContents  = "-contents";
		String cmdPassword  = "-password";
		String cmdVersion  = "-version";
		String help = "用法：java -jar MCPTool.jar [" + cmdPath + "] [arg0] [" + cmdOutdir + "] [arg1] [" + cmdContents + "] [arg2] [" + cmdPassword + "] [arg3]"
				+ "\n" + cmdPath + "		APK文件路径"
				+ "\n" + cmdOutdir + "		输出路径（可选），默认输出到APK文件同一级目录"
				+ "\n" + cmdContents + "	写入内容集合，多个内容之间用“;”分割，如：googleplay;m360; 当没有" + cmdContents + "”参数时输出已有文件中的contents"
				+ "\n" + cmdPassword + "	加密密钥（可选），长度8位以上，如果没有该参数，不加密"
				+ "\n" + cmdVersion + "	显示版本号"
				+ "\n例如："
				+ "\n写入：java -jar MCPTool.jar -path D:/test.apk -outdir ./ -contents googleplay;m360; -password 12345678"
				+ "\n读取：java -jar MCPTool.jar -path D:/test.apk -password 12345678";
		
		Map<String, String> argsMap = new LinkedHashMap<String, String>();
		if (args.length > 0) {
			for (int i = 0; i < args.length; i += 2) {
				if (args[i + 1].startsWith("-")) {
					throw new IllegalStateException("args is error, help: \n" + help);
				} else {
					argsMap.put(args[i], args[i + 1]);
				}
				
			}
		}
		System.out.println("argsMap = " + argsMap);
		if (args.length == 0 || args[0] == null || args[0].trim().length() == 0) {
			System.out.println(help);
		} else {
			File path = new File(argsMap.get(cmdPath));
			String parent = path.getParent() == null ? "./" : path.getParent();
			File outdir = new File(argsMap.containsKey(cmdOutdir) ? argsMap.get(cmdOutdir) : parent);
			String[] contents = argsMap.containsKey(cmdContents) ? argsMap.get(cmdContents).split(";") : null;
			String password = argsMap.get(cmdPassword);
			String version = argsMap.get(cmdVersion);
			if (version != null) {
				System.out.println("version: " + version);
			} else if (path != null) {
				System.out.println("path: " + path);
				System.out.println("outdir: " + outdir);
				if (contents != null && contents.length > 0) {
					System.out.println("contents: " + Arrays.toString(contents));
				}
				System.out.println("password: " + password);
				if (contents == null || contents.length == 0) {
					System.out.println("content: " + readContent(path, password));
				} else {
					String fileName = path.getName();
					int dot = fileName.lastIndexOf(".");
					String prefix = fileName.substring(0, dot);
					String suffix = fileName.substring(dot);
					for (String content : contents) {
						File target = new File(outdir, prefix + "_" + content + suffix);
						if (nioTransferCopy(path, target)) {
							write(target, content, password);
							System.out.println("Write finish, [" + content + "] to [" + target + "]");
						}
					}
				}
			}
		}
	}
}