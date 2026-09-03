<template>
  <div>
    <el-card>
      <el-form inline>
        <el-button type="success" @click="openAuto">智能组卷</el-button>
        <el-button @click="openManual">手动组卷</el-button>
        <el-button @click="load">刷新</el-button>
      </el-form>
      <el-table :data="page.records" border stripe>
        <el-table-column prop="id" label="ID" width="70"/>
        <el-table-column prop="name" label="试卷名称"/>
        <el-table-column prop="totalScore" label="总分" width="80"/>
        <el-table-column prop="suggestDuration" label="时长(分)" width="90"/>
        <el-table-column label="组卷方式" width="100">
          <template #default="{row}">
            <el-tag :type="row.genType==='AUTO'?'success':'info'">
              {{row.genType==='AUTO'?'智能':'手动'}}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{row}">
            <el-tag :type="row.status===1?'primary':'warning'">{{row.status===1?'正式':'草稿'}}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230">
          <template #default="{row}">
            <el-button link type="primary" @click="view(row.id)">预览</el-button>
            <el-button v-if="row.status===0" link type="success" @click="doPublish(row.id)">发布</el-button>
            <el-popconfirm title="删除试卷？" @confirm="remove(row.id)">
              <template #reference><el-button link type="danger">删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:12px" layout="total,prev,pager,next"
                     :total="page.total" :current-page="pageNum" :page-size="10"
                     @current-change="p=>{pageNum=p;load()}"/>
    </el-card>

    <!-- 智能组卷 -->
    <el-dialog v-model="autoDialog" title="智能组卷（贪心 + 回溯约束求解）" width="760px">
      <el-form label-width="120px">
        <el-form-item label="试卷名称"><el-input v-model="auto.name"/></el-form-item>
        <el-form-item label="建议时长(分钟)"><el-input-number v-model="auto.suggestDuration" :min="10" :max="300"/></el-form-item>
        <el-form-item label="期望平均难度">
          <el-slider v-model="targetDiff" :min="1" :max="5" :step="0.1" show-input style="max-width:420px"/>
        </el-form-item>
        <el-form-item label="题型数量与分值">
          <div style="width:100%">
            <div v-for="(g,i) in auto.groups" :key="i" style="display:flex;gap:8px;margin-bottom:8px">
              <el-select v-model="g.questionType" style="width:130px">
                <el-option v-for="t in types" :key="t.code" :label="t.desc" :value="t.code"/>
              </el-select>
              <el-input-number v-model="g.count" :min="1" :max="50" placeholder="数量"/>
              <el-input-number v-model="g.scorePer" :min="1" :max="100" placeholder="每题分"/>
              <el-button link type="danger" @click="auto.groups.splice(i,1)">移除</el-button>
            </div>
            <el-button size="small" @click="auto.groups.push({questionType:1,count:5,scorePer:4})">添加题型</el-button>
          </div>
        </el-form-item>
        <el-form-item label="知识点覆盖">
          <div style="width:100%">
            <div v-for="(c,i) in auto.coverage" :key="i" style="display:flex;gap:8px;margin-bottom:8px">
              <el-select v-model="c.knowledgeId" style="width:220px">
                <el-option v-for="k in knowledges" :key="k.id" :label="k.name" :value="k.id"/>
              </el-select>
              <span>至少</span>
              <el-input-number v-model="c.minCount" :min="1" :max="20"/>
              <span>题</span>
              <el-button link type="danger" @click="auto.coverage.splice(i,1)">移除</el-button>
            </div>
            <el-button size="small" @click="auto.coverage.push({knowledgeId:knowledges[0]?.id,minCount:2})">添加知识点约束</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="autoDialog=false">取消</el-button>
        <el-button type="primary" :loading="generating" @click="generate">开始组卷</el-button>
      </template>
    </el-dialog>

    <!-- 手动组卷选题 -->
    <el-dialog v-model="manualDialog" title="手动组卷" width="820px">
      <el-form inline>
        <el-form-item label="名称"><el-input v-model="manual.name" style="width:180px"/></el-form-item>
        <el-form-item label="时长"><el-input-number v-model="manual.suggestDuration" :min="10" :max="300"/></el-form-item>
        <el-form-item label="题型筛选">
          <el-select v-model="manualType" clearable style="width:120px" @change="loadBank">
            <el-option v-for="t in types" :key="t.code" :label="t.desc" :value="t.code"/>
          </el-select>
        </el-form-item>
        <el-form-item label="每题分值"><el-input-number v-model="manual.scorePer" :min="1" :max="100"/></el-form-item>
      </el-form>
      <el-table :data="bank" border max-height="360"
                @selection-change="rows=>manualSelected=rows" ref="bankTable">
        <el-table-column type="selection" width="45"/>
        <el-table-column prop="id" label="ID" width="70"/>
        <el-table-column prop="typeDesc" label="题型" width="90"/>
        <el-table-column prop="stem" label="题干" show-overflow-tooltip/>
        <el-table-column prop="difficulty" label="难度" width="80"/>
      </el-table>
      <template #footer>
        <el-button @click="manualDialog=false">取消</el-button>
        <el-button type="primary" @click="saveManual">保存草稿</el-button>
      </template>
    </el-dialog>

    <!-- 试卷详情抽屉 -->
    <el-drawer v-model="detailDialog" size="60%" :title="detail?.name">
      <template v-if="detail">
        <el-descriptions :column="3" border>
          <el-descriptions-item label="总分">{{detail.totalScore}}</el-descriptions-item>
          <el-descriptions-item label="时长">{{detail.suggestDuration}} 分钟</el-descriptions-item>
          <el-descriptions-item label="状态">{{detail.status===1?'正式':'草稿'}}</el-descriptions-item>
        </el-descriptions>
        <div v-for="g in detail.groups" :key="g.questionType" style="margin-top:16px">
          <el-divider content-position="left">{{g.typeDesc}}</el-divider>
          <div v-for="(item,idx) in g.items" :key="item.questionId" class="q-item">
            <p><b>{{idx+1}}.（{{item.score}}分）</b>{{item.stem}}</p>
            <div v-if="item.options" style="padding-left:16px;color:#555">
              <p v-for="opt in parseOptions(item.options)" :key="opt.key">{{opt.key}}. {{opt.text}}</p>
            </div>
            <p class="ans">参考答案：{{item.answer}}　<span v-if="item.analysis">解析：{{item.analysis}}</span></p>
          </div>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  paperPage, autoGenerate, paperDetail, publishPaper, deletePaper,
  savePaper, knowledgeTree, pageQuestion
} from '../../utils/api'

const types = [
  { code: 1, desc: '单选题' }, { code: 2, desc: '多选题' }, { code: 3, desc: '判断题' },
  { code: 4, desc: '填空题' }, { code: 5, desc: '简答题' }
]
const pageNum = ref(1)
const page = ref({ records: [], total: 0 })
const knowledges = ref([])
const load = async () => { page.value = await paperPage({ pageNum: pageNum.value, pageSize: 10 }) }
onMounted(() => { load(); knowledgeTree().then(d => knowledges.value = d) })

// 智能组卷
const autoDialog = ref(false)
const generating = ref(false)
const targetDiff = ref(3)
const auto = reactive({ name: '', suggestDuration: 60, groups: [{ questionType: 1, count: 5, scorePer: 4 }], coverage: [] })
const openAuto = () => {
  Object.assign(auto, { name: '智能组卷-' + new Date().toLocaleString(), suggestDuration: 60, groups: [{ questionType: 1, count: 5, scorePer: 4 }], coverage: [] })
  autoDialog.value = true
}
const generate = async () => {
  if (!auto.groups.length) return ElMessage.warning('至少配置一个题型')
  generating.value = true
  try {
    const res = await autoGenerate({ ...auto, targetDifficulty: targetDiff.value, difficultyTolerance: 0.4 })
    if (res.unmetConstraints?.length) {
      ElMessageBox.alert(res.unmetConstraints.join('<br/>'), `已生成草稿，平均难度${res.avgDifficulty}，但存在未满足约束`, { dangerouslyUseHTMLString: true })
    } else {
      ElMessage.success(`组卷成功，总分${res.totalScore}，平均难度${res.avgDifficulty}`)
    }
    autoDialog.value = false; load()
  } finally { generating.value = false }
}

// 手动组卷
const manualDialog = ref(false)
const manual = reactive({ name: '', suggestDuration: 60, scorePer: 4 })
const manualType = ref(null)
const bank = ref([])
const manualSelected = ref([])
const openManual = async () => {
  Object.assign(manual, { name: '手动组卷-' + new Date().toLocaleString(), suggestDuration: 60, scorePer: 4 })
  manualType.value = null; manualDialog.value = true; loadBank()
}
const loadBank = async () => {
  bank.value = (await pageQuestion({ pageNum: 1, pageSize: 100, type: manualType.value })).records
}
const saveManual = async () => {
  if (!manualSelected.value.length) return ElMessage.warning('请先勾选题目')
  await savePaper({
    name: manual.name, suggestDuration: manual.suggestDuration,
    items: manualSelected.value.map(q => ({ questionId: q.id, score: manual.scorePer }))
  })
  ElMessage.success('草稿已保存'); manualDialog.value = false; load()
}

const doPublish = async (id) => { await publishPaper(id); ElMessage.success('试卷已发布'); load() }
const remove = async (id) => { await deletePaper(id); load() }

// 详情
const detailDialog = ref(false)
const detail = ref(null)
const view = async (id) => { detail.value = await paperDetail(id); detailDialog.value = true }
const parseOptions = (s) => { try { return JSON.parse(s) } catch { return [] } }
</script>

<style scoped>
.q-item { margin-bottom: 14px; }
.q-item p { margin: 4px 0; }
.ans { color: #67c23a; font-size: 13px; }
</style>
