<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { clearStoredToken, getStoredToken, validateStoredToken } from '../services/backendApi'

const tokenPreview = ref('')
const validationMessage = ref('')
const validationType = ref<'success' | 'error' | ''>('')

onMounted(() => {
  const token = getStoredToken()
  tokenPreview.value = token ? `${token.slice(0, 24)}...` : 'Nenhum token salvo'
})

async function handleValidateToken(): Promise<void> {
  validationMessage.value = ''
  validationType.value = ''

  try {
    const response = await validateStoredToken()
    validationType.value = 'success'
    validationMessage.value = response.tokenResponse ?? 'Token válido'
  } catch (error) {
    validationType.value = 'error'
    validationMessage.value = error instanceof Error ? error.message : 'Falha ao validar token'
  }
}

function handleLogout(): void {
  clearStoredToken()
  tokenPreview.value = 'Nenhum token salvo'
  validationMessage.value = 'Token removido do navegador.'
  validationType.value = 'success'
}
</script>

<template>
  <div class="app-container app-container-screen">
    <h1 class="app-title adm">MENU ADMINISTRATIVO</h1>

    <section class="users-empty">
      <h3>Sessão</h3>
      <p>Token: {{ tokenPreview }}</p>
      <button class="form-btn" type="button" @click="handleValidateToken">Validar token</button>
      <button class="form-btn" type="button" @click="handleLogout">Sair</button>
      <p v-if="validationMessage" :class="validationType === 'error' ? 'error' : 'success'">{{ validationMessage }}</p>
    </section>

    <div class="app-form">
      <router-link to="/docs" class="adm-btn" tag="button">
        <section class="adm-content">
        <img class="icon-cog" id="cloudwhite" src="../assets/cloud-white.png" alt="">
        <p>Adicionar Documentos</p>
      </section>
      </router-link>

      <router-link to="/register" class="adm-btn" tag="button">
        <section class="adm-content">
          <img class="icon-cog" src="../assets/Vector.png" alt="">
          <p>Cadastrar um Novo Usuário</p>
        </section>
      </router-link>

      <router-link to="/usuarios" class="adm-btn" tag="button">
        <section class="adm-content">
          <img class="icon-cog" src="../assets/users.png" alt="">
          <p>Listar Usuarios</p>
        </section>
      </router-link>


        <router-link to="/arquivos" class="adm-btn" tag="button">
        <section class="adm-content">
          <img class="icon-cog" src="../assets/file.png" alt="">
          <p>Listar Arquivos</p>
        </section>
      </router-link>
    </div>
  </div>
</template>

<style scoped src="../assets/admin.css"></style>
