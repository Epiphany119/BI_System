# Smart BI 多模块工程

本目录是智能 BI 的渐进式 Maven 多模块工程。当前阶段与旧版单体后端并行存在，先验证模块边界和依赖方向，再逐步迁移业务代码。

## 模块地图

```text
bi-web
├── bi-common
├── bi-infra
├── bi-ai-runtime
├── bi-module-user-biz
├── bi-module-auth-biz
├── bi-module-analysis-biz
├── bi-module-chart-biz
├── bi-module-file-biz
├── bi-module-dashboard-biz
└── bi-module-report-biz
```

## 当前阶段

- `bi-module-analysis-biz` 已包含任务路由和状态机领域测试。
- 其他模块先完成 Maven 边界和依赖占位，暂不复制旧业务代码。
- `bi-web` 作为最终启动模块，后续迁移 Spring Boot 启动类和接口层。

## 构建

```bash
cd modules
mvn test
```

迁移期间，旧后端仍从上一级 `pom.xml` 构建；模块工程通过测试后再切换为最终父工程。
