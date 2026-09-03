<template>
  <el-card style="max-width:720px;margin:0 auto">
    <template #header>我的成绩单</template>
    <div v-loading="loading">
      <el-result v-if="record"
                 :icon="record.status===7?'success':'info'"
                 :title="titleText"
                 :sub-title="subText">
        <template #extra>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="客观题得分">{{record.objectiveScore}}</el-descriptions-item>
            <el-descriptions-item label="主观题得分">{{record.subjectiveScore}}</el-descriptions-item>
            <el-descriptions-item label="总分">
              <b style="color:#f56c6c;font-size:20px">{{record.totalScore ?? '批改中'}}</b>
            </el-descriptions-item>
            <el-descriptions-item label="违规次数">{{record.violationCount}}</el-descriptions-item>
            <el-descriptions-item label="交卷时间">{{record.submitTime}}</el-descriptions-item>
          </el-descriptions>
          <div style="margin-top:16px">
            <el-button @click="$router.push('/exam/list')">返回考试列表</el-button>
          </div>
        </template>
      </el-result>
    </div>
  </el-card>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { myResult } from '../../utils/api'

const route = useRoute()
const loading = ref(true)
const record = ref(null)
onMounted(async () => {
  try { record.value = await myResult(Number(route.params.examId)) }
  finally { loading.value = false }
})
const titleText = computed(() => {
  if (!record.value) return ''
  return { 5: '已交卷，等待主观题批改', 6: '阅卷完成，等待教师发布', 7: '成绩已发布' }[record.value.status] || '考试记录'
})
const subText = computed(() => record.value?.status === 7 ? '可查看最终成绩' : '客观题已由系统即时判分')
</script>
