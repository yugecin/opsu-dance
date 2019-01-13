/*
 * opsu! - an open-source osu! client
 * Copyright (C) 2014, 2015 Jeffrey Han
 *
 * opsu! is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * opsu! is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with opsu!.  If not, see <http://www.gnu.org/licenses/>.
 */

package itdelatrisu.opsu;

import itdelatrisu.opsu.downloads.Download;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Scanner;
import java.util.jar.JarFile;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lwjgl.input.Keyboard;
import org.newdawn.slick.Animation;
import org.newdawn.slick.Color;
import org.newdawn.slick.util.Log;

import com.sun.jna.platform.FileUtils;
import yugecin.opsudance.core.NotNull;

import static yugecin.opsudance.core.errorhandling.ErrorHandler.*;
import static yugecin.opsudance.core.InstanceContainer.*;

/**
 * Contains miscellaneous utilities.
 */
public class Utils {

	/**
	 * Draws an animation based on its center.
	 * @param anim the animation to draw
	 * @param x the center x coordinate
	 * @param y the center y coordinate
	 */
	public static void drawCentered(Animation anim, float x, float y) {
		anim.draw(x - (anim.getWidth() / 2f), y - (anim.getHeight() / 2f));
	}

	/**
	 * Returns the luminance of a color.
	 * @param c the color
	 */
	public static float getLuminance(Color c) {
		return 0.299f*c.r + 0.587f*c.g + 0.114f*c.b;
	}

	/**
	 * Clamps a value between a lower and upper bound.
	 * @param val the value to clamp
	 * @param low the lower bound
	 * @param high the upper bound
	 * @return the clamped value
	 * @author fluddokt
	 */
	public static int clamp(int val, int low, int high) {
		if (val < low)
			return low;
		if (val > high)
			return high;
		return val;
	}

	/**
	 * Clamps a value between a lower and upper bound.
	 * @param val the value to clamp
	 * @param low the lower bound
	 * @param high the upper bound
	 * @return the clamped value
	 * @author fluddokt
	 */
	public static float clamp(float val, float low, float high) {
		if (val < low)
			return low;
		if (val > high)
			return high;
		return val;
	}
	/**
	 * Clamps a value between a lower and upper bound.
	 * @param val the value to clamp
	 * @param low the lower bound
	 * @param high the upper bound
	 * @return the clamped value
	 * @author fluddokt
	 */
	public static double clamp(double val, double low, double high) {
		if (val < low)
			return low;
		if (val > high)
			return high;
		return val;
	}

	/**
	 * Returns the distance between two points.
	 * @param x1 the x-component of the first point
	 * @param y1 the y-component of the first point
	 * @param x2 the x-component of the second point
	 * @param y2 the y-component of the second point
	 * @return the Euclidean distance between points (x1,y1) and (x2,y2)
	 */
	public static float distance(float x1, float y1, float x2, float y2) {
		float v1 = x1 - x2;
		float v2 = y1 - y2;
		return (float) Math.sqrt(v1 * v1 + v2 * v2);
	}

	public static double distance(double x1, double y1, double x2, double y2) {
		double dx = x1 - x2;
		double dy = y1 - y2;
		return Math.sqrt(dx * dx + dy * dy);
	}

	/**
	 * Linear interpolation of a and b at t.
	 */
	public static float lerp(float a, float b, float t) {
		return a * (1 - t) + b * t;
	}

	/**
	 * Returns a human-readable representation of a given number of bytes.
	 * @param bytes the number of bytes
	 * @return the string representation
	 * @author aioobe (http://stackoverflow.com/a/3758880)
	 */
	public static String bytesToString(long bytes) {
		if (bytes < 1024)
			return bytes + " B";
		int exp = (int) (Math.log(bytes) / Math.log(1024));
		char pre = "KMGTPE".charAt(exp - 1);
		return String.format("%.1f %cB", bytes / Math.pow(1024, exp), pre);
	}

	/**
	 * Changes bad characters to the replacement char given.
	 * Bad characters:
	 * non-printable (0-31)
	 * " (34) * (42) / (47) : (58)
	 * < (60) > (62) ? (63) \ (92)
	 * DEL (124)
	 */
	public static String cleanFileName(@NotNull String badFileName, char replacement) {
		char[] chars = badFileName.toCharArray();
		long additionalBadChars =
			1L << (34 - 32)|
			1L << (42 - 32)|
			1L << (47 - 32)|
			1L << (58 - 32)|
			1L << (60 - 32)|
			1L << (62 - 32)|
			1L << (63 - 32)|
			1L << (92 - 32);
		for (int i = 0; i < chars.length; i++) {
			char c = chars[i];
			if (c < 32 || c == 124 || (c < 93 && 0 != (additionalBadChars & (1L << (c - 32))))) {
				chars[i] = replacement;
			}
		}
		return new String(chars);
	}

	/**
	 * Deletes a file or directory.  If a system trash directory is available,
	 * the file or directory will be moved there instead.
	 * @param file the file or directory to delete
	 * @return true if moved to trash, and false if deleted
	 * @throws IOException if given file does not exist
	 */
	public static boolean deleteToTrash(File file) throws IOException {
		if (file == null)
			throw new IOException("File cannot be null.");
		if (!file.exists())
			throw new IOException(String.format("File '%s' does not exist.", file.getAbsolutePath()));

		// move to system trash, if possible
		FileUtils fileUtils = FileUtils.getInstance();
		if (fileUtils.hasTrash()) {
			try {
				fileUtils.moveToTrash(new File[] { file });
				return true;
			} catch (IOException e) {
				Log.warn(String.format("Failed to move file '%s' to trash.", file.getAbsolutePath()), e);
			}
		}

		// delete otherwise
		if (file.isDirectory())
			deleteDirectory(file);
		else
			file.delete();
		return false;
	}

	/**
	 * Recursively deletes all files and folders in a directory, then
	 * deletes the directory itself.
	 * @param dir the directory to delete
	 */
	public static void deleteDirectory(File dir) {
		if (dir == null || !dir.isDirectory())
			return;

		// recursively delete contents of directory
		File[] files = dir.listFiles();
		if (files != null && files.length > 0) {
			for (File file : files) {
				if (file.isDirectory())
					deleteDirectory(file);
				else
					file.delete();
			}
		}

		// delete the directory
		dir.delete();
	}

	/**
	 * Returns a the contents of a URL as a string.
	 * @param url the remote URL
	 * @return the contents as a string, or null if any error occurred
	 * @author Roland Illig (http://stackoverflow.com/a/4308662)
	 * @throws IOException if an I/O exception occurs
	 */
	public static String readDataFromUrl(URL url) throws IOException {
		// open connection
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setConnectTimeout(Download.CONNECTION_TIMEOUT);
		conn.setReadTimeout(Download.READ_TIMEOUT);
		conn.setUseCaches(false);
		try {
			conn.connect();
		} catch (SocketTimeoutException e) {
			Log.warn("Connection to server timed out.", e);
			throw e;
		}

		if (Thread.interrupted())
			return null;

		// read contents
		try (InputStream in = conn.getInputStream()) {
			BufferedReader rd = new BufferedReader(new InputStreamReader(in));
			StringBuilder sb = new StringBuilder();
			int c;
			while ((c = rd.read()) != -1)
				sb.append((char) c);
			return sb.toString();
		} catch (SocketTimeoutException e) {
			Log.warn("Connection to server timed out.", e);
			throw e;
		}
	}

	/**
	 * Returns a JSON object from a URL.
	 * @param url the remote URL
	 * @return the JSON object, or null if an error occurred
	 * @throws IOException if an I/O exception occurs
	 */
	public static JSONObject readJsonObjectFromUrl(URL url) throws IOException {
		String s = Utils.readDataFromUrl(url);
		JSONObject json = null;
		if (s != null) {
			try {
				json = new JSONObject(s);
			} catch (JSONException e) {
				explode("Failed to create JSON object.", e, DEFAULT_OPTIONS);
			}
		}
		return json;
	}

	/**
	 * Returns a JSON array from a URL.
	 * @param url the remote URL
	 * @return the JSON array, or null if an error occurred
	 * @throws IOException if an I/O exception occurs
	 */
	public static JSONArray readJsonArrayFromUrl(URL url) throws IOException {
		String s = Utils.readDataFromUrl(url);
		JSONArray json = null;
		if (s != null) {
			try {
				json = new JSONArray(s);
			} catch (JSONException e) {
				explode("Failed to create JSON array.", e, DEFAULT_OPTIONS);
			}
		}
		return json;
	}

	/**
	 * Converts an input stream to a string.
	 * @param is the input stream
	 * @author Pavel Repin, earcam (http://stackoverflow.com/a/5445161)
	 */
	public static String convertStreamToString(InputStream is) {
		try (Scanner s = new Scanner(is)) {
			return s.useDelimiter("\\A").hasNext() ? s.next() : "";
		}
	}

	/**
	 * Returns the md5 hash of a file in hex form.
	 * @param file the file to hash
	 * @return the md5 hash
	 */
	public static String getMD5(File file) {
		try {
			InputStream in = new BufferedInputStream(new FileInputStream(file));
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] buf = new byte[4096];

			while (true) {
				int len = in.read(buf);
				if (len < 0)
					break;
				md.update(buf, 0, len);
			}
			in.close();

			byte[] md5byte = md.digest();
			StringBuilder result = new StringBuilder();
			for (byte b : md5byte)
				result.append(String.format("%02x", b));
			return result.toString();
		} catch (NoSuchAlgorithmException | IOException e) {
			explode("Failed to calculate MD5 hash.", e, DEFAULT_OPTIONS);
		}
		return null;
	}

	/**
	 * Returns a formatted time string for a given number of seconds.
	 * @param seconds the number of seconds
	 * @return the time as a readable string
	 */
	public static String getTimeString(int seconds) {
		if (seconds < 60)
			return (seconds == 1) ? "1 second" : String.format("%d seconds", seconds);
		else if (seconds < 3600)
			return String.format("%02d:%02d", seconds / 60, seconds % 60);
		else
			return String.format("%02d:%02d:%02d", seconds / 3600, (seconds / 60) % 60, seconds % 60);
	}

	/**
	 * Parses the integer string argument as a boolean:
	 * {@code 1} is {@code true}, and all other values are {@code false}.
	 * @param s the {@code String} containing the boolean representation to be parsed
	 * @return the boolean represented by the string argument
	 */
	public static boolean parseBoolean(String s) {
		return (Integer.parseInt(s) == 1);
	}

	/**
	 * Switches validation of SSL certificates on or off by installing a default
	 * all-trusting {@link TrustManager}.
	 * @param enabled whether to validate SSL certificates
	 * @author neu242 (http://stackoverflow.com/a/876785)
	 */
	public static void setSSLCertValidation(boolean enabled) {
		// create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = new TrustManager[]{
			new X509TrustManager() {
				@Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
				@Override public void checkClientTrusted(X509Certificate[] certs, String authType) {}
				@Override public void checkServerTrusted(X509Certificate[] certs, String authType) {}
			}
		};

		// install the all-trusting trust manager
		try {
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, enabled ? null : trustAllCerts, null);
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (Exception e) {}
	}

	/**
	 * Gets the region where the given point is in.
	 * First bit is set if x > half
	 * Second bit is set if y > half
	 *
	 * 2 | 3
	 * --+--
	 * 0 | 1
	 */
	public static int getRegion(double x, double y) {
		int q = 0;
		if (y < height2) q = 2;
		if (x < width2) q |= 1;
		return q;
	}

	/*
	public static Color shiftHue(Color color, double H) {
		double U = Math.cos(H * Math.PI / 180d);
		double W = Math.sin(H * Math.PI / 180d);

		Color n = new Color(0, 0, 0);
		n.r = (float) ((0.299d + 0.701d * U + 0.168d * W) * color.r + (0.587d - 0.587d * U + 0.330d * W) * color.g + (0.114d - 0.114d * U - 0.497 * W) * color.b);
		n.g = (float) ((0.299 + 0.299 * U - 0.328 * W) * color.r + (0.587d - 0.413 * U + 0.035 * W) * color.g + (0.114d - 0.114d * U - 0.292 * W) * color.b);
		n.b = (float) ((0.299d + 0.300d * U + 1.250d * W) * color.r + (0.587d - 0.585d * U + 1.050d * W) * color.g + (0.114 - 0.886 * U - 0.203 * W) * color.b);
		return n;
	}
	*/

	public static float[] mirrorPoint(float x, float y) {
		double dx = x - width2;
		double dy = y - height2;
		double ang = Math.atan2(dy, dx);
		double d = -Math.sqrt(dx * dx + dy * dy);
		return new float[]{
			(float) (width2 + Math.cos(ang) * d),
			(float) (height2 + Math.sin(ang) * d)
		};
	}

	public static float[] mirrorPoint(float x, float y, float degrees) {
		double dx = x - width2;
		double dy = y - height2;
		double ang = Math.atan2(dy, dx) + (degrees * Math.PI / 180d);
		double d = Math.sqrt(dx * dx + dy * dy);
		return new float[]{
			(float) (width2 + Math.cos(ang) * d),
			(float) (height2 + Math.sin(ang) * d)
		};
	}

	/**
	 * Returns the file extension of a file.
	 * @param file the file name
	 */
	public static String getFileExtension(String file) {
		int i = file.lastIndexOf('.');
		return (i != -1) ? file.substring(i + 1).toLowerCase() : "";
	}

	public static boolean isValidGameKey(int key) {
		return (key != Keyboard.KEY_ESCAPE && key != Keyboard.KEY_SPACE &&
			key != Keyboard.KEY_UP && key != Keyboard.KEY_DOWN &&
			key != Keyboard.KEY_F7 && key != Keyboard.KEY_F10 && key != Keyboard.KEY_F12);
	}

	public static void unpackFromJar(@NotNull JarFile jarfile, @NotNull File unpackedFile,
			@NotNull String filename) throws IOException {
		InputStream in = jarfile.getInputStream(jarfile.getEntry(filename));
		OutputStream out = new FileOutputStream(unpackedFile);

		byte[] buffer = new byte[65536];
		int bufferSize;
		while ((bufferSize = in.read(buffer, 0, buffer.length)) != -1) {
			out.write(buffer, 0, bufferSize);
		}

		in.close();
		out.close();
	}

}
