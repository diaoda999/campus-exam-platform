<template>
  <div>
    <el-card>
      <el-form inline>
        <el-form-item label="选择考试">
          <el-select v-model="examId" placeholder="请选择考试" filterable style="width:320px" @change="loadTodo">
            <el-option v-for="e in exams" :key="e.id"
                       :label="`${e.id}. ${e.examName}（${e.statusDesc}）`" :value="e.id"/>
          </el-select>
        </el-form-item>
        <el-button type="primary" @click="loadTodo">刷新</el-button>
        <el-button v-if="examId" type="success" @click="publish">发布本场成绩</el-button>
      </el-form>

      <el-table :data="todoList" border stripe>
        <el-table-column prop="userId" label="学号" width="90"/>
        <el-table-column prop="studentName" label="姓名" width="120"/>
        <el-table-column label="状态" width="120">
          <template #default="{row}">
            <el-tag :type="row.recordStatus===5?'warning':(row.recordStatus===7?'success':'info')">
              {{row.recordStatusDesc}}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="objectiveScore" label="客观分" width="90"/>
        <el-table-column prop="subjectiveScore" label="主观分" width="90"/>
        <el-table-column prop="totalScore" label="总分" width="90"/>
        <el-table-column label="待批主观题" width="110">
          <template #default="{row}">
            <el-badge :value="row.remainSubjectiveCount" :hidden="!row.remainSubjectiveCount" type="danger">
              <el-tag size="small">{{row.remainSubjectiveCount}} 题</el-tag>
            </el-badge>
          </template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="{row}">
            <el-button link type="primary" @click="openGrade(row)">批改</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-drawer v-model="gradeDialog" size="62%" :title="`批改 - ${detail?.studentName}`">
      <template v-if="detail">
        <el-descriptions :column="3" border style="margin-bottom:12px">
          <el-descriptions-item label="客观题得分">{{detail.objectiveScore}}</el-descriptions-item>
        </el-descriptions>
        <div v-for="(item,idx) in detail.items" :key="item.questionId" class="grade-item">
          <p><b>{{idx+1}}.（满分{{item.fullScore}}分 · {{item.typeDesc}}）</b>{{item.stem}}</p>
          <div v-if="item.options" style="padding-left:14px;color:#555">
            <p v-for="opt in parse(item.options)" :key="opt.key">{{opt.key}}. {{opt.text}}</p>
          </div>
          <p class="std">标准答案：{{item.standardAnswer}}</p>
          <p class="stu">学生作答：{{item.content || '（未作答）'}}</p>
          <template v-if="!item.objective">
            <el-input-number v-model="item.score" :min="0" :max="item.fullScore" :precision="1" size="small"/>
            <el-input v-model="item.comment" size="small" placeholder="评语（可选）" style="width:360px;margin-left:12px"/>
            <el-button size="small" type="primary" @click="saveScore(item)">保存该题</el-button>
          </template>
          <template v-else>
            <el-tag size="small" type="success">系统判分：{{item.score}}</el-tag>
          </template>
        </div>
        <div style="margin:16px 0">
          <el-button type="success" size="large" @click="finish">完成批改并汇总成绩</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { examPage, gradeTodo, gradeDetail, gradeScore, gradeFinish, gradePublish } from '../../utils/api'

const exams = ref([])
const examId = ref(null)
const todoList = ref([])
const gradeDialog = ref(false)
const detail = ref(null)
const currentUser = ref(null)

onMounted(async () => { exams.value = (await examPage({ pageNum: 1, pageSize: 100 })).records })
const loadTodo = async () => {
  if (!examId.value) return
  todoList.value = await gradeTodo(examId.value)
}
const openGrade = async (row) => {
  currentUser.value = row.userId
  detail.value = await gradeDetail(examId.value, row.userId)
  gradeDialog.value = true
}
const saveScore = async (item) => {
  await gradeScore({
    examId: examId.value, userId: currentUser.value,
    questionId: item.questionId, score: item.score, comment: item.comment
  })
  ElMessage.success('已保存')
}
const finish = async () => {
  await gradeFinish({ examId: examId.value, userId: currentUser.value })
  ElMessage.success('该考生成绩已异步汇总'); gradeDialog.value = false; loadTodo()
}
const publish = async () => {
  await ElMessageBox.confirm('确认发布本场考试成绩？发布后学生可查看，并异步刷新统计。', '提示', { type: 'warning' })
  await gradePublish(examId.value)
  ElMessage.success('成绩已发布'); loadTodo()
}
const parse = (s) => { try { return JSON.parse(s) } catch { return [] } }
</script>

<style scoped>
.grade-item { border-bottom: 1px dashed #ddd; padding: 10px 0; }
.std { color: #67c23a; font-size: 13px; margin: 4px 0; }
.stu { color: #e6a23c; font-size: 13px; margin: 4px 0; }
</style>
