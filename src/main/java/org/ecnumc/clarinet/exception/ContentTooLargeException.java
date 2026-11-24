package org.ecnumc.clarinet.exception;

/**
 * 内容过长异常
 *
 * @author liudongyu
 */
public class ContentTooLargeException extends RuntimeException {
	public ContentTooLargeException(String message) {
		super(message);
	}
}
