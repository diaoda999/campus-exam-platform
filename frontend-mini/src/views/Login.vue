<template>
  <div class="login-wrap">
    <el-card class="login-card">
      <div class="title">校内在线考试平台</div>
      <div class="sub">Spring Boot · MyBatis-Plus · Redis · RabbitMQ</div>
      <el-form :model="form" @keyup.enter="doLogin">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" size="large" :prefix-icon="User"/>
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" size="large"
                    :prefix-icon="Lock" show-password/>
        </el-form-item>
        <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="doLogin">
          登 录
        </el-button>
      </el-form>
      <div class="tips">
        演示账号（密码均为 123456）：teacher1 教师 / s1~s4 学生 / admin 管理员
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { login } from '../utils/api'
import { useUserStore } from '../store/user'

const router = useRouter()
const store = useUserStore()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

const doLogin = async () => {
  if (!form.username || !form.password) return ElMessage.warning('请输入账号密码')
  loading.value = true
  try {
    const info = await login(form)
    store.setLogin(info)
    ElMessage.success('登录成功')
    router.push(info.role === 'STUDENT' ? '/exam/list' : '/exam/manage')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrap {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1e3c72 0%, #2a5298 100%);
}
.login-card { width: 400px; padding: 20px 16px; border-radius: 10px; }
.title { font-size: 22px; font-weight: 700; text-align: center; color: #1e3c72; }
.sub { font-size: 12px; color: #888; text-align: center; margin: 8px 0 24px; }
.tips { margin-top: 14px; font-size: 12px; color: #999; line-height: 1.6; }
</style>
