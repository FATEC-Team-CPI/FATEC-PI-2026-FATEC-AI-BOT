<script setup lang="ts">
import { ref } from 'vue'
import { getAdminByEmail } from '../services/backendApi'

const email = ref('')
const isLoading = ref(false)
const feedback = ref('')
const feedbackType = ref<'success' | 'error' | ''>('')
const user = ref<{ email?: string; name?: string; role?: string; status?: string; createdAt?: string; updatedAt?: string } | null>(null)

async function handleSearch(): Promise<void> {
  feedback.value = ''
  feedbackType.value = ''
  user.value = null

  if (!email.value.trim()) {
    feedbackType.value = 'error'
    feedback.value = 'Informe um email para buscar.'
    return
  }

  isLoading.value = true

  try {
    const response = await getAdminByEmail(email.value.trim())
    user.value = response
    feedbackType.value = 'success'
    feedback.value = 'Administrador encontrado.'
  } catch (error) {
    feedbackType.value = 'error'
    feedback.value = error instanceof Error ? error.message : 'Falha ao buscar administrador'
  } finally {
    isLoading.value = false
  }
}
</script>

<template>
  <router-link to="/admin" tag="button" class="seta"><img src="../assets/seta.png" alt=""></router-link>

  <div class="app-container app-container-bar">
    <section>
      <h1 class="app-title">USUARIOS</h1>
      <h2 class="app-subtitle sub">Consulte os usuarios cadastrados no sistema</h2>
    </section>

    <section class="users-list">
      <form class="users-empty" @submit.prevent="handleSearch">
        <h3>Buscar administrador</h3>
        <p>O backend atual expõe consulta por email, então esta tela pesquisa um admin específico.</p>
        <input v-model="email" class="input" type="email" placeholder="admin@fatec.sp.gov.br">
        <button class="form-btn" type="submit" :disabled="isLoading">{{ isLoading ? 'Buscando...' : 'Buscar' }}</button>
      </form>

      <div v-if="user" class="users-empty">
        <h3>Resultado</h3>
        <p>Nome: {{ user.name ?? '-' }}</p>
        <p>Email: {{ user.email ?? '-' }}</p>
        <p>Perfil: {{ user.role ?? '-' }}</p>
        <p>Status: {{ user.status ?? '-' }}</p>
        <p>Criado em: {{ user.createdAt ?? '-' }}</p>
      </div>

      <div v-if="feedback" class="users-empty" :class="feedbackType === 'error' ? 'error' : 'success'">
        <p>{{ feedback }}</p>
      </div>
    </section>
  </div>
</template>

<style scoped src="../assets/users.css"></style>
