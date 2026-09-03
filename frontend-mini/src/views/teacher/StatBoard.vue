<template>
  <div>
    <el-card>
      <el-form inline>
        <el-form-item label="选择考试">
          <el-select v-model="examId" filterable placeholder="请选择已发布考试" style="width:320px" @change="load">
            <el-option v-for="e in exams" :key="e.id" :label="`${e.id}. ${e.examName}`" :value="e.id"/>
          </el-select>
        </el-form-item>
        <el-button type="primary" @click="load">查询</el-button>
        <el-tag v-if="stat" :type="stat.fromCache?'info':'success'" style="margin-left:12px">
          {{stat.fromCache?'命中 Redis 缓存':'实时计算并回写缓存'}}
        </el-tag>
      </el-form>

      <template v-if="stat">
        <el-row :gutter="16">
          <el-col :span="4" v-for="card in cards" :key="card.label">
            <el-statistic :title="card.label" :value="card.value" :precision="card.precision||0"/>
          </el-col>
        </el-row>

        <el-divider content-position="left">分数段分布（按满分比例）</el-divider>
        <div v-for="(seg,i) in stat.scoreSegments" :key="i" class="seg-row">
          <span class="seg-label">{{segLabels[i]}}</span>
          <el-progress :percentage="percent(seg)" :format="()=>seg+'人'" style="flex:1"/>
        </div>

        <el-divider content-position="left">每题得分率</el-divider>
        <el-table :data="questionRates" border>
          <el-table-column prop="questionId" label="题目ID" width="100"/>
          <el-table-column label="得分率">
            <template #default="{row}">
              <el-progress :percentage="Math.round(row.rate*100)"
                           :status="row.rate>=0.8?'success':(row.rate<0.6?'exception':'')"/>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { examPage, classStat } from '../../utils/api'

const exams = ref([])
const examId = ref(null)
const stat = ref(null)
const segLabels = ['<60%', '60-70%', '70-80%', '80-90%', '≥90%']

examPage({ pageNum: 1, pageSize: 100 }).then(d => exams.value = d.records)

const load = async () => {
  if (!examId.value) return
  stat.value = await classStat(examId.value)
}
const cards = computed(() => {
  if (!stat.value) return []
  const s = stat.value
  return [
    { label: '参考人数', value: s.attendCount },
    { label: '平均分', value: s.avgScore, precision: 2 },
    { label: '最高分', value: s.maxScore, precision: 1 },
    { label: '最低分', value: s.minScore, precision: 1 },
    { label: '及格率', value: (s.passRate * 100).toFixed(2) + '%' }
  ]
})
const percent = (n) => {
  const total = stat.value.scoreSegments.reduce((a, b) => a + b, 0)
  return total ? Math.round(n / total * 100) : 0
}
const questionRates = computed(() => {
  if (!stat.value?.questionCorrectRate) return []
  return Object.entries(stat.value.questionCorrectRate).map(([questionId, rate]) => ({ questionId, rate: Number(rate) }))
})
</script>

<style scoped>
.seg-row { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; }
.seg-label { width: 80px; color: #666; }
</style>
