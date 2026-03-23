import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import MarkdownMessage from './MarkdownMessage.vue'

describe('MarkdownMessage', () => {
  it('renders markdown heading and code', () => {
    const wrapper = mount(MarkdownMessage, {
      props: {
        content: '# 标题\n```ts\nconst a = 1\n```'
      }
    })
    expect(wrapper.html()).toContain('<h1>标题</h1>')
    expect(wrapper.html()).toContain('hljs')
  })
})
