import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

const createConversationMock = vi.hoisted(() => vi.fn())
const listConversationsMock = vi.hoisted(() => vi.fn())
const loadConversationMock = vi.hoisted(() => vi.fn())
const streamChatMock = vi.hoisted(() => vi.fn())

vi.mock('../api', () => ({
  createConversation: createConversationMock,
  listConversations: listConversationsMock,
  loadConversation: loadConversationMock,
  streamChat: streamChatMock
}))

describe('chat store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('init should load first conversation', async () => {
    const { useChatStore } = await import('./chat')
    listConversationsMock.mockResolvedValue([{ id: 1, title: '会话1', createdAt: '2026-01-01' }])
    loadConversationMock.mockResolvedValue({ id: 1, title: '会话1', messages: [{ role: 'assistant', content: 'hi' }] })
    const store = useChatStore()

    await store.init()

    expect(store.activeConversationId).toBe(1)
    expect(store.messages[0].content).toBe('hi')
  })

  it('init should keep empty state when no conversations', async () => {
    const { useChatStore } = await import('./chat')
    listConversationsMock.mockResolvedValue([])
    const store = useChatStore()

    await store.init()

    expect(store.activeConversationId).toBe(0)
    expect(store.messages).toHaveLength(0)
  })

  it('sendMessage should append assistant stream', async () => {
    const { useChatStore } = await import('./chat')
    createConversationMock.mockResolvedValue({ id: 2, title: '新会话', createdAt: '2026-01-01' })
    streamChatMock.mockImplementation(async (_id: number, _payload: unknown, onChunk: (content: string, done: boolean) => void) => {
      onChunk('hel', false)
      onChunk('lo', true)
    })
    const store = useChatStore()

    await store.sendMessage('hello', {
      provider: 'OPENAI',
      model: 'gpt-4o-mini',
      prompt: 'hello',
      temperature: 0.7,
      topP: 0.9,
      maxTokens: 100
    })

    expect(store.messages[0].role).toBe('user')
    expect(store.messages[1].content).toBe('hello')
    expect(store.loading).toBe(false)
  })

  it('sendMessage should queue when loading and skip processing', async () => {
    const { useChatStore } = await import('./chat')
    const store = useChatStore()
    store.loading = true

    await store.sendMessage('queued', {
      provider: 'OPENAI',
      model: 'gpt-4o-mini',
      prompt: 'queued',
      temperature: 0.7,
      topP: 0.9,
      maxTokens: 100
    })

    expect(store.queue).toHaveLength(1)
    expect(streamChatMock).not.toHaveBeenCalled()
  })
})
