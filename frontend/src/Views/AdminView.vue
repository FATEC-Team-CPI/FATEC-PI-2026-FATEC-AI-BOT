<script setup lang="ts"></script>

<template>
  <div class="app-container app-container-screen">
    <h1 class="app-title adm">MENU ADMINISTRATIVO</h1>

    <div class="app-form">
      <router-link to="docs" class="adm-btn" tag="button">
        <section class="adm-content">
        <img class="icon-cog" src="../assets/Vector.png" alt="">
        <p>Adicionar Documentos</p>
      </section>
      </router-link>

      <router-link to="register" class="adm-btn" tag="button">
        <section class="adm-content">
          <img class="icon-cog" src="../assets/Vector.png" alt="">
          <p>Cadastrar um Novo Usuário</p>
        </section>
      </router-link>
    </div>
  </div>
</template>

<style scoped src="../assets/admin.css"></style>
