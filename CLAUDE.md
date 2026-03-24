# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

GO-AI is a multi-vendor AI conversation platform with a Vue 3 frontend and Spring Boot WebFlux backend. The platform supports multiple AI providers (OpenAI, Tongyi Qwen, Ernie Bot, Spark, Claude, Gemini) with unified API abstraction, streaming responses, session management, JWT authentication, and observability features.

## Common Development Commands

### Frontend (in `frontend/` directory)
- `npm run dev` - Start development server on port 5173
- `npm run build` - Production build (TypeScript + Vite)
- `npm run lint` - ESLint checks
- `npm run typecheck` - TypeScript type checking
- `npm run test` - Run Vitest tests once
- `npm run test -- --watch` - Vitest watch mode
- `npm run test -- --run src/path/to/file.test.ts` - Run single test file

### Backend (in `backend/` directory)
- `mvn spring-boot:run` - Run application on port 8080
- `mvn test` - Run all tests
- `mvn test -Dtest=ClassName` - Run single test class
- `mvn test -Dtest=ClassName#methodName` - Run single test method
- `mvn -B clean verify` - Full verification (tests + JaCoCo coverage)

### Docker
- `docker-compose up --build` - Full stack startup
- `docker-compose up --build -d` - Background startup
- `docker-compose logs -f backend` - View backend logs

## Architecture

### Backend Structure (`backend/src/main/java/com/goaiplatform/backend/`)
- **ai/** - AI provider clients interface and implementations (OpenAI, Xiaomi, Mock)
- **config/** - Application configuration, security, rate limiting properties
- **controller/** - REST controllers (Auth, Chat, API Key management)
- **domain/** - JPA entities (User, Conversation, Message, ApiKey)
- **dto/** - Request/response records for API contracts
- **repository/** - Spring Data R2DBC reactive repositories
- **security/** - JWT authentication, password encoding
- **service/** - Business logic services

### Frontend Structure (`frontend/src/`)
- **stores/** - Pinia state management (auth, chat)
- **components/** - Vue SFC components
- **api.ts** - Axios client with API functions
- **types.ts** - TypeScript interface definitions
- **utils.ts** - Utility functions

## Key Patterns and Constraints

### Backend (Java + Spring Boot WebFlux)
- **Reactive types**: Use `Mono<T>` for single elements, `Flux<T>` for streams
- **Dependency injection**: Constructor injection only, no `@Autowired` on fields
- **Error handling**: Use `ResponseStatusException` with specific HTTP status codes
- **DTOs**: Use Java records (e.g., `record LoginRequest(String username, String password)`)
- **Naming**: camelCase for fields, PascalCase for classes, suffixes: `*Entity`, `*Service`
- **Testing**: JUnit 5 + Mockito + Testcontainers, 80% line coverage required

### Frontend (Vue 3 + TypeScript)
- **Components**: Single-file components with `<script setup lang="ts">`
- **State management**: Pinia stores with `defineStore` pattern
- **API calls**: Use async/await with try/catch, wrap in client functions
- **Import order**: (1) Vue/Pinia, (2) local stores/api, (3) types
- **Testing**: Vitest + @vue/test-utils with mocked APIs

## Core Features

- **Authentication**: JWT tokens via `Authorization: Bearer <token>` header, stored in localStorage
- **Streaming**: SSE via `/api/chat/stream`, frontend uses TextDecoder with ReadableStream
- **LLM Providers**: Abstracted through `AiProviderClient` interface with vendor-specific implementations
- **Session Context**: Redis stores last 20 messages per conversation, key format: `conv:ctx:{id}`
- **Observability**: Micrometer + Zipkin + Prometheus + Grafana for monitoring

## Configuration Notes

- Database: MySQL on port 3307 (dev), R2DBC reactive driver
- Redis: Port 6380 for session context
- Services: Backend 8080, Frontend 5173 (dev) or 80 (production)
- OpenAPI: Available at `/swagger-ui.html`
- Prometheus: Port 9090, Grafana: Port 3000, Zipkin: Port 9411

## Constraints

- **TypeScript**: No `as any` or `@ts-ignore` allowed
- **Java**: No suppressing compiler errors
- **Backend**: JaCoCo requires 80% line coverage
- **Frontend**: Use unique fonts (no Inter, Roboto, etc.)
- **Theme**: Dark theme mandated by design requirements