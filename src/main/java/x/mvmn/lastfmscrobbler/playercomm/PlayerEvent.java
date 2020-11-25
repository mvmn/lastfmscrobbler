package x.mvmn.lastfmscrobbler.playercomm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerEvent {
	private EventType eventType;
	private String playerId;
	private TrackInfo trackInfo;
}
