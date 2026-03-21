<template>
  <div class="layout-bg"></div>
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
.layout-bg {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: url('/images/login%20background.jpg') center center / cover no-repeat;
  z-index: 0;
}

.layout-container {
  position: relative;
  z-index: 1;
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
