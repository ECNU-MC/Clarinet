package org.ecnumc.clarinet.tool;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.permission.PermissionBehavior;
import io.agentscope.core.permission.PermissionDecision;
import io.agentscope.core.tool.ToolCallParam;
import org.ecnumc.clarinet.Clarinet;
import org.ecnumc.clarinet.support.ToolResults;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 文件大小工具单元测试
 *
 * @author liudongyu
 */
class StatToolTest {
	private final StatTool tool = new StatTool();

	@TempDir
	private Path tempDir;

	private static Path workspaceFile;

	/**
	 * 在真实工作区中准备一个文件，用于验证工作区内免授权分支
	 */
	@BeforeAll
	static void createWorkspaceFile() throws IOException {
		workspaceFile = Files.createTempFile(Paths.get(Clarinet.WORKSPACE), "stat-test-", ".bin");
	}

	/**
	 * 清理工作区临时文件
	 */
	@AfterAll
	static void deleteWorkspaceFile() throws IOException {
		Files.deleteIfExists(workspaceFile);
	}

	/**
	 * 2 KB 文件输出字节数与近似大小
	 */
	@Test
	void testFileSizeWithUnit() throws IOException {
		Path file = this.write("data.bin", 2048);
		ToolResultBlock result = this.call(Map.of("file", file.toString()));
		assertEquals("2048 字节（2.00 KB）", ToolResults.text(result));
	}

	/**
	 * 小于 1 KB 的文件只输出字节数
	 */
	@Test
	void testSmallFileSize() throws IOException {
		Path file = this.write("small.txt", 123);
		ToolResultBlock result = this.call(Map.of("file", file.toString()));
		assertEquals("123 字节", ToolResults.text(result));
	}

	/**
	 * 空文件大小为 0
	 */
	@Test
	void testEmptyFileSize() throws IOException {
		Path file = this.write("empty.txt", 0);
		ToolResultBlock result = this.call(Map.of("file", file.toString()));
		assertEquals("0 字节", ToolResults.text(result));
	}

	/**
	 * 文件不存在应返回错误
	 */
	@Test
	void testMissingFileReturnsError() {
		ToolResultBlock result = this.call(Map.of("file", this.tempDir.resolve("missing.bin").toString()));
		assertTrue(ToolResults.text(result).startsWith("Error:"));
	}

	/**
	 * 文件路径为空应被拒绝
	 */
	@Test
	void testEmptyFileDenied() {
		assertEquals(PermissionBehavior.DENY, this.check(Map.of()));
	}

	/**
	 * 文件不存在应被拒绝
	 */
	@Test
	void testMissingFileDenied() {
		assertEquals(PermissionBehavior.DENY, this.check(Map.of("file", this.tempDir.resolve("missing.bin").toString())));
	}

	/**
	 * 路径是目录应被拒绝
	 */
	@Test
	void testDirectoryDenied() {
		assertEquals(PermissionBehavior.DENY, this.check(Map.of("file", this.tempDir.toString())));
	}

	/**
	 * 参数类型非法应被拒绝
	 */
	@Test
	void testInvalidTypeDenied() {
		assertEquals(PermissionBehavior.DENY, this.check(Map.of("file", 1024)));
	}

	/**
	 * 工作区外文件需要授权
	 */
	@Test
	void testOutsideWorkspaceRequiresAsk() throws IOException {
		Path file = this.write("outside.bin", 16);
		assertEquals(PermissionBehavior.ASK, this.check(Map.of("file", file.toString())));
	}

	/**
	 * 工作区内文件无需授权
	 */
	@Test
	void testInsideWorkspaceAllowed() {
		assertEquals(PermissionBehavior.ALLOW, this.check(Map.of("file", workspaceFile.toString())));
	}

	private Path write(String name, int size) throws IOException {
		Path file = this.tempDir.resolve(name);
		Files.write(file, new byte[size]);
		return file;
	}

	private ToolResultBlock call(Map<String, Object> input) {
		return this.tool.callAsync(ToolCallParam.builder().input(input).build()).block();
	}

	private PermissionBehavior check(Map<String, Object> input) {
		PermissionDecision decision = this.tool.checkPermissions(input, null).block();
		return decision.getBehavior();
	}
}
