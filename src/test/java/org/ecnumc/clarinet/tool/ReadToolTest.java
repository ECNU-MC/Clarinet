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
 * 读文件工具单元测试
 *
 * @author liudongyu
 */
class ReadToolTest {
	private final ReadTool tool = new ReadTool();

	@TempDir
	private Path tempDir;

	private static Path workspaceFile;

	/**
	 * 在真实工作区中准备一个文件，用于验证工作区内免授权分支
	 */
	@BeforeAll
	static void createWorkspaceFile() throws IOException {
		workspaceFile = Files.createTempFile(Paths.get(Clarinet.WORKSPACE), "read-tool-test-", ".txt");
		Files.writeString(workspaceFile, "workspace-file");
	}

	/**
	 * 清理工作区临时文件
	 */
	@AfterAll
	static void deleteWorkspaceFile() throws IOException {
		Files.deleteIfExists(workspaceFile);
	}

	/**
	 * 全量读取文件，默认附带行号
	 */
	@Test
	void testReadWholeFile() throws IOException {
		Path file = this.file("crash.txt", "line1\nline2\nline3");
		ToolResultBlock result = this.call(Map.of("file", file.toString()));
		assertEquals("1: line1\n2: line2\n3: line3", ToolResults.text(result));
	}

	/**
	 * 关闭行号后全量读取，返回原始内容
	 */
	@Test
	void testReadWholeFileWithoutLineNumbers() throws IOException {
		Path file = this.file("plain.txt", "line1\nline2\nline3");
		ToolResultBlock result = this.call(Map.of("file", file.toString(), "line_number", false));
		assertEquals("line1\nline2\nline3", ToolResults.text(result));
	}

	/**
	 * 读取文件末尾若干行，行号从文件真实行号起算
	 */
	@Test
	void testReadTailLines() throws IOException {
		Path file = this.file("latest.log", "l1\nl2\nl3\nl4\nl5");
		ToolResultBlock result = this.call(Map.of("file", file.toString(), "tail", 2));
		assertEquals("4: l4\n5: l5", ToolResults.text(result));
	}

	/**
	 * 读取指定行区间，行号从起始行起算
	 */
	@Test
	void testReadLineRange() throws IOException {
		Path file = this.file("debug.log", "l1\nl2\nl3\nl4");
		ToolResultBlock result = this.call(Map.of("file", file.toString(), "begin", 2, "end", 3));
		assertEquals("2: l2\n3: l3", ToolResults.text(result));
	}

	/**
	 * 读取不存在的文件应返回错误结果
	 */
	@Test
	void testReadMissingFileReturnsError() {
		Path file = this.tempDir.resolve("missing.txt");
		ToolResultBlock result = this.call(Map.of("file", file.toString()));
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
		Path file = this.tempDir.resolve("nope.txt");
		assertEquals(PermissionBehavior.DENY, this.check(Map.of("file", file.toString())));
	}

	/**
	 * 非法的读取参数组合应被拒绝
	 */
	@Test
	void testInvalidArgumentsDenied() throws IOException {
		Path file = this.file("args.txt", "l1\nl2\nl3");
		String path = file.toString();

		assertEquals(PermissionBehavior.DENY, this.check(Map.of("file", path, "begin", 3, "end", 2)));
		assertEquals(PermissionBehavior.DENY, this.check(Map.of("file", path, "begin", 2)));
		assertEquals(PermissionBehavior.DENY, this.check(Map.of("file", path, "end", 2)));
		assertEquals(PermissionBehavior.DENY, this.check(Map.of("file", path, "begin", 1, "end", 2, "tail", 2)));
	}

	/**
	 * 行号参数类型非法应被拒绝
	 */
	@Test
	void testInvalidLineNumberDenied() throws IOException {
		Path file = this.file("ln.txt", "l1");
		assertEquals(PermissionBehavior.DENY, this.check(Map.of("file", file.toString(), "line_number", "yes")));
	}

	/**
	 * 工作区外文件需要授权
	 */
	@Test
	void testOutsideWorkspaceRequiresAsk() throws IOException {
		Path file = this.file("outside.txt", "content");
		assertEquals(PermissionBehavior.ASK, this.check(Map.of("file", file.toString())));
	}

	/**
	 * 工作区内文件无需授权
	 */
	@Test
	void testInsideWorkspaceAllowed() {
		assertEquals(PermissionBehavior.ALLOW, this.check(Map.of("file", workspaceFile.toString())));
	}

	private Path file(String name, String content) throws IOException {
		Path file = this.tempDir.resolve(name);
		Files.writeString(file, content);
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
