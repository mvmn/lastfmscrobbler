package x.mvmn.lastfmscrobbler.playercomm;

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
}
