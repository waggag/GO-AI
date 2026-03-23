import type { Provider, ThemeMode } from './types'

// ==================== 错误处理 ====================

/**
 * Axios 错误类型检查
 */
interface AxiosError {
  response?: {
    status?: number
    data?: {
      message?: string
    }
  }
}

/**
 * 检查是否为 Axios 错误
 */
function isAxiosError(error: unknown): error is AxiosError {
  return (
    typeof error === 'object' &&
    error !== null &&
    'response' in error
  )
}

/**
 * 提取 API 错误消息
 */
export function extractApiError(error: unknown, fallbackMessage?: string): string {
  if (isAxiosError(error) && typeof error.response?.data?.message === 'string') {
    return error.response.data.message
  }
  if (error instanceof Error) {
    return error.message
  }
  return fallbackMessage ?? ''
}

/**
 * 提取 HTTP 状态码
 */
export function extractStatusCode(error: unknown): number {
  if (isAxiosError(error) && typeof error.response?.status === 'number') {
    return error.response.status
  }
  return 0
}

/**
 * 检查是否为未授权错误 (401)
 */
export function isUnauthorized(error: unknown): boolean {
  return extractStatusCode(error) === 401
}

// ==================== localStorage 常量 ====================

export const STORAGE_KEYS = {
  TOKEN: 'goai_token',
  USER: 'goai_user',
  THEME: 'goai_theme',
  THEME_MODE: 'goai_theme_mode',
  API_KEYS: 'goai_api_keys'
} as const

export function isThemeMode(value: string | null): value is ThemeMode {
  return value === 'light' || value === 'dark' || value === 'system'
}

export function resolveTheme(mode: ThemeMode, systemPrefersDark: boolean): 'light' | 'dark' {
  if (mode === 'system') {
    return systemPrefersDark ? 'dark' : 'light'
  }

  return mode
}

// ==================== 配置常量 ====================

export interface ProviderConfig {
  name: string
  icon: string
  models: string[]
  baseUrl: string
}

export const PROVIDER_CONFIG: Record<Provider, ProviderConfig> = {
  OPENAI: {
    name: 'OpenAI',
    icon: '🟢',
    models: ['gpt-4o', 'gpt-4o-mini', 'gpt-3.5-turbo'],
    baseUrl: 'api.openai.com'
  },
  QWEN: {
    name: '通义千问',
    icon: '🔵',
    models: ['qwen-turbo', 'qwen-plus', 'qwen-max'],
    baseUrl: 'dashscope.aliyuncs.com'
  },
  ERNIE: {
    name: '文心一言',
    icon: '🟠',
    models: ['ERNIE-4.0-8K', 'ERNIE-3.5-8K', 'ERNIE-3.0-8K'],
    baseUrl: 'aip.baidubce.com'
  },
  SPARK: {
    name: '讯飞星火',
    icon: '🔴',
    models: ['Spark Pro', 'Spark V2.0', 'Spark Lite'],
    baseUrl: 'spark-api.xf-yun.com'
  },
  CLAUDE: {
    name: 'Claude',
    icon: '🟣',
    models: ['claude-3-opus', 'claude-3-sonnet', 'claude-3-haiku'],
    baseUrl: 'api.anthropic.com'
  },
  GEMINI: {
    name: 'Gemini',
    icon: '🟡',
    models: ['gemini-pro', 'gemini-pro-vision'],
    baseUrl: 'generativelanguage.googleapis.com'
  },
  XIAOMI: {
    name: '小米MiLM',
    icon: '🟤',
    models: ['MiLM-7B', 'MiLM-1.3B', 'MiLM-6B'],
    baseUrl: 'api.xiaoai.mi.com'
  }
}

// ==================== 限制常量 ====================

export const LIMITS = {
  /** 会话标题最大长度 */
  MAX_TITLE_LENGTH: 16,
  /** 上下文最大消息数 */
  MAX_CONTEXT_MESSAGES: 20
} as const
