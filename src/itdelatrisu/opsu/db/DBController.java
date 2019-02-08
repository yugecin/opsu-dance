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

package itdelatrisu.opsu.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Database controller.
 */
public class DBController {
	// This class should not be instantiated.
	private DBController() {}

	/**
	 * Initializes all databases.
	 */
	public static void init() {
		// load the sqlite-JDBC driver using the current class loader
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Could not load sqlite driver", e);
		}

		// initialize the databases
		BeatmapDB.init();
		ScoreDB.init();
	}

	/**
	 * Closes all database connections.
	 */
	public static void closeConnections() {
		BeatmapDB.closeConnection();
		ScoreDB.closeConnection();
	}

	/**
	 * Establishes a connection to the database given by the path string.
	 * @param path the database path
	 * @return the Connection, or null if a connection could not be established
	 */
	public static Connection createConnection(String path) {
		try {
			return DriverManager.getConnection(String.format("jdbc:sqlite:%s", path));
		} catch (SQLException e) {
			// if the error message is "out of memory",
			// it probably means no database file is found
			throw new RuntimeException("Could not connect to db " + path, e);
		}
	}
}
