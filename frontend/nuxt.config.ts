// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  compatibilityDate: '2025-07-15',
  devtools: { enabled: true },
  css: ['~/assets/main.css'],
  app: {
    head: {
      title: 'Mini Messenger - Real-time Distributed Chat',
      meta: [
        { name: 'description', content: 'Distributed real-time messaging application powered by Spring Boot, Redis Pub/Sub, MongoDB, PostgreSQL, and Nuxt.' },
        { name: 'viewport', content: 'width=device-width, initial-scale=1' }
      ]
    }
  },
  runtimeConfig: {
    public: {
      apiBase: '/api',
      wsBase: '/ws',
    }
  }
})
