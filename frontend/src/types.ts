export type Provider = 'OPENAI' | 'QWEN' | 'ERNIE' | 'SPARK' | 'CLAUDE' | 'GEMINI' | 'XIAOMI'

export type ThemeMode = 'light' | 'dark' | 'system'

export interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
}

export interface Conversation {
  id: number
  title: string
  createdAt: string
}

export interface StreamPayload {
  provider: Provider
  model: string
  prompt: string
  temperature: number
  topP: number
  maxTokens: number
}

export interface ProviderInfo {
  name: string
  icon: string
  models: string[]
  baseUrl: string
}

// ==================== API 响应类型 ====================

export interface AuthResponse {
  token: string
  username: string
  role: string
}

export interface ConversationDetail {
  id: number
  title: string
  messages: ChatMessage[]
}

export interface MessageResponse {
  id: number
  role: 'user' | 'assistant'
  content: string
  model: string
  createdAt: string
}

export interface ConversationExportResponse {
  id: number
  title: string
  exportedAt: string
  messages: MessageResponse[]
}

export interface UsageStatsResponse {
  conversationCount: number
  messageCount: number
  userMessageCount: number
  assistantMessageCount: number
  totalCharacters: number
  queuedRequests: number
  windowStart: string
  windowEnd: string
}

export interface UsageTrendPoint {
  date: string
  userMessages: number
  assistantMessages: number
  totalCharacters: number
}

export interface UsageTrendResponse {
  windowStart: string
  windowEnd: string
  points: UsageTrendPoint[]
}

// ==================== SSE 流式响应类型 ====================

export interface ChatChunkResponse {
  conversationId: number
  delta: string
  done: boolean
}
