<script setup lang="ts"></script>

<template>
  <div class="app-container app-container-bar">
    <section>
      <h1 class="app-title">CONFIGURAR DOCUMENTOS</h1>
      <h2 class="app-subtitle sub">Insira os documentos a serem inseridos no sistema</h2>
    </section>
    

    <section class="options">

      <section class="options-area">

        <section class=" options-card">
          
          <img id="cloud" class="img-icon" src="../assets/cloud.png" alt="">

          <section class="options-content">
              <div>
              <h1 class="">CONFIGURAÇÃO POR DOCUMENTO</h1>
              <h2 class="" >Selecione ou arraste um arquivo aqui para enviá-lo</h2>
              </div>
              <input type="file" name="" id="" class="options-input">
          </section>
        </section> 
        <button class="options-btn">></button>
      </section>
      
      <h1 class="">------------ OU ------------</h1>

 <section class="options">

 <section class="options-area">

   <section class="options-card">

     <img id="link" src="../assets/link.png" alt="">

     <section class="options-content">
       <h1 class="">CONFIGURAÇÃO POR LINK</h1>
       <h2 class="" >Arraste ou insira abaixo o link para enviá-lo</h2>
       <input type="text" class="options-link" placeholder="Insira o link aqui">
      </section>

    </section> 
    <button class="options-btn">></button>
  </section>
</section>
</section>
</div>
</template>

<style scoped src="../assets/docs.css"></style>
