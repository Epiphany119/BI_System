# 智能 BI 平台 V1.0

## `bi-module-analysis-biz` 分模块 PRD

| 项目 | 内容 |
|---|---|
| Maven 模块 | `modules/bi-module-analysis-biz` |
| ArtifactId | `bi-module-analysis-biz` |
| 模块定位 | 智能分析任务业务编排 |
| 版本 | V1.0-draft |
| 上游依赖 | `bi-common`、`bi-infra`、`bi-ai-runtime`、`bi-module-user-biz` |
| 文档状态 | 初稿，待评审 |

## 1. 需求分析

本模块承接用户发起的“上传结构化文件并生成分析结果”流程，负责把文件、分析目标和图表类型组织为一次完整的分析任务。模块必须调用统一的 `AiChatClient`，不得直接依赖智谱 SDK 或供应商 HTTP 协议。

### 1.1 用户目标

- 上传 CSV/XLSX 文件并填写分析目标；
- 获得图表配置和文字分析结论；
- 查看任务状态和失败原因；
- 对失败任务进行手动重试；
- 只能访问本人创建的任务和结果。

### 1.2 V1.0 范围

- 以同步分析为主；
- 保留异步 MQ 扩展点，但不在本模块首轮实现复杂自动重试；
- 继续使用现有 `chart` 表保存分析结果；
- 负责 CSV/XLSX 内容读取和 Prompt 数据文本转换；
- 负责 `genChart`、`genResult` 业务结构解析和校验；
- 失败任务明确记录为 `FAILED`。

### 1.3 非目标

- 不实现 AI 供应商调用、API Key 和 HTTP 协议；
- 不实现登录、注册和用户认证；
- 不实现 COS/Redis/RabbitMQ 底层连接；
- 不实现多图表仪表盘和复杂数据清洗；
- 不实现流式 Token 展示；
- 不实现复杂自动重试策略。

## 2. 模块边界

| 能力 | 归属 |
|---|---|
| 分析任务参数校验、流程编排、结果落库 | `bi-module-analysis-biz` |
| Excel/CSV 读取和分析文本转换 | `bi-module-analysis-biz`，底层库可由 `bi-infra` 提供 |
| AI 调用、模型选择、供应商错误映射 | `bi-ai-runtime` |
| 文件上传、对象存储 | `bi-module-file-biz` / `bi-infra` |
| 用户身份和归属校验 | `bi-module-user-biz` / 公共认证能力 |
| 消息队列、Redis、数据库连接 | `bi-infra` |
| 前端图表渲染 | 前端应用 |

核心边界：

```text
analysis-biz 负责“分析什么、结果是否符合业务要求、任务如何结束”
ai-runtime   负责“如何调用模型、上游返回了什么文本”
```

## 3. 核心流程

```text
Controller
   ↓
AnalysisApplicationService
   ├─ 校验登录用户和文件
   ├─ 读取 CSV/XLSX 并转换为文本
   ├─ 组装分析 Prompt
   ├─ AiChatClient.chat(request)
   ├─ 解析 genChart / genResult
   ├─ 校验结果完整性
   └─ 保存 chart 并返回结果
```

V1.0 首轮默认同步执行。后续保留 `BiMessageConsumer` 作为异步入口时，消费者必须复用同一个应用服务，不能复制一套 Prompt 和解析逻辑。

## 4. 任务状态

```text
WAITING  → RUNNING → SUCCEEDED
                  └→ FAILED
```

- 创建任务后进入 `WAITING`；
- 开始调用 AI 前进入 `RUNNING`；
- 业务结果解析和保存成功后进入 `SUCCEEDED`；
- 参数、文件、AI 调用或业务结果解析失败后进入 `FAILED`；
- V1.0 暂不增加复杂 `RETRYING` 状态，手动重试重新执行任务并覆盖失败信息。

## 5. 业务输入与输出

### 5.1 输入

- 文件：CSV 或 XLSX，大小和行数受统一配置限制；
- `name`：图表名称；
- `goal`：分析目标，必填；
- `chartType`：期望图表类型，可选；
- 当前登录用户 ID：由认证上下文获取，不允许由前端任意指定。

### 5.2 业务结果

模型文本必须最终解析为：

```json
{
  "genChart": {},
  "genResult": "分析结论"
}
```

解析器应兼容历史 Markdown 代码块和旧分隔符，但统一输出干净的图表 JSON 与结论字符串。缺少任一字段、JSON 无法解析或图表配置为空时，任务失败并记录脱敏原因。

## 6. AI 调用契约

```java
AiChatRequest request = new AiChatRequest();
request.setSystemPrompt(analysisSystemPrompt);
request.setUserMessage(userInput);
request.setTraceId(taskId.toString());
AiChatResponse response = aiChatClient.chat(request);
String modelContent = response.getContent();
```

本模块 V1.0 不主动设置 `model`，使用 Runtime 配置的默认模型；不读取智谱 DTO，不处理 HTTP 状态码。

## 7. 失败处理

| 场景 | 任务结果 | 处理 |
|---|---|---|
| 文件格式或参数错误 | 不创建成功任务 | 直接返回参数错误 |
| API Key/模型配置错误 | `FAILED` | 记录统一错误码，提示配置问题 |
| AI 限流、超时、网络异常 | `FAILED` | 保留 `retryable` 信息，V1.0 不自动无限重试 |
| AI 返回格式错误 | `FAILED` | 保存脱敏错误摘要，不保存完整模型响应 |
| 数据库保存失败 | `FAILED` 或事务回滚 | 记录日志并告警 |

## 8. 包结构建议

```text
com.yupi.springbootinit.analysis
├── controller
├── application
│   └── AnalysisTaskApplicationService
├── service
├── parser
│   └── AnalysisResultParser
├── prompt
│   └── AnalysisPromptBuilder
├── converter
│   └── SpreadsheetDataConverter
├── model
├── mapper
└── bizmq
```

现有 Controller、消费者和解析器可渐进迁入上述职责，不要求首轮一次性重命名全部历史类。

## 9. 测试要求

- 参数和文件格式校验测试；
- CSV/XLSX 转文本测试；
- Prompt 组装测试；
- 合法 `genChart/genResult` 解析测试；
- Markdown、旧分隔符和非法 JSON 解析测试；
- `AiChatClient` Mock 调用测试；
- AI Runtime 异常到 `FAILED` 的状态测试；
- 用户归属校验测试；
- 不访问真实智谱 API 的模块单元测试。

真实 API 调用属于 `bi-web` Spring 集成测试，不放入本模块单元测试。

## 10. 验收标准

- 模块可以通过 `mvn -pl modules/bi-module-analysis-biz -am test`；
- 业务代码只依赖 `AiChatClient`，不出现智谱 HTTP 调用；
- 同步分析成功时可保存并返回图表配置和结论；
- 任意失败路径都有明确任务状态和错误信息；
- 解析器不把供应商协议细节泄露到业务接口；
- 模块测试不需要 API Key、Redis、RabbitMQ 或真实数据库。

## 11. 版本演进

- V1.0：同步分析、统一 Runtime 调用、结果解析、失败落库；
- 后续版本：异步队列完整接入、自动重试、任务取消、结果版本和流式展示。
