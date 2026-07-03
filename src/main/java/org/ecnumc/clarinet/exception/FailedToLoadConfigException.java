package org.ecnumc.clarinet.exception;

/**
 * 加载配置异常
 *
 * @author liudongyu
 */
public class FailedToLoadConfigException extends RuntimeException {
	public FailedToLoadConfigException(String message, Throwable t) {
		super(message, t);
	}
}
