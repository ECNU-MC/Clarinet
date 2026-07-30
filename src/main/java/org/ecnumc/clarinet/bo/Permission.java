package org.ecnumc.clarinet.bo;

import lombok.AllArgsConstructor;

/**
 * 用户权限枚举类
 *
 * @author liudongyu
 */
@AllArgsConstructor
public enum Permission {
	SUPER_ADMIN(4),
	ADMIN(3),
	ANALYST(2),
	NORMAL(1),
	NONE(0),
	BANNED(-1);

	private final int level;

	/**
	 * 是否可以发起会话
	 *
	 * @param permission 权限字符串
	 * @return 是否可以发起会话
	 */
	public static boolean canStartSession(String permission) {
		try {
			Permission p = Permission.valueOf(permission);
			return p.level > 1;
		} catch (Exception e) {
			return false;
		}
	}
}
