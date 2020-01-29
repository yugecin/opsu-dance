// Copyright 2017-2019 yugecin - this source is licensed under GPL
// see the LICENSE file for more details
package yugecin.opsudance.core.errorhandling;

import org.newdawn.slick.util.Log;

import itdelatrisu.opsu.ui.Colors;
import itdelatrisu.opsu.ui.Fonts;
import yugecin.opsudance.core.Constants;
import yugecin.opsudance.core.Entrypoint;
import yugecin.opsudance.core.Environment;
import yugecin.opsudance.utils.MiscUtils;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import java.awt.*;
import java.awt.Desktop.Action;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Formatter;

// no wildcards here because everything has to be checked before use
import static yugecin.opsudance.core.Constants.PROJECT_NAME;
import static yugecin.opsudance.core.InstanceContainer.bubNotifs;
import static yugecin.opsudance.core.InstanceContainer.displayContainer;
import static yugecin.opsudance.core.InstanceContainer.env;

/**
 * based on itdelatrisu.opsu.ErrorHandler
 */
public class ErrorHandler
{
	public static final int DEFAULT_OPTIONS = 0;
	public static final int PREVENT_CONTINUE = 1;
	public static final int PREVENT_REPORT = 2;
	public static final int ALLOW_TERMINATE = 4;
	public static final int FORCE_TERMINATE = ALLOW_TERMINATE | PREVENT_CONTINUE;

	public static final Image dialogIcon;

	private static final int RESULT_CONTINUE = 0;
	private static final int RESULT_TERMINATE = 1;

	// see javax.swing.plaf.basic.BasicOptionPaneUI#getIconForType
	private static final String ICONKEY_ERROR = "OptionPane.errorIcon";
	private static final String ICONKEY_INFO = "OptionPane.informationIcon";

	static
	{
		final byte[] bytes =
			("iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAMAAAAoLQ9TAAAACXBIWXMAAA3WAAAN1gGQb3mcA"
			+"AAAXVBMVEVHcExxcXFnZ2czMzRQUFFGRkhbW1wvLzArKyw+Pj+bm5z///93d3f////+/v57e3"
			+"z+/v7W1tb+/v7v7+/t7e3+/v7+/v7+/v7+/v6FhYXc3Nz+/v7+/v7+/v7+/v6/rNyxAAAAH3R"
			+"STlMAwb6ms7m5p6avzQK/evjDHeeo9fOFgsLiw+ncvMjo3EyqYAAAALJJREFUGNMtT1cWgzAM"
			+"ix3HI2QQVqHr/sdsgOr5x3qWJTnXMeb36/XOo7sx5PKMxxGfJQ/XPs3Ji1fVNE8nk2fy56oiN"
			+"OeuL0m9F5V+JVBGl5saExqSmWnL7hE9cA01MCP6+HB79USshgwYpO6d0IpoB2MClrR3iUACqB"
			+"GoE12yNWUj7h+JSNvWbVEt2Dl22bptofAHLdsVfQGRMxos011mLS2mFFtZh3/fcf18v5/1qv8"
			+"DBMMJdZb3ELQAAAAASUVORK5CYII=").getBytes(StandardCharsets.US_ASCII);

		dialogIcon = new ImageIcon(Base64.getDecoder().decode(bytes)).getImage();
	}

	/**
	 * Shows a warning in the form of a bubble notification. Also sends message to the log.
	 */
	@SuppressWarnings("resource")
	public static void softWarn(String format, Object... args)
	{
		format = new Formatter().format(format, args).toString();
		Log.warn(format);
		/*don't send bubble notif when game was not inited*/
		if (Fonts.BOLD != null) {
			bubNotifs.send(Colors.BUB_ORANGE, format);
		}
	}

	/**
	 * Shows an error in the form of a bubble notification. Also sends message to the log.
	 */
	@SuppressWarnings("resource")
	public static void softErr(Throwable e, String format, Object... args)
	{
		format = new Formatter().format(format, args).toString();
		if (Fonts.BOLD != null) {
			/*game was not inited yet, so can't show a soft error*/
			explode(format, e, PREVENT_CONTINUE);
			return;
		}
		Log.error(format, e);
		bubNotifs.send(Colors.BUB_RED, format + "\nSee opsu.log for details.");
	}

	/**
	 * @return {@code true} if user wants to keep running
	 */
	public static boolean explode(String customMessage, Throwable cause, int flags)
	{
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
		cause.printStackTrace(new PrintWriter(dump));
		dump.append("\n").append(errorDump);
		final String messageBody = dump.toString();
		final String logdump = customMessage + "\n\n" + messageBody;
		Log.error("==== START UNHANDLED EXCEPTION DUMP ====\n\n" + logdump);
		Log.error("==== CLOSE UNHANDLED EXCEPTION DUMP ====");

		int result = show(messageBody, customMessage, cause, errorDump, flags);
		return result == RESULT_CONTINUE;
	}

	private static int show(
		String messageBody,
		String customMessage,
		Throwable cause,
		String errorDump,
		int flags)
	{
		if ((flags & PREVENT_CONTINUE) != 0) {
			flags |= ALLOW_TERMINATE;
		}

		Entrypoint.setLAF();

		JButton defaultbtn = null;
		final int[] result = { RESULT_TERMINATE };
		final JDialog d = new JDialog((Window) null, PROJECT_NAME + " error");
		d.setIconImage(dialogIcon);
		d.setModal(true);
		d.setLayout(new GridBagLayout());
		final GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.CENTER;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(12, 18, 4, 5);
		c.weightx = 0d;
		c.weighty = 0d;
		c.gridx = 1;
		c.gridy = 1;

		class ResultButton extends JButton
		{
			ResultButton(String text, int res)
			{
				super(text);
				this.addActionListener(e -> {
					result[0] = res;
					d.dispose();
				});
			}
		}

		final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		if ((flags & ALLOW_TERMINATE) != 0) {
			buttonPanel.add(defaultbtn = new ResultButton("Exit", RESULT_TERMINATE));
		}
		if ((flags & PREVENT_CONTINUE) == 0) {
			// if continuing is allowed, default action is to continue
			result[0] = RESULT_CONTINUE;
			defaultbtn = new ResultButton("Ignore & continue", RESULT_CONTINUE);
			buttonPanel.add(defaultbtn, 0);
		}

		// see javax.swing.plaf.basic.BasicOptionPaneUI#getIconForType
		final Icon icon = UIManager.getIcon("OptionPane.errorIcon");
		if (icon != null) {
			c.gridheight = 2;
			d.add(new JLabel(icon), c);
			c.gridheight = 1;
			c.insets.left = 5;
		}
		c.insets.right = 18;
		c.gridx = 2;
		c.weightx = 1d;

		// inner content
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.BOTH;
		String messageText = PROJECT_NAME + " has encountered an error.";
		if ((flags & PREVENT_REPORT) == 0) {
			messageText += " Please report this!";
		}
		d.add(new JLabel(messageText), c);
		c.insets.top = 3;
		c.gridy++;
		final String txt = "<html>" + customMessage.replaceAll("\n", "<br/>") + "</html>";
		d.add(new JLabel(txt), c);
		c.insets.top = 6;
		c.weighty = 1d;
		c.gridy++;
		JTextArea messagebody = readonlyTextarea(messageBody);
		d.add(new JScrollPane(messagebody), c);
		c.weighty = 0d;
		c.gridy++;
		d.add(actionButton("View log file", Action.OPEN, () ->
		{
			try {
				Desktop.getDesktop().open(Entrypoint.LOGFILE);
			} catch (IOException e) {
				Log.warn("Could not open log file", e);
				simpleMessage(
					d,
					"Failed to open log file: " + e.toString(),
					ICONKEY_ERROR
				);
			}
		}), c);
		c.gridy++;
		if ((flags & PREVENT_REPORT) == 0) {
			c.insets.top = 2;
			d.add(defaultbtn = actionButton("Report error", Action.BROWSE, () -> {
				String dump = createIssueDump(customMessage, cause, errorDump);
				messagebody.setText(dump);
				try {
					URI url = createGithubIssueUrl(customMessage, cause);
					Desktop.getDesktop().browse(url);
					simpleMessage(
						d,
						"A browser should have been opened, please copy"
						+ " ALL the text and paste it into the big box.",
						ICONKEY_INFO
					);
				} catch (IOException t) {
					Log.warn("Could not open browser to report issue", t);
					simpleMessage(
						d,
						"Failed to launch a browser: " + t.toString(),
						ICONKEY_ERROR
					);
				}
			}), c);
			c.insets.top = 6;
			c.gridy++;
		}

		// buttons
		c.gridx = 1;
		c.gridwidth = 2;
		c.insets.left = 18;
		c.insets.top = 12;
		d.add(buttonPanel, c);
		if (defaultbtn != null) {
			d.getRootPane().setDefaultButton(defaultbtn);
		}

		d.pack();
		d.setMinimumSize(d.getSize());
		final Rectangle r = d.getGraphicsConfiguration().getBounds();
		final int x = r.x + (r.width - d.getWidth()) / 2;
		final int y = r.y + (r.height - d.getHeight()) / 2;
		d.setLocation(x, y);
		d.setVisible(true);
		d.dispose();

		return result[0];
	}

	private static JButton actionButton(String text, Desktop.Action action, Runnable listener)
	{
		final JButton button = new JButton(text);
		if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(action)) {
			button.addActionListener(e -> listener.run());
			return button;
		}
		button.setEnabled(false);
		return button;
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
	
	private static String createIssueDump(
		String customMessage,
		Throwable cause,
		String errorDump)
	{
		StringWriter dump = new StringWriter();

		dump.append(customMessage).append("\n\n");
		String version;
		try {
			version = MiscUtils.buildProperties.get().getProperty("version");
		} catch (Throwable t) {
			version = t.toString();
		}
		dump.append("**ver** ").append(version).append('\n');
		if (env == null) {
			env = new Environment();
		}
		if (env.gitHash != null) {
			dump.append("**rev** ").append(env.gitHash).append('\n');
		}

		dump.append("**os** ").append(System.getProperty("os.name"))
			.append(" (").append(System.getProperty("os.arch")).append(")\n");
		dump.append("**jre** ");
		dump.append(System.getProperty("java.version"));
		dump.append(" (").append(System.getProperty("java.vendor")).append(")");
		dump.append('\n');

		dump.append("**trace**").append("\n```\n");
		cause.printStackTrace(new PrintWriter(dump));
		dump.append("\n```\n");

		dump.append("**info dump**").append('\n');
		dump.append("```\n").append(errorDump).append("\n```\n\n");
		return dump.toString();
	}

	private static JTextArea readonlyTextarea(String contents)
	{
		JTextArea textArea = new JTextArea(15, 80);
		textArea.setEditable(false);
		textArea.setBackground(UIManager.getColor("Panel.background"));
		textArea.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
		textArea.setTabSize(2);
		textArea.setLineWrap(false);
		textArea.setWrapStyleWord(true);
		textArea.setFont(new JLabel().getFont());
		textArea.setText(contents);
		final EmptyBorder padding = new EmptyBorder(5, 5, 5, 5);
		textArea.setBorder(new CompoundBorder(textArea.getBorder(), padding));
		return textArea;
	}

	/**
	 * @param iconkey {@link #ICONKEY_ERROR} or {@link #ICONKEY_INFO}
	 */
	private static void simpleMessage(Window parent, String message, String iconkey)
	{
		final JDialog d = new JDialog(parent, PROJECT_NAME + " error");
		d.setIconImage(dialogIcon);
		d.setModal(true);
		d.setLayout(new GridBagLayout());
		final GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.CENTER;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(12, 18, 4, 5);
		c.gridx = 1;
		c.gridy = 1;
		final Icon icon = UIManager.getIcon(iconkey);
		if (icon != null) {
			d.add(new JLabel(icon), c);
			c.insets.left = 5;
		}
		c.insets.right = 18;
		c.gridx = 2;
		d.add(new JLabel(message), c);
		c.insets.top = 3;
		c.gridx = 1;
		c.gridy = 2;
		c.gridwidth = 2;
		c.insets.top = 12;
		c.insets.left = 18;
		JButton btn = new JButton("Close");
		btn.addActionListener(e -> d.dispose());
		d.add(btn, c);
		d.getRootPane().setDefaultButton(btn);
		d.pack();
		d.setResizable(false);
		d.setLocationRelativeTo(parent);
		d.setVisible(true);
		d.dispose();
	}
}
