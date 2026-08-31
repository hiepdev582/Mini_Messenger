<template>
  <div class="app-layout">
    <!-- Login / Registration Modal if not authenticated -->
    <AuthModal v-if="!isAuthenticated" @auth-success="onAuthSuccess" />

    <!-- Main Messenger Interface -->
    <template v-else>
      <Sidebar />
      <ChatArea />
    </template>
  </div>
</template>

<script setup lang="ts">
import { onMounted, watch } from 'vue';
import { useAuth } from './composables/useAuth';
import { useChat } from './composables/useChat';
import AuthModal from './components/AuthModal.vue';
import Sidebar from './components/Sidebar.vue';
import ChatArea from './components/ChatArea.vue';

const { isAuthenticated, initAuth } = useAuth();
const { connectSocket, disconnectSocket } = useChat();

const onAuthSuccess = () => {
  connectSocket();
};

watch(isAuthenticated, (authed) => {
  if (authed) {
    connectSocket();
  } else {
    disconnectSocket();
  }
});

onMounted(() => {
  initAuth();
  if (isAuthenticated.value) {
    connectSocket();
  }
});
</script>

<style scoped>
.app-layout {
  display: flex;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  position: relative;
}
</style>
