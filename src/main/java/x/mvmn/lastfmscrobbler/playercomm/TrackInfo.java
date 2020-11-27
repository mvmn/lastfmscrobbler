package x.mvmn.lastfmscrobbler.playercomm;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import de.umass.lastfm.scrobble.ScrobbleData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackInfo {
	private String artist;
	private String albumArtist;
	private String title;
	private String album;
	private String mbid;
	private String filePath;
	private Integer duration;

	public void serialize(OutputStream out) {
		try {
			append(out, artist);
			append(out, albumArtist);
			append(out, title);
			append(out, album);
			append(out, mbid);
			append(out, filePath);
			append(out, duration != null ? duration.toString() : null);
			out.write(255);
		} catch (IOException ioe) {
			throw new RuntimeException("Failed to serialize TrackInfo", ioe);
		}
	}

	public static TrackInfo deserialize(byte[] value) {
		String[] fieldValues = new String[7];

		byte[] buffer = new byte[255];
		int fieldStartIdx = 0;
		int fieldNumber = 0;
		for (int i = 0; i < value.length; i++) {
			int nextIdx = i - fieldStartIdx + 1;
			byte b = value[i];
			if (b == 255) {
				break;
			}
			if (b == 0) {
				fieldStartIdx = i + 1;
				fieldValues[fieldNumber++] = nextIdx > 1 ? new String(buffer, 0, nextIdx - 1, StandardCharsets.UTF_8) : null;
			} else {
				// If buffer overflow - extend buffer
				if (nextIdx >= buffer.length) {
					buffer = Arrays.copyOf(buffer, buffer.length + 255);
				}
				buffer[nextIdx - 1] = b;
			}
		}

		return TrackInfo.builder().artist(fieldValues[0]).albumArtist(fieldValues[1]).title(fieldValues[2]).album(fieldValues[3])
				.mbid(fieldValues[4]).filePath(fieldValues[5]).duration(Integer.parseInt(fieldValues[6])).build();
	}

	private void append(OutputStream baos, String value) throws IOException {
		if (value != null) {
			baos.write(value.getBytes(StandardCharsets.UTF_8));
		}
		baos.write(0);
	}

	public ScrobbleData toScrobbleData() {
		ScrobbleData result = new ScrobbleData();
		result.setChosenByUser(true);
		result.setArtist(artist);
		result.setAlbumArtist(albumArtist);
		result.setAlbum(album);
		result.setTrack(title);
		result.setDuration(duration);
		result.setMusicBrainzId(mbid);
		return result;
	}

	// TODO: convert to a unit test
	public static void main(String args[]) {
		TrackInfo ti = TrackInfo.builder().artist("test value").title("mock & test").duration(12345).build();
		System.out.println(ti);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ti.serialize(baos);
		System.out.println(deserialize(baos.toByteArray()));
	}
}
