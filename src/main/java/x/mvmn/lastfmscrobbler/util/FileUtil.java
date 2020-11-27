package x.mvmn.lastfmscrobbler.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;

public class FileUtil {

	public static void appendToFile(File file, byte[] data) throws IOException {
		FileUtils.writeByteArrayToFile(file, data, true);
	}

	public static void appendToFile(File file, String string) throws IOException {
		FileUtils.writeStringToFile(file, string, StandardCharsets.UTF_8, true);
	}
}
