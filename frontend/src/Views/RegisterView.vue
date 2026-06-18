<script setup lang="ts">
import { reactive, ref } from 'vue'
import { createAdmin } from '../services/backendApi'

const form = reactive({
  unidade: 'FatecItaquera',
  email: '',
  name: '',
  password: '',
  role: 'EDITOR' as 'SUPER_ADMIN' | 'EDITOR',
})

const isLoading = ref(false)
const feedback = ref('')
const feedbackType = ref<'success' | 'error' | ''>('')

function closeFeedback(): void {
  feedback.value = ''
  feedbackType.value = ''
}

async function handleCreate(): Promise<void> {
  feedback.value = ''
  feedbackType.value = ''
  isLoading.value = true

  try {
    const response = await createAdmin(form)
    feedbackType.value = 'success'
    feedback.value = `Usuário criado com sucesso: ${response.email ?? form.email}`
    form.email = ''
    form.name = ''
    form.password = ''
    form.role = 'EDITOR'
  } catch (error) {
    feedbackType.value = 'error'
    feedback.value = error instanceof Error ? error.message : 'Falha ao criar usuário'
  } finally {
    isLoading.value = false
  }
}
</script>

<template>
  <router-link to="/admin" tag="button" class="seta"><img src="../assets/seta.png" alt=""></router-link>
  <div class="app-container app-container-bar">
    <section>
      <h1 class="app-title">CADASTRO</h1>
      <h2 class="app-subtitle sub">Insira as informações para criar uma nova conta</h2>
    </section>

    <form class="app-form" @submit.prevent="handleCreate">
      <div class="form-group">
        <label>Unidade: *</label>
        <input v-model="form.unidade" class="input" type="text" placeholder="FatecItaquera" />
      </div>

      <div class="form-group">
        <label>Email: *</label>
        <input v-model="form.email" class="input" type="email" placeholder="admin@fatec.sp.gov.br" />
      </div>

      <div class="form-group">
        <label>Nome: *</label>
        <input v-model="form.name" class="input" type="text" placeholder="Nome completo" />
      </div>

      <div class="form-group">
        <label>Senha: *</label>
        <input v-model="form.password" class="input" type="password" placeholder="Senha forte" />
      </div>

      <div class="form-group">
        <label>Perfil: *</label>
        <select v-model="form.role" class="input">
          <option value="EDITOR">EDITOR</option>
          <option value="SUPER_ADMIN">SUPER_ADMIN</option>
        </select>
      </div>

      <div v-if="feedback" class="feedback-overlay" role="presentation">
        <div
          class="feedback-modal"
          :class="feedbackType === 'error' ? 'feedback-modal--error' : 'feedback-modal--success'"
          role="dialog"
          aria-modal="true"
          aria-live="polite"
        >
          <p class="feedback-title">
            {{ feedbackType === 'error' ? 'Não foi possível concluir' : 'Cadastro concluído' }}
          </p>
          <p class="feedback-message">{{ feedback }}</p>
          <div class="feedback-actions">
            <button type="button" class="feedback-button" @click="closeFeedback">OK</button>
            <router-link v-if="feedbackType === 'success'" to="/login" class="feedback-link" @click="closeFeedback">Ir para login</router-link>
          </div>
        </div>
      </div>

      <button class="form-btn" type="submit" :disabled="isLoading">{{ isLoading ? 'Registrando...' : 'Registrar' }}</button>
    </form>
  </div>
</template>

<style scoped src="../assets/register.css"></style>
