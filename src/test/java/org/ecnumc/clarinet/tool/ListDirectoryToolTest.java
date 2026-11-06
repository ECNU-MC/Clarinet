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
 * 列举目录工具单元测试
 *
 * @author liudongyu
 */
class ListDirectoryToolTest {
	private final ListDirectoryTool tool = new ListDirectoryTool();

	@TempDir
	private Path tempDir;

	private static Path workspaceDir;

	/**
	 * 在真实工作区中准备一个目录，用于验证工作区内免授权分支
	 */
	@BeforeAll
	static void createWorkspaceDir() throws IOException {
		workspaceDir = Files.createTempDirectory(Paths.get(Clarinet.WORKSPACE), "list-dir-test-");
	}

	/**
	 * 清理工作区临时目录
	 */
	@AfterAll
	static void deleteWorkspaceDir() throws IOException {
		Files.deleteIfExists(workspaceDir);
	}

	/**
	 * 构造测试目录结构：
	 * <pre>
	 * a/
	 * b.txt
	 * c/
	 *   Main.java
	 *   sub/
	 *     notes.txt
	 * d.db
	 * empty/
	 * </pre>
	 */
	@BeforeEach
	void setUpTree() throws IOException {
		Files.createDirectories(this.tempDir.resolve("a"));
		Files.createDirectories(this.tempDir.resolve("c/sub"));
		Files.createDirectories(this.tempDir.resolve("empty"));
		Files.writeString(this.tempDir.resolve("b.txt"), "");
		Files.writeString(this.tempDir.resolve("c/Main.java"), "");
		Files.writeString(this.tempDir.resolve("c/sub/notes.txt"), "");
		Files.writeString(this.tempDir.resolve("d.db"), "");
	}

	/**
	 * 默认递归列举，输出 tree 风格结构
	 */
	@Test
	void testListRecursively() {
		ToolResultBlock result = this.call(Map.of("directory", this.root()));
		assertEquals(
				String.join("\n",
						this.root(),
						"├── a",
						"├── b.txt",
						"├── c",
						"│   ├── Main.java",
						"│   └── sub",
						"│       └── notes.txt",
						"├── d.db",
						"└── empty"
				),
				ToolResults.text(result)
		);
	}

	/**
	 * 非递归列举时只输出一层
	 */
	@Test
	void testListNonRecursively() {
		ToolResultBlock result = this.call(Map.of("directory", this.root(), "recursive", false));
		assertEquals(
				String.join("\n",
						this.root(),
						"├── a",
						"├── b.txt",
						"├── c",
						"├── d.db",
						"└── empty"
				),
				ToolResults.text(result)
		);
	}

	/**
	 * 按扩展名筛选文件，目录始终列举
	 */
	@Test
	void testFilterByExtension() {
		ToolResultBlock result = this.call(Map.of("directory", this.root(), "extension", "txt"));
		assertEquals(
				String.join("\n",
						this.root(),
						"├── a",
						"├── b.txt",
						"├── c",
						"│   └── sub",
						"│       └── notes.txt",
						"└── empty"
				),
				ToolResults.text(result)
		);
	}

	/**
	 * 扩展名筛选不区分大小写，且容忍前导点
	 */
	@Test
	void testFilterExtensionIsCaseInsensitive() throws IOException {
		Files.writeString(this.tempDir.resolve("E.TXT"), "");
		ToolResultBlock result = this.call(Map.of("directory", this.root(), "extension", ".TXT"));
		assertEquals(
				String.join("\n",
						this.root(),
						"├── a",
						"├── b.txt",
						"├── c",
						"│   └── sub",
						"│       └── notes.txt",
						"├── E.TXT",
						"└── empty"
				),
				ToolResults.text(result)
		);
	}

	/**
	 * 目录路径为空应被拒绝
	 */
	@Test
	void testEmptyDirectoryDenied() {
		assertEquals(PermissionBehavior.DENY, this.check(Map.of()));
	}

	/**
	 * 目录不存在应被拒绝
	 */
	@Test
	void testMissingDirectoryDenied() {
		assertEquals(PermissionBehavior.DENY, this.check(Map.of("directory", this.tempDir.resolve("missing").toString())));
	}

	/**
	 * 路径不是目录应被拒绝
	 */
	@Test
	void testFileNotDirectoryDenied() {
		assertEquals(PermissionBehavior.DENY, this.check(Map.of("directory", this.tempDir.resolve("b.txt").toString())));
	}

	/**
	 * 参数类型非法应被拒绝
	 */
	@Test
	void testInvalidRecursiveTypeDenied() {
		assertEquals(PermissionBehavior.DENY, this.check(Map.of("directory", this.root(), "recursive", "yes")));
	}

	/**
	 * 工作区外目录需要授权
	 */
	@Test
	void testOutsideWorkspaceRequiresAsk() {
		assertEquals(PermissionBehavior.ASK, this.check(Map.of("directory", this.root())));
	}

	/**
	 * 工作区内目录无需授权
	 */
	@Test
	void testInsideWorkspaceAllowed() {
		assertEquals(PermissionBehavior.ALLOW, this.check(Map.of("directory", workspaceDir.toString())));
	}

	/**
	 * 列举不存在的目录应返回错误结果
	 */
	@Test
	void testListMissingDirectoryReturnsError() {
		Path missing = this.tempDir.resolve("nope");
		ToolResultBlock result = this.call(Map.of("directory", missing.toString()));
		assertTrue(ToolResults.text(result).startsWith("Error:"));
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
