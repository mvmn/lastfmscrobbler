package x.mvmn.lastfmscrobbler.model;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import de.umass.lastfm.scrobble.ScrobbleData;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import x.mvmn.lastfmscrobbler.playercomm.TrackInfo;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TrackLogEntry extends TrackInfo {
	public static final int END_OF_ENTRY_MARKER = '\n';

	protected final Integer timestamp;

	public void serialize(OutputStream out) {
		try {
			append(out, artist);
			append(out, albumArtist);
			append(out, title);
			append(out, album);
			append(out, mbid);
			append(out, filePath);
			append(out, duration != null ? duration.toString() : null);
			append(out, timestamp != null ? timestamp.toString() : null);
			out.write(END_OF_ENTRY_MARKER);
		} catch (IOException ioe) {
			throw new RuntimeException("Failed to serialize TrackInfo", ioe);
		}
	}

	public static TrackLogEntry deserialize(byte[] value) {
		String[] fieldValues = new String[8];

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

		return TrackLogEntry.builder()
				.artist(fieldValues[0])
				.albumArtist(fieldValues[1])
				.title(fieldValues[2])
				.album(fieldValues[3])
				.mbid(fieldValues[4])
				.filePath(fieldValues[5])
				.duration(Integer.parseInt(fieldValues[6]))
				.timestamp(Integer.parseInt(fieldValues[7]))
				.build();
	}

	private void append(OutputStream baos, String value) throws IOException {
		if (value != null) {
			baos.write(value.getBytes(StandardCharsets.UTF_8));
		}
		baos.write(0);
	}

	public static TrackLogEntry of(TrackInfo trackInfo, int playStartTime) {
		return TrackLogEntry.builder()
				.timestamp(playStartTime)
				.artist(trackInfo.getArtist())
				.albumArtist(trackInfo.getAlbumArtist())
				.title(trackInfo.getTitle())
				.album(trackInfo.getAlbum())
				.duration(trackInfo.getDuration())
				.mbid(trackInfo.getMbid())
				.filePath(trackInfo.getFilePath())
				.build();
	}

	@Override
	public ScrobbleData toScrobbleData() {
		ScrobbleData result = super.toScrobbleData();
		if (timestamp != null) {
			result.setTimestamp(timestamp);
		}
		return result;
	}
}
