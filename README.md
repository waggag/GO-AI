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

## 压测建议

基于 k6 或 JMeter 对 `/api/chat/stream` 进行并发压测，建议配置：

- 并发用户：1000
- 断言：错误率 0%、平均响应 < 500ms、P99 < 1s

## 演示地址

- 线上演示地址：待部署后填入（建议 Vercel + Render / ECS）
