package x.mvmn.lastfmscrobbler;

import java.awt.AWTException;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import de.umass.lastfm.Authenticator;
import de.umass.lastfm.Session;
import de.umass.lastfm.Track;
import de.umass.lastfm.scrobble.ScrobbleResult;
import x.mvmn.lastfmscrobbler.gui.UsernamePasswordDialog;
import x.mvmn.lastfmscrobbler.model.PlayTime;
import x.mvmn.lastfmscrobbler.model.TrackLogEntry;
import x.mvmn.lastfmscrobbler.playercomm.CommParser;
import x.mvmn.lastfmscrobbler.playercomm.PlayerEvent;
import x.mvmn.lastfmscrobbler.playercomm.TrackInfo;
import x.mvmn.lastfmscrobbler.util.LangUtils;
import x.mvmn.lastfmscrobbler.util.Pair;
import x.mvmn.lastfmscrobbler.util.SwingUtil;

public class LastFMScrobbler {

	public static final LastFMScrobbler INSTANCE;
	private final PopupMenu popup;
	private final MenuItem statusDisplay = new MenuItem("Status: starting...");
	private final MenuItem nowPlayingDisplay = new MenuItem("");
	private final TrayIcon trayIcon;
	private final BufferedImage imgDisconnected;
	private final BufferedImage imgConnected;
	private final Server server;
	private final AppSettings prefs;
	private final AtomicReference<Session> currentSessionRef = new AtomicReference<>();
	private final Map<String, Pair<TrackLogEntry, PlayTime>> playingNowPerPlayer = Collections.synchronizedMap(new HashMap<>());
	private volatile int percentageToScrobbleAt = 50;

	static {
		System.setProperty("apple.awt.UIElement", "true");
		Logger.getLogger("de.umass.lastfm.Caller").setLevel(Level.WARNING);
		// TODO: Logger
		// TODO: Store tracks on submit fail, resubmit on startup/login
		// TODO: Better error handling in Server

		try {
			BufferedImage imgDisconnected = ImageIO.read(LastFMScrobbler.class.getResourceAsStream("/logo_disconnected.png"));
			BufferedImage imgConnected = ImageIO.read(LastFMScrobbler.class.getResourceAsStream("/logo_connected.png"));

			AppSettings prefs = new AppSettings();

			Server server = new Server(33367, line -> LastFMScrobbler.getInstance().processPlayerEvent(line));

			INSTANCE = new LastFMScrobbler(prefs, server, imgDisconnected, imgConnected);
		} catch (Exception e) {
			ExceptionDisplayer.INSTANCE.accept(e);
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) throws GeneralSecurityException {
		INSTANCE.start();
		INSTANCE.connect();
	}

	public void start() {
		try {
			this.server.start();
		} catch (IOException e) {
			ExceptionDisplayer.INSTANCE.accept(e);
			throw new RuntimeException(e);
		}
		Thread submitThread = new Thread(() -> {
			boolean go = true;
			while (go) {
				processSubmissions();
				try {
					Thread.sleep(5000L);
				} catch (InterruptedException e) {
					Thread.interrupted();
					go = false;
				}
			}
		});
		submitThread.setDaemon(true);
		submitThread.start();
	}

	public synchronized void connect() {
		String user = prefs.getUsername();
		String password = null;
		try {
			password = prefs.getPassword();
		} catch (Exception e) {
			ExceptionDisplayer.INSTANCE.accept(e);
			// TODO: log
		}
		if (user != null && password != null) {
			try {
				setStatus("Connecting...");
				Session session = Authenticator.getMobileSession(user, password, prefs.getApiKey(), prefs.getApiSecret());
				if (session == null) {
					throw new Exception("Failed to connect");
				}
				currentSessionRef.set(session);

				setStatus("Connected");
				setConnected(true);
			} catch (Exception e) {
				// TODO: log
				ExceptionDisplayer.INSTANCE.accept(e);
				setStatus("Connection error");
				setConnected(false);
			}
		} else {
			setStatus("Credentials not set");
			setConnected(false);
		}
	}

	public void setStatus(String status) {
		SwingUtilities.invokeLater(() -> statusDisplay.setLabel("Status: " + status));
	}

	public void setConnected(boolean connected) {
		SwingUtilities.invokeLater(() -> trayIcon.setImage(connected ? imgConnected : imgDisconnected));
	}

	public static LastFMScrobbler getInstance() {
		return INSTANCE;
	}

	public LastFMScrobbler(AppSettings prefs, Server server, BufferedImage imgDisconnected, BufferedImage imgConnected) {
		this.prefs = prefs;
		this.server = server;
		this.imgDisconnected = imgDisconnected;
		this.imgConnected = imgConnected;
		this.trayIcon = new TrayIcon(imgDisconnected);
		this.trayIcon.setImageAutoSize(true);

		// Init GUI
		popup = initGui();
		trayIcon.setPopupMenu(popup);

		try {
			SystemTray.getSystemTray().add(trayIcon);
		} catch (AWTException e) {
			throw new RuntimeException(e);
		}
	}

	private PopupMenu initGui() {
		PopupMenu result = new PopupMenu();
		result.add(statusDisplay);
		result.add(nowPlayingDisplay);
		result.addSeparator();

		// Add components to pop-up menu
		Set<Integer> possibleValues = new TreeSet<Integer>(Arrays.asList(10, 30, 50, 70, 90));
		Map<String, Integer> values = new LinkedHashMap<>();
		for (Integer value : possibleValues) {
			values.put("" + value + "% of track", value);
		}
		int percentageVal = prefs.getPercentageToScrobbleAt(50);
		if (!possibleValues.contains(percentageVal)) {
			percentageVal = 50;
		}
		percentageToScrobbleAt = percentageVal;
		result.add(SwingUtil.radiobuttonsSubmenu("Scrobble on...", values, percentageVal, val -> {
			percentageToScrobbleAt = val;
			prefs.setPercentageToScrobbleAt(val);
		}));
		result.add(SwingUtil.menuItem("Set credentials...", event -> {
			try {
				String user = prefs.getUsername();
				String password = prefs.getPassword();
				new UsernamePasswordDialog(null, "Set credentials", true, new Pair<String, String>(user, password), unPwdPair -> {
					new Thread(() -> {
						try {
							prefs.setUsername(unPwdPair.getK());
							prefs.setPassword(new String(unPwdPair.getV()));
							LastFMScrobbler.INSTANCE.connect();
						} catch (Exception e) {
							ExceptionDisplayer.INSTANCE.accept(e);
							// TODO: log
						}
					}).start();
				}).setVisible(true);
			} catch (Exception e) {
				ExceptionDisplayer.INSTANCE.accept(e);
				// TODO: log
			}
		}));
		result.add(SwingUtil.menuItem("Set API keys...", event -> {
			try {
				String key = prefs.getApiKey();
				String secret = prefs.getApiSecret();
				new UsernamePasswordDialog(null, "Set API keys", true, new Pair<String, String>(key, secret), unPwdPair -> {
					new Thread(() -> {
						try {
							prefs.setApiKey(unPwdPair.getK());
							prefs.setApiSecret(new String(unPwdPair.getV()));
							LastFMScrobbler.INSTANCE.connect();
						} catch (Exception e) {
							ExceptionDisplayer.INSTANCE.accept(e);
							// TODO: log
						}
					}).start();
				}, "API Key", "API Secret").setVisible(true);
			} catch (Exception e) {
				ExceptionDisplayer.INSTANCE.accept(e);
				// TODO: log
			}
		}));
		result.addSeparator();
		result.add(SwingUtil.menuItem("About", event -> JOptionPane.showMessageDialog(null, "Last.fm scrobbler by Mykola Makhin")));
		result.add(SwingUtil.menuItem("Quit", event -> {
			SystemTray.getSystemTray().remove(trayIcon);
			try {
				server.stop();
			} catch (IOException e) {
				// TODO: log
				e.printStackTrace();
			}
		}));
		return result;
	}

	public void processPlayerEvent(String playerEventData) {
		PlayerEvent playerCommand = null;
		try {
			// TODO: debug log
			playerCommand = CommParser.parse(playerEventData);
		} catch (Exception e) {
			ExceptionDisplayer.INSTANCE.accept(e);
			e.printStackTrace();
			// TODO: log
		}
		if (playerCommand != null && playerCommand.getEventType() != null) {
			String playerId = LangUtils.getOrDefault(playerCommand.getPlayerId(), "");
			int nowSeconds = (int) (System.currentTimeMillis() / 1000);
			switch (playerCommand.getEventType()) {
				case START:
					if (playerCommand.getTrackInfo() != null) {
						// Make sure to scrobble previous track before switching to new

						Pair<TrackLogEntry, PlayTime> previouslyPlayed = playingNowPerPlayer.put(playerId,
								new Pair<>(TrackLogEntry.of(playerCommand.getTrackInfo(), nowSeconds),
										PlayTime.builder().startTime(nowSeconds).build()));
						if (previouslyPlayed != null) {
							scrobblePlayingTrackIfTimeTo(nowSeconds, previouslyPlayed);
						}

						Session session = currentSessionRef.get();
						if (session != null) {
							try {
								System.out.println("Now playing " + playerCommand.getTrackInfo());
								TrackInfo trackInfo = playerCommand.getTrackInfo();
								SwingUtilities.invokeLater(() -> nowPlayingDisplay.setLabel(toNowPlayingDisplay(trackInfo)));
								ScrobbleResult result = Track.updateNowPlaying(trackInfo.toScrobbleData(), session);
								if (!result.isSuccessful()) {
									// TODO: handle
									System.err.println(result.getErrorMessage());
								}
							} catch (Exception e) {
								e.printStackTrace();
								// TODO: log
							}
						}
					} else {
						// TODO: log warning
					}
				break;
				case TERM:
				case STOP:
					playingNowPerPlayer.remove(playerId);
					SwingUtilities.invokeLater(() -> nowPlayingDisplay.setLabel(""));
				break;
				case PAUSE: {
					SwingUtilities.invokeLater(() -> nowPlayingDisplay.setLabel("Paused"));
					Pair<TrackLogEntry, PlayTime> nowPlaying = playingNowPerPlayer.get(playerId);
					if (nowPlaying != null) {
						PlayTime playTime = nowPlaying.getV();
						if (playTime.getStartTime() != null) {
							playTime.setTotalPlayTime(playTime.getTotalPlayTime() + (nowSeconds - playTime.getStartTime()));
							playTime.setStartTime(null);
						}
					}
				}
				break;
				case RESUME: {
					Pair<TrackLogEntry, PlayTime> nowPlaying = playingNowPerPlayer.get(playerId);
					if (nowPlaying != null) {
						PlayTime playTime = nowPlaying.getV();
						if (playTime.getStartTime() == null) {
							playTime.setStartTime(nowSeconds);
						}
						SwingUtilities.invokeLater(() -> nowPlayingDisplay.setLabel(toNowPlayingDisplay(nowPlaying.getK())));
					} else {
						SwingUtilities.invokeLater(() -> nowPlayingDisplay.setLabel(""));
					}
				}
				break;
				case BOOTSTRAP:
				case INIT:
				default:
			}
		} else {
			// TODO: log warning
		}
	}

	public void processSubmissions() {
		try {
			int nowSec = (int) (System.currentTimeMillis() / 1000);
			Map<String, Pair<TrackLogEntry, PlayTime>> nowPlayingAllPlayers;
			synchronized (playingNowPerPlayer) {
				nowPlayingAllPlayers = new HashMap<>(playingNowPerPlayer);
			}
			for (Map.Entry<String, Pair<TrackLogEntry, PlayTime>> nowPlayingEntry : nowPlayingAllPlayers.entrySet()) {
				Pair<TrackLogEntry, PlayTime> nowPlayingData = nowPlayingEntry.getValue();
				if (scrobblePlayingTrackIfTimeTo(nowSec, nowPlayingData)) {
					playingNowPerPlayer.remove(nowPlayingEntry.getKey());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			// TODO: log
		}
	}

	private boolean scrobblePlayingTrackIfTimeTo(int nowSec, Pair<TrackLogEntry, PlayTime> nowPlayingData) {
		PlayTime playTime = nowPlayingData.getV();
		TrackLogEntry trackData = nowPlayingData.getK();
		// start time == null means the track is paused. If it's paused - take sum of play time.
		// If it is not paysed - take sum of play time + difference between now and last start time.
		// FYI: Last start time is either time from beginning of track play, or time since last resume after pause.
		long listenTimeSec = (playTime.getStartTime() != null ? (nowSec - playTime.getStartTime()) : 0) + playTime.getTotalPlayTime();
		long listenPercentage = Math.round(100 * ((double) listenTimeSec) / ((double) trackData.getDuration()));
		if (listenPercentage > percentageToScrobbleAt) {
			// Listened beyond specified percentage - time to scrobble
			attemptScrobble(trackData);
			return true;
		} else {
			return false;
		}
	}

	private void attemptScrobble(TrackLogEntry trackData) {
		Session session = currentSessionRef.get();
		if (session != null) {
			try {
				System.out.println("Scrobbling " + trackData);
				ScrobbleResult scrobbleResult = Track.scrobble(trackData.toScrobbleData(), session);
				if (!scrobbleResult.isSuccessful()) {
					setConnected(false);
					setStatus("Scrobbling failed");
					// TODO: log
					// TODO: queue for scrobbling
					System.out.println("Scrobble error: " + scrobbleResult.getErrorMessage());
				} else {
					setConnected(true);
					setStatus("Connected");
				}
			} catch (Exception e) {
				setConnected(false);
				setStatus("Scrobbling failed");
				// TODO: log
				// TODO: queue for scrobbling
				e.printStackTrace();
			}
		} else {
			// TODO: queue for scrobbling
		}
	}

	private String toNowPlayingDisplay(TrackInfo trackInfo) {
		return trackInfo != null ? ("Playing: '" + trackInfo.getTitle() + "' by " + trackInfo.getArtist()) : "";
	}
}
