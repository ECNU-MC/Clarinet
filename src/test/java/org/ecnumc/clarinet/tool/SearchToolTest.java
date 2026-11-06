package org.ecnumc.clarinet.tool;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.permission.PermissionBehavior;
import io.agentscope.core.permission.PermissionDecision;
import io.agentscope.core.tool.ToolCallParam;
import org.ecnumc.clarinet.Clarinet;
import org.ecnumc.clarinet.support.ToolResults;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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
 * 文件内容搜索工具单元测试
 *
 * @author liudongyu
 */
class SearchToolTest {
	private final SearchTool tool = new SearchTool();

	@TempDir
	private Path tempDir;

	private static Path workspaceFile;

	/**
	 * 在真实工作区中准备一个文件，用于验证工作区内免授权分支
	 */
	@BeforeAll
	static void createWorkspaceFile() throws IOException {
		workspaceFile = Files.createTempFile(Paths.get(Clarinet.WORKSPACE), "search-test-", ".log");
		Files.writeString(workspaceFile, "unique-marker\n");
	}

	/**
	 * 清理工作区临时文件
	 */
	@AfterAll
	static void deleteWorkspaceFile() throws IOException {
		Files.deleteIfExists(workspaceFile);
	}

	/**
	 * 构造测试目录结构，并放置一个非法 UTF-8 的二进制文件用于验证跳过逻辑
	 */
	@BeforeEach
	void setUpTree() throws IOException {
		Files.createDirectories(this.tempDir.resolve("logs/nested"));
		Files.writeString(this.tempDir.resolve("logs/a.log"), "INFO start\nERROR boom\nWARN careful");
		Files.writeString(this.tempDir.resolve("logs/nested/b.log"), "ERROR nested boom");
		Files.writeString(this.tempDir.resolve("notes.txt"), "nothing here");
		Files.write(this.tempDir.resolve("binary.bin"), new byte[]{ (byte) 0xFF, (byte) 0xFE, 'E', 'R', 'R', 'O', 'R' });
	}

	/**
	 * 目录搜索：返回文件名、行号与匹配行，二进制文件被跳过
	 */
	@Test
	void testSearchInDirectory() {
		ToolResultBlock result = this.call(Map.of("pattern", "ERROR", "directory", this.root()));
		assertEquals(
				String.join("\n",
						this.tempDir.resolve("logs/a.log") + ":2: ERROR boom",
						this.tempDir.resolve("logs/nested/b.log") + ":1: ERROR nested boom"
				),
				ToolResults.text(result)
		);
	}

	/**
	 * 单文件搜索：只返回行号与匹配行
	 */
	@Test
	void testSearchInSingleFile() {
		Path file = this.tempDir.resolve("logs/a.log");
		ToolResultBlock result = this.call(Map.of("pattern", "WARN", "file", file.toString()));
		assertEquals("3: WARN careful", ToolResults.text(result));
	}

	/**
	 * pattern 按正则表达式解释
	 */
	@Test
	void testSearchWithRegex() {
		Path file = this.tempDir.resolve("logs/a.log");
		ToolResultBlock result = this.call(Map.of("pattern", "^(INFO|WARN)", "file", file.toString()));
		assertEquals(
				String.join("\n", "1: INFO start", "3: WARN careful"),
				ToolResults.text(result)
		);
	}

	/**
	 * 匹配行数超过上限时应直接返回错误
	 */
	@Test
	void testTooManyMatchesReturnsError() throws IOException {
		Path file = this.tempDir.resolve("many.log");
		Files.writeString(file, "hit\nhit\nhit");
		ToolResultBlock result = this.call(Map.of("pattern", "hit", "file", file.toString(), "max_matches", 2));
		String text = ToolResults.text(result);
		assertTrue(text.startsWith("Error:"));
		assertTrue(text.contains("max_matches=2"));
	}

	/**
	 * 非法正则表达式应返回错误
	 */
	@Test
	void testInvalidRegexReturnsError() {
		ToolResultBlock result = this.call(Map.of("pattern", "[", "directory", this.root()));
		assertTrue(ToolResults.text(result).startsWith("Error:"));
	}

	/**
	 * 无匹配结果时的提示
	 */
	@Test
	void testNoMatches() {
		ToolResultBlock result = this.call(Map.of("pattern", "zzz-not-found", "directory", this.root()));
		assertEquals("未找到匹配。", ToolResults.text(result));
	}

	/**
	 * 单文件搜索时文件不存在应返回错误
	 */
	@Test
	void testSearchMissingFileReturnsError() {
		ToolResultBlock result = this.call(Map.of("pattern", "x", "file", this.tempDir.resolve("missing.log").toString()));
		assertTrue(ToolResults.text(result).startsWith("Error:"));
	}

	/**
	 * 不提供目录时默认搜索整个工作区
	 */
	@Test
	void testSearchDefaultsToWorkspace() throws IOException {
		Path marker = Files.createTempFile(Paths.get(Clarinet.WORKSPACE), "search-default-", ".log");
		try {
			Files.writeString(marker, "default-marker-content ERROR");
			ToolResultBlock result = this.call(Map.of("pattern", "default-marker-content"));
			assertTrue(ToolResults.text(result).contains("default-marker-content"));
		} finally {
			Files.deleteIfExists(marker);
		}
	}

	/**
	 * pattern 为空应被拒绝
	 */
	@Test
	void testEmptyPatternDenied() {
		assertEquals(PermissionBehavior.DENY, this.check(Map.of()));
	}

	/**
	 * 目录不存在应被拒绝
	 */
	@Test
	void testMissingDirectoryDenied() {
		assertEquals(PermissionBehavior.DENY, this.check(Map.of("pattern", "x", "directory", this.tempDir.resolve("missing").toString())));
	}

	/**
	 * 目录参数不是目录应被拒绝
	 */
	@Test
	void testFileNotDirectoryDenied() {
		assertEquals(PermissionBehavior.DENY, this.check(Map.of("pattern", "x", "directory", this.tempDir.resolve("notes.txt").toString())));
	}

	/**
	 * 文件参数不是文件（是目录）应被拒绝
	 */
	@Test
	void testDirectoryAsFileDenied() {
		assertEquals(PermissionBehavior.DENY, this.check(Map.of("pattern", "x", "file", this.tempDir.toString())));
	}

	/**
	 * max_matches 参数类型非法应被拒绝
	 */
	@Test
	void testInvalidMaxMatchesTypeDenied() {
		assertEquals(PermissionBehavior.DENY, this.check(Map.of("pattern", "x", "max_matches", "many")));
	}

	/**
	 * max_matches 为 0 应被拒绝
	 */
	@Test
	void testZeroMaxMatchesDenied() {
		assertEquals(PermissionBehavior.DENY, this.check(Map.of("pattern", "x", "max_matches", 0)));
	}

	/**
	 * 工作区外目录需要授权
	 */
	@Test
	void testOutsideWorkspaceRequiresAsk() {
		assertEquals(PermissionBehavior.ASK, this.check(Map.of("pattern", "x", "directory", this.root())));
	}

	/**
	 * 指定文件优先于目录：文件在工作区内时无需授权，即便目录在工作区外
	 */
	@Test
	void testFileTakesPrecedenceOverDirectory() {
		assertEquals(
				PermissionBehavior.ALLOW,
				this.check(Map.of("pattern", "x", "directory", this.root(), "file", workspaceFile.toString()))
		);
	}

	/**
	 * 默认目录为工作区，无需授权
	 */
	@Test
	void testDefaultDirectoryAllowed() {
		assertEquals(PermissionBehavior.ALLOW, this.check(Map.of("pattern", "x")));
	}

	private String root() {
		return this.tempDir.toString();
	}

	private ToolResultBlock call(Map<String, Object> input) {
		return this.tool.callAsync(ToolCallParam.builder().input(input).build()).block();
	}

	private PermissionBehavior check(Map<String, Object> input) {
		PermissionDecision decision = this.tool.checkPermissions(input, null).block();
		return decision.getBehavior();
	}
}
