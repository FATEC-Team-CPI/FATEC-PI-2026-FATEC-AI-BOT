<script setup lang="ts"></script>

<template>
  <div class="app-container">
    <h1 class="app-title">AUTENTICAR-SE</h1>
    <h2 class="app-subtitle">Escolha uma opção para continuar</h2>

    <div class="app-form">
      <router-link to="/register" class="form-btn" tag="button">Registrar</router-link>
      <router-link to="/login" class="form-btn" tag="button">Entrar</router-link>
    </div>
    <img class="cps-fatec-logo" src="../assets/image 4.png" alt="">
  </div>
</template>

<style scoped src="../assets/menu.css"></style>
