package x.mvmn.lastfmscrobbler.util;

import java.awt.CheckboxMenuItem;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.util.Map;
import java.util.function.Consumer;

public class SwingUtil {
	public static MenuItem menuItem(String text, ActionListener handler) {
		MenuItem menuItem = new MenuItem(text);
		menuItem.addActionListener(handler);
		return menuItem;
	}

	public static CheckboxMenuItem checkboxMenuItem(String text, boolean state, ItemListener handler) {
		CheckboxMenuItem menuItem = new CheckboxMenuItem(text, state);
		menuItem.addItemListener(handler);
		return menuItem;
	}

	public static <T> Menu radiobuttonsSubmenu(String name, Map<String, T> options, T currentVal, Consumer<T> onOptionSelect) {
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

	public static void moveToScreenCenter(final Component component) {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension componentSize = component.getSize();
		int newComponentX = screenSize.width - componentSize.width;
		if (newComponentX >= 0) {
			newComponentX = newComponentX / 2;
		} else {
			newComponentX = 0;
		}
		int newComponentY = screenSize.height - componentSize.height;
		if (newComponentY >= 0) {
			newComponentY = newComponentY / 2;
		} else {
			newComponentY = 0;
		}
		component.setLocation(newComponentX, newComponentY);
	}
}
