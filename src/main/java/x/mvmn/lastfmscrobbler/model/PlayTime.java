package x.mvmn.lastfmscrobbler.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayTime {
	@Builder.Default
	private int totalPlayTime = 0;
	private Integer startTime;
}
