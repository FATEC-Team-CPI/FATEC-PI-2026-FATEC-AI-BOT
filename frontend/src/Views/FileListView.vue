<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { listDocuments, type DocumentListItem } from '../services/backendApi'

const documents = ref<DocumentListItem[]>([])
const isLoading = ref(false)
const feedback = ref('')
const feedbackType = ref<'success' | 'error' | ''>('')

async function loadDocuments(): Promise<void> {
  isLoading.value = true
  feedback.value = ''
  feedbackType.value = ''

  try {
    documents.value = await listDocuments()
    feedbackType.value = 'success'
    feedback.value = documents.value.length
      ? `${documents.value.length} documento(s) encontrado(s).`
      : 'Nenhum documento encontrado.'
  } catch (error) {
    feedbackType.value = 'error'
    feedback.value = error instanceof Error ? error.message : 'Falha ao listar documentos'
    documents.value = []
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  void loadDocuments()
})
</script>

<template>
  <router-link to="/admin" tag="button" class="seta"><img src="../assets/seta.png" alt=""></router-link>

  <div class="app-container app-container-bar">
    <section>
      <h1 class="app-title">DOCUMENTOS</h1>
      <h2 class="app-subtitle sub">Listagem dos documentos salvos no DynamoDB</h2>
    </section>

    <section class="list-actions">
      <router-link to="/docs" class="form-btn list-action-btn">Subir documentos</router-link>
    </section>

    <section class="file-area">
      <section v-if="isLoading" class="file-list">
        <p class="end-scroll">Carregando documentos...</p>
      </section>

      <section v-else class="file-list">
        <section v-for="document in documents" :key="`${document.pk}-${document.sk}`" class="file">
          <section class="file-info">
            <img id="file" src="../assets/file.png" alt="">
            <div class="doc-content">
              <p class="doc-name">{{ document.sk ?? '-' }}</p>
              <p class="doc-pk">PK: {{ document.pk ?? '-' }}</p>
            </div>
          </section>
          <span class="doc-status" :class="`doc-status--${(document.status ?? 'unknown').toLowerCase()}`">
            {{ document.status ?? 'UNKNOWN' }}
          </span>
        </section>

        <section v-if="!documents.length">
          <p class="end-scroll">Nenhum documento encontrado.</p>
        </section>

        <section>
          <p class="end-scroll">-- Sem mais resultados --</p>
        </section>
      </section>
    </section>

    <div v-if="feedback" class="users-empty" :class="feedbackType === 'error' ? 'error' : 'success'">
      <p>{{ feedback }}</p>
    </div>
  </div>
</template>

<style scoped src="../assets/docs.css"></style>
