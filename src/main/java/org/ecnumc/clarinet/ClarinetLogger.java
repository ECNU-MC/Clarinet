package org.ecnumc.clarinet;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 日志
 *
 * @author liudongyu
 */
@SuppressWarnings({"java:S1176"})
public final class ClarinetLogger {
	private static final Logger LOGGER = LogManager.getLogger("Clarinet");

	public static void log(Level logLevel, Object object) {
		LOGGER.log(logLevel, object);
	}

	public static void error(Object object) {
		LOGGER.log(Level.ERROR, object);
	}

	public static void info(Object object) {
		LOGGER.log(Level.INFO, object);
	}

	public static void warn(Object object) {
		LOGGER.log(Level.WARN, object);
	}

	public static void debug(Object object) {
		LOGGER.log(Level.DEBUG, object);
	}

	public static void error(String message, Object... params) {
		LOGGER.log(Level.ERROR, message, params);
	}

	public static void info(String message, Object... params) {
		LOGGER.log(Level.INFO, message, params);
	}

	public static void warn(String message, Object... params) {
		LOGGER.log(Level.WARN, message, params);
	}

	public static void debug(String format, Object... params) {
		LOGGER.log(Level.DEBUG, format, params);
	}

	private ClarinetLogger() {
	}
}
