# bi-module-chart-biz V1.0 产品需求与技术设计

## 1. 文档定位

`bi-module-chart-biz` 是智能 BI 系统的图表领域模块，负责图表数据、图表结果和图表生命周期管理，为分析任务、看板和报表提供稳定的图表业务能力。

本版本目标是完成图表领域独立化和可交付 MVP，不改变现有前端接口路径，不引入复杂领域框架。

## 2. 模块边界

### 2.1 本模块负责

- `Chart` 图表实体及持久化；
- 图表创建、编辑、详情、分页查询和逻辑删除；
- 图表所属用户和管理员权限校验所需的业务能力；
- 图表分析状态和分析结果的保存、更新；
- 为 `bi-module-analysis-biz` 提供图表应用服务接口；
- 图表 CRUD HTTP 接口。

### 2.2 本模块不负责

- 文件上传、文件解析和 CSV/XLSX 转换；
- Prompt 构造和大模型调用；
- AI 返回结果解析；
- 分析任务重试策略；
- RabbitMQ 消费和异步任务调度；
- 看板布局和报表编排。

## 3. 用户需求

普通用户可以创建、查看、分页查询、编辑和删除自己的图表，不得查看或修改其他用户的图表。管理员可以分页查看全部未删除图表，并在权限范围内管理图表，但不能绕过逻辑删除和状态约束。

## 4. 图表生命周期

分析模块使用统一的大写状态：

```text
WAITING    待执行
RUNNING    执行中
SUCCEEDED  执行成功
FAILED     执行失败
```

状态流转：

```text
创建       -> WAITING
开始执行   -> RUNNING
执行成功   -> SUCCEEDED
执行失败   -> FAILED
失败重试   -> WAITING -> RUNNING
```

运行中的图表不能删除；成功或失败图表可以逻辑删除；已删除图表默认不进入普通查询结果。

## 5. V1.0 接口需求

接口继续使用现有 `/api/chart` 前缀，保证前端兼容。

| 方法 | 路径 | 说明 | 归属 |
|---|---|---|---|
| POST | `/api/chart/add` | 手动创建图表 | chart-biz |
| POST | `/api/chart/delete` | 逻辑删除图表 | chart-biz |
| POST | `/api/chart/update` | 更新图表基础信息 | chart-biz |
| POST | `/api/chart/edit` | 编辑图表 | chart-biz |
| GET | `/api/chart/get` | 查询图表详情 | chart-biz |
| POST | `/api/chart/list/page` | 管理员分页查询 | chart-biz |
| POST | `/api/chart/my/list/page` | 当前用户分页查询 | chart-biz |
| POST | `/api/chart/gen` | 同步 AI 分析 | analysis-biz |
| POST | `/api/chart/gen/async` | 线程池异步 AI 分析 | analysis-biz |
| POST | `/api/chart/gen/async/mq` | MQ 异步 AI 分析 | analysis-biz |
| POST | `/api/chart/retry` | 失败分析重试 | analysis-biz |

### 5.1 ChartApplicationService

`chart-biz` 对外提供面向业务的 `ChartApplicationService`，至少包含：

- `createChart`：创建图表；
- `getChart`：按 ID 查询图表；
- `listMyCharts`：查询用户自己的图表；
- `listAllCharts`：管理员查询全部图表；
- `updateChart`：更新图表基础信息；
- `deleteChart`：执行逻辑删除和状态校验；
- `createAnalysisTask`：创建 WAITING 状态的分析图表；
- `markRunning`：更新为 RUNNING；
- `markSucceeded`：保存图表配置和分析结论；
- `markFailed`：保存失败状态和脱敏后的错误信息。

分析模块只能依赖这些业务接口，不直接调用 `ChartMapper`，不直接拼接 SQL。

## 6. 数据语义

| 字段 | 语义 |
|---|---|
| `id` | 图表唯一标识 |
| `name` | 图表名称 |
| `goal` | 分析目标 |
| `chartData` | 转换后的结构化数据文本或任务输入摘要 |
| `chartType` | 用户指定或模型生成的图表类型 |
| `genChart` | ECharts 配置 JSON 字符串 |
| `genResult` | AI 生成的业务分析结论 |
| `status` | 图表/分析任务状态 |
| `execMessage` | 失败原因或执行说明，需脱敏并限制长度 |
| `userId` | 所属用户 |
| `isDelete` | 逻辑删除标识 |
| `createTime` / `updateTime` | 审计时间 |

## 7. 技术设计

```text
bi-module-chart-biz
└── com.yupi.springbootinit.modules.chart
    ├── controller
    │   └── ChartController
    ├── application
    │   └── ChartApplicationService
    ├── domain
    │   └── ChartStatus
    └── service
        ├── ChartService
        └── ChartServiceImpl
```

数据库 Mapper 和 XML 继续由 `bi-infra` 承载，迁移阶段可以先保留现有 Mapper 包路径，避免同时改变数据库基础设施。

依赖方向：

```text
bi-module-analysis-biz -> bi-module-chart-biz
bi-module-dashboard-biz -> bi-module-chart-biz
bi-module-report-biz -> bi-module-chart-biz
bi-module-chart-biz -> bi-infra -> bi-common
```

禁止 `chart-biz` 依赖 `analysis-biz`、`AiChatClient`；禁止 `analysis-biz` 直接依赖 `ChartMapper`。

## 8. Controller 拆分与迁移

现有 `ChartController` 拆为：

- chart-biz：图表 CRUD、查询和权限相关接口；
- analysis-biz：AI 分析、异步分析和重试接口。

Java Controller 类可以变化，但 HTTP 路径必须保持兼容。迁移期间不能同时注册同一路径的两个 Controller。

迁移分三阶段：

1. 建立 chart-biz 骨架和应用服务接口，补充单元测试；
2. 迁移 `Chart`、Service 和持久化依赖，改造 analysis-biz 调用；
3. 拆分 HTTP Controller，补充 bi-web Spring 集成测试。

## 9. 异常和权限边界

- 图表不存在：返回资源不存在错误；
- 非所属用户操作：返回无权限错误；
- 非法 ID、空名称、超长字段：返回参数错误；
- 删除 RUNNING 任务：拒绝操作；
- 重复删除：保持幂等或返回明确业务结果；
- 所有查询默认过滤 `isDelete = 0`；
- 管理员能力必须显式判断；
- `execMessage` 不保存 API Key、Authorization、Bearer Token 等敏感信息。

## 10. 测试和验收标准

模块测试必须覆盖：

- 状态转换；
- 用户归属和管理员权限；
- 运行中任务不可删除；
- 逻辑删除过滤；
- 图表结果和失败信息更新；
- analysis-biz 调用 ChartApplicationService 的交互。

Spring 集成测试必须验证：

- 图表 CRUD 接口权限；
- 分析任务创建后为 WAITING；
- AI 成功后为 SUCCEEDED；
- AI 失败后为 FAILED；
- 用户只能查询自己的图表；
- 真实智谱 API 测试默认关闭，仅通过系统属性显式开启。

MVP 完成标准：

1. chart-biz 可以独立编译并通过模块测试；
2. analysis-biz 不再直接依赖 ChartMapper；
3. 图表 CRUD 与 AI 分析职责分离；
4. 原有 `/api/chart/**` 路径不变；
5. 成功、失败、重试状态正确落库；
6. dashboard、report 后续可以只依赖 chart-biz 获取图表能力。

## 11. 非本版本范围

- 图表版本管理；
- 图表收藏、分享和公开链接；
- 看板布局；
- 报表导出；
- SQL Agent；
- 多图表联动；
- 图表缓存和搜索索引优化。
