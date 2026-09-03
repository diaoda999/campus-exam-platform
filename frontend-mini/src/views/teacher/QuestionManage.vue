<template>
  <div>
    <el-card>
      <el-form inline>
        <el-form-item label="题型">
          <el-select v-model="query.type" placeholder="全部" clearable style="width:120px" @change="load">
            <el-option v-for="t in typeOptions" :key="t.code" :label="t.desc" :value="t.code"/>
          </el-select>
        </el-form-item>
        <el-form-item label="难度">
          <el-select v-model="query.difficulty" placeholder="全部" clearable style="width:110px" @change="load">
            <el-option v-for="d in [1,2,3,4,5]" :key="d" :label="d+'星'" :value="d"/>
          </el-select>
        </el-form-item>
        <el-form-item label="知识点">
          <el-select v-model="query.knowledgeId" placeholder="全部" clearable style="width:150px" @change="load">
            <el-option v-for="k in knowledges" :key="k.id" :label="k.name" :value="k.id"/>
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="题干关键词" clearable @keyup.enter="load"/>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button type="success" @click="openCreate">新建题目</el-button>
          <el-button @click="openKnowledge">知识点维护</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="page.records" border stripe>
        <el-table-column prop="id" label="ID" width="70"/>
        <el-table-column prop="typeDesc" label="题型" width="90"/>
        <el-table-column prop="stem" label="题干" show-overflow-tooltip/>
        <el-table-column label="难度" width="110">
          <template #default="{row}">
            <el-rate :model-value="row.difficulty" disabled size="small"/>
          </template>
        </el-table-column>
        <el-table-column prop="useCount" label="引用" width="70"/>
        <el-table-column label="操作" width="150">
          <template #default="{row}">
            <el-button link type="primary" @click="openEdit(row.id)">编辑</el-button>
            <el-popconfirm title="确认删除该题？" @confirm="remove(row.id)">
              <template #reference><el-button link type="danger">删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:12px" layout="total,prev,pager,next"
                     :total="page.total" :page-size="query.pageSize"
                     :current-page="query.pageNum" @current-change="p => {query.pageNum=p;load()}"/>
    </el-card>

    <!-- 题目编辑 -->
    <el-dialog v-model="dialog" :title="form.id?'编辑题目':'新建题目'" width="720px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="题型" required>
          <el-radio-group v-model="form.type">
            <el-radio-button v-for="t in typeOptions" :key="t.code" :value="t.code">{{t.desc}}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="题干" required>
          <el-input v-model="form.stem" type="textarea" :rows="3"/>
        </el-form-item>
        <template v-if="[1,2].includes(form.type)">
          <el-form-item label="选项">
            <div v-for="(opt,i) in optionList" :key="i" style="display:flex;gap:8px;margin-bottom:6px">
              <el-input v-model="opt.key" style="width:70px"/>
              <el-input v-model="opt.text" placeholder="选项内容"/>
              <el-button link type="danger" @click="optionList.splice(i,1)">删除</el-button>
            </div>
            <el-button size="small" @click="addOption">新增选项</el-button>
          </el-form-item>
        </template>
        <el-form-item label="标准答案">
          <el-radio-group v-if="form.type===1" v-model="form.answer">
            <el-radio v-for="opt in optionList" :key="opt.key" :value="opt.key">{{opt.key}}</el-radio>
          </el-radio-group>
          <el-checkbox-group v-else-if="form.type===2" v-model="multiAnswer">
            <el-checkbox v-for="opt in optionList" :key="opt.key" :value="opt.key">{{opt.key}}</el-checkbox>
          </el-checkbox-group>
          <el-radio-group v-else-if="form.type===3" v-model="form.answer">
            <el-radio value="TRUE">正确</el-radio>
            <el-radio value="FALSE">错误</el-radio>
          </el-radio-group>
          <el-input v-else-if="form.type===4" v-model="form.answer"
                    placeholder="多空用 ## 分隔，同一空多个可接受答案用 || 分隔"/>
          <el-input v-else v-model="form.answer" type="textarea" :rows="2" placeholder="参考答案"/>
        </el-form-item>
        <el-form-item label="解析">
          <el-input v-model="form.analysis" type="textarea" :rows="2"/>
        </el-form-item>
        <el-form-item label="难度">
          <el-rate v-model="form.difficulty"/>
        </el-form-item>
        <el-form-item label="知识点">
          <el-select v-model="form.knowledgeIds" multiple style="width:100%">
            <el-option v-for="k in knowledges" :key="k.id" :label="k.name" :value="k.id"/>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog=false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 知识点维护 -->
    <el-dialog v-model="kpDialog" title="知识点维护" width="480px">
      <el-input v-model="newKp" placeholder="新增知识点名称" @keyup.enter="addKp">
        <template #append><el-button @click="addKp">添加</el-button></template>
      </el-input>
      <el-table :data="knowledges" style="margin-top:12px">
        <el-table-column prop="id" label="ID" width="70"/>
        <el-table-column prop="name" label="名称"/>
        <el-table-column label="操作" width="90">
          <template #default="{row}">
            <el-popconfirm title="删除？" @confirm="delKp(row.id)">
              <template #reference><el-button link type="danger">删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  pageQuestion, saveQuestion, questionDetail, deleteQuestion,
  knowledgeTree, saveKnowledge, deleteKnowledge
} from '../../utils/api'

const typeOptions = [
  { code: 1, desc: '单选题' }, { code: 2, desc: '多选题' }, { code: 3, desc: '判断题' },
  { code: 4, desc: '填空题' }, { code: 5, desc: '简答题' }
]
const query = reactive({ pageNum: 1, pageSize: 10 })
const page = ref({ records: [], total: 0 })
const knowledges = ref([])
const dialog = ref(false)
const kpDialog = ref(false)
const newKp = ref('')
const form = reactive({})
const optionList = ref([])
const multiAnswer = ref([])

watch(() => form.type, (t) => {
  if (t === 3) { form.answer = 'TRUE' }
  if ([1, 2].includes(t) && optionList.value.length === 0) {
    optionList.value = [{ key: 'A', text: '' }, { key: 'B', text: '' }]
  }
})

const load = async () => { page.value = await pageQuestion(query) }
const loadKp = async () => { knowledges.value = await knowledgeTree() }
onMounted(() => { load(); loadKp() })

const resetForm = () => {
  Object.keys(form).forEach(k => delete form[k])
  form.type = 1; form.difficulty = 3; form.knowledgeIds = []
  optionList.value = [{ key: 'A', text: '' }, { key: 'B', text: '' }]
  multiAnswer.value = []
}
const openCreate = () => { resetForm(); dialog.value = true }
const addOption = () => {
  const next = String.fromCharCode(65 + optionList.value.length)
  optionList.value.push({ key: next, text: '' })
}
const openEdit = async (id) => {
  resetForm()
  const detail = await questionDetail(id)
  Object.assign(form, detail)
  multiAnswer.value = form.type === 2 && form.answer ? form.answer.split('') : []
  optionList.value = form.options ? JSON.parse(form.options) : []
  dialog.value = true
}
const save = async () => {
  if (!form.stem) return ElMessage.warning('请填写题干')
  if ([1, 2].includes(form.type)) form.options = JSON.stringify(optionList.value)
  if (form.type === 2) form.answer = multiAnswer.value.sort().join('')
  await saveQuestion(form)
  ElMessage.success('已保存'); dialog.value = false; load()
}
const remove = async (id) => { await deleteQuestion(id); ElMessage.success('已删除'); load() }
const addKp = async () => {
  if (!newKp.value) return
  await saveKnowledge({ name: newKp.value }); newKp.value = ''; loadKp()
}
const delKp = async (id) => { await deleteKnowledge(id); loadKp() }
</script>
