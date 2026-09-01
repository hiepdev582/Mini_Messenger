<template>
  <aside class="sidebar glass-panel">
    <!-- User Profile Header -->
    <div class="user-card">
      <div class="avatar-wrap">
        <div class="avatar gradient-avatar">
          {{ userInitials }}
        </div>
        <span class="status-indicator online"></span>
      </div>
      <div class="user-info">
        <h3>{{ currentUser?.displayName || currentUser?.username }}</h3>
        <p class="user-sub">@{{ currentUser?.username }}</p>
      </div>
      <button
        class="icon-btn logout-btn"
        @click="handleLogout"
        title="Đăng xuất"
      >
        <svg
          width="20"
          height="20"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
          <polyline points="16 17 21 12 16 7"></polyline>
          <line x1="21" y1="12" x2="9" y2="12"></line>
        </svg>
      </button>
    </div>

    <!-- Search / Add Friend Bar -->
    <div class="action-bar">
      <button class="add-friend-btn glow-btn" @click="showAddModal = true">
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
          <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
          <circle cx="8.5" cy="7" radius="4"></circle>
          <line x1="20" y1="8" x2="20" y2="14"></line>
          <line x1="23" y1="11" x2="17" y2="11"></line>
        </svg>
        <span>Kết bạn mới</span>
      </button>
    </div>

    <!-- Conversation / Friends List -->
    <div class="friends-list-wrap">
      <div class="section-title">
        <span>Danh sách bạn bè ({{ friends.length }})</span>
      </div>
      <div class="friends-list">
        <div
          v-for="friend in friends"
          :key="friend.id"
          class="friend-item"
          :class="{ active: activeFriend?.id === friend.id }"
          @click="selectFriend(friend)"
        >
          <div class="avatar-wrap">
            <div class="avatar letter-avatar">
              {{
                friend.displayName
                  ? friend.displayName.charAt(0).toUpperCase()
                  : friend.username.charAt(0).toUpperCase()
              }}
            </div>
            <span class="status-indicator" :class="friend.status"></span>
          </div>
          <div class="friend-info">
            <div class="friend-top">
              <span class="friend-name">{{
                friend.displayName || friend.username
              }}</span>
            </div>
            <span class="friend-status-text" :class="friend.status">
              {{ friend.status === "online" ? "Đang hoạt động" : "Offline" }}
            </span>
          </div>
        </div>

        <div v-if="friends.length === 0" class="empty-state">
          <p>Chưa có bạn bè nào.</p>
          <small>Bấm nút "Kết bạn mới" để bắt đầu trò chuyện!</small>
        </div>
      </div>
    </div>

    <!-- Add Friend Modal -->
    <div
      v-if="showAddModal"
      class="modal-backdrop"
      @click.self="showAddModal = false"
    >
      <div class="modal-content glass-panel">
        <h3>Tìm kiếm & Kết bạn</h3>
        <p class="modal-desc">Tìm kiếm bạn bè theo tên hoặc tên tài khoản</p>

        <!-- Search Input -->
        <div class="search-input-wrap">
          <input
            v-model="searchQuery"
            type="text"
            placeholder="Nhập tên hiển thị hoặc @username..."
            class="modal-search-input"
            @input="onSearchInput"
          />
        </div>

        <div class="available-users-list">
          <div
            v-for="user in allUsers"
            :key="user.id"
            class="available-user-item"
          >
            <div class="available-user-info">
              <strong>{{ user.displayName || user.username }}</strong>
              <small>@{{ user.username }}</small>
            </div>
            <button
              class="glow-btn btn-sm"
              :disabled="
                isAlreadyFriend(user.id) || user.id === currentUser?.id
              "
              @click="onAddFriend(user.id)"
            >
              {{
                isAlreadyFriend(user.id)
                  ? "Đã là bạn"
                  : user.id === currentUser?.id
                    ? "Bạn"
                    : "Kết bạn"
              }}
            </button>
          </div>

          <div v-if="allUsers.length === 0" class="empty-search">
            <p>Không tìm thấy người dùng phù hợp.</p>
          </div>
        </div>

        <div class="modal-actions">
          <button class="close-btn" @click="showAddModal = false">Đóng</button>
        </div>
      </div>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { useAuth } from "../composables/useAuth";
import { useChat } from "../composables/useChat";

const { currentUser, logout } = useAuth();
const { friends, activeFriend, selectFriend, addFriend, fetchFriends, disconnectSocket } =
  useChat();
const config = useRuntimeConfig();

const showAddModal = ref(false);
const searchQuery = ref("");
const allUsers = ref<any[]>([]);
let searchTimeout: any = null;
let presencePollInterval: any = null;

const userInitials = computed(() => {
  if (!currentUser.value) return "?";
  const name = currentUser.value.displayName || currentUser.value.username;
  return name.charAt(0).toUpperCase();
});

const isAlreadyFriend = (userId: number) => {
  return friends.value.some((f) => f.id === userId);
};

const fetchUsers = async (query = "") => {
  try {
    const url = query.trim()
      ? `${config.public.apiBase}/auth/users?query=${encodeURIComponent(query.trim())}`
      : `${config.public.apiBase}/auth/users`;
    const data = await $fetch<any[]>(url);
    allUsers.value = data;
  } catch (err) {
    console.error("Failed to fetch users", err);
  }
};

const onSearchInput = () => {
  if (searchTimeout) clearTimeout(searchTimeout);
  searchTimeout = setTimeout(() => {
    fetchUsers(searchQuery.value);
  }, 300);
};

const onAddFriend = async (friendId: number) => {
  try {
    await addFriend(friendId);
  } catch (err: any) {
    alert(err.message);
  }
};

const handleLogout = () => {
  if (presencePollInterval) clearInterval(presencePollInterval);
  disconnectSocket();
  logout();
};

onMounted(() => {
  fetchUsers();
  fetchFriends();
  // Poll presence periodically (every 4 seconds) to update online badge in real time
  presencePollInterval = setInterval(() => {
    fetchFriends();
  }, 4000);
});
</script>

<style scoped>
.sidebar {
  width: 340px;
  height: 100vh;
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--glass-border);
  background: var(--bg-secondary);
}

.user-card {
  padding: 1.5rem;
  display: flex;
  align-items: center;
  gap: 1rem;
  border-bottom: 1px solid var(--glass-border);
}

.avatar-wrap {
  position: relative;
}

.avatar {
  width: 46px;
  height: 46px;
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
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
}

.letter-avatar {
  background: var(--bg-tertiary);
  color: var(--text-main);
  border: 1px solid var(--glass-border);
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

.user-info {
  flex: 1;
  overflow: hidden;
}

.user-info h3 {
  font-size: 0.95rem;
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-sub {
  font-size: 0.8rem;
  color: var(--text-muted);
}

.icon-btn {
  background: transparent;
  border: none;
  color: var(--text-dim);
  cursor: pointer;
  padding: 0.5rem;
  border-radius: var(--radius-sm);
  transition: all 0.2s;
}

.icon-btn:hover {
  color: var(--text-main);
  background: var(--bg-hover);
}

.action-bar {
  padding: 1rem 1.5rem;
}

.add-friend-btn {
  width: 100%;
  padding: 0.75rem;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  font-size: 0.9rem;
}

.friends-list-wrap {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.section-title {
  padding: 0.5rem 1.5rem;
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--text-dim);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.friends-list {
  flex: 1;
  overflow-y: auto;
  padding: 0.5rem 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.friend-item {
  display: flex;
  align-items: center;
  gap: 0.85rem;
  padding: 0.75rem 1rem;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.friend-item:hover {
  background: var(--bg-hover);
}

.friend-item.active {
  background: var(--bg-tertiary);
  border: 1px solid var(--glass-border);
}

.friend-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.friend-name {
  font-weight: 600;
  font-size: 0.9rem;
}

.friend-status-text {
  font-size: 0.75rem;
}

.friend-status-text.online {
  color: var(--online);
}

.friend-status-text.offline {
  color: var(--offline);
}

.empty-state {
  text-align: center;
  padding: 3rem 1rem;
  color: var(--text-muted);
}

.empty-state small {
  display: block;
  margin-top: 0.5rem;
  color: var(--text-dim);
}

/* Modal styles */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.7);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  width: 90%;
  max-width: 480px;
  padding: 2rem;
  border-radius: var(--radius-lg);
}

.modal-desc {
  font-size: 0.85rem;
  color: var(--text-muted);
  margin-bottom: 1.5rem;
}

.search-input-wrap {
  margin-bottom: 1rem;
}

.modal-search-input {
  width: 100%;
  padding: 0.75rem 1rem;
  border-radius: var(--radius-md);
  border: 1px solid var(--glass-border);
  background: var(--bg-primary);
  color: var(--text-main);
  outline: none;
  font-size: 0.9rem;
}

.modal-search-input:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.2);
}

.empty-search {
  text-align: center;
  padding: 2rem 1rem;
  color: var(--text-muted);
  font-size: 0.85rem;
}

.available-users-list {
  max-height: 280px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.available-user-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem;
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
}

.available-user-info {
  display: flex;
  flex-direction: column;
}

.btn-sm {
  padding: 0.4rem 0.8rem;
  font-size: 0.8rem;
  border-radius: var(--radius-sm);
}

.btn-sm:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.modal-actions {
  margin-top: 1.5rem;
  display: flex;
  justify-content: flex-end;
}

.close-btn {
  padding: 0.5rem 1rem;
  background: transparent;
  border: 1px solid var(--glass-border);
  color: var(--text-muted);
  border-radius: var(--radius-sm);
  cursor: pointer;
}
</style>
