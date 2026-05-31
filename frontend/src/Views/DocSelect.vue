<script setup lang="ts">
import { computed, ref } from 'vue'
import { uploadDocument } from '../services/backendApi'

const selectedFile = ref<File | null>(null)
const isLoading = ref(false)
const feedback = ref('')
const feedbackType = ref<'success' | 'error' | ''>('')
const uploadProgress = ref(0)

const selectedFileName = computed(() => selectedFile.value?.name ?? 'Nenhum arquivo selecionado')

function closeFeedback(): void {
  feedback.value = ''
  feedbackType.value = ''
}

function handleFileChange(event: Event): void {
  const input = event.target as HTMLInputElement
  selectedFile.value = input.files?.[0] ?? null
  feedback.value = ''
  feedbackType.value = ''
  uploadProgress.value = 0
}

async function handleUpload(): Promise<void> {
  feedback.value = ''
  feedbackType.value = ''
  uploadProgress.value = 0

  if (!selectedFile.value) {
    feedbackType.value = 'error'
    feedback.value = 'Selecione um arquivo antes de enviar.'
    return
  }

  isLoading.value = true

  try {
    const response = await uploadDocument(selectedFile.value, (progress) => {
      uploadProgress.value = progress
    })

    uploadProgress.value = 100
    feedbackType.value = 'success'
    feedback.value = response.mensagem ?? 'Documento enviado com sucesso.'
    selectedFile.value = null
  } catch (error) {
    feedbackType.value = 'error'
    feedback.value = error instanceof Error ? error.message : 'Falha ao enviar documento'
  } finally {
    isLoading.value = false
  }
}
</script>

<template>
  <div class="app-container app-container-bar">
    <section>
      <h1 class="app-title">CONFIGURAR DOCUMENTOS</h1>
      <h2 class="app-subtitle sub">Insira os documentos a serem inseridos no sistema</h2>
    </section>

    <section class="options">
      <section class="options-area">
        <section class="options-card">
          <img id="cloud" class="img-icon" src="../assets/cloud.png" alt="">

          <section class="options-content">
            <div>
              <h1>CONFIGURAÇÃO POR DOCUMENTO</h1>
              <h2>Selecione ou arraste um arquivo aqui para enviá-lo</h2>
            </div>
            <input type="file" name="file" class="options-input" @change="handleFileChange">
            <p class="sub">{{ selectedFileName }}</p>

            <div v-if="isLoading || uploadProgress > 0" class="progress-area" aria-live="polite">
              <div class="progress-track" role="progressbar" :aria-valuenow="uploadProgress" aria-valuemin="0" aria-valuemax="100">
                <div class="progress-fill" :style="{ width: `${uploadProgress}%` }"></div>
              </div>
              <p class="progress-label">{{ isLoading ? 'Enviando...' : 'Concluído' }} {{ uploadProgress }}%</p>
            </div>
          </section>
        </section>

        <button class="options-btn" :disabled="isLoading" @click.prevent="handleUpload">{{ isLoading ? '...' : '>' }}</button>
      </section>

      <h1>------------ OU ------------</h1>

      <section class="options-area">
        <section class="options-card">
          <img id="link" src="../assets/link.png" alt="">

          <section class="options-content">
            <h1>CONFIGURAÇÃO POR LINK</h1>
            <h2>Arraste ou insira abaixo o link para enviá-lo</h2>
            <input type="text" class="options-link" placeholder="Insira o link aqui" disabled>
          </section>
        </section>

        <button class="options-btn" disabled>></button>
      </section>
    </section>

    <div v-if="feedback" class="feedback-overlay" role="presentation">
      <div
        class="feedback-modal"
        :class="feedbackType === 'error' ? 'feedback-modal--error' : 'feedback-modal--success'"
        role="dialog"
        aria-modal="true"
        aria-live="polite"
      >
        <p class="feedback-title">
          {{ feedbackType === 'error' ? 'Erro ao enviar' : 'Envio concluído' }}
        </p>
        <p class="feedback-message">{{ feedback }}</p>
        <div class="feedback-actions">
          <button type="button" class="feedback-button" @click="closeFeedback">OK</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped src="../assets/docs.css"></style>
<style scoped src="../assets/register.css"></style>
