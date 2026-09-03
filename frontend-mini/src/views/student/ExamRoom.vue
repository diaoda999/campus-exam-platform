<template>
  <div class="room">
    <div class="room-header">
      <div class="exam-name">{{paper?.examName}}</div>
      <div class="timer" :class="{urgent: remain<300}">
        剩余时间：{{fmt(remain)}}
      </div>
      <el-button type="danger" @click="confirmSubmit">交卷</el-button>
    </div>

    <div class="room-body" v-loading="loading">
      <div class="question-area" v-if="paper">
        <div v-for="g in paper.groups" :key="g.questionType" class="group">
          <h3>{{g.typeDesc}}</h3>
          <div v-for="(q,idx) in g.items" :key="q.questionId" class="q-block" :id="'q-'+q.questionId">
            <p><b>{{idx+1}}.（{{q.score}}分）</b>{{q.stem}}</p>
            <template v-if="g.questionType===1">
              <el-radio-group v-model="answers[q.questionId]" @change="v=>onAnswer(q.questionId,v)">
                <el-radio v-for="opt in parse(q.options)" :key="opt.key" :value="opt.key" style="display:block">
                  {{opt.key}}. {{opt.text}}
                </el-radio>
              </el-radio-group>
            </template>
            <template v-else-if="g.questionType===2">
              <el-checkbox-group v-model="multi[q.questionId]" @change="v=>onAnswer(q.questionId,[...v].sort().join(''))">
                <el-checkbox v-for="opt in parse(q.options)" :key="opt.key" :value="opt.key" style="display:block">
                  {{opt.key}}. {{opt.text}}
                </el-checkbox>
              </el-checkbox-group>
            </template>
            <template v-else-if="g.questionType===3">
              <el-radio-group v-model="answers[q.questionId]" @change="v=>onAnswer(q.questionId,v)">
                <el-radio value="TRUE">正确</el-radio>
                <el-radio value="FALSE">错误</el-radio>
              </el-radio-group>
            </template>
            <template v-else-if="g.questionType===4">
              <el-input v-model="answers[q.questionId]" placeholder="请输入答案"
                        @input="v=>onAnswer(q.questionId,v)" style="max-width:420px"/>
            </template>
            <template v-else>
              <el-input v-model="answers[q.questionId]" type="textarea" :rows="4"
                        @input="v=>onAnswer(q.questionId,v)"/>
            </template>
          </div>
        </div>
      </div>

      <div class="nav-area" v-if="paper">
        <h4>答题卡</h4>
        <template v-for="g in paper.groups" :key="g.questionType">
          <p class="nav-group">{{g.typeDesc}}</p>
          <div class="nav-grid">
            <div v-for="(q,idx) in g.items" :key="q.questionId"
                 class="nav-cell" :class="{done: isAnswered(q.questionId)}"
                 @click="scrollTo(q.questionId)">{{idx+1}}</div>
          </div>
        </template>
        <p class="save-tip">{{saveTip}}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { enterExam, autoSave, heartbeat, reportViolation, submitExam } from '../../utils/api'
import { setExamToken } from '../../utils/request'

const route = useRoute()
const router = useRouter()
const examId = Number(route.params.examId)
const loading = ref(true)
const paper = ref(null)
const answers = reactive({})
const multi = reactive({})
const remain = ref(0)
const saveTip = ref('答案将自动保存')
let timer = null, heartbeatTimer = null, saveTimers = {}

const parse = (s) => { try { return JSON.parse(s) } catch { return [] } }

onMounted(async () => {
  try {
    const resume = await enterExam(examId)
    paper.value = resume.paper
    setExamToken(resume.paper.examToken)
    remain.value = resume.remainSeconds
    // 恢复 Redis 快照（断点续考）
    Object.entries(resume.answers || {}).forEach(([qid, val]) => {
      answers[qid] = val
      multi[qid] = val ? val.split('') : []
    })
    startTimers()
    document.addEventListener('visibilitychange', onVisibility)
  } finally {
    loading.value = false
  }
})

onBeforeUnmount(() => {
  clearInterval(timer); clearInterval(heartbeatTimer)
  document.removeEventListener('visibilitychange', onVisibility)
  setExamToken('')
})

const startTimers = () => {
  timer = setInterval(() => {
    remain.value--
    if (remain.value <= 0) { clearInterval(timer); doSubmit(true) }
  }, 1000)
  heartbeatTimer = setInterval(() => heartbeat(examId).catch(() => {}), 20000)
}

// 防抖 3 秒自动保存单题
const onAnswer = (qid, value) => {
  answers[qid] = value
  clearTimeout(saveTimers[qid])
  saveTimers[qid] = setTimeout(async () => {
    await autoSave({ examId, questionId: qid, content: value ?? '' })
    saveTip.value = `已自动保存 ${new Date().toLocaleTimeString()}`
  }, 3000)
}

const isAnswered = (qid) => {
  const v = answers[qid]
  return v !== undefined && v !== null && String(v).length > 0
}
const scrollTo = (qid) => document.getElementById('q-' + qid)?.scrollIntoView({ behavior: 'smooth' })

const onVisibility = async () => {
  if (document.hidden) {
    try {
      await reportViolation({ examId, type: 'SWITCH_TAB', detail: '离开考试页面' })
      ElMessage.warning('检测到切屏，违规已记录，多次切屏将强制交卷')
    } catch (e) { /* 强制交卷等错误由下次请求体现 */ }
  }
}

const fmt = (s) => {
  s = Math.max(0, s)
  const h = String(Math.floor(s / 3600)).padStart(2, '0')
  const m = String(Math.floor(s % 3600 / 60)).padStart(2, '0')
  const ss = String(s % 60).padStart(2, '0')
  return `${h}:${m}:${ss}`
}

const confirmSubmit = () => {
  ElMessageBox.confirm('确认交卷？交卷后不可修改。', '提示', { type: 'warning' })
    .then(() => doSubmit(false)).catch(() => {})
}

const doSubmit = async (auto) => {
  try {
    const res = await submitExam({ examId, answers })
    ElMessage.success(auto ? '考试时间到，已自动交卷' : '交卷成功')
    if (res.waitingGrade) ElMessage.info('客观题已即时出分，主观题等待教师批改')
    router.replace(`/exam/result/${examId}`)
  } catch (e) {
    ElMessage.error('交卷失败：' + (e.message || ''))
  }
}
</script>

<style scoped>
.room { position: fixed; inset: 0; z-index: 2000; background: #f5f6fa; display: flex; flex-direction: column; }
.room-header {
  height: 56px; background: #1e3c72; color: #fff; display: flex;
  align-items: center; justify-content: space-between; padding: 0 24px;
}
.exam-name { font-size: 17px; font-weight: 600; }
.timer { font-size: 20px; font-weight: 700; font-family: monospace; }
.timer.urgent { color: #ff8080; }
.room-body { flex: 1; display: flex; overflow: hidden; }
.question-area { flex: 1; overflow-y: auto; padding: 20px 32px; }
.group h3 { border-left: 4px solid #409eff; padding-left: 8px; }
.q-block { background: #fff; border-radius: 6px; padding: 14px 18px; margin-bottom: 14px; }
.nav-area { width: 260px; background: #fff; border-left: 1px solid #eee; padding: 16px; overflow-y: auto; }
.nav-group { font-size: 12px; color: #999; margin: 10px 0 6px; }
.nav-grid { display: grid; grid-template-columns: repeat(6,1fr); gap: 6px; }
.nav-cell {
  height: 28px; line-height: 28px; text-align: center; border: 1px solid #dcdfe6;
  border-radius: 4px; cursor: pointer; font-size: 13px;
}
.nav-cell.done { background: #67c23a; color: #fff; border-color: #67c23a; }
.save-tip { color: #999; font-size: 12px; margin-top: 16px; }
</style>
