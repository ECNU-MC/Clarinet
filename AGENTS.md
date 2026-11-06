# AGENTS.md

本文档面向在本仓库中工作的 AI 编程助手与开发者，描述 Clarinet（柯拉妮）项目的定位、技术栈、项目结构、分层设计、构建运行方式与开发约定。修改代码前请先阅读本文件。

## 项目简介

Clarinet（柯拉妮）是一个基于 [AgentScope Harness](https://github.com/agentscope-ai/agentscope-java/tree/main/agentscope-harness)（`io.agentscope:agentscope-harness:2.0.0-RC3`）构建的 **Minecraft 崩溃分析智能体** 服务。

它不是通用聊天机器人，而是专门为 Minecraft 模组开发与崩溃分析场景设计：通过 HTTP 接口接收 QQ 群聊消息，交给大模型（默认 DeepSeek）驱动的 `HarnessAgent` 处理；智能体通过注册好的工具读取崩溃日志文件、查询用户历史求助、统计求助数量等，最终以文本消息或表情包的形式回复群聊。

核心特征：

- 面向 QQ 群聊的 Agent 服务，对外暴露 `POST /api/msg` 接口；
- 智能体工具（Toolkit）驱动：读文件、列举目录、提取 Markdown 大纲、Base64 编解码、查历史、发消息、发表情包；
- 会话（Session）按 JSON 文件持久化，群聊上下文按群维护最近 100 条消息；
- 用户、求助记录等业务数据存储于 MySQL（MyBatis-Plus）；
- 消息脱敏：将 QQ 消息中的 `[CQ:at,qq=xxx]` 替换为不含 QQ 号的 `[at, uid=xxx]` 后再送入模型。

## 技术栈

| 类别 | 选型 |
| :--- | :--- |
| 语言 | Java 17（`sourceCompatibility = 17`） |
| 构建 | Gradle 9.2.1（wrapper：`gradlew` / `gradlew.bat`） |
| Agent 框架 | `io.agentscope:agentscope-harness:2.0.0-RC3` |
| ORM | MyBatis-Plus `3.5.16` + MySQL Connector/J `8.0.33`（原生 MyBatis `SqlSessionFactory`） |
| 日志 | Log4j2 `2.26.0`（log4j-api + log4j-core） |
| 代码生成 | Lombok `1.18.42` + MapStruct `1.6.3` |
| 缓存 | Caffeine `3.2.2`（会话与消息映射缓存） |
| JSON | fastjson `2.0.60` / fastjson2 |
| 工具库 | Guava `31.1-jre`、jsoup `1.21.2`、JSpecify / JSR-305 空值注解 |
| 测试 | JUnit 5（`useJUnitPlatform()`）+ Mockito `5.14.2`，单元测试不依赖数据库与网络 |

## 快速开始

### 前置条件

1. JDK 17 及以上；
2. MySQL 8（本地默认 `localhost:3306`），库名 `clarinet`，含 `customer`、`assistance`、`agendum` 三张表（见下文“数据库”一节）；
3. 可访问的大模型 API（默认 DeepSeek，OpenAI 兼容协议）。

### 构建

```bash
# Windows
gradlew.bat build   # 完整构建（编译 + 打包 + 测试）

# Linux / macOS
./gradlew build
```

构建产物位于 `build/libs/`（`clarinet-0.0.1.jar`）。

注意：`build.gradle` 仅启用 `java` 插件，未配置 `application` 插件与 `Main-Class`，因此产物 jar 不可直接 `java -jar` 运行，需要通过 IDE 或手动指定 classpath 启动。

编码约定：`build.gradle` 通过 `tasks.withType(JavaCompile) { options.encoding = 'UTF-8' }` 统一 javac 编码，所有源码均为 UTF-8，请勿保存为 GBK，否则在中文 Windows 默认字符集下会编译失败。

### 运行

主类：`org.ecnumc.clarinet.Clarinet`

- 方式一（推荐）：在 IDE（IDEA 等）中直接运行 `Clarinet.main`；
- 方式二：先 `gradlew build`，再手动拼 classpath 运行（依赖较多，不推荐手工拼装）。

启动后监听 `system.json` 中配置的端口（默认 `2565`），日志输出到控制台、`logs/latest.log` 与 `logs/debug.log`。

### 测试

单元测试基于 JUnit 5 + Mockito，运行：

```bash
gradlew.bat test      # Windows
./gradlew test        # Linux / macOS
```

全部单测通过 Mockito 模拟下游依赖，**不依赖本地 MySQL、不访问网络**：

- `tool/*Test`：10 个工具各一份单测（`SendMessageToolTest`、`SendReactionToolTest`、`ReadToolTest`、`ListDirectoryToolTest`、`MdOutlineToolTest`、`Base64EncodeToolTest`、`Base64DecodeToolTest`、`RecentAssistanceToolTest`、`StatsToolTest`、`CollectChatLogToolTest`），覆盖权限校验（`checkPermissions`）与执行逻辑（`callAsync`）；
- `RecentAssistanceToolTest` / `StatsToolTest` 通过 mock `SqlSessionFactory` → `SqlSession` → `AssistanceMapper` 链路模拟数据访问；
- `CollectChatLogToolTest` 直接 mock `SessionRepository`；
- `ReadToolTest` 使用 `@TempDir` 临时文件，工作区授权分支使用真实工作区中的临时文件（测试后自动清理）；
- `convert/ContentConvertTest` 通过 mock `CustomerRepository` 验证脱敏替换；
- `repository/*Test`：`CustomerRepositoryTest`、`AssistanceQueryRepositoryTest`（mock `SqlSession` 链路）、`SessionRepositoryTest`（`@TempDir` 文件持久化，不依赖数据库）；
- `support/ToolResults`：测试辅助类，将 `ToolResultBlock` 的文本内容拼接为字符串供断言。

新增工具或修改下游交互时请同步维护对应 `*Test`，保持测试可独立运行、不落库。

## 配置说明

首次启动时，程序会自动创建工作区目录：

```
<user.home>/.agentscope/workspaces/clarinet
```

并在其中生成默认配置文件 `system.json`（若不存在），后续启动读取该文件。字段如下：

| 字段 | 默认值 | 说明 |
| :--- | :--- | :--- |
| `modelProtocol` | `openai` | 模型协议，`openai` 或 `anthropic`，决定使用 `OpenAIChatModel` 还是 `AnthropicChatModel` |
| `modelName` | `deepseek-v4-flash` | 模型名称 |
| `apiKey` | （默认示例值） | 模型 API Key |
| `baseUrl` | `https://api.deepseek.com` | 模型服务地址 |
| `port` | `2565` | HTTP 服务端口，接收外部消息 |
| `wakeupWord` | `柯拉妮` | 唤醒词，普通消息需包含唤醒词且用户权限满足要求才可开启新会话 |

工作区目录同时用于会话持久化（`sessions/` 子目录，每个会话一个 JSON 文件）。`Clarinet.WORKSPACE` 常量即该路径，`ReadTool` 以该路径判定“工作区内文件免授权”。

## 项目结构

```
clarinet/
├── build.gradle                  # Gradle 构建脚本（依赖、Java 17、JUnit/Mockito、UTF-8 编码）
├── settings.gradle               # rootProject.name = 'clarinet'
├── gradlew / gradlew.bat         # Gradle wrapper
├── gradle/wrapper/               # wrapper 配置（Gradle 9.2.1）
├── README.md                     # 项目 README（智能体定位与分析方法论）
├── LICENSE                       # Artistic License 2.0
├── logs/                         # 运行时日志（gitignore）
└── src/
    ├── main/
    │   ├── java/org/ecnumc/clarinet/
    │   │   ├── Clarinet.java            # 入口：初始化 MyBatis、注册工具、构建 HarnessAgent、启动 HTTP 服务
    │   │   ├── ClarinetLogger.java      # Log4j2 静态日志门面
    │   │   ├── bo/                      # 业务枚举：MessageType、MessageRole、Reaction（表情包）、Permission（权限）
    │   │   ├── config/SystemConfig.java # 系统配置加载与默认值
    │   │   ├── convert/                 # 转换器：ContentConvert（CQ 码脱敏）、AssistanceConvert（求助记录转 JSON）
    │   │   ├── data/                    # 会话数据模型：Message、Session（含转 agentscope Msg 逻辑）
    │   │   ├── exception/               # FailedToLoadConfigException
    │   │   ├── handler/MessageHandler.java  # HTTP 处理器：解析消息、会话判定、调用 agent
    │   │   ├── mapper/                  # MyBatis Mapper 接口：CustomerMapper、AssistanceMapper
    │   │   ├── po/                      # 持久化对象：Customer、Assistance、Agendum
    │   │   ├── prompt/                  # 预留：提示词包（当前为空）
    │   │   ├── repository/              # 数据访问：BaseRepository、CustomerRepository、SessionRepository、AssistanceQueryRepository
    │   │   ├── state/ClarinetStateStore.java  # 禁用 agentscope 原版状态存储（空实现）
    │   │   └── tool/                    # Agent 工具集（见“工具清单”）
    │   └── resources/
    │       ├── mybatis-config.xml       # MyBatis 全局配置（MySQL 连接、下划线转驼峰）
    │       ├── log4j2.xml               # 日志配置（控制台 + 滚动文件）
    │       └── mapper/                  # Mapper XML：CustomerMapper.xml、AssistanceMapper.xml
    └── test/
        └── java/org/ecnumc/clarinet/
            ├── support/ToolResults.java        # 测试辅助：工具结果文本提取
            ├── convert/ContentConvertTest.java # 脱敏逻辑单测（mock 用户仓库）
            ├── repository/
            │   ├── AssistanceQueryRepositoryTest.java  # 求助仓库单测（mock SqlSession）
            │   ├── CustomerRepositoryTest.java         # 用户仓库单测（mock SqlSession）
            │   └── SessionRepositoryTest.java          # 会话仓库单测（临时目录持久化）
            └── tool/
                ├── Base64DecodeToolTest.java
                ├── Base64EncodeToolTest.java
                ├── CollectChatLogToolTest.java
                ├── ListDirectoryToolTest.java
                ├── MdOutlineToolTest.java
                ├── ReadToolTest.java
                ├── RecentAssistanceToolTest.java
                ├── SendMessageToolTest.java
                ├── SendReactionToolTest.java
                └── StatsToolTest.java
```

## 分层设计

项目遵循清晰的纵向分层，依赖方向为：入口层 → 接口层 → 工具层 / 数据层 → 基础设施。

```
┌─────────────────────────────────────────────────────────────┐
│ 入口层（Clarinet.main）                                      │
│   构建 MyBatis SqlSessionFactory、注册 ToolKit、构建          │
│   HarnessAgent、启动 com.sun.net.httpserver.HttpServer       │
└──────────────────────────┬──────────────────────────────────┘
                           │ HTTP POST /api/msg
┌──────────────────────────▼──────────────────────────────────┐
│ 接口层（handler.MessageHandler）                             │
│   解析消息 → 脱敏（convert.ContentConvert）→ 用户与会话判定 →  │
│   组装 Session 并调用 agent.call(...) → 收集工具输出并返回     │
└──────────────────────────┬──────────────────────────────────┘
                           │ 调用
┌──────────────────────────▼──────────────────────────────────┐
│ Agent 层（agentscope-harness：HarnessAgent + 大模型）        │
│   会话消息（data.Session.cast → List<Msg>）送入模型，         │
│   模型按需调用工具（tool.*）                                  │
└───────┬─────────────────────────────────────┬───────────────┘
        │ 工具调用                             │ 数据读写
┌───────▼───────────────┐   ┌─────────────────▼──────────────────────┐
│ 工具层（tool.*）       │   │ 数据层                                 │
│ SendMessageTool       │   │ repository.*（BaseRepository 模板方法   │
│ SendReactionTool      │   │   封装 SqlSession 获取/提交）            │
│ ReadTool              │   │   ├── SessionRepository（JSON 文件持久化 │
│ ListDirectoryTool     │   │   │   + Caffeine 缓存 + 群消息队列）     │
│ MdOutlineTool         │   │   ├── CustomerRepository（用户）        │
│ Base64Encode/Decode   │   │   └── AssistanceQueryRepository        │
│ RecentAssistanceTool  │   │ mapper.*（MyBatis 接口 + XML）          │
│ StatsTool             │   │ po.*（Customer / Assistance / Agendum）│
│ CollectChatLogTool    │   │                                        │
└───────────────────────┘   └───────────────────────────────────────┘
```

各层职责：

- **入口层**（`Clarinet`）：进程生命周期管理。静态块中创建 `WORKSPACE` 目录并加载 `SYSTEM_CONFIG`；`main` 中初始化 MyBatis、注册全部工具、构建 `HarnessAgent`（160k token 触发上下文压缩）、启动 HTTP 服务（10 线程固定线程池）、注册关闭钩子。
- **接口层**（`handler`）：唯一的 HTTP 入口 `POST /api/msg`。负责参数解析、`ContentConvert` 脱敏、用户/会话管理、调用 agent（`synchronized (agent)` 串行化，避免并发调用同一 agent 实例）、返回模型通过 `send_message` / `send_reaction` 产生的输出。
- **数据模型层**（`data`、`bo`）：`Message` / `Session` 描述会话结构；`Session.cast()` 将内部模型转为 agentscope `Msg` 列表；`bo` 包承载 `MessageType`（消息类型与会话开启判定）、`MessageRole`（角色映射）、`Permission`（权限等级）、`Reaction`（表情包枚举，含面向模型的描述表）。
- **工具层**（`tool`）：所有 `ToolBase` 子类。每个工具定义 `inputSchema`、`checkPermissions`（权限校验）与 `callAsync`（实际执行），只读类工具标记 `readOnly(true)` 与 `concurrencySafe(true)`。
- **数据层**（`repository` / `mapper` / `po`）：`BaseRepository` 提供模板方法 `execute`，统一管理 `SqlSession` 打开、Mapper 获取与提交；`po` 为数据库表映射对象；`mapper` 接口与 XML 一一对应（SQL 见 `resources/mapper/`）。
- **基础设施**（`config` / `convert` / `state` / `exception`）：配置加载、消息转换、状态存储开关（`ClarinetStateStore` 为空实现，禁用 agentscope 默认持久化，会话持久化完全交由 `SessionRepository` 负责）。

## 核心消息处理流程

以收到一条 QQ 群消息为例，完整链路如下：

1. 外部机器人转发消息到 `POST /api/msg`，请求体为 JSON：`group_id`、`message_id`、`role`、`type`（plain/image/file/directory）、`user_id`、`user_name`、`content`、`sent_at`（`yyyy-MM-dd HH:mm:ss`）、可选 `replied_message` 对象；
2. `MessageHandler` 解析请求，`ContentConvert.encode` 将 `[CQ:at,qq=xxx]` 脱敏为 `[at, uid=xxx]`；
3. `CustomerRepository.getOrCreateCustomerByQq` 按 QQ 号查询用户，不存在则创建（默认 `NORMAL` 权限，`uid` 由 QQ 号 UUID 化生成）；
4. `SessionRepository.addMessage` 将消息加入该群的消息上下文队列（每群最多保留最近 100 条）；
5. 会话判定：
   - 若消息引用了 `replied_message`，通过 `getSessionFromMessageId` 反查原会话（依赖发送消息时写入的 `messageSessionLookup` 缓存）；
   - 否则若 `MessageType.byCode(type).canStartSession(...)` 为真（文件消息，或消息包含唤醒词且用户权限等级 > 1）则创建新会话；
   - 都不满足则直接返回，不调用模型；
6. 将新消息加入会话并持久化（`SessionRepository.updateSession` 写 JSON 文件）；
7. `synchronized (agent)` 下调用 `agent.call(session.cast(), context)`，`RuntimeContext` 携带 `groupId`；
8. 模型执行过程中调用的 `send_message` / `send_reaction` 输出被收集进返回体的 `message` 数组；
9. 返回 `{"message": [...]}`，外部机器人据此发送消息与表情包。

## 工具清单

| 工具名 | 类 | 说明 |
| :--- | :--- | :--- |
| `send_message` | `SendMessageTool` | 发送文本消息，是与提问者交互的唯一途径（长度 ≤ 4095） |
| `send_reaction` | `SendReactionTool` | 发送表情包，12 种枚举（见 `bo.Reaction`），仅闲聊场景 |
| `read` | `ReadTool` | 读文件：全量 / `tail` 末尾 N 行 / `begin`+`end` 区间，默认附带行号（`line_number=false` 可关闭）；工作区内免授权，工作区外需授权 |
| `list_directory` | `ListDirectoryTool` | 以 GNU tree 风格（`├──`/`└──`/`│`）列举目录，可选递归（默认 true）与按扩展名筛选文件；工作区内免授权，工作区外需授权 |
| `md_outline` | `MdOutlineTool` | 提取 Markdown 标题大纲（“行号: 标题”，忽略代码块内标题，末尾输出“文档共 xx 行”），可选行号与最高标题级数（默认 6）；权限同 `read` |
| `base64_decode` | `Base64DecodeTool` | Base64 解码 |
| `base64_encode` | `Base64EncodeTool` | Base64 编码 |
| `recent_assistance` | `RecentAssistanceTool` | 查询用户近期求助历史（按 uid，默认 5 条，最多 100 条） |
| `stats` | `StatsTool` | 统计某日求助总数，支持按 uid / dealt（是否解决）筛选 |
| `collect_chat_log` | `CollectChatLogTool` | 收集群聊最近若干条聊天记录 |

新增工具的标准步骤：在 `tool` 包中继承 `ToolBase`，实现 `inputSchema` / `checkPermissions` / `callAsync`，最后在 `Clarinet.main` 中 `toolkit.registerAgentTool(...)` 注册。

## 数据库

数据库 `clarinet`（连接配置见 `resources/mybatis-config.xml`，默认 `root/123456@localhost:3306`）：

- **`customer`**：C 端用户。字段：`id`、`uid`（UUID 字符串）、`qq`、`appellation`（称谓，可空）、`permission`（`Permission` 枚举名）、`created_at`；
- **`assistance`**：求助/解答记录。字段：`id`、`session_id`、`uid`、`filename`、`dealt`（是否解决）、`description`、`parent_session_id`（可空）、`created_at`、`updated_at`；`description` 列上的关键词检索依赖 MySQL 全文索引（`MATCH ... AGAINST`），建表时需为 `description` 添加 `FULLTEXT` 索引；
- **`agendum`**：待办/提醒记录。字段：`id`、`session_id`、`content`、`dealt`、`notification_time`（可空）、`created_at`、`updated_at`。

`AssistanceMapper.xml` 与 `CustomerMapper.xml` 中的 SQL 为实际执行语句，修改 SQL 时需保持 Mapper 接口、XML 与 `po` 字段三方一致。

单元测试不访问数据库（下游链路均由 Mockito 模拟），数据库仅在运行服务与真实集成验证时使用。

## 开发约定

1. **构建验证**：修改后执行 `gradlew.bat build` 确认完整构建（编译 + 单元测试 + 打包）通过；单元测试通过 Mockito 模拟下游依赖，无需本地 MySQL；如需验证真实 SQL 或数据层，再在本地 MySQL 环境中手工验证。
2. **命名与风格**：遵循仓库统一约定——类名大驼峰、方法/变量小驼峰、常量全大写加下划线、包名全小写；Javadoc 使用中文；可能为空的字段/参数/返回值使用 `@Nullable` 注解。
3. **会话并发**：`MessageHandler` 对 `agent` 实例做了 `synchronized` 串行化，新增并发路径时不得绕过该锁并发调用同一 `HarnessAgent`。
4. **新工具接入**：工具类需同时完成权限校验（`checkPermissions`）与注册（`Clarinet.main`），否则不会生效；只读工具应声明 `readOnly(true)` / `concurrencySafe(true)`。
5. **会话持久化**：会话数据不落数据库，由 `SessionRepository` 以 JSON 文件形式保存在 `WORKSPACE/sessions/`，并辅以 Caffeine 缓存；不要轻易改为数据库存储，除非同步调整 `SessionRepository` 的读写路径。
6. **敏感信息**：QQ 号等个人敏感信息必须在送入模型前脱敏（参照 `ContentConvert`），模型输出侧不应回传原始 QQ 号。
7. **日志**：统一通过 `ClarinetLogger` 输出；调试信息使用 `debug` 级别，避免污染 `logs/latest.log`。
