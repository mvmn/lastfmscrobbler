package x.mvmn.lastfmscrobbler;

import java.awt.AWTException;
import java.awt.CheckboxMenuItem;
import java.awt.Menu;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

import x.mvmn.lastfmscrobbler.playercomm.CommParser;
import x.mvmn.lastfmscrobbler.playercomm.PlayerEvent;
import x.mvmn.lastfmscrobbler.util.SwingUtil;

public class LastFMScrobbler {

	protected final PopupMenu popup;
	// TODO: Make configurable
	protected final String key = "9e89b44de1ff37c5246ad0af18406454";
	// TODO: Make configurable
	protected final String secret = "147320ea9b8930fe196a4231da50ada4";

	public static void main(String[] args) {
		new LastFMScrobbler();
	}

	public LastFMScrobbler() {
		// TODO: Store configuration
		// TODO: Configurable username/password
		// TODO: Encrypt password
		// TODO: Scrobbler handler - timers
		// TODO: Scrobbler handler - submit to Last.fm
		// TODO: Scrobbler - indicate last.fm session state
		// TODO: Store tracks on submit fail, resubmit on startup/login
		// TODO: Refactor spaghetti
		Server server = new Server(33367, line -> {
			PlayerEvent playerCommand = CommParser.parse(line);
			System.out.println(playerCommand);
		});
		try {
			server.start();
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}

		TrayIcon trayIcon;
		try {
			trayIcon = new TrayIcon(ImageIO.read(this.getClass().getResourceAsStream("/logo.png")));
			trayIcon.setImageAutoSize(true);
		} catch (IOException e1) {
			throw new RuntimeException(e1);
		}
		popup = new PopupMenu();

		// Add components to pop-up menu
		popup.add(SwingUtil.menuItem("About", event -> JOptionPane.showMessageDialog(null, "Last.fm scrobbler")));
		Map<String, Integer> values = new LinkedHashMap<>();
		for (int value : new int[] { 10, 30, 50, 70, 90 }) {
			values.put("" + value + "% of track", value);
		}
		popup.add(optionsSubmenu("Scrobble on...", values, 50, val -> System.out.println(val)));
		popup.addSeparator();
		popup.add(SwingUtil.menuItem("Quit", event -> {
			SystemTray.getSystemTray().remove(trayIcon);
			try {
				server.stop();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}));

		trayIcon.setPopupMenu(popup);

		try {
			SystemTray.getSystemTray().add(trayIcon);
		} catch (AWTException e) {
			// Show error and quit app
			ExceptionDisplayer.INSTANCE.accept(e);
			throw new RuntimeException(e);
		}
	}

	protected static <T> Menu optionsSubmenu(String name, Map<String, T> options, T currentVal, Consumer<T> onOptionSelect) {
		Menu menu = new Menu(name);

		for (Map.Entry<String, T> option : options.entrySet()) {
			CheckboxMenuItem menuItem = new CheckboxMenuItem(option.getKey(), option.getValue().equals(currentVal));
			menuItem.addItemListener(event -> {
				T selectedValue = null;
				for (int i = 0; i < menu.getItemCount(); i++) {
					CheckboxMenuItem cbmi = (CheckboxMenuItem) menu.getItem(i);
					boolean match = cbmi == menuItem;
					cbmi.setState(match);
					if (match) {
						selectedValue = options.get(cbmi.getLabel());
					}
				}
				onOptionSelect.accept(selectedValue);
			});
			menu.add(menuItem);
		}

		return menu;
	}
}
