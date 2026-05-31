<script setup lang="ts"></script>

<template>
  <div class="app-container app-container-bar">
    <section>
      <h1 class="app-title">ARQUIVOS</h1>
      <h2 class="app-subtitle sub">Lista de arquivos enviados</h2>
    </section>
    <section class="file-area">
        <section class="file-list">
            <section class="file">
                <section class="file-info">
                    <img id="file" src="../assets/file.png" alt="">
                    <p id="filename">Nomedoarquivo.txt</p>
                    <p>Tipo: Arquivo txt</p>
                    <p>Status: Pendente</p>
                    <p>Data: 26/05/2026</p>
                    <p>Hora: 21:25:51</p>
                </section>
                    <button class="trash-btn"><img src="../assets/trash.png" alt=""/></button>
            </section>
            <section class="file">
                <section class="file-info">
                    <img id="file" src="../assets/file.png" alt="">
                    <p id="filename">Nomedoarquivo.txt</p>
                    <p>Tipo: Arquivo txt</p>
                    <p>Status: Pendente</p>
                    <p>Data: 26/05/2026</p>
                    <p>Hora: 21:25:51</p>
                </section>
                    <button class="trash-btn"><img src="../assets/trash.png" alt=""/></button>
            </section>
            <section class="file">
                <section class="file-info">
                    <img id="file" src="../assets/file.png" alt="">
                    <p id="filename">Nomedoarquivo.txt</p>
                    <p>Tipo: Arquivo txt</p>
                    <p>Status: Pendente</p>
                    <p>Data: 26/05/2026</p>
                    <p>Hora: 21:25:51</p>
                </section>
                    <button class="trash-btn"><img src="../assets/trash.png" alt=""/></button>
            </section>
            <section class="file">
                <section class="file-info">
                    <img id="file" src="../assets/file.png" alt="">
                    <p id="filename">Nomedoarquivo.txt</p>
                    <p>Tipo: Arquivo txt</p>
                    <p>Status: Pendente</p>
                    <p>Data: 26/05/2026</p>
                    <p>Hora: 21:25:51</p>
                </section>
                    <button class="trash-btn"><img src="../assets/trash.png" alt=""/></button>
            </section>
            <section class="file">
                <section class="file-info">
                    <img id="file" src="../assets/file.png" alt="">
                    <p id="filename">Nomedoarquivo.txt</p>
                    <p>Tipo: Arquivo txt</p>
                    <p>Status: Pendente</p>
                    <p>Data: 26/05/2026</p>
                    <p>Hora: 21:25:51</p>
                </section>
                    <button class="trash-btn"><img src="../assets/trash.png" alt=""/></button>
            </section>
            <section class="file">
                <section class="file-info">
                    <img id="file" src="../assets/file.png" alt="">
                    <p id="filename">Nomedoarquivo.txt</p>
                    <p>Tipo: Arquivo txt</p>
                    <p>Status: Pendente</p>
                    <p>Data: 26/05/2026</p>
                    <p>Hora: 21:25:51</p>
                </section>
                    <button class="trash-btn"><img src="../assets/trash.png" alt=""/></button>
            </section>

            <section>
                <p class="end-scroll">-- Sem mais resultados --</p>
            </section>

        </section>
    </section>
<script setup lang="ts">
import { onMounted, ref } from 'vue'

type UploadHistoryItem = {
    fileName: string
    key: string | null
    mensagem: string
    sucesso: boolean
    uploadedAt: string
}

const uploads = ref<UploadHistoryItem[]>([])

function loadUploads(): void {
    try {
        uploads.value = JSON.parse(localStorage.getItem('fateco_upload_history') ?? '[]') as UploadHistoryItem[]
    } catch {
        uploads.value = []
    }
}

onMounted(() => {
    loadUploads()
})
</script>

<template>
    <router-link to="/admin" tag="button" class="seta"><img src="../assets/seta.png" alt=""></router-link>
    <div class="app-container app-container-bar">
        <section>
            <h1 class="app-title">ARQUIVOS</h1>
            <h2 class="app-subtitle sub">Histórico local dos arquivos enviados ao backend</h2>
        </section>

        <section class="file-area">
            <section class="file-list">
                <section v-for="upload in uploads" :key="`${upload.fileName}-${upload.uploadedAt}`" class="file">
                    <section class="file-info">
                        <img id="file" src="../assets/file.png" alt="">
                        <p id="filename">{{ upload.fileName }}</p>
                        <p>Chave: {{ upload.key ?? '-' }}</p>
                        <p>Status: {{ upload.sucesso ? 'Concluído' : 'Falhou' }}</p>
                        <p>Mensagem: {{ upload.mensagem }}</p>
                        <p>Data: {{ new Date(upload.uploadedAt).toLocaleDateString('pt-BR') }}</p>
                        <p>Hora: {{ new Date(upload.uploadedAt).toLocaleTimeString('pt-BR') }}</p>
                    </section>
                </section>

                <section v-if="!uploads.length">
                    <p class="end-scroll">Nenhum upload registrado ainda.</p>
                </section>

                <section>
                    <p class="end-scroll">-- Sem mais resultados --</p>
                </section>
            </section>
        </section>
    </div>
</template>
