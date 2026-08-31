<template>
  <main class="chat-container">
    <!-- Active Chat Header -->
    <header v-if="activeFriend" class="chat-header glass-panel">
      <div class="header-user">
        <div class="avatar-wrap">
          <div class="avatar gradient-avatar">
            {{
              activeFriend.displayName
                ? activeFriend.displayName.charAt(0).toUpperCase()
                : activeFriend.username.charAt(0).toUpperCase()
            }}
          </div>
          <span class="status-indicator" :class="activeFriend.status"></span>
        </div>
        <div class="user-meta">
          <h2>{{ activeFriend.displayName || activeFriend.username }}</h2>
          <span class="presence-badge" :class="activeFriend.status">
            {{
              activeFriend.status === "online"
                ? "Đang hoạt động"
                : "Ngoại tuyến"
            }}
          </span>
        </div>
      </div>

      <div class="header-actions">
        <span class="protocol-badge">
          <span class="pulse-dot"></span>
          STOMP over WebSocket
        </span>
      </div>
    </header>

    <!-- Empty State / No Conversation Selected -->
    <div v-if="!activeFriend" class="no-chat-state">
      <div class="welcome-box glass-panel">
        <div class="welcome-icon">
          <svg
            width="48"
            height="48"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="1.5"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path
              d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"
            ></path>
          </svg>
        </div>
        <h2>Chào mừng bạn đến với Mini Messenger!</h2>
        <p>
          Chọn một người bạn ở danh sách bên trái hoặc kết bạn mới để bắt đầu
          nhắn tin thời gian thực.
        </p>
      </div>
    </div>

    <!-- Messages Feed -->
    <section v-else class="messages-feed" ref="feedRef">
      <div class="feed-content">
        <div
          v-for="(msg, index) in messages"
          :key="msg.id || index"
          class="message-row"
          :class="{ 'is-me': msg.senderId === currentUser?.id }"
        >
          <div class="message-bubble">
            <!-- Image Attachment if present -->
            <div v-if="msg.mediaUrl" class="media-attachment">
              <img :src="msg.mediaUrl" alt="Media attachment" loading="lazy" />
            </div>

            <!-- Text Content -->
            <p v-if="msg.content" class="text-content">{{ msg.content }}</p>

            <!-- Metadata: Timestamp & Status -->
            <div class="message-meta">
              <span class="time">{{ formatTime(msg.timestamp) }}</span>
              <span v-if="msg.senderId === currentUser?.id" class="status-icon">
                <svg
                  width="14"
                  height="14"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                >
                  <polyline points="20 6 9 17 4 12"></polyline>
                </svg>
              </span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Chat Input Area -->
    <footer v-if="activeFriend" class="chat-input-area glass-panel">
      <div class="input-form">
        <!-- Media Upload Button -->
        <label class="attachment-btn" :class="{ disabled: isUploading }">
          <input
            type="file"
            accept="image/*"
            class="hidden-input"
            @change="handleFileUpload"
            :disabled="isUploading"
          />
          <svg
            v-if="!isUploading"
            width="22"
            height="22"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>
            <circle cx="8.5" cy="8.5" r="1.5"></circle>
            <polyline points="21 15 16 10 5 21"></polyline>
          </svg>
          <span v-else class="spinner"></span>
        </label>

        <!-- Text Input -->
        <label class="text-input" :class="{ disabled: isUploading }">
          <input
            v-model="inputContent"
            type="text"
            placeholder="Nhập tin nhắn..."
            class="text-input"
            @keydown.enter="handleSend"
          />
        </label>

        <!-- Send Button -->
        <button
          class="send-btn glow-btn"
          :disabled="!inputContent.trim()"
          @click="handleSend"
        >
          <svg
            width="18"
            height="18"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <line x1="22" y1="2" x2="11" y2="13"></line>
            <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
          </svg>
        </button>
      </div>
    </footer>
  </main>
</template>

<script setup lang="ts">
import { ref, watch, nextTick } from "vue";
import { useAuth } from "../composables/useAuth";
import { useChat } from "../composables/useChat";

const { currentUser } = useAuth();
const { activeFriend, messages, isUploading, sendMessage, uploadMedia } =
  useChat();

const inputContent = ref("");
const feedRef = ref<HTMLElement | null>(null);

const scrollToBottom = () => {
  nextTick(() => {
    if (feedRef.value) {
      feedRef.value.scrollTop = feedRef.value.scrollHeight;
    }
  });
};

watch(
  () => messages.value.length,
  () => {
    scrollToBottom();
  },
);

const handleSend = () => {
  const text = inputContent.value.trim();
  if (!text) return;
  sendMessage(text);
  inputContent.value = "";
  scrollToBottom();
};

const handleFileUpload = async (event: Event) => {
  const target = event.target as HTMLInputElement;
  if (target.files && target.files[0]) {
    try {
      const file = target.files[0];
      const url = await uploadMedia(file);
      sendMessage("", url);
      scrollToBottom();
    } catch {
      alert("Upload failed. Please check MinIO storage.");
    } finally {
      target.value = "";
    }
  }
};

const formatTime = (timestamp?: number) => {
  if (!timestamp) return "";
  const date = new Date(timestamp);
  return date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
};
</script>

<style scoped>
.chat-container {
  flex: 1;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg-primary);
  position: relative;
}

.chat-header {
  padding: 1rem 2rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--glass-border);
  z-index: 10;
}

.header-user {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.avatar-wrap {
  position: relative;
}

.avatar {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 1.1rem;
}

.gradient-avatar {
  background: var(--accent-gradient);
  color: white;
}

.status-indicator {
  position: absolute;
  bottom: -2px;
  right: -2px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 2px solid var(--bg-primary);
}

.status-indicator.online {
  background-color: var(--online);
  box-shadow: 0 0 8px var(--online);
}

.status-indicator.offline {
  background-color: var(--offline);
}

.user-meta h2 {
  font-size: 1.1rem;
  font-weight: 700;
}

.presence-badge {
  font-size: 0.75rem;
  font-weight: 600;
}

.presence-badge.online {
  color: var(--online);
}

.presence-badge.offline {
  color: var(--offline);
}

.protocol-badge {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.75rem;
  padding: 0.35rem 0.75rem;
  background: var(--bg-tertiary);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-full);
  color: var(--text-muted);
}

.pulse-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--online);
  box-shadow: 0 0 8px var(--online);
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% {
    transform: scale(0.95);
    opacity: 0.7;
  }
  50% {
    transform: scale(1.15);
    opacity: 1;
  }
  100% {
    transform: scale(0.95);
    opacity: 0.7;
  }
}

.no-chat-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem;
}

.welcome-box {
  text-align: center;
  max-width: 460px;
  padding: 3rem 2rem;
  border-radius: var(--radius-lg);
}

.welcome-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto 1.5rem;
  border-radius: var(--radius-lg);
  background: var(--bg-tertiary);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--accent-primary);
  border: 1px solid var(--glass-border);
}

.welcome-box h2 {
  font-size: 1.3rem;
  margin-bottom: 0.5rem;
}

.welcome-box p {
  font-size: 0.9rem;
  color: var(--text-muted);
  line-height: 1.5;
}

.messages-feed {
  flex: 1;
  overflow-y: auto;
  padding: 1.5rem 2rem;
}

.feed-content {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.message-row {
  display: flex;
  justify-content: flex-start;
}

.message-row.is-me {
  justify-content: flex-end;
}

.message-bubble {
  max-width: 65%;
  background: var(--bg-tertiary);
  border: 1px solid var(--glass-border);
  padding: 0.85rem 1.1rem;
  border-radius: var(--radius-md);
  border-bottom-left-radius: 4px;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.message-row.is-me .message-bubble {
  background: var(--accent-gradient);
  border: none;
  border-bottom-left-radius: var(--radius-md);
  border-bottom-right-radius: 4px;
  color: white;
  box-shadow: 0 4px 15px rgba(99, 102, 241, 0.25);
}

.media-attachment img {
  max-width: 100%;
  max-height: 280px;
  border-radius: var(--radius-sm);
  object-fit: cover;
  margin-bottom: 0.3rem;
}

.text-content {
  font-size: 0.95rem;
  line-height: 1.45;
  word-break: break-word;
}

.message-meta {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.3rem;
  font-size: 0.7rem;
  color: rgba(255, 255, 255, 0.6);
}

.chat-input-area {
  padding: 1.2rem 2rem;
  border-top: 1px solid var(--glass-border);
}

.input-form {
  display: flex;
  align-items: center;
  gap: 0.8rem;
}

.attachment-btn {
  background: var(--bg-tertiary);
  border: 1px solid var(--glass-border);
  width: 44px;
  height: 44px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
  cursor: pointer;
  transition: all 0.2s;
}

.attachment-btn:hover {
  color: var(--text-main);
  background: var(--bg-hover);
}

.hidden-input {
  display: none;
}

.text-input {
  flex: 1;
  background: var(--bg-tertiary);
  border: 1px solid var(--glass-border);
  padding: 0.8rem 1.2rem;
  border-radius: var(--radius-md);
  color: var(--text-main);
  font-size: 0.95rem;
  outline: none;
  transition: border-color 0.2s;
}

.text-input:focus {
  border-color: var(--accent-primary);
}

.send-btn {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
}

.send-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.2);
  border-top-color: var(--accent-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
