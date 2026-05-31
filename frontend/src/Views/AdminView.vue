<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { clearStoredToken, getStoredToken, validateStoredToken } from '../services/backendApi'

const tokenPreview = ref('')
const validationMessage = ref('')
const validationType = ref<'success' | 'error' | ''>('')
const router = useRouter()

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
  validationMessage.value = ''
  validationType.value = ''
  void router.push('/menu')
}
</script>

<template>
  <div class="app-container app-container-screen">
    <h1 class="app-title adm">MENU ADMINISTRATIVO</h1>

    <section class="users-empty admin-session">
      <h3>Sessão</h3>
      <p>Token: {{ tokenPreview }}</p>
    </section>

    <div class="app-form">
      <router-link to="/arquivos" class="adm-btn" tag="button">
        <section class="adm-content">
          <img class="icon-cog" id="cloudwhite" src="../assets/file.png" alt="">
          <p>Documentos</p>
        </section>
      </router-link>

      <router-link to="/usuarios" class="adm-btn" tag="button">
        <section class="adm-content">
          <img class="icon-cog" src="../assets/users.png" alt="">
          <p>Usuários</p>
        </section>
      </router-link>

      <router-link to="/chat" class="adm-btn" tag="button">
        <section class="adm-content">
          <img class="icon-cog" src="../assets/Vector.png" alt="">
          <p>Voltar ao Chat</p>
        </section>
      </router-link>
    </div>

    <section class="admin-footer">
      <button class="form-btn admin-logout-btn" type="button" @click="handleLogout">Sair</button>
    </section>
  </div>
</template>

<style scoped src="../assets/admin.css"></style>
