import { ref, computed } from 'vue';

export interface User {
  id: number;
  username: string;
  displayName: string;
}

const currentUser = ref<User | null>(null);

export const useAuth = () => {
  const config = useRuntimeConfig();
  const apiBase = config.public.apiBase;

  // Initialize from localStorage in client-side
  const initAuth = () => {
    if (import.meta.client) {
      const stored = localStorage.getItem('mini_messenger_user');
      if (stored) {
        try {
          currentUser.value = JSON.parse(stored);
        } catch {
          localStorage.removeItem('mini_messenger_user');
        }
      }
    }
  };

  const login = async (username: string, password: string): Promise<boolean> => {
    try {
      const res = await $fetch<User>(`${apiBase}/auth/login`, {
        method: 'POST',
        body: { username, password },
      });
      currentUser.value = res;
      if (import.meta.client) {
        localStorage.setItem('mini_messenger_user', JSON.stringify(res));
      }
      return true;
    } catch (err: any) {
      throw new Error(err.data || 'Login failed');
    }
  };

  const register = async (username: string, password: string, displayName: string): Promise<boolean> => {
    try {
      const res = await $fetch<User>(`${apiBase}/auth/register`, {
        method: 'POST',
        body: { username, password, displayName },
      });
      currentUser.value = res;
      if (import.meta.client) {
        localStorage.setItem('mini_messenger_user', JSON.stringify(res));
      }
      return true;
    } catch (err: any) {
      throw new Error(err.data || 'Registration failed');
    }
  };

  const logout = () => {
    currentUser.value = null;
    if (import.meta.client) {
      localStorage.removeItem('mini_messenger_user');
    }
  };

  const isAuthenticated = computed(() => !!currentUser.value);

  return {
    currentUser,
    isAuthenticated,
    initAuth,
    login,
    register,
    logout,
  };
};
