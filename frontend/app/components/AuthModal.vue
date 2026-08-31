<template>
  <div class="auth-overlay">
    <div class="auth-card glass-panel">
      <div class="auth-header">
        <div class="logo-circle">
          <svg
            width="28"
            height="28"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path
              d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"
            ></path>
          </svg>
        </div>
        <h2>Mini Messenger</h2>
        <p class="subtitle">Distributed Real-time Messaging System</p>
      </div>

      <div class="auth-tabs">
        <button :class="{ active: isLogin }" @click="isLogin = true">
          Đăng Nhập
        </button>
        <button :class="{ active: !isLogin }" @click="isLogin = false">
          Đăng Ký
        </button>
      </div>

      <form @submit.prevent="handleSubmit" class="auth-form">
        <div v-if="!isLogin" class="form-group">
          <label for="displayName">Tên hiển thị</label>
          <input
            v-model="displayName"
            id="displayName"
            type="text"
            placeholder="Nguyễn Văn A"
            required
          />
        </div>

        <div class="form-group">
          <label for="username">Tên tài khoản</label>
          <input
            v-model="username"
            id="username"
            type="text"
            placeholder="username"
            required
          />
        </div>

        <div class="form-group">
          <label for="password">Mật khẩu</label>
          <input
            v-model="password"
            id="password"
            type="password"
            placeholder="••••••••"
            required
          />
        </div>

        <p v-if="errorMsg" class="error-text">{{ errorMsg }}</p>

        <button type="submit" class="glow-btn submit-btn" :disabled="loading">
          <span v-if="loading">Đang xử lý...</span>
          <span v-else>{{ isLogin ? "Đăng Nhập" : "Tạo Tài Khoản" }}</span>
        </button>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { useAuth } from "../composables/useAuth";

const emit = defineEmits(["auth-success"]);
const { login, register } = useAuth();

const isLogin = ref(true);
const username = ref("");
const password = ref("");
const displayName = ref("");
const errorMsg = ref("");
const loading = ref(false);

const handleSubmit = async () => {
  errorMsg.value = "";
  loading.value = true;
  try {
    if (isLogin.value) {
      await login(username.value, password.value);
    } else {
      await register(
        username.value,
        password.value,
        displayName.value || username.value,
      );
    }
    emit("auth-success");
  } catch (err: any) {
    errorMsg.value = err.message || "Đã có lỗi xảy ra";
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.auth-overlay {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(10, 15, 29, 0.85);
  backdrop-filter: blur(12px);
  z-index: 999;
}

.auth-card {
  width: 100%;
  max-width: 420px;
  padding: 2.5rem;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-main);
  animation: modalIn 0.35s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes modalIn {
  from {
    opacity: 0;
    transform: scale(0.95) translateY(10px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.auth-header {
  text-align: center;
  margin-bottom: 2rem;
}

.logo-circle {
  width: 56px;
  height: 56px;
  margin: 0 auto 1rem;
  border-radius: var(--radius-md);
  background: var(--accent-gradient);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  box-shadow: var(--shadow-glow);
}

.auth-header h2 {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-main);
}

.subtitle {
  font-size: 0.85rem;
  color: var(--text-muted);
  margin-top: 0.25rem;
}

.auth-tabs {
  display: flex;
  background: var(--bg-tertiary);
  padding: 0.25rem;
  border-radius: var(--radius-md);
  margin-bottom: 1.5rem;
}

.auth-tabs button {
  flex: 1;
  padding: 0.6rem;
  border: none;
  background: transparent;
  color: var(--text-muted);
  font-weight: 600;
  font-size: 0.9rem;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all 0.2s;
}

.auth-tabs button.active {
  background: var(--bg-secondary);
  color: var(--text-main);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 1.2rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.form-group label {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.form-group input {
  background: var(--bg-tertiary);
  border: 1px solid var(--glass-border);
  padding: 0.8rem 1rem;
  border-radius: var(--radius-sm);
  color: var(--text-main);
  font-size: 0.95rem;
  outline: none;
  transition: border-color 0.2s;
}

.form-group input:focus {
  border-color: var(--accent-primary);
}

.error-text {
  color: var(--error);
  font-size: 0.85rem;
  text-align: center;
}

.submit-btn {
  padding: 0.9rem;
  border-radius: var(--radius-sm);
  font-size: 1rem;
  margin-top: 0.5rem;
}
</style>
