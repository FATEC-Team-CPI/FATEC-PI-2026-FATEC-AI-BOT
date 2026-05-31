<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { loginAdmin, storeToken } from '../services/backendApi'

const router = useRouter()

const form = reactive({
  unidade: 'FatecItaquera',
  email: '',
  password: '',
})

const isLoading = ref(false)
const feedback = ref('')
const feedbackType = ref<'success' | 'error' | ''>('')

async function handleLogin(): Promise<void> {
  feedback.value = ''
  feedbackType.value = ''
  isLoading.value = true

  try {
    const response = await loginAdmin(form)

    if (response.token) {
      storeToken(response.token)
    }

    feedbackType.value = 'success'
    feedback.value = response.message ?? 'Login realizado com sucesso.'

    const redirectTarget = new URLSearchParams(window.location.search).get('redirect') ?? '/admin'
    await router.replace(redirectTarget)
  } catch (error) {
    feedbackType.value = 'error'
    feedback.value = error instanceof Error ? error.message : 'Falha ao autenticar'
  } finally {
    isLoading.value = false
  }
}
</script>

<template>
    <section class="app">
      <router-link to="/menu" tag="button" class="seta"><img src="../assets/seta.png" alt=""></router-link>

      <section class ="app-container app-container-stripe">
        <form class ="input-area" @submit.prevent="handleLogin">
        <section >
          <h1 class="app-title">LOGIN</h1>
          <h2 class="app-subtitle">Insira suas credenciais para entrar no sistema</h2>
        </section>

          <input v-model="form.unidade" class="input" type="text" name="unidade" placeholder="Unidade">
          <input v-model="form.email" class="input" type="email" name="email" placeholder="Email">
          <input v-model="form.password" class="input" type="password" name="senha" placeholder="Senha">
          <router-link to="/password-reset" tag="link" class="link">Esqueceu a senha?</router-link>
          <p v-if="feedback" class="link" :class="feedbackType === 'error' ? 'error' : 'success'">{{ feedback }}</p>
          <button class="btn" :disabled="isLoading">{{ isLoading ? 'Entrando...' : 'Entrar' }}</button>
        </form>

      </section>
      <img class="logo-fatec" src="../assets/logo.png" alt="Fatec e Centro Paula Souza">
    </section>
</template>

<style scoped src="../assets/login.css"></style>
