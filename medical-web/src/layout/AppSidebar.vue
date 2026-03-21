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
      background-color="transparent"
      text-color="rgba(255,255,255,0.9)"
      active-text-color="#ffd04b"
      popper-class="sidebar-menu-popup"
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
import { useRoute } from 'vue-router'
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

// 只展开包含当前路由的子菜单，减少 DOM 和动画负担，缓解顿挫
const route = useRoute()
const defaultOpeneds = computed(() => {
  const path = route.path
  const item = menuItems.value.find((m) => m.children?.some((c) => path.startsWith(c.url)))
  return item ? [item.index] : []
})
</script>

<style scoped>
.sidebar-header {
  height: 70px;
  min-height: 70px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.sidebar-header i {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 1.75rem;
}
</style>
