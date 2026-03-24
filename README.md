# GO-AI 多厂商对话平台

前后端分离的多厂商 AI 对话平台，支持统一模型调用、流式输出、会话管理、权限控制与可观测性。

## 技术栈

- 前端：Vue 3 + TypeScript + Vite + Pinia + Vue I18n + Markdown-it + highlight.js
- 后端：Java 17 + Spring Boot 3 + Spring WebFlux + Spring Security + JWT + Spring AI
- 存储：MySQL（会话持久化）+ Redis（上下文记忆）
- 治理：Bucket4j（限流）+ Micrometer + Zipkin + Prometheus + Grafana
- 测试：Vitest + JUnit 5 + Testcontainers
- 文档：OpenAPI 3（SpringDoc）
- 交付：Docker + docker-compose + GitHub Actions

## 功能清单

- 多厂商模型抽象：OpenAI、通义千问、文心一言、星火、Claude、Gemini
- 动态参数：model、temperature、top-p、max_tokens
- SSE 流式输出与多轮上下文记忆
- JWT 用户认证与权限控制
- 会话管理、主题切换、国际化、Markdown 渲染与代码高亮
- 敏感词过滤与请求限流
- API Key 安全加密存储与指纹校验
- 对话使用统计与趋势分析
- 会话导出（JSON / Markdown 格式）

## 项目结构

```
GO-AI/
├── backend/                    # Spring Boot WebFlux 后端
│   └── src/
│       ├── main/java/
│       │   └── com/goaiplatform/backend/
│       │       ├── controller/    # REST 控制器（Auth, Chat, ApiKey）
│       │       ├── service/       # 业务逻辑（9个服务类）
│       │       ├── domain/        # 实体类（User, Conversation, Message, ApiKey）
│       │       ├── dto/           # 请求/响应 DTO（record 风格）
│       │       ├── repository/    # Spring Data R2DBC 接口
│       │       ├── security/      # JWT 认证相关
│       │       ├── ai/            # AI 厂商客户端接口与实现
│       │       └── config/        # 配置类
│       ├── test/java/             # 单元测试（6个测试类）
│       └── main/resources/application.yml
│
└── frontend/                   # Vue 3 前端
    └── src/
        ├── stores/               # Pinia 状态管理（auth, chat）
        ├── components/           # Vue SFC 组件（AuthScreen, MarkdownMessage）
        ├── api.ts                # Axios API 客户端
        ├── types.ts              # TypeScript 类型定义
        ├── utils.ts              # 工具函数
        ├── styles.css            # 全局样式系统
        ├── App.vue               # 主应用组件
        └── main.ts               # 入口文件
```

## API 端点

### 认证（Auth）
- `POST /api/auth/register` — 用户注册
- `POST /api/auth/login` — 用户登录

### 对话（Chat）
- `POST /api/chat/conversations` — 创建会话
- `GET /api/chat/conversations` — 会话列表
- `GET /api/chat/conversations/{id}` — 会话详情
- `GET /api/chat/conversations/{id}/export` — 导出 JSON
- `GET /api/chat/conversations/{id}/export.md` — 导出 Markdown
- `GET /api/chat/stats` — 使用统计
- `GET /api/chat/stats/trend` — 使用趋势
- `POST /api/chat/stream` — SSE 流式对话

### API Key 管理
- `POST /api/api-keys/save` — 保存 API Key
- `GET /api/api-keys/list` — Key 列表
- `DELETE /api/api-keys/{id}` — 删除 Key
- `POST /api/api-keys/validate/{provider}` — 校验 Key
- `POST /api/api-keys/rotate` — 轮换 Key

## 本地启动

### 1）仅开发模式

```bash
cd backend
mvn spring-boot:run
```

```bash
cd frontend
npm install
npm run dev
```

前端默认端口为 `5173`，访问地址：

- http://localhost:5173

### 2）一键容器启动

```bash
docker-compose up --build
```

如果出现 `403 Forbidden` 且日志包含 `mirror.aliyuncs.com`，可在当前终端先覆盖镜像来源再启动：

```bash
$env:MYSQL_IMAGE="m.daocloud.io/docker.io/library/mysql:8.0.39"
$env:REDIS_IMAGE="m.daocloud.io/docker.io/library/redis:7.4-alpine"
$env:ZIPKIN_IMAGE="m.daocloud.io/docker.io/openzipkin/zipkin:3.4"
$env:PROMETHEUS_IMAGE="m.daocloud.io/docker.io/prom/prometheus:v2.54.1"
$env:GRAFANA_IMAGE="m.daocloud.io/docker.io/grafana/grafana:11.2.0"
$env:BACKEND_BUILDER_IMAGE="m.daocloud.io/docker.io/library/maven:3.9.9-eclipse-temurin-17"
$env:BACKEND_RUNTIME_IMAGE="m.daocloud.io/docker.io/library/eclipse-temurin:17-jre"
$env:FRONTEND_NODE_IMAGE="m.daocloud.io/docker.io/library/node:22-alpine"
$env:FRONTEND_NGINX_IMAGE="m.daocloud.io/docker.io/library/nginx:1.27-alpine"
docker-compose up --build
```

服务访问：

- 前端：http://localhost
- 后端：http://localhost:8080
- OpenAPI：http://localhost:8080/swagger-ui.html
- Prometheus：http://localhost:9090
- Grafana：http://localhost:3000
- Zipkin：http://localhost:9411

## 质量校验命令

前端：

```bash
cd frontend
npm run lint
npm run typecheck
npm run test
```

后端：

```bash
cd backend
mvn -B clean verify
```

## 已知问题

1. **AI 厂商实现不完整**：当前仅实现 OpenAI 和小米 MiLM，其他厂商（通义千问、文心一言、星火、Claude、Gemini）为占位符
2. **用户 ID 提取逻辑不一致**：ApiKeyController 使用 hashCode() 生成用户 ID，与 AuthService 的真实用户 ID 不一致
3. **敏感词测试编码问题**：SensitiveWordServiceTest 在某些环境下编码导致断言失败
4. **Testcontainers 依赖 Docker**：部分集成测试需要 Docker 环境

## 压测建议

基于 k6 或 JMeter 对 `/api/chat/stream` 进行并发压测，建议配置：

- 并发用户：1000
- 断言：错误率 0%、平均响应 < 500ms、P99 < 1s

## 演示地址

- 线上演示地址：待部署后填入（建议 Vercel + Render / ECS）

## 贡献指南

1. Fork 仓库
2. 创建功能分支：`git checkout -b feature/your-feature`
3. 提交更改：`git commit -m 'Add your feature'`
4. 推送分支：`git push origin feature/your-feature`
5. 创建 Pull Request

## 许可证

[待补充]
