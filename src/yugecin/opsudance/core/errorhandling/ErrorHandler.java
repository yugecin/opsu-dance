// Copyright 2017-2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.core.errorhandling;

import org.newdawn.slick.util.Log;
import yugecin.opsudance.core.Constants;
import yugecin.opsudance.utils.MiscUtils;

import javax.swing.*;
import java.awt.Desktop;
import java.awt.Dialog.ModalityType;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;

import static yugecin.opsudance.core.InstanceContainer.*;

/**
 * based on itdelatrisu.opsu.ErrorHandler
 */
public class ErrorHandler
{
	public final static int DEFAULT_OPTIONS = 0;
	public final static int PREVENT_CONTINUE = 1;
	public final static int PREVENT_REPORT = 2;
	public final static int ALLOW_TERMINATE = 4;

	public static boolean explode(String customMessage, Throwable cause, int flags) {
		StringWriter dump = new StringWriter();
		if (displayContainer == null) {
			dump.append("displayContainer is null!\n");
		} else {
			try {
				displayContainer.writeErrorDump(dump);
			} catch (Exception e) {
				dump
					.append("### ")
					.append(e.getClass().getSimpleName())
					.append(" while creating errordump");
				e.printStackTrace(new PrintWriter(dump));
			}
		}
		String errorDump = dump.toString();

		dump = new StringWriter();
		dump.append(customMessage).append("\n");
		cause.printStackTrace(new PrintWriter(dump));
		dump.append("\n").append(errorDump);
		String messageBody = dump.toString();

		Log.error("====== start unhandled exception dump");
		Log.error(messageBody);
		Log.error("====== end unhandled exception dump");

		int result = show(messageBody, customMessage, cause, errorDump, flags);

		return (flags & ALLOW_TERMINATE) == 0 || result == 1;
	}

	private static int show(final String messageBody, final String customMessage, final Throwable cause,
		       final String errorDump, final int flags) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			Log.warn("Unable to set look and feel for error dialog");
		}

		String title = "opsu!dance error - " + customMessage;

		String messageText = "opsu!dance has encountered an error.";
		if ((flags & PREVENT_REPORT) == 0) {
			messageText += " Please report this!";
		}
		JLabel message = new JLabel(messageText);

		ActionListener reportAction = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				Window parent = SwingUtilities.getWindowAncestor(message);
				showCreateIssueDialog(parent, errorDump, customMessage, cause);
			}
		};

		Object[] messageComponents = new Object[] { message, readonlyTextarea(messageBody), createViewLogButton(),
			createReportButton(flags, reportAction) };

		String[] buttons;
		if ((flags & (ALLOW_TERMINATE | PREVENT_CONTINUE)) == 0) {
			buttons = new String[] { "Ignore & continue" };
		} else if ((flags & PREVENT_CONTINUE) == 0) {
			buttons = new String[] { "Terminate" };
		} else {
			buttons = new String[] { "Terminate", "Ignore & continue" };
		}

		JFrame frame = new JFrame(title);
		frame.setUndecorated(true);
		frame.setVisible(true);
		frame.setLocationRelativeTo(null);
		int result = JOptionPane.showOptionDialog(frame, messageComponents, title, JOptionPane.DEFAULT_OPTION,
			JOptionPane.ERROR_MESSAGE, null, buttons, buttons[buttons.length - 1]);
		frame.dispose();

		return result;
	}

	private static JComponent createViewLogButton() {
		return createButton("View log", Desktop.Action.OPEN, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				openLogfile();
			}
		});
	}

	private static void openLogfile() {
		if (config == null) {
			JOptionPane.showMessageDialog(null,
				"Cannot open logfile, check your opsu! installation folder for .opsu.cfg",
				"errorception", JOptionPane.ERROR_MESSAGE);
			return;
		}
		try {
			Desktop.getDesktop().open(config.LOG_FILE);
		} catch (IOException e) {
			Log.warn("Could not open log file", e);
			JOptionPane.showMessageDialog(null, "whoops could not open log file",
				"errorception", JOptionPane.ERROR_MESSAGE);
		}
	}

	private static JComponent createReportButton(int flags, ActionListener reportAction) {
		if ((flags & PREVENT_REPORT) > 0) {
			return new JLabel();
		}
		return createButton("Report error", Desktop.Action.BROWSE, reportAction);
	}

	private static JButton createButton(String buttonText, Desktop.Action action, ActionListener listener) {
		JButton button = new JButton(buttonText);
		if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(action)) {
			button.addActionListener(listener);
			return button;
		}
		button.setEnabled(false);
		return button;
	}
	
	private static void showCreateIssueDialog(
		Window parent,
		String errorDump,
		String customMessage,
		Throwable cause)
	{
		final String dump = createIssueDump(customMessage, cause, errorDump);

		final String title = "report error";
		JDialog d = new JDialog(parent, title, ModalityType.APPLICATION_MODAL);
		d.setLayout(new GridBagLayout());
		final GridBagConstraints c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 1;
		c.weightx = 1d;
		c.weighty = 0d;
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(4, 8, 4, 8);
		
		d.add(new JLabel(
			"<html>Copy the text in the box below.<br/>"
			+ "Then click the button below.<br/>"
			+ "Your browser should open a page where you can report the issue.<br/>"
			+ "Please paste the dump below in the issue box."
		), c);

		c.gridy++;
		c.weighty = 1d;
		d.add(readonlyTextarea(dump), c);
		c.gridy++;
		c.weighty = c.weightx = 0d;
		c.fill = GridBagConstraints.NONE;
		JButton btn = new JButton("Report");
		btn.addActionListener(e -> {
			try {
				URI url = createGithubIssueUrl(customMessage, cause);
				Desktop.getDesktop().browse(url);
				d.dispose();
			} catch (IOException t) {
				Log.warn("Could not open browser to report issue", t);
				JOptionPane.showMessageDialog(null, "whoops could not launch a browser",
					"errorception", JOptionPane.ERROR_MESSAGE);
			}
		});
		d.add(btn, c);
		d.pack();
		d.setLocationRelativeTo(parent);
		d.setVisible(true);
	}

	private static URI createGithubIssueUrl(String customMessage, Throwable cause) {
		String issueTitle = "";
		String issueBody = "";
		try {
			issueTitle = URLEncoder.encode("*** Unhandled " + cause.getClass().getSimpleName() + ": " +
				customMessage, "UTF-8");
			issueBody = URLEncoder.encode("PASTE THE DUMP HERE", "UTF-8");
		} catch (UnsupportedEncodingException e) {
			Log.warn("URLEncoder failed to encode the auto-filled issue report URL.", e);
		}
		return URI.create(String.format(Constants.ISSUES_URL, issueTitle, issueBody));
	}
	
	private static String createIssueDump(String customMessage, Throwable cause, String errorDump) {
		StringWriter dump = new StringWriter();

		dump.append(customMessage).append("\n");

		dump.append("**ver** ").append(MiscUtils.buildProperties.get().getProperty("version")).append('\n');
		if (env.gitHash != null) {
			dump.append("**git hash** ")
				.append(env.gitHash.substring(0, 12)).append('\n');
		}

		dump.append("**os** ").append(System.getProperty("os.name"))
			.append(" (").append(System.getProperty("os.arch")).append(")\n");
		dump.append("**jre** ").append(System.getProperty("java.version")).append('\n');

		dump.append("**trace**").append("\n```\n");
		cause.printStackTrace(new PrintWriter(dump));
		dump.append("\n```\n");

		dump.append("**info dump**").append('\n');
		dump.append("```\n").append(errorDump).append("\n```\n\n");
		return dump.toString();
	}

	private static JComponent readonlyTextarea(String contents) {
		JTextArea textArea = new JTextArea(15, 100);
		textArea.setEditable(false);
		textArea.setBackground(UIManager.getColor("Panel.background"));
		textArea.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
		textArea.setTabSize(2);
		textArea.setLineWrap(false);
		textArea.setWrapStyleWord(true);
		textArea.setFont(new JLabel().getFont());
		textArea.setText(contents);
		return new JScrollPane(textArea);
	}
}
