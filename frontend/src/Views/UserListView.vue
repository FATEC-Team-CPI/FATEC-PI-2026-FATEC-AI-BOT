<script setup lang="ts">
type User = {
  id: number
  nome: string
  email: string
  perfil: string
}

const users: User[] = [
  {
    id: 1,
    nome: 'Ana Silva',
    email: 'ana.silva@fatec.sp.gov.br',
    perfil: 'Administrador',
  },
  {
    id: 2,
    nome: 'Bruno Santos',
    email: 'bruno.santos@fatec.sp.gov.br',
    perfil: 'Professor',
  },
  {
    id: 3,
    nome: 'Carla Oliveira',
    email: 'carla.oliveira@fatec.sp.gov.br',
    perfil: 'Aluno',
  },
  {
    id: 4,
    nome: 'Diego Almeida',
    email: 'diego.almeida@fatec.sp.gov.br',
    perfil: 'Aluno',
  },
  {
    id: 5,
    nome: 'Maria Souza',
    email: 'maria.souza@fatec.sp.gov.br',
    perfil: 'Aluno',
  },
]
</script>

<template>
  <router-link to="/admin" tag="button" class="seta"><img src="../assets/seta.png" alt=""></router-link>

  <div class="app-container app-container-bar">
    <section>
      <h1 class="app-title">USUARIOS</h1>
      <h2 class="app-subtitle sub">Consulte os usuarios cadastrados no sistema</h2>
    </section>

    <section class="users-list">
      <table v-if="users.length" class="users-table">
        <thead>
          <tr>
            <th>Nome</th>
            <th>Email</th>
            <th>Perfil</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in users" :key="user.id">
            <td>{{ user.nome }}</td>
            <td>{{ user.email }}</td>
            <td>{{ user.perfil }}</td>
          </tr>
        </tbody>
      </table>

      <div v-else class="users-empty">
        <h3>Nenhum usuario cadastrado</h3>
        <p>Quando houver usuarios cadastrados, eles serao exibidos nesta lista.</p>
      </div>
    </section>
  </div>
</template>

<style scoped src="../assets/users.css"></style>
