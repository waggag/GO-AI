<script setup lang="ts">
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import { computed } from 'vue'

const props = defineProps<{
  content: string
}>()

function escapeHtml(raw: string): string {
  return raw.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
}

const md: MarkdownIt = new MarkdownIt({
  highlight(str: string, lang: string): string {
    if (lang && hljs.getLanguage(lang)) {
      return `<pre><code class="hljs language-${lang}">${hljs.highlight(str, { language: lang }).value}</code></pre>`
    }
    return `<pre><code class="hljs">${escapeHtml(str)}</code></pre>`
  }
})

const html = computed(() => md.render(props.content))
</script>

<template>
  <div class="markdown-body" v-html="html"></div>
</template>

<style scoped>
.markdown-body {
  font-family: var(--font-body);
  color: var(--text-primary);
  line-height: 1.7;
}

.markdown-body h1,
.markdown-body h2,
.markdown-body h3,
.markdown-body h4,
.markdown-body h5,
.markdown-body h6 {
  font-family: var(--font-display);
  margin-top: 24px;
  margin-bottom: 12px;
  font-weight: 600;
  line-height: 1.3;
  color: var(--text-primary);
}

.markdown-body h1 {
  font-size: 1.75em;
  border-bottom: 1px solid var(--border-subtle);
  padding-bottom: 8px;
}

.markdown-body h2 {
  font-size: 1.4em;
}

.markdown-body h3 {
  font-size: 1.2em;
}

.markdown-body h4 {
  font-size: 1.1em;
}

.markdown-body p {
  margin-bottom: 14px;
}

.markdown-body ul,
.markdown-body ol {
  padding-left: 24px;
  margin: 10px 0;
}

.markdown-body li {
  margin: 6px 0;
}

.markdown-body blockquote {
  border-left: 3px solid var(--accent-primary);
  padding: 12px 16px;
  margin: 12px 0;
  color: var(--text-secondary);
  background: var(--bg-secondary);
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
}

.markdown-body a {
  color: var(--accent-secondary);
  text-decoration: none;
  border-bottom: 1px solid transparent;
  transition: border-color var(--transition-fast);
}

.markdown-body a:hover {
  border-bottom-color: var(--accent-secondary);
}

.markdown-body code {
  background: var(--bg-tertiary);
  padding: 2px 8px;
  border-radius: var(--radius-xs);
  font-family: var(--font-mono);
  font-size: 0.88em;
  color: var(--accent-secondary);
}

.markdown-body pre {
  background: var(--bg-primary);
  padding: 16px;
  border-radius: var(--radius-md);
  overflow-x: auto;
  margin: 14px 0;
  border: 1px solid var(--border-subtle);
}

.markdown-body pre code {
  background: transparent;
  padding: 0;
  font-size: 0.85em;
  color: var(--text-primary);
}

.markdown-body table {
  border-collapse: collapse;
  width: 100%;
  margin: 14px 0;
}

.markdown-body th,
.markdown-body td {
  border: 1px solid var(--border-normal);
  padding: 10px 14px;
  text-align: left;
}

.markdown-body th {
  background: var(--bg-secondary);
  font-weight: 600;
  color: var(--text-primary);
}

.markdown-body td {
  color: var(--text-secondary);
}

.markdown-body hr {
  border: none;
  height: 1px;
  background: var(--border-normal);
  margin: 24px 0;
}

.markdown-body img {
  max-width: 100%;
  border-radius: var(--radius-md);
  margin: 12px 0;
}
</style>
