<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { listAdmins, type UserListItem } from '../services/backendApi'

const admins = ref<UserListItem[]>([])
const isLoading = ref(false)
const feedback = ref('')
const feedbackType = ref<'success' | 'error' | ''>('')

function formatDateTime(value?: string): string {
  if (!value) {
    return '-'
  }

  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) {
    return value
  }

  return new Intl.DateTimeFormat('pt-BR', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(parsed)
}

async function loadAdmins(): Promise<void> {
  isLoading.value = true
  feedback.value = ''
  feedbackType.value = ''

  try {
    admins.value = await listAdmins()
    feedbackType.value = 'success'
    feedback.value = admins.value.length
      ? `${admins.value.length} usuário(s) encontrado(s).`
      : 'Nenhum usuário encontrado.'
  } catch (error) {
    feedbackType.value = 'error'
    feedback.value = error instanceof Error ? error.message : 'Falha ao listar usuários'
    admins.value = []
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  void loadAdmins()
})
</script>

<template>
  <router-link to="/admin" tag="button" class="seta"><img src="../assets/seta.png" alt=""></router-link>

  <div class="app-container app-container-bar">
    <section>
      <h1 class="app-title">USUARIOS</h1>
      <h2 class="app-subtitle sub">Listagem dos administradores e editores cadastrados</h2>
    </section>

    <section class="list-actions">
      <router-link to="/register" class="form-btn list-action-btn">Cadastrar novo usuário</router-link>
    </section>

    <section class="users-list">
      <div v-if="isLoading" class="users-empty">
        <p>Carregando usuários...</p>
      </div>

      <div v-for="admin in admins" :key="`${admin.email}-${admin.createdAt ?? ''}`" class="users-row">
        <div class="users-row-main">
          <h3>{{ admin.name ?? '-' }}</h3>
          <p>Email: {{ admin.email ?? '-' }}</p>
          <p>Perfil: {{ admin.role ?? '-' }}</p>
          <p>Status: {{ admin.status ?? '-' }}</p>
        </div>
        <div class="users-row-dates">
          <p>Criado em: {{ formatDateTime(admin.createdAt) }}</p>
          <p>Atualizado em: {{ formatDateTime(admin.updatedAt) }}</p>
        </div>
      </div>

      <div v-if="!isLoading && !admins.length" class="users-empty">
        <p>Nenhum usuário encontrado.</p>
      </div>

      <div v-if="feedback" class="users-empty" :class="feedbackType === 'error' ? 'error' : 'success'">
        <p>{{ feedback }}</p>
      </div>
    </section>
  </div>
</template>

<style scoped src="../assets/users.css"></style>
