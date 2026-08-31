import { ref } from 'vue';
import { Client } from '@stomp/stompjs';
import { useAuth } from './useAuth';

export interface Friend {
  id: number;
  username: string;
  displayName: string;
  status: 'online' | 'offline';
}

export interface ChatMessage {
  id?: number;
  senderId: number;
  recipientId: number;
  content: string;
  mediaUrl?: string;
  timestamp?: number;
  status?: 'SENT' | 'DELIVERED' | 'READ';
}

const stompClient = ref<Client | null>(null);
const isConnected = ref(false);
const friends = ref<Friend[]>([]);
const activeFriend = ref<Friend | null>(null);
const messages = ref<ChatMessage[]>([]);
const isUploading = ref(false);

export const useChat = () => {
  const { currentUser } = useAuth();
  const config = useRuntimeConfig();
  const apiBase = config.public.apiBase;

  // Initialize STOMP client connection
  const connectSocket = () => {
    if (!currentUser.value || stompClient.value?.active) return;

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const host = window.location.host;
    const wsPath = (config.public.wsBase as string) || '/ws';
    const wsUrl = wsPath.startsWith('ws') ? wsPath : `${protocol}//${host}${wsPath}`;

    const client = new Client({
      brokerURL: wsUrl,
      connectHeaders: {
        userId: currentUser.value.id.toString(),
      },
      debug: (str) => {
        if (process.env.NODE_ENV === 'development') {
          console.log('[STOMP]:', str);
        }
      },
      reconnectDelay: 4000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
    });

    client.onConnect = () => {
      isConnected.value = true;
      // Subscribe to user-specific message queue
      client.subscribe('/user/queue/messages', (msg) => {
        const payload: ChatMessage = JSON.parse(msg.body);
        // If message is relevant to active conversation, append it
        if (
          activeFriend.value &&
          (payload.senderId === activeFriend.value.id || payload.recipientId === activeFriend.value.id)
        ) {
          // Avoid duplicate by Snowflake ID
          if (!messages.value.some((m) => m.id === payload.id)) {
            messages.value.push(payload);
          }
        }
      });
      // Refresh presence status
      fetchFriends();
    };

    client.onDisconnect = () => {
      isConnected.value = false;
    };

    client.activate();
    stompClient.value = client;
  };

  const disconnectSocket = () => {
    if (stompClient.value) {
      stompClient.value.deactivate();
      stompClient.value = null;
      isConnected.value = false;
    }
  };

  // Fetch list of friends
  const fetchFriends = async () => {
    if (!currentUser.value) return;
    try {
      const data = await $fetch<Friend[]>(`${apiBase}/auth/friends/${currentUser.value.id}`);
      friends.value = data;
      // Update activeFriend presence status if present
      if (activeFriend.value) {
        const match = data.find((f) => f.id === activeFriend.value?.id);
        if (match) {
          activeFriend.value.status = match.status;
        }
      }
    } catch (err) {
      console.error('Failed to fetch friends', err);
    }
  };

  // Add friend
  const addFriend = async (friendId: number) => {
    if (!currentUser.value) return;
    try {
      await $fetch(`${apiBase}/auth/friends?userId=${currentUser.value.id}&friendId=${friendId}`, {
        method: 'POST',
      });
      await fetchFriends();
    } catch (err: any) {
      throw new Error(err.data || 'Failed to add friend');
    }
  };

  // Select active conversation & fetch historical messages
  const selectFriend = async (friend: Friend) => {
    activeFriend.value = friend;
    if (!currentUser.value) return;
    try {
      const history = await $fetch<ChatMessage[]>(
        `${apiBase}/chat/history?senderId=${currentUser.value.id}&recipientId=${friend.id}`
      );
      messages.value = history;
    } catch (err) {
      console.error('Failed to fetch chat history', err);
      messages.value = [];
    }
  };

  // Send a chat message over STOMP
  const sendMessage = (content: string, mediaUrl?: string) => {
    if (!stompClient.value?.connected || !currentUser.value || !activeFriend.value) {
      return;
    }

    const payload: ChatMessage = {
      senderId: currentUser.value.id,
      recipientId: activeFriend.value.id,
      content,
      mediaUrl,
    };

    stompClient.value.publish({
      destination: '/app/chat.sendMessage',
      body: JSON.stringify(payload),
    });
  };

  // Upload attachment file to MinIO
  const uploadMedia = async (file: File): Promise<string> => {
    isUploading.value = true;
    try {
      const formData = new FormData();
      formData.append('file', file);
      const res = await $fetch<{ url: string }>(`${apiBase}/media/upload`, {
        method: 'POST',
        body: formData,
      });
      return res.url;
    } finally {
      isUploading.value = false;
    }
  };

  return {
    isConnected,
    friends,
    activeFriend,
    messages,
    isUploading,
    connectSocket,
    disconnectSocket,
    fetchFriends,
    addFriend,
    selectFriend,
    sendMessage,
    uploadMedia,
  };
};
