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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Markdown 骨架工具单元测试
 *
 * @author liudongyu
 */
class MdOutlineToolTest {
	private static final String CONTENT = String.join("\n",
			"# Title",
			"some text",
			"## Section A",
			"```java",
			"# not a heading",
			"```",
			"### Sub",
			"####### Too deep",
			"#no-space",
			"## Section B"
	);

	private final MdOutlineTool tool = new MdOutlineTool();

	@TempDir
	private Path tempDir;

	private static Path workspaceFile;

	/**
	 * 在真实工作区中准备一个文件，用于验证工作区内免授权分支
	 */
	@BeforeAll
	static void createWorkspaceFile() throws IOException {
		workspaceFile = Files.createTempFile(Paths.get(Clarinet.WORKSPACE), "md-outline-test-", ".md");
	}

	/**
	 * 清理工作区临时文件
	 */
	@AfterAll
	static void deleteWorkspaceFile() throws IOException {
		Files.deleteIfExists(workspaceFile);
	}

	/**
	 * 默认输出：行号 + 标题，忽略代码块内标题与超过 6 级的标题
	 */
	@Test
	void testOutlineDefault() throws IOException {
		Path file = this.write("doc.md", CONTENT);
		ToolResultBlock result = this.call(Map.of("file", file.toString()));
		assertEquals(
				String.join("\n",
						"1: # Title",
						"3: ## Section A",
						"7: ### Sub",
						"10: ## Section B",
						"文档共 10 行"
				),
				ToolResults.text(result)
		);
	}

	/**
	 * 关闭行号输出时只保留标题与总行数
	 */
	@Test
	void testOutlineWithoutLineNumbers() throws IOException {
		Path file = this.write("doc.md", CONTENT);
		ToolResultBlock result = this.call(Map.of("file", file.toString(), "line_number", false));
		assertEquals(
				String.join("\n",
						"# Title",
						"## Section A",
						"### Sub",
						"## Section B",
						"文档共 10 行"
				),
				ToolResults.text(result)
		);
	}

	/**
	 * 限制最高标题级数
	 */
	@Test
	void testOutlineWithMaxLevel() throws IOException {
		Path file = this.write("doc.md", CONTENT);
		ToolResultBlock result = this.call(Map.of("file", file.toString(), "max_level", 2));
		assertEquals(
				String.join("\n",
						"1: # Title",
						"3: ## Section A",
						"10: ## Section B",
						"文档共 10 行"
				),
				ToolResults.text(result)
		);
	}

	/**
	 * 超过 6 的标题级数按 6 处理
	 */
	@Test
	void testOutlineMaxLevelClamped() throws IOException {
		Path file = this.write("doc.md", CONTENT);
		ToolResultBlock result = this.call(Map.of("file", file.toString(), "max_level", 10));
		assertEquals(
				String.join("\n",
						"1: # Title",
						"3: ## Section A",
						"7: ### Sub",
						"10: ## Section B",
						"文档共 10 行"
				),
				ToolResults.text(result)
		);
	}

	/**
	 * 空文档输出总行数为 0
	 */
	@Test
	void testEmptyDocument() throws IOException {
		Path file = this.write("empty.md", "");
		ToolResultBlock result = this.call(Map.of("file", file.toString()));
		assertEquals("文档共 0 行", ToolResults.text(result));
	}

	/**
	 * 读取不存在的文件应返回错误结果
	 */
	@Test
	void testMissingFileReturnsError() {
		ToolResultBlock result = this.call(Map.of("file", this.tempDir.resolve("missing.md").toString()));
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
		assertEquals(PermissionBehavior.DENY, this.check(Map.of("file", this.tempDir.resolve("missing.md").toString())));
	}

	/**
	 * 参数类型非法应被拒绝
	 */
	@Test
	void testInvalidMaxLevelTypeDenied() throws IOException {
		Path file = this.write("doc.md", CONTENT);
		assertEquals(PermissionBehavior.DENY, this.check(Map.of("file", file.toString(), "max_level", "two")));
	}

	/**
	 * 工作区外文件需要授权
	 */
	@Test
	void testOutsideWorkspaceRequiresAsk() throws IOException {
		Path file = this.write("outside.md", CONTENT);
		assertEquals(PermissionBehavior.ASK, this.check(Map.of("file", file.toString())));
	}

	/**
	 * 工作区内文件无需授权
	 */
	@Test
	void testInsideWorkspaceAllowed() {
		assertEquals(PermissionBehavior.ALLOW, this.check(Map.of("file", workspaceFile.toString())));
	}

	private Path write(String name, String content) throws IOException {
		Path file = this.tempDir.resolve(name);
		Files.writeString(file, content, StandardCharsets.UTF_8);
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
