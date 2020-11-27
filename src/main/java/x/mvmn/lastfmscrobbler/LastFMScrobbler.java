package x.mvmn.lastfmscrobbler;

import java.awt.AWTException;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import de.umass.lastfm.Authenticator;
import de.umass.lastfm.Session;
import de.umass.lastfm.Track;
import de.umass.lastfm.scrobble.ScrobbleResult;
import x.mvmn.lastfmscrobbler.gui.UsernamePasswordDialog;
import x.mvmn.lastfmscrobbler.playercomm.CommParser;
import x.mvmn.lastfmscrobbler.playercomm.EventType;
import x.mvmn.lastfmscrobbler.playercomm.PlayerEvent;
import x.mvmn.lastfmscrobbler.util.Pair;
import x.mvmn.lastfmscrobbler.util.SwingUtil;

public class LastFMScrobbler {

	public static final LastFMScrobbler INSTANCE;
	private final PopupMenu popup;
	private final MenuItem statusDisplay = new MenuItem("Status: loading credentials...");
	private final TrayIcon trayIcon;
	private final BufferedImage imgDisconnected;
	private final BufferedImage imgConnected;
	private final Server server;
	private final AppSettings prefs;
	private final AtomicReference<Session> currentSessionRef = new AtomicReference<>();

	static {
		// TODO: Logger
		// TODO: Scrobbler handler - timers
		// TODO: Scrobbler handler - submit to Last.fm
		// TODO: Scrobbler - indicate last.fm session state
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
		INSTANCE.startServer();
		INSTANCE.connect();
	}

	public void startServer() {
		try {
			this.server.start();
		} catch (IOException e) {
			ExceptionDisplayer.INSTANCE.accept(e);
			throw new RuntimeException(e);
		}
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

		// Add components to pop-up menu
		result.add(SwingUtil.menuItem("About", event -> JOptionPane.showMessageDialog(null, "Last.fm scrobbler")));
		Map<String, Integer> values = new LinkedHashMap<>();
		for (int value : new int[] { 10, 30, 50, 70, 90 }) {
			values.put("" + value + "% of track", value);
		}
		result.add(SwingUtil.radiobuttonsSubmenu("Scrobble on...", values, 50, val -> System.out.println(val)));
		result.addSeparator();
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
		try {
			PlayerEvent playerCommand = CommParser.parse(playerEventData);
			if (EventType.START.equals(playerCommand.getEventType()) && playerCommand.getTrackInfo() != null) {
				Session session = currentSessionRef.get();
				if (session != null) {
					try {
						ScrobbleResult result = Track.updateNowPlaying(playerCommand.getTrackInfo().toScrobbleData(), session);
						if (!result.isSuccessful()) {
							// TODO: handle
							System.err.println(result.getErrorMessage());
						}
					} catch (Exception e) {
						e.printStackTrace();
						// TODO: log
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			// TODO: log
		}
	}
}
