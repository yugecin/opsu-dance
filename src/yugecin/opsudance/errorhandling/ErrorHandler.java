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
package yugecin.opsudance.errorhandling;

import itdelatrisu.opsu.Options;
import itdelatrisu.opsu.Utils;
import org.newdawn.slick.util.Log;
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

/**
 * based on itdelatrisu.opsu.ErrorHandler
 */
public class ErrorHandler {

	private final String customMessage;
	private final Throwable cause;
	private final String errorDump;
	private final String messageBody;

	private boolean preventContinue;
	private boolean preventReport;
	private boolean ignoreAndContinue;

	private ErrorHandler(String customMessage, Throwable cause, ErrorDumpable[] errorInfoProviders) {
		this.customMessage = customMessage;
		this.cause = cause;

		StringWriter dump = new StringWriter();
		for (ErrorDumpable infoProvider : errorInfoProviders) {
			try {
				infoProvider.writeErrorDump(dump);
			} catch (Exception e) {
				dump
					.append("### ")
					.append(e.getClass().getSimpleName())
					.append(" while creating errordump for ")
					.append(infoProvider.getClass().getSimpleName());
				e.printStackTrace(new PrintWriter(dump));
			}
		}
		errorDump = dump.toString();

		dump = new StringWriter();
		dump.append(customMessage).append("\n");
		dump.append("unhandled ").append(cause.getClass().getSimpleName()).append("\n\n");
		cause.printStackTrace(new PrintWriter(dump));
		dump.append("\n\n").append(errorDump);
		messageBody = dump.toString();
		Log.error(messageBody);
	}

	public static ErrorHandler error(String message, Throwable cause, ErrorDumpable... errorInfoProviders) {
		return new ErrorHandler(message, cause, errorInfoProviders);
	}

	public ErrorHandler preventReport() {
		preventReport = true;
		return this;
	}

	public ErrorHandler preventContinue() {
		preventContinue = true;
		return this;
	}

	public ErrorHandler show() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			Log.warn("Unable to set look and feel for error dialog");
		}

		String title = "opsu!dance error - " + customMessage;

		String messageText = "opsu!dance has encountered an error.";
		if (!preventReport) {
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
		textArea.setText(messageBody);

		Object[] messageComponents = new Object[] { message, new JScrollPane(textArea), createViewLogButton(), createReportButton() };

		String[] buttons;
		if (preventContinue) {
			buttons = new String[] { "Terminate" };
		} else {
			buttons = new String[] { "Terminate", "Ignore & continue" };
		}

		JFrame frame = new JFrame(title);
		frame.setUndecorated(true);
		frame.setVisible(true);
		frame.setLocationRelativeTo(null);
		int result = JOptionPane.showOptionDialog(frame,
			                                        messageComponents,
			                                        title,
			                                        JOptionPane.DEFAULT_OPTION,
			                                        JOptionPane.ERROR_MESSAGE,
			                                        null,
			                                        buttons,
			                                        buttons[buttons.length - 1]);
		ignoreAndContinue = result == 1;
		frame.dispose();

		return this;
	}

	private JComponent createViewLogButton() {
		return createButton("View log", Desktop.Action.OPEN, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				try {
					Desktop.getDesktop().open(Options.LOG_FILE);
				} catch (IOException e) {
					Log.warn("Could not open log file", e);
					JOptionPane.showMessageDialog(null, "whoops could not open log file", "errorception", JOptionPane.ERROR_MESSAGE);
				}
			}
		});
	}

	private JComponent createReportButton() {
		if (preventReport) {
			return new JLabel();
		}
		return createButton("Report error", Desktop.Action.BROWSE, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				try {
					Desktop.getDesktop().browse(createGithubIssueUrl());
				} catch (IOException e) {
					Log.warn("Could not open browser to report issue", e);
					JOptionPane.showMessageDialog(null, "whoops could not launch a browser", "errorception", JOptionPane.ERROR_MESSAGE);
				}
			}
		});
	}

	private JButton createButton(String buttonText, Desktop.Action action, ActionListener listener) {
		JButton button = new JButton(buttonText);
		if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(action)) {
			button.addActionListener(listener);
			return button;
		}
		button.setEnabled(false);
		return button;
	}

	private URI createGithubIssueUrl() {
		StringWriter dump = new StringWriter();

		dump.append("**opsu!dance version:** ").append(MiscUtils.buildProperties.get().getProperty("version")).append('\n');
		String gitHash = Utils.getGitHash();
		if (gitHash != null) {
			dump.append("**git hash:** ").append(gitHash.substring(0, 12)).append('\n');
		}

		dump.append("**os:** ").append(System.getProperty("os.name"))
			.append(" (").append(System.getProperty("os.arch")).append(")\n");
		dump.append("**jre:** ").append(System.getProperty("java.version")).append('\n');
		dump.append("**info dump:**").append('\n');
		dump.append("```\n").append(errorDump).append("```").append("\n\n");

		dump.append("**trace:**").append("\n```\n");
		cause.printStackTrace(new PrintWriter(dump));
		dump.append("```");

		String issueTitle = "";
		String issueBody = "";
		try {
			issueTitle = URLEncoder.encode("*** Unhandled " + cause.getClass().getSimpleName() + " " + customMessage, "UTF-8");
			issueBody = URLEncoder.encode(dump.toString(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			Log.warn("URLEncoder failed to encode the auto-filled issue report URL.", e);
		}
		return URI.create(String.format(Options.ISSUES_URL, issueTitle, issueBody));
	}

	public boolean shouldIgnoreAndContinue() {
		return ignoreAndContinue;
	}

	public void showAndExit() {
		show();
		System.exit(1);
	}

}
