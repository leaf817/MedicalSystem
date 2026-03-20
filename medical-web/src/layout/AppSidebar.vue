<template>
  <el-aside :class="{ collapsed: sidebarCollapsed }">
    <div class="sidebar-header">
      <i class="fa-solid fa-hospital fa-2xl" style="color: #ffd04b"></i>
    </div>
    <el-menu
      :default-active="$route.path"
      :default-openeds="defaultOpeneds"
      :collapse="sidebarCollapsed"
      router
      class="el-menu-vertical-demo"
      background-color="#344a5f"
      text-color="#fff"
      active-text-color="#ffd04b"
    >
      <template v-for="item in menuItems" :key="item.index">
        <el-sub-menu v-if="item.children?.length" :index="item.index">
          <template #title>
            <i :class="item.icon"></i>
            <span>{{ item.title }}</span>
          </template>
          <el-menu-item
            v-for="child in item.children"
            :key="child.url"
            :index="child.url"
          >
            <i :class="child.icon"></i>
            <span>{{ child.label }}</span>
          </el-menu-item>
        </el-sub-menu>
        <el-menu-item v-else :index="item.url">
          <i :class="item.icon"></i>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </template>
    </el-menu>
  </el-aside>
</template>

<script setup>
import { computed } from 'vue'
import { getMenuByRole } from '@/config/menu-config'

const props = defineProps({
  sidebarCollapsed: { type: Boolean, default: false }
})

const userInfo = computed(() => {
  try {
    return JSON.parse(sessionStorage.getItem('userInfo') || '{}')
  } catch {
    return {}
  }
})

const menuItems = computed(() => getMenuByRole(userInfo.value?.roles))

const defaultOpeneds = computed(() => menuItems.value.map((item) => item.index))
</script>

<style scoped>
.sidebar-header {
  padding: 20px;
  text-align: center;
}
.sidebar-header i {
  display: inline-block;
}
</style>
