<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">在线考试平台</div>
      <el-menu :default-active="$route.path" router background-color="#1f2d3d"
               text-color="#bfcbd9" active-text-color="#409eff">
        <template v-if="role !== 'STUDENT'">
          <el-menu-item index="/question"><el-icon><Collection/></el-icon><span>题库管理</span></el-menu-item>
          <el-menu-item index="/paper"><el-icon><Document/></el-icon><span>试卷与组卷</span></el-menu-item>
          <el-menu-item index="/exam/manage"><el-icon><Calendar/></el-icon><span>考试管理</span></el-menu-item>
          <el-menu-item index="/grade"><el-icon><EditPen/></el-icon><span>阅卷批改</span></el-menu-item>
          <el-menu-item index="/stat"><el-icon><DataAnalysis/></el-icon><span>成绩统计</span></el-menu-item>
        </template>
        <el-menu-item index="/exam/list"><el-icon><Tickets/></el-icon><span>我的考试</span></el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div>{{ $route.meta.title || '' }}</div>
        <div class="user">
          <span>{{ store.realName }}（{{ roleText }}）</span>
          <el-button link type="primary" @click="logout">退出登录</el-button>
        </div>
      </el-header>
      <el-main class="main"><router-view/></el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/user'

const router = useRouter()
const store = useUserStore()
const role = computed(() => store.role)
const roleText = computed(() => ({ ADMIN: '管理员', TEACHER: '教师', STUDENT: '学生' }[store.role]))
const logout = () => { store.logout(); router.push('/login') }
</script>

<style scoped>
.layout { height: 100vh; }
.aside { background: #1f2d3d; }
.logo { height: 60px; line-height: 60px; text-align: center; color: #fff; font-weight: 700; font-size: 16px; }
.aside :deep(.el-menu) { border-right: none; }
.header {
  display: flex; justify-content: space-between; align-items: center;
  background: #fff; border-bottom: 1px solid #eee; font-weight: 600;
}
.user { font-weight: 400; font-size: 14px; color: #666; display: flex; gap: 12px; align-items: center; }
.main { background: #f5f6fa; }
</style>
