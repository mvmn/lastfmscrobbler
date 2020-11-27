package x.mvmn.lastfmscrobbler.playercomm;

import de.umass.lastfm.scrobble.ScrobbleData;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@AllArgsConstructor
@SuperBuilder
public class TrackInfo {
	protected final String artist;
	protected final String albumArtist;
	protected final String title;
	protected final String album;
	protected final String mbid;
	protected final String filePath;
	protected final Integer duration;

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
}
