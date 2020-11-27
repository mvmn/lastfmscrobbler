package x.mvmn.lastfmscrobbler.playercomm;

import java.util.HashMap;
import java.util.Map;

public class CommParser {

	public static PlayerEvent parse(String input) {
		if (input == null || input.trim().isEmpty()) {
			return null;
		}
		int firstSpaceIdx = input.indexOf(" ");
		if (firstSpaceIdx > 0) {
			String command = input.substring(0, firstSpaceIdx);
			String params = input.substring(firstSpaceIdx + 1);

			EventType eventType = EventType.valueOf(command);

			Map<String, String> paramsMap = parseParams(params);
			String playerId = paramsMap.get("c");
			if (playerId == null) {
				playerId = "";
			}

			return PlayerEvent.builder()
					.eventType(eventType)
					.playerId(playerId.trim())
					.trackInfo(EventType.START.equals(eventType) ? getTrackInfo(paramsMap) : null)
					.build();
		} else {
			throw new IllegalArgumentException("Bad input - doesn not match command+params format: " + input);
		}

	}

	protected static TrackInfo getTrackInfo(Map<String, String> p) {
		// See https://github.com/lastfm/lastfm-desktop/blob/master/lib/listener/PlayerCommandParser.cpp
		return TrackInfo.builder()
				.artist(p.get("a"))
				.albumArtist(p.get("d"))
				.title(p.get("t"))
				.album(p.get("b"))
				.mbid(p.get("m"))
				.duration(Integer.parseInt(p.get("l")))
				.filePath(p.get("p"))
				.build();
	}

	protected static Map<String, String> parseParams(String params) {
		Map<String, String> result = new HashMap<>();
		StringBuilder tmp = new StringBuilder();
		String paramName = null;
		boolean postAmpersand = false;
		for (char c : params.toCharArray()) {
			if (c == '&') {
				if (postAmpersand) {
					tmp.append('&');
					postAmpersand = false;
				} else {
					postAmpersand = true;
				}
			} else {
				if (postAmpersand) {
					result.put(paramName, tmp.toString());
					paramName = null;
					tmp.setLength(0);
					tmp.append(c);
				} else if (c == '=') {
					paramName = tmp.toString();
					tmp.setLength(0);
				} else {
					tmp.append(c);
				}
				postAmpersand = false;
			}
		}
		if (paramName != null && tmp.length() > 0) {
			result.put(paramName, tmp.toString());
		}
		return result;
	}
}
