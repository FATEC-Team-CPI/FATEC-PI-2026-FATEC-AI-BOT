<script setup lang="ts"></script>

<template>
  <router-link to="/menu" tag="button" class="seta"><img src="../assets/seta.png" alt=""></router-link>
  <div class="app-container">
    <h1 class="app-title">RECUPERAR SENHA</h1>
    <h2 class="app-subtitle">Digite seu email para continuar</h2>

    <div class="app-form">
      <input class="input" type="email" name="email" placeholder="Email">
      <router-link to="/login" class="form-btn" tag="button">Enviar</router-link>
    </div>
    <img class="cps-fatec-logo" src="../assets/image 4.png" alt="">
  </div>
</template>

<style scoped src="../assets/menu.css"></style>
