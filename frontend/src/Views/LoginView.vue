<script setup lang="ts"></script>

<template>
    <section class="app">
      <router-link to="/menu" tag="button" class="seta"><img src="../assets/seta.png" alt=""></router-link>
    
      <section class ="app-container app-container-stripe">
        <form class ="input-area">
        <section >
          <h1 class="app-title">LOGIN</h1>
          <h2 class="app-subtitle">Insira suas credenciais para entrar no sistema</h2>
        </section>
        
          <input class="input" type="email" name="email" placeholder="Email">
          <input class="input" type="password" name="senha" placeholder="Senha">
          <router-link to="/password-reset" tag="link" class="link">Esqueceu a senha?</router-link>
          <button class="btn">Entrar</button>
        </form>
        
      </section>
      <img class="logo-fatec" src="../assets/logo.png" alt="Fatec e Centro Paula Souza">
    </section>
</template>

<style scoped src="../assets/login.css"></style>
