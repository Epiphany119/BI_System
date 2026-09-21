# 智能 BI 平台 V1.0

## bi-ai-runtime 模块 PRD（含需求分析与技术设计）

| 项目 | 内容 |
|---|---|
| 文档类型 | 分模块 PRD + 技术设计 |
| Maven 模块 | `modules/bi-ai-runtime` |
| ArtifactId | `bi-ai-runtime` |
| 文档版本 | V1.0-draft |
| 文档状态 | 初稿，待逐项评审 |
| 上级文档 | `PRD-V1.0-创建智能分析任务.md` |
| 关联模块 | `bi-module-analysis-biz`、`bi-common`、`bi-web` |
| 首期供应商 | 智谱 AI（OpenAI-compatible Chat Completions） |

## 1. 文档目的

本文定义 `bi-ai-runtime` 的产品需求、模块边界、技术方案、Maven 依赖、包结构、异常规范、测试方案和验收标准。

本文既是研发输入，也是模块交付和验收依据。代码中的 Maven 模块、Java 包、公开接口和测试用例必须能够映射到本文档中的需求编号。

## 2. 需求分析

### 2.1 当前问题

当前项目已经能够调用智谱 AI，但存在以下结构问题：

- AI HTTP 调用位于 `bi-infra`，AI 能力没有形成独立模块。
- AI 客户端内部硬编码了智能分析 Prompt，导致运行时模块了解 ECharts 和分析业务。
- 供应商协议解析、图表结果解析和业务异常混在一起。
- 上层代码直接依赖具体实现 `AiManager`，未来切换供应商成本较高。
- HTTP 401、429、5xx、连接超时和响应格式错误没有形成稳定的错误分类。
- 缺少独立模块级契约、Mock 测试和可验收指标。

### 2.2 核心诉求

业务模块只需要表达“使用什么 Prompt、发送什么内容”，不应该关心：

- API 地址和鉴权头如何拼接；
- 智谱返回的 `choices[0].message.content` 如何提取；
- HTTP 状态码如何转换成系统错误；
- 使用哪个 HTTP 客户端；
- API Key 和超时参数从哪里加载。

### 2.3 模块使用者

`bi-ai-runtime` 是内部技术能力模块，不直接面向最终用户。主要调用方：

| 调用方 | 使用场景 |
|---|---|
| `bi-module-analysis-biz` | 提交数据分析 Prompt，获取模型文本 |
| 后续报告模块 | 生成报告摘要、标题或说明 |
| 后续智能问答模块 | 提交对话请求 |
| `bi-web` | 负责 Spring Boot 模块装配，不承载 AI 业务逻辑 |

## 3. 产品目标

### 3.1 V1.0 目标

- 提供供应商无关的文本对话接口。
- 首期实现智谱 AI 适配器。
- 统一配置、鉴权、超时、错误分类和响应元数据。
- 允许调用方传入业务 Prompt，模块内部不硬编码分析业务。
- 为未来增加其他供应商保留扩展点。
- 使用 Mock 响应完成模块测试，不依赖真实 API Key。

### 3.2 V1.0 非目标

- 不解析 Excel、CSV 或其他业务文件。
- 不生成或校验 ECharts 配置。
- 不解析 `genChart`、`genResult` 等分析业务结构。
- 不保存任务、图表、对话记录或 Token 账单。
- 不处理用户登录、权限、限流和套餐额度。
- 不负责 RabbitMQ 投递、任务状态机和业务级重试。
- 不实现流式输出、图片生成、语音或多模态输入。

## 4. 模块边界

### 4.1 负责范围

`bi-ai-runtime` 负责：

- 定义统一 AI 对话接口。
- 接收系统 Prompt、用户消息和可选生成参数。
- 根据配置选择供应商适配器。
- 构造供应商请求并执行 HTTP 调用。
- 提取供应商响应中的模型文本和基础元数据。
- 将供应商错误转换为统一异常。
- 对敏感配置和日志输出进行保护。

### 4.2 不负责范围

以下职责必须由其他模块承担：

| 职责 | 所属模块 |
|---|---|
| 分析 Prompt 模板 | `bi-module-analysis-biz` |
| 图表配置和分析结论解析 | `bi-module-analysis-biz` |
| Excel/CSV 转换 | `bi-module-analysis-biz` 或后续数据集模块 |
| AI 任务状态、重试、降级到 MQ | `bi-module-analysis-biz` |
| 通用异常响应对象 | `bi-common` |
| 应用启动和 Bean 装配 | `bi-web` |

### 4.3 依赖规则

```text
bi-module-analysis-biz ──> bi-ai-runtime ──> bi-common
bi-web                  ──> bi-ai-runtime
```

禁止出现：

```text
bi-ai-runtime ──> bi-module-analysis-biz
bi-ai-runtime ──> bi-infra
bi-ai-runtime ──> bi-web
```

## 5. 功能需求

| 编号 | 需求 | 优先级 | 验收说明 |
|---|---|---|---|
| AIR-FR-001 | 提供统一同步文本对话接口 | P0 | 业务模块不引用智谱 DTO 即可调用 |
| AIR-FR-002 | 支持 system 和 user 消息 | P0 | 两类消息按正确顺序发送 |
| AIR-FR-012 | V1.0 支持同步非流式调用 | P0 | 返回完整 assistant 文本后再结束请求 |
| AIR-FR-003 | 支持配置默认模型、模型白名单、温度和超时 | P0 | 未指定模型时使用默认模型；指定模型必须通过白名单校验 |
| AIR-FR-004 | 实现智谱 Chat Completions 适配器 | P0 | 成功提取首个 assistant 文本 |
| AIR-FR-005 | 识别配置、鉴权、限流、超时、5xx 和格式错误 | P0 | 不同场景映射到稳定错误码 |
| AIR-FR-006 | 返回供应商、模型、请求 ID 和 Token 使用量 | P1 | 上游缺失字段时允许为空 |
| AIR-FR-007 | API Key 不出现在日志和异常信息中 | P0 | 自动化测试和代码检查通过 |
| AIR-FR-008 | 支持 Mock HTTP 单元测试 | P0 | 测试不访问公网、不消耗额度 |
| AIR-FR-009 | 使用统一的 `bi.ai.*` 配置前缀 | P0 | Runtime 配置均从统一前缀绑定和校验 |
| AIR-FR-010 | 为未来新增供应商提供扩展接口 | P1 | 新适配器无需修改业务模块 |
| AIR-FR-011 | 按环境执行 API Key 配置校验 | P0 | local 允许启动、调用时失败；test 使用 Mock；prod 缺失时启动失败 |
| AIR-FR-013 | 使用 Java 17 HttpClient 完成供应商调用 | P0 | HTTP 实现封装在 provider 内部，不暴露给业务模块 |
| AIR-FR-014 | 提供基础结构化调用日志 | P0 | 记录耗时、成功状态、错误码和 Token 用量，且不泄露敏感信息 |

## 6. 非功能需求

### 6.1 性能与超时

- V1.0 提供同步非流式调用。
- V1.0 不建立 SSE/WebSocket 流式连接，不向业务层暴露增量 Token 回调。
- 流式调用排入后续版本，需另行设计结果拼接、断线恢复和前端展示协议。
- 连接超时默认 10 秒，读取超时默认 60 秒，均可配置。
- Runtime 不执行无上限重试。
- 是否重试、转异步或失败由业务模块根据异常类型决定。

### 6.2 安全

- API Key 只能来自环境变量或不提交 Git 的本地配置。
- 禁止在日志中打印 Authorization Header。
- 默认不打印完整 Prompt、文件内容和完整模型响应。
- 对外异常只返回脱敏摘要；完整排查信息写入受控日志。

### 6.3 可观测性

每次调用建议记录以下非敏感字段：

- provider；
- model；
- durationMs；
- success；
- errorCode；
- upstreamRequestId；
- inputTokens、outputTokens；
- 业务方传入的 traceId。

### 6.4 可维护性

- 供应商 DTO 不进入业务模块。
- Runtime 公开接口不能出现 `Chart`、`User`、`MultipartFile` 等业务类型。
- 每种错误分类必须有单元测试。
- 新增供应商时不得修改已有业务 Prompt 和任务流程。

## 7. Maven 设计

### 7.1 当前 Maven 坐标

```xml
<artifactId>bi-ai-runtime</artifactId>
```

模块直接依赖：

```xml
<dependency>
    <groupId>com.yupi</groupId>
    <artifactId>bi-common</artifactId>
    <version>${project.version}</version>
</dependency>
```

当前 Spring Boot、Hutool 和测试依赖由父 POM 继承。V1.0 编码阶段可以保持现状；后续整理父 POM 时，再将具体库收敛到 `dependencyManagement`，由模块显式声明实际使用的依赖。

### 7.2 Maven 依赖约束

- 必须依赖：`bi-common`。
- 可以使用：父 POM 管理的 Spring、HTTP/JSON 和测试库。
- 禁止依赖：所有 `bi-module-*-biz`、`bi-infra`、`bi-web`。
- `bi-module-analysis-biz` 可以依赖 `bi-ai-runtime`。

## 8. Java 包结构

包根路径使用 `com.yupi.springbootinit.airuntime`，保证能够被当前 `MainApplication` 的默认组件扫描发现。

```text
bi-ai-runtime/src/main/java/com/yupi/springbootinit/airuntime
├── api
│   ├── AiChatClient.java
│   ├── AiChatRequest.java
│   └── AiChatResponse.java
├── config
│   └── AiRuntimeProperties.java
├── exception
│   ├── AiRuntimeException.java
│   └── AiRuntimeErrorCode.java
└── provider
    └── zhipu
        ├── ZhipuAiChatClient.java
        ├── ZhipuRequest.java
        ├── ZhipuResponse.java
        └── ZhipuResponseMapper.java
```

测试包与生产包保持一一对应：

```text
bi-ai-runtime/src/test/java/com/yupi/springbootinit/airuntime
├── provider/zhipu/ZhipuAiChatClientTest.java
└── provider/zhipu/ZhipuResponseMapperTest.java
```

## 9. 核心接口设计

### 9.1 AiChatClient

```java
public interface AiChatClient {
    AiChatResponse chat(AiChatRequest request);
}
```

该接口是业务模块唯一需要依赖的 AI 调用入口。

### 9.2 AiChatRequest

```java
public class AiChatRequest {
    private String systemPrompt;
    private String userMessage;
    private String model;
    private Double temperature;
    private String traceId;
}
```

约束：

- `userMessage` 必填；
- `systemPrompt` 可选；
- `model` 为空时使用默认模型；
- `model` 不为空时必须位于配置的 `allowed-models` 白名单；
- `temperature` 为空时使用默认配置；
- `traceId` 用于日志关联，不发送敏感内容。

模型选择规则：请求未指定模型时使用配置中的默认模型；请求指定模型且通过白名单校验时才允许覆盖默认模型；不在白名单中的模型直接抛出 `MODEL_NOT_ALLOWED`，不得调用上游。当前分析业务默认不主动指定模型。

### 9.3 AiChatResponse

```java
public class AiChatResponse {
    private String content;
    private String provider;
    private String model;
    private String requestId;
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer totalTokens;
}
```

`content` 是模型原始文本。业务层是否将其解释为 JSON、图表或报告，不属于 Runtime 职责。

## 10. 配置设计

目标配置结构：

```yaml
bi:
  ai:
    provider: zhipu
    default-model: glm-4-flash
    startup-validation: false
    allowed-models:
      - glm-4-flash
      - glm-4-plus
    temperature: 0.2
    connect-timeout-ms: 10000
    read-timeout-ms: 60000
    zhipu:
      api-key: ${ZHIPU_API_KEY:}
      base-url: ${ZHIPU_BASE_URL:https://open.bigmodel.cn/api/paas/v4/chat/completions}
```

配置约定：

- V1.0 只定义并实现 `bi.ai.*` 这一套正式配置。
- 现有本地 `zhipu.*` 配置由开发者一次性人工调整为新格式。
- V1.0 不提供配置迁移命令、自动生成工具、双前缀兼容和冲突处理。
- 后续仅在出现真实兼容需求时增加兼容逻辑。
- V1.0 保留 `bi.ai.provider` 字段，但仅实现 `zhipu`；配置未实现的供应商时按 `AI_CONFIGURATION_ERROR` 处理。

配置校验策略：

| 环境 | `startup-validation` | API Key 缺失时的行为 |
|---|---:|---|
| `local` | `false` | 应用正常启动，首次调用 AI 时抛出 `AI_CONFIGURATION_ERROR` |
| `test` | `false` | 使用 Mock HTTP，不依赖真实 API Key |
| `prod` | `true` | Spring 容器启动阶段直接失败，阻止错误配置上线 |

API Key 只允许通过环境变量或外部配置注入，例如 `${ZHIPU_API_KEY:}`，不得提交到代码仓库。

## 11. 调用流程

```text
业务模块创建 AiChatRequest
        ↓
AiChatClient 校验通用参数
        ↓
选择 ZhipuAiChatClient
        ↓
构造供应商请求与 Authorization Header
        ↓
执行 HTTP 请求
        ↓
解析 HTTP 状态和供应商响应
        ├─ 成功：返回 AiChatResponse
        └─ 失败：抛出 AiRuntimeException
```

## 12. 智谱适配器设计

### 12.1 请求映射

智谱适配器将统一请求映射为 OpenAI-compatible 消息结构：

```json
{
  "model": "glm-4-flash",
  "messages": [
    {"role": "system", "content": "..."},
    {"role": "user", "content": "..."}
  ],
  "stream": false,
  "temperature": 0.2
}
```

### 12.2 响应映射

适配器只负责提取供应商协议字段：

```text
choices[0].message.content -> AiChatResponse.content
model                      -> AiChatResponse.model
usage.prompt_tokens        -> inputTokens
usage.completion_tokens    -> outputTokens
usage.total_tokens         -> totalTokens
HTTP request id            -> requestId
```

适配器不得解析 `content` 中的 `genChart` 或 `genResult`。

## 13. 异常设计

### 13.1 统一错误码

| 错误码 | 场景 | 建议业务策略 |
|---|---|---|
| `AI_CONFIGURATION_ERROR` | 缺少 API Key、URL 或默认模型 | 不重试，直接失败 |
| `MODEL_NOT_ALLOWED` | 请求指定模型不在白名单 | 不重试，直接失败 |
| `AI_AUTHENTICATION_ERROR` | 401/403 | 不重试，检查配置 |
| `AI_RATE_LIMITED` | 429 | 可重试或转异步 |
| `AI_TIMEOUT` | 连接或读取超时 | 可重试或转异步 |
| `AI_UPSTREAM_UNAVAILABLE` | 供应商 5xx/服务过载 | 可重试或转异步 |
| `AI_RESPONSE_INVALID` | 外层 JSON 或 choices 缺失 | 通常不可重试 |
| `AI_NETWORK_ERROR` | DNS、连接拒绝等网络错误 | 可重试 |
| `AI_UNKNOWN_ERROR` | 未分类异常 | 谨慎重试并告警 |

### 13.2 异常对象

```java
public class AiRuntimeException extends RuntimeException {
    private AiRuntimeErrorCode errorCode;
    private boolean retryable;
    private Integer httpStatus;
    private String upstreamRequestId;
    private String sanitizedMessage;
    private String traceId;
}
```

字段约束：

- `errorCode`：必填，调用方据此区分错误类型；
- `retryable`：必填，由 Runtime 根据错误类型和 HTTP 状态码判断；
- `httpStatus`：可选，仅记录上游 HTTP 状态码，不把完整响应写入异常；
- `upstreamRequestId`：可选，用于向智谱客服或平台日志追踪请求；
- `sanitizedMessage`：可选，必须脱敏、截断，不能包含 API Key、Authorization、完整 Prompt 或完整响应；
- `traceId`：可选，与业务调用链关联。

Runtime 只声明异常是否具备重试条件，不直接执行分析任务级重试。异常应通过工厂方法或统一映射器创建，禁止业务层根据原始供应商异常自行判断重试。

建议的可重试规则：

| 错误 | `retryable` |
|---|---:|
| 配置错误、模型不在白名单、鉴权失败、响应格式错误 | `false` |
| 限流、连接超时、读取超时、网络错误、供应商 5xx | `true` |
| 未分类错误 | `false`，记录告警后人工判断 |

## 14. 日志与数据保护

允许记录：

- 模型名、供应商、耗时、状态码、请求 ID、Token 用量；
- Prompt 长度、响应长度；
- 经过脱敏和截断的错误摘要。

禁止记录：

- API Key 和 Authorization Header；
- 完整 Excel/CSV 内容；
- 完整用户 Prompt；
- 用户密码、Cookie、Session 和 Token；
- 未脱敏的供应商错误响应。

错误摘要建议最多保留 500 个字符；对于上游返回的 JSON，只允许提取 `code`、`message` 等非敏感字段，并再次执行关键字脱敏。

## 15. 测试设计

### 15.1 单元测试

| 测试编号 | 场景 | 对应需求 |
|---|---|---|
| AIR-UT-001 | 正常提取 assistant content | AIR-FR-004 |
| AIR-UT-002 | choices 为空 | AIR-FR-005 |
| AIR-UT-003 | content 为空 | AIR-FR-005 |
| AIR-UT-004 | 401 映射为鉴权错误 | AIR-FR-005 |
| AIR-UT-005 | 429 映射为限流且可重试 | AIR-FR-005 |
| AIR-UT-006 | 5xx 映射为上游不可用且可重试 | AIR-FR-005 |
| AIR-UT-007 | 异常摘要脱敏且截断 | AIR-FR-007 |
| AIR-UT-008 | API Key 不出现在异常、日志和响应中 | AIR-FR-007 |
| AIR-UT-005 | 429 映射为限流错误且可重试 | AIR-FR-005 |
| AIR-UT-006 | 5xx 映射为上游不可用 | AIR-FR-005 |
| AIR-UT-007 | 连接/读取超时映射 | AIR-FR-003、005 |
| AIR-UT-008 | 缺少 API Key | AIR-FR-007 |
| AIR-UT-009 | 日志和异常不包含 API Key | AIR-FR-007 |
| AIR-UT-010 | `bi.ai.*` 配置能够正确绑定和校验 | AIR-FR-009 |

### 15.2 模块集成测试

- 使用本地 Mock Server，不调用真实智谱服务。
- 校验请求 Header、Body 和超时设置。
- 校验供应商响应到统一响应的完整映射。
- 校验 `bi-ai-runtime` 不需要数据库、Redis 和 RabbitMQ 即可测试。

## 16. 验收标准

### AIR-AC-001 模块独立编译

执行：

```bash
mvn -pl modules/bi-ai-runtime -am test
```

模块及其上游依赖测试通过。

### AIR-AC-002 依赖边界

- `bi-ai-runtime/pom.xml` 不依赖任何业务模块。
- Runtime 公开接口中不存在图表、用户、文件和任务类型。

### AIR-AC-003 智谱成功调用

Mock 智谱返回正常 Chat Completions JSON 时，Runtime 返回统一的 `AiChatResponse`。

### AIR-AC-004 错误分类

401、429、5xx、超时和非法响应能够映射到不同错误码及正确的 `retryable` 值。

### AIR-AC-005 安全

测试日志、异常消息和 HTTP 错误摘要中均不包含完整 API Key。

### AIR-AC-006 业务解耦

智能分析 Prompt 和 `genChart/genResult` 解析代码不位于 `bi-ai-runtime`。

### AIR-AC-007 非流式完整性

- Runtime 仅在供应商返回完整响应后返回 `AiChatResponse`。
- 不返回中间 Token，不产生半截的 `genChart` 或 `genResult`。
- 供应商响应被截断或 `finish_reason=length` 时，必须保留完成原因，并由调用方按不完整结果处理。

## 17. 迁移方案

第一阶段：

- 在 `bi-ai-runtime` 建立统一接口、请求、响应、配置和异常。
- 将智谱 HTTP 调用从 `bi-infra` 迁入 Runtime。
- 保持现有配置项和同步分析接口可用。

第二阶段：

- `bi-module-analysis-biz` 改为依赖 `AiChatClient`。
- 将分析 Prompt 和图表响应解析留在分析模块。
- 删除旧 `AiManager` 的业务耦合实现。

第三阶段：

- 增加调用指标、供应商扩展和模型路由能力。

## 18. 需求—代码—测试追踪矩阵

| 需求 | 代码位置 | 测试位置 |
|---|---|---|
| AIR-FR-001/002 | `airuntime/api` | `AiChatClient` 契约测试 |
| AIR-FR-003/009 | `airuntime/config` | 配置绑定测试 |
| AIR-FR-004/006 | `airuntime/provider/zhipu` | 智谱响应映射测试 |
| AIR-FR-005 | `airuntime/exception`、`provider/zhipu` | HTTP/异常分类测试 |
| AIR-FR-007 | `airuntime/provider/zhipu` | 敏感信息测试 |
| AIR-FR-008 | `src/test` Mock Server | 模块集成测试 |
| AIR-FR-010 | `airuntime/api` | 多实现装配测试 |
| AIR-FR-011 | `airuntime/config` | local/test/prod 配置校验测试 |
| AIR-FR-012 | `airuntime/provider/zhipu` | 非流式完整响应测试 |

## 19. 已定稿设计决策

1. V1.0 默认模型由配置统一管理；请求可选覆盖，但必须经过模型白名单校验。分析业务默认不传模型。
2. API Key 按环境校验：`local` 允许启动、`test` 使用 Mock、`prod` 启动时强校验。
3. Runtime 只返回 Token 用量，不负责将用量写入数据库；调用方可用于日志、指标或任务记录。
4. Runtime 只负责供应商协议解析，不解析 `genChart`、`genResult` 等业务结果。
5. Runtime 公开接口不得依赖智谱 SDK、数据库、Redis、RabbitMQ、用户、文件和图表领域对象。
6. V1.0 只支持同步非流式调用；流式调用排入后续版本。
7. V1.0 使用 Java 17 `HttpClient`，实现封装在智谱 provider 内部。
8. V1.0 先提供结构化日志和基础调用信息；Micrometer 指标作为后续增强。
9. V1.0 只实现正式配置前缀 `bi.ai.*`，不预先开发旧配置兼容和迁移工具。
10. V1.0 保留 `provider` 配置字段，但只实现智谱供应商。

## 20. 剩余评审问题

当前无阻塞 V1.0 的待评审配置问题；真实兼容需求出现后再补充设计。

## 20. 变更记录

| 日期 | 版本 | 说明 |
|---|---|---|
| 2026-09-21 | V1.0-draft | 建立模块 PRD、技术设计、Maven/包结构映射和验收标准 |
| 2026-09-21 | V1.1-draft | 定稿模型白名单、异常契约、按环境配置校验和模块边界 |
