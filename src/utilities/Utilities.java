package utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

public class Utilities {

	public static String toString(final String filename) throws IOException {
		File file = new File(filename);
		if (file.exists()) {
			FileInputStream stream = null;
			try {
				stream = new FileInputStream(file);
			} catch (final FileNotFoundException e) {
			}

			final FileChannel fc = stream.getChannel();
			final MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());

			final String content = Charset.defaultCharset().decode(bb).toString();

			stream.close();

			return content;
		}
		return null;
	}
}