<template>
  <el-card>
    <el-table :data="exams" border stripe>
      <el-table-column prop="examName" label="考试名称" min-width="200"/>
      <el-table-column prop="paperName" label="试卷" min-width="160"/>
      <el-table-column prop="totalScore" label="满分" width="80"/>
      <el-table-column prop="startTime" label="开始" width="170"/>
      <el-table-column prop="endTime" label="结束" width="170"/>
      <el-table-column label="我的状态" width="120">
        <template #default="{row}">
          <el-tag v-if="row.recordStatus==null" type="info">未进入</el-tag>
          <el-tag v-else-if="[1,3].includes(row.recordStatus)" type="warning">
            {{row.recordStatus===3?'异常中断，可续考':'答题中'}}
          </el-tag>
          <el-tag v-else-if="[2,4,5].includes(row.recordStatus)" type="primary">已交卷待批改</el-tag>
          <el-tag v-else-if="row.recordStatus===6" type="success">已出分</el-tag>
          <el-tag v-else-if="row.recordStatus===7" type="success">成绩已发布</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{row}">
          <el-button v-if="canEnter(row)" type="primary" size="small" @click="enter(row)">
            {{row.recordStatus==null?'进入考试':'继续考试'}}
          </el-button>
          <el-button v-if="row.recordStatus>=6" size="small" @click="result(row)">查看成绩</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { studentExams } from '../../utils/api'

const router = useRouter()
const exams = ref([])
onMounted(async () => { exams.value = await studentExams() })

const canEnter = (row) => {
  const now = new Date(row.serverNow || Date.now())
  const start = new Date(row.startTime), end = new Date(row.endTime)
  return now >= start && now <= end && ![2, 4, 6, 7].includes(row.recordStatus)
}
const enter = (row) => router.push(`/exam/room/${row.examId}`)
const result = (row) => router.push(`/exam/result/${row.examId}`)
</script>
