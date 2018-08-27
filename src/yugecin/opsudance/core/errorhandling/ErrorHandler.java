/*
 * opsu!dance - fork of opsu! with cursordance auto
 * Copyright (C) 2017 yugecin
 *
 * opsu!dance is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * opsu!dance is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with opsu!dance.  If not, see <http://www.gnu.org/licenses/>.
 */
package yugecin.opsudance.core.errorhandling;

import itdelatrisu.opsu.Utils;
import org.newdawn.slick.util.Log;
import yugecin.opsudance.core.Constants;
import yugecin.opsudance.utils.MiscUtils;

import javax.swing.*;
import java.awt.Desktop;
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
public class ErrorHandler {

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

		JTextArea textArea = new JTextArea(15, 100);
		textArea.setEditable(false);
		textArea.setBackground(UIManager.getColor("Panel.background"));
		textArea.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
		textArea.setTabSize(2);
		textArea.setLineWrap(false);
		textArea.setWrapStyleWord(true);
		textArea.setFont(message.getFont());
		textArea.setText(messageBody);

		ActionListener reportAction = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				try {
					URI url = createGithubIssueUrl(customMessage, cause, errorDump);
					Desktop.getDesktop().browse(url);
				} catch (IOException e) {
					Log.warn("Could not open browser to report issue", e);
					JOptionPane.showMessageDialog(null, "whoops could not launch a browser",
						"errorception", JOptionPane.ERROR_MESSAGE);
				}
			}
		};

		Object[] messageComponents = new Object[] { message, new JScrollPane(textArea), createViewLogButton(),
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

	private static URI createGithubIssueUrl(String customMessage, Throwable cause, String errorDump) {
		StringWriter dump = new StringWriter();

		dump.append(customMessage).append("\n");

		dump.append("**ver** ").append(MiscUtils.buildProperties.get().getProperty("version")).append('\n');
		String gitHash = Utils.getGitHash();
		if (gitHash != null) {
			dump.append("**git hash** ").append(gitHash.substring(0, 12)).append('\n');
		}

		dump.append("**os** ").append(System.getProperty("os.name"))
			.append(" (").append(System.getProperty("os.arch")).append(")\n");
		dump.append("**jre** ").append(System.getProperty("java.version")).append('\n');

		dump.append("**trace**").append("\n```\n");
		cause.printStackTrace(new PrintWriter(dump));
		dump.append("\n```\n");

		dump.append("**info dump**").append('\n');
		dump.append("```\n").append(errorDump).append("\n```\n\n");

		String issueTitle = "";
		String issueBody = "";
		try {
			issueTitle = URLEncoder.encode("*** Unhandled " + cause.getClass().getSimpleName() + " " +
				customMessage, "UTF-8");
			issueBody = URLEncoder.encode(truncateGithubIssueBody(dump.toString()), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			Log.warn("URLEncoder failed to encode the auto-filled issue report URL.", e);
		}
		return URI.create(String.format(Constants.ISSUES_URL, issueTitle, issueBody));
	}

	private static String truncateGithubIssueBody(String body) {
		if (body.replaceAll("[^a-zA-Z+-]", "").length() < 1750) {
			return body;
		}
		Log.warn("error dump too long to fit into github issue url, truncating");
		return body.substring(0, 1640) + "** TRUNCATED **\n```";
	}

}
