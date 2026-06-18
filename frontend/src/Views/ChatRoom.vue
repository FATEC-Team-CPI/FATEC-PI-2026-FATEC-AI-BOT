<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import MarkdownIt from 'markdown-it'
import DOMPurify from 'dompurify'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'

type ChatMessage = {
    id: number
    role: 'user' | 'bot'
    text: string
}

const inputText = ref('')
const connectionState = ref<'connecting' | 'connected' | 'reconnecting' | 'disconnected'>('connecting')
const isAwaitingResponse = ref(false)
const messages = ref<ChatMessage[]>([
    {
        id: 1,
        role: 'bot',
        text: 'Olá! Sou o Fateco. Como posso te ajudar hoje?',
    },
])

const messageAreaRef = ref<HTMLElement | null>(null)

let nextMessageId = 2
let socket: WebSocket | null = null
let reconnectTimer: ReturnType<typeof setTimeout> | null = null
let reconnectAttempts = 0
let isUnmounted = false
const pendingMessages: string[] = []

const sessionStorageKey = 'fateco_chat_session_id'

function generateSessionId(): string {
    return `web-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
}

function getSessionId(): string {
    const existing = localStorage.getItem(sessionStorageKey)
    if (existing) return existing

    const created = generateSessionId()
    localStorage.setItem(sessionStorageKey, created)
    return created
}

const sessionId = getSessionId()

function logWs(event: string, details?: Record<string, unknown>): void {
    const prefix = `[Fateco WS][${sessionId}] ${event}`
    if (details && Object.keys(details).length > 0) {
        console.info(prefix, details)
        return
    }
    console.info(prefix)
}

function getWebSocketBaseUrl(): string {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    return `${protocol}//${window.location.host}`
}

function buildWebSocketUrl(): string {
    return `${getWebSocketBaseUrl()}/ws/chat/${encodeURIComponent(sessionId)}`
}

function scrollToBottom(): void {
    nextTick(() => {
        if (messageAreaRef.value) {
            messageAreaRef.value.scrollTop = messageAreaRef.value.scrollHeight
        }
    })
}

function clearReconnectTimer(): void {
    if (reconnectTimer) {
        console.info(`[Fateco WS][${sessionId}] clearing reconnect timer`)
        clearTimeout(reconnectTimer)
        reconnectTimer = null
    }
}

function scheduleReconnect(): void {
    if (isUnmounted) return

    clearReconnectTimer()
    connectionState.value = reconnectAttempts === 0 ? 'disconnected' : 'reconnecting'

    const delay = Math.min(15000, 1000 * (reconnectAttempts + 1))
    logWs('schedule reconnect', { attempt: reconnectAttempts + 1, delayMs: delay })
    reconnectAttempts += 1

    reconnectTimer = setTimeout(() => {
        logWs('reconnect timer fired')
        connectWebSocket()
    }, delay)
}

function pushMessage(role: 'user' | 'bot', text: string): void {
    const trimmed = text.trim()
    if (!trimmed) return

    messages.value.push({
        id: nextMessageId++,
        role,
        text: trimmed,
    })
    scrollToBottom()
}

const md = new MarkdownIt({
    html: true,
    linkify: true,
    typographer: true,
})

function renderMarkdown(text: string): string {
    const safeText = text ?? ''
    const html = md.render(safeText)
    const clean = DOMPurify.sanitize(html)
    // highlight code blocks after DOM update
    nextTick(() => {
        try {
            hljs.highlightAll()
        } catch (e) {
            // ignore
        }
    })
    return clean
}

function flushPendingMessages(): void {
    if (!socket || socket.readyState !== WebSocket.OPEN || isAwaitingResponse.value) {
        return
    }

    const nextMessage = pendingMessages.shift()
    if (!nextMessage) {
        return
    }

    logWs('sending queued message', { pendingMessages: pendingMessages.length + 1, textLength: nextMessage.length })
    pushMessage('user', nextMessage)
    socket.send(nextMessage)
    isAwaitingResponse.value = true
}

function connectWebSocket(): void {
    clearReconnectTimer()

    if (socket && (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING)) {
        logWs('connect skipped', { readyState: socket.readyState })
        return
    }

    connectionState.value = reconnectAttempts > 0 ? 'reconnecting' : 'connecting'
    const wsUrl = buildWebSocketUrl()
    logWs('connecting', { url: wsUrl, reconnectAttempts })
    const currentSocket = new WebSocket(wsUrl)
    socket = currentSocket

    currentSocket.onopen = () => {
        if (socket !== currentSocket) return

        connectionState.value = 'connected'
        reconnectAttempts = 0
        clearReconnectTimer()
        logWs('connected')
        flushPendingMessages()
    }

    currentSocket.onmessage = (event) => {
        logWs('message received', { textLength: String(event.data ?? '').length })
        let responseText = String(event.data ?? '')

        try {
            const parsed = JSON.parse(responseText) as { error?: string; message?: string; response?: string }
            if (parsed.error) {
                responseText = parsed.error
            } else if (parsed.response) {
                responseText = parsed.response
            } else if (parsed.message) {
                responseText = parsed.message
            }
        } catch {
            // Ignore parse errors because the endpoint usually returns plain text.
        }

        pushMessage('bot', responseText)
        isAwaitingResponse.value = false
        flushPendingMessages()
    }

    currentSocket.onerror = (event) => {
        logWs('socket error', { eventType: event.type, readyState: currentSocket.readyState })
        isAwaitingResponse.value = false
    }

    currentSocket.onclose = (event) => {
        if (socket !== currentSocket) return

        logWs('socket closed', {
            code: event.code,
            reason: event.reason,
            wasClean: event.wasClean,
            readyState: currentSocket.readyState,
        })
        connectionState.value = 'disconnected'
        isAwaitingResponse.value = false
        socket = null
        scheduleReconnect()
    }
}

function sendMessage(): void {
    const text = inputText.value.trim()
    if (!text) return

    inputText.value = ''
    pendingMessages.push(text)
    logWs('message queued', { queuedMessages: pendingMessages.length, textLength: text.length })

    if (!socket || socket.readyState !== WebSocket.OPEN) {
        logWs('send deferred until reconnect')
        connectWebSocket()
        return
    }

    flushPendingMessages()
}

onMounted(() => {
    isUnmounted = false
    connectWebSocket()
})

onBeforeUnmount(() => {
    isUnmounted = true
    clearReconnectTimer()
    if (socket) {
        socket.close()
        socket = null
    }
})
</script>

<template>
    <div class="chat">
        <div class="chat-title">
            <p>Fateco</p>
            <img src="../assets/Btnicon.png" alt="Fechar" class="chat-fechar">
        </div>
                <div ref="messageAreaRef" class="chat-msg-area">
                        <div
                            v-for="msg in messages"
                            :key="msg.id"
                            class="chat-msg"
                            :class="msg.role === 'user' ? 'chat-msg-user' : 'chat-msg-bot'"
                        >
                            <template v-if="msg.role === 'bot'">
                                <div class="chat-msg-bot-content" v-html="renderMarkdown(msg.text)"></div>
                            </template>
                            <template v-else>
                                <p class="chat-msg-user-text">{{ msg.text }}</p>
                            </template>
                        </div>

                        <section v-if="isAwaitingResponse" class="chat-msg chat-msg-bot">
                            <div class="chat-loading-animation"><div/><div/><div/></div>
                        </section>

                        <p v-if="connectionState !== 'connected'" class="chat-msg chat-msg-bot">
                            Status: {{ connectionState === 'reconnecting' ? 'reconectando' : 'desconectado' }}
                        </p>
            <div></div>
        </div>
                <form class="chat-input-area" @submit.prevent="sendMessage">
                        <input
                            v-model="inputText"
                            type="text"
                            name="chat"
                            class="input-chat"
                            placeholder="Digite sua mensagem"
                            :disabled="isAwaitingResponse"
                        >
                        <button type="submit" class="btn-chat" :disabled="isAwaitingResponse">
                <img src="../assets/Btnicon.png" alt="Enviar" class="chat-enviar">
            </button>
                </form>
    </div>
</template>

<style scoped src="../assets/chat.css"></style>
