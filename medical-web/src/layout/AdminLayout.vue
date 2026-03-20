<template>
  <el-container class="layout-container" direction="horizontal">
    <app-sidebar :sidebar-collapsed="sidebarCollapsed" />
    <el-container class="main-wrapper" direction="vertical">
      <app-header :sidebar-collapsed="sidebarCollapsed" @toggle-sidebar="toggleSidebar" />
      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <component :is="Component" />
        </router-view>
      </el-main>
      <app-footer />
    </el-container>
  </el-container>
</template>

<style scoped>
.layout-container {
  height: 100vh;
  overflow: hidden;
  display: flex;
  flex-direction: row;
}
.main-wrapper {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.main-content {
  flex: 1;
  overflow-y: auto;
}
</style>

<script setup>
import { ref } from 'vue'
import AppSidebar from './AppSidebar.vue'
import AppHeader from './AppHeader.vue'
import AppFooter from './AppFooter.vue'

const sidebarCollapsed = ref(false)
const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value
}
</script>
