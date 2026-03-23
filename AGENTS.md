# AGENTS.md — GO-AI 多厂商对话平台

多厂商 AI 对话平台：Vue 3 前端 + Spring Boot WebFlux 后端。

## 构建 / 运行 / 测试命令

### 前端（在 `frontend/` 目录）

| 命令 | 说明 |
|------|------|
| `npm run dev` | 开发服务器（localhost:4173） |
| `npm run build` | 生产构建（vue-tsc + vite） |
| `npm run lint` | ESLint 检查（flat config） |
| `npm run typecheck` | TypeScript 类型检查 |
| `npm run test` | Vitest 单次运行 |
| `npm run test -- --watch` | Vitest 监听模式 |
| `npm run test -- --run src/path/to/file.test.ts` | 单个测试文件 |

### 后端（在 `backend/` 目录）

| 命令 | 说明 |
|------|------|
| `mvn spring-boot:run` | 本地运行（端口 8080） |
| `mvn test` | 运行全部测试 |
| `mvn test -Dtest=SensitiveWordServiceTest` | 单个测试类 |
| `mvn test -Dtest=SensitiveWordServiceTest#sanitizeShouldMaskSensitiveWords` | 单个测试方法 |
| `mvn -B clean verify` | 完整验证（测试 + JaCoCo） |

### Docker

```bash
docker-compose up --build          # 全栈启动
docker-compose up --build -d       # 后台启动
docker-compose ps / docker ps      # 查看状态
docker-compose logs -f backend     # 查看日志
```

阿里云镜像失败时，设置环境变量（见 README.md）使用 `m.daocloud.io`。

## 代码规范

### 前端 — Vue 3 + TypeScript

**导入顺序**：按（1）Vue/Pinia、（2）本地 stores/api、（3）类型 分组。`src/` 内使用相对路径。

**组件**：单文件 Vue（SFC），使用 `<script setup lang="ts">`。`vue/multi-word-component-names` 规则已关闭。

**状态管理**：Pinia `defineStore` 模式，包含 `state` 和 `actions`。在 actions 中请求数据，存储在 state 中。

**命名规范**：变量/函数用 `camelCase`，类型/接口/组件用 `PascalCase`。Store 名称：`useXxxStore`。

**异步处理**：始终使用 `async/await`。API 调用包裹在 try/catch 中，面向用户显示错误消息。

**测试**：使用 Vitest + `@vue/test-utils`。Mock API 用 `vi.hoisted` + `vi.mock`。使用 `describe/it` 代码块。

```typescript
// ✅ 正确
const { data } = await client.post('/auth/login', { username, password })

// ❌ 错误
const resp = await fetch(url, {method: 'POST', body: JSON.stringify(body)})
```

### 后端 — Java 17 + Spring Boot

**导入顺序**：按（1）Java 标准库、（2）Spring、（3）Reactor、（4）项目代码 分组。禁止通配符导入。

**依赖注入**：仅使用构造函数注入。禁止在字段上使用 `@Autowired`。

**响应式类型**：`Mono<T>` 用于单个元素，`Flux<T>` 用于流。使用 `switchIfEmpty` + `Mono.error` 处理 404/403。

**错误处理**：抛出 `ResponseStatusException` 并指定 HTTP 状态码和消息。禁止使用通用异常。

**记录类型**：使用 Java record 作为 DTO（`record LoginRequest(String username, String password) {}`）。

**命名规范**：字段用 `camelCase`，类用 `PascalCase`。实体后缀：`UserEntity`。服务后缀：`AuthService`。

**测试**：JUnit 5 + Mockito + Testcontainers。类命名：`ServiceNameTest`。测试类不加 `public` 修饰符。

```java
// ✅ 正确
public Mono<AuthResponse> login(LoginRequest req) {
    return userRepository.findByUsername(req.username())
        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误")))
        .flatMap(user -> ...);
}

// ❌ 错误
@Autowired private UserRepository repo;
public AuthResponse login(String username) { ... }
```

## 项目结构

```
backend/
  src/main/java/com/goaiplatform/backend/
    controller/   # REST 控制器（AuthController, ChatController）
    service/      # 业务逻辑服务
    domain/       # 实体类（UserEntity, ConversationEntity）
    dto/          # 请求/响应 Java record
    repository/   # Spring Data R2DBC 接口
    security/     # JWT、密码编码器
    config/       # AppProperties、WebSecurity
  src/test/java/  # 单元测试 + 集成测试

frontend/
  src/
    stores/       # Pinia 状态管理（auth.ts, chat.ts）
    api.ts        # Axios 客户端 + API 函数
    components/   # Vue SFC 组件
    types.ts      # TypeScript 接口定义
```

## 核心模式

- **认证**：JWT 令牌通过 `Authorization: Bearer <token>` 头传递，存储在 `localStorage` 中。
- **流式输出**：通过 `/api/chat/stream` 实现 SSE。前端使用 `TextDecoder` 读取 `ReadableStream`。
- **LLM 供应商**：通过 `AiProviderClient` 接口抽象，每种供应商一个实现（OpenAI、通义千问等）。
- **Redis**：用于会话上下文（最近 20 条消息）。Key 格式：`conv:ctx:{id}`。

## 约束条件

- TypeScript 中禁止使用 `as any` 或 `@ts-ignore`。
- Java 中禁止抑制编译错误。
- 后端 JaCoCo 强制 80% 行覆盖率。
- 前端：禁止使用 `Inter`、`Roboto` 等通用字体，使用独特字体。
