package org.highscreen.utility;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Vector;

public class Extractor {
	private int sections;

	private Vector<ChunkInfo> chunks;
	private String[] chunkNames = { "bootloader", "kernel", "system", "data",
			"package", "recovery", "cache" };
	private static RandomAccessFile image;

	public Extractor(String fileName) {
		try {
			image = new RandomAccessFile(fileName, "r");
			readHeader();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void readHeader() throws Exception {

		sections = readLEInt();
		chunks = new Vector<ChunkInfo>();
		for (int i = 0; i < sections; i++) {
			chunks.add(new ChunkInfo(readLEInt(), readLEInt(), readMD5(),
					chunkNames[i]));
		}
	}

	private int readLEInt() throws IOException {
		return Integer.reverseBytes(image.readInt());
	}

	private String readMD5() throws IOException {
		byte[] md5 = new byte[16];
		String result = "";
		image.read(md5);
		for (byte b : md5) {
			result += Integer.toString((b & 0xff) + 0x100, 16).substring(1);
		}
		return result;
	}

	private static String getMD5Checksum(byte[] data) {
		MessageDigest chk;
		String result = "";
		try {
			chk = MessageDigest.getInstance("MD5");

			chk.update(data);
			byte[] digest = chk.digest();
			for (byte b : digest) {
				result += Integer.toString((b & 0xff) + 0x100, 16).substring(1);
			}
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return result;
	}

	public byte[] readChunk(ChunkInfo info) throws Exception {
		byte[] data = new byte[info.size];
		image.seek(info.start);
		image.read(data);
		if (!getMD5Checksum(data).equals(info.hash)) {
			throw new Exception("Broken image!");
		}
		System.out.println("Chunk " + info.name + "("
				+ Integer.toHexString(info.start) + " to "
				+ Integer.toHexString(info.start + info.size)
				+ ") read successfully");
		return data;
	}

	public void writeChunkToFile(byte[] data, String filename) throws Exception {
		RandomAccessFile file = new RandomAccessFile(filename, "rw");
		file.write(data);
		System.out.println("Chunk extracted successfully: check " + filename);
	}

	public void splitImage() {
		try {
			for (ChunkInfo c : chunks) {
				writeChunkToFile(readChunk(c), c.name);

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {

		Extractor ex = new Extractor(args[0]);
		ex.splitImage();

	}

}
