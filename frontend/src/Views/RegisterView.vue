<script setup lang="ts"></script>

<template>
  <router-link to="/menu" tag="button" class="seta"><img src="../assets/seta.png" alt=""></router-link>
  <div class="app-container app-container-bar">
    <section>
      <h1 class="app-title">CADASTRO</h1>
      <h2 class="app-subtitle sub">Insira as informações para criar uma nova conta</h2>
    </section>

    <div class="app-form">
      <div class="form-group">
        <label>Campo 1: *</label>
        <input class="input" type="text" placeholder="Digite..." />
      </div>

      <div class="form-group">
        <label>Campo 2: *</label>
        <input class="input" type="text" placeholder="Digite..." />
      </div>

      <div class="form-group">
        <label>Campo 3: *</label>
        <input class="input" type="text" placeholder="Digite..." />
      </div>

      <div class="form-group">
        <label>Campo 4: *</label>
        <input  class="input"type="text" placeholder="Digite..." />
      </div>

      <div class="form-group">
        <label>Campo 5: *</label>
        <input class="input" type="text" placeholder="Digite..." />
      </div>

      <div class="form-group">
        <label>Campo 6: *</label>
        <input class="input" type="text" placeholder="Digite..." />
      </div>

      <div class="form-group">
        <label>Campo 7: *</label>
        <input class="input" type="text" placeholder="Digite..." />
      </div>

      <div class="form-group">
        <label>Campo 8: *</label>
        <input class="input" type="text" placeholder="Digite..." />
      </div>
    </div>

    <button class="form-btn">Registrar</button>
  </div>
</template>

<style scoped src="../assets/register.css"></style>
