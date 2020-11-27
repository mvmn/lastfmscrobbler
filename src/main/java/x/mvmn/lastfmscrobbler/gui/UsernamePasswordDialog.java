package x.mvmn.lastfmscrobbler.gui;

import java.awt.Container;
import java.awt.Frame;
import java.awt.GridLayout;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import x.mvmn.lastfmscrobbler.ExceptionDisplayer;
import x.mvmn.lastfmscrobbler.util.Pair;

public class UsernamePasswordDialog extends JDialog {
	private static final long serialVersionUID = 3943012295659417955L;

	private final Consumer<Pair<String, char[]>> okHandler;
	private final JTextField fUsername;
	private final JPasswordField fPassword;
	private final JButton btnOk;
	private final JButton btnCancel;

	public UsernamePasswordDialog(Frame owner, String title, boolean modal, Pair<String, String> initVals,
			Consumer<Pair<String, char[]>> okHandler) {
		this(owner, title, modal, initVals, okHandler, null, null);
	}

	public UsernamePasswordDialog(Frame owner, String title, boolean modal, Pair<String, String> initVals,
			Consumer<Pair<String, char[]>> okHandler, String usernameFieldName, String passwordFieldName) {
		super(owner, title, modal);
		this.okHandler = okHandler;
		this.fUsername = new JTextField();
		this.fPassword = new JPasswordField();
		if (initVals != null) {
			fUsername.setText(initVals.getK() != null ? initVals.getK() : "");
			fPassword.setText(initVals.getV() != null ? initVals.getV() : "");
		}
		this.btnOk = new JButton("Ok");
		this.btnCancel = new JButton("Cancel");

		this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		btnCancel.addActionListener(evt -> {
			this.setVisible(false);
			this.dispose();
		});
		btnOk.addActionListener(evt -> {
			this.setVisible(false);
			this.dispose();
			try {
				this.okHandler.accept(new Pair<>(fUsername.getText(), fPassword.getPassword()));
			} catch (Exception e) {
				ExceptionDisplayer.INSTANCE.accept(e);
				// TODO: log
			}
		});

		fUsername.setBorder(BorderFactory.createTitledBorder(usernameFieldName != null ? usernameFieldName : "Username"));
		fPassword.setBorder(BorderFactory.createTitledBorder(passwordFieldName != null ? passwordFieldName : "Password"));

		Container pane = this.getContentPane();
		pane.setLayout(new GridLayout(2, 2));
		pane.add(fUsername);
		pane.add(fPassword);
		pane.add(btnOk);
		pane.add(btnCancel);
		this.pack();
	}
}
