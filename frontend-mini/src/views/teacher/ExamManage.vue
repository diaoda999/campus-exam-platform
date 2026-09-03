<template>
  <div>
    <el-card>
      <el-form inline>
        <el-button type="success" @click="openCreate">创建考试</el-button>
        <el-button @click="load">刷新</el-button>
      </el-form>
      <el-table :data="page.records" border stripe>
        <el-table-column prop="id" label="ID" width="60"/>
        <el-table-column prop="examName" label="考试名称" min-width="180"/>
        <el-table-column prop="paperName" label="试卷" min-width="160"/>
        <el-table-column prop="className" label="班级" width="120"/>
        <el-table-column prop="startTime" label="开始时间" width="170"/>
        <el-table-column prop="endTime" label="结束时间" width="170"/>
        <el-table-column label="状态" width="100">
          <template #default="{row}">
            <el-tag :type="statusType(row.status)">{{row.statusDesc}}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="交卷进度" width="100">
          <template #default="{row}">{{row.submittedCount}}/{{row.totalStudents}}</template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{row}">
            <el-popconfirm title="删除考试？" @confirm="remove(row.id)">
              <template #reference><el-button link type="danger">删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:12px" layout="total,prev,pager,next"
                     :total="page.total" :current-page="pageNum" :page-size="10"
                     @current-change="p=>{pageNum=p;load()}"/>
    </el-card>

    <el-dialog v-model="dialog" title="创建考试" width="560px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="考试名称" required><el-input v-model="form.examName"/></el-form-item>
        <el-form-item label="选择试卷" required>
          <el-select v-model="form.paperId" placeholder="仅可选择正式试卷" style="width:100%">
            <el-option v-for="p in papers" :key="p.id"
                       :label="`${p.name}（${p.totalScore}分）`" :value="p.id"/>
          </el-select>
        </el-form-item>
        <el-form-item label="参考班级" required>
          <el-select v-model="form.classId" style="width:100%">
            <el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id"/>
          </el-select>
        </el-form-item>
        <el-form-item label="时间窗口" required>
          <el-date-picker v-model="timeRange" type="datetimerange" range-separator="至"
                          start-placeholder="开始" end-placeholder="结束"
                          value-format="YYYY-MM-DD HH:mm:ss" style="width:100%"/>
        </el-form-item>
        <el-form-item label="切屏上限">
          <el-input-number v-model="form.switchLimit" :min="1" :max="20"/>
          <span style="color:#999;font-size:12px">达到次数自动交卷，留空不限制</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog=false">取消</el-button>
        <el-button type="primary" @click="save">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { examPage, createExam, deleteExam, paperPage, classList } from '../../utils/api'

const pageNum = ref(1)
const page = ref({ records: [], total: 0 })
const papers = ref([])
const classes = ref([])
const dialog = ref(false)
const form = reactive({})
const timeRange = ref([])

const load = async () => { page.value = await examPage({ pageNum: pageNum.value, pageSize: 10 }) }
onMounted(async () => {
  load()
  papers.value = (await paperPage({ pageNum: 1, pageSize: 100, status: 1 })).records
  classes.value = await classList()
})
const openCreate = () => {
  Object.keys(form).forEach(k => delete form[k])
  form.switchLimit = 3; timeRange.value = []; dialog.value = true
}
const save = async () => {
  if (!form.examName || !form.paperId || !form.classId || !timeRange.value?.length) {
    return ElMessage.warning('请完整填写考试信息')
  }
  await createExam({ ...form, startTime: timeRange.value[0], endTime: timeRange.value[1] })
  ElMessage.success('考试已创建'); dialog.value = false; load()
}
const remove = async (id) => { await deleteExam(id); load() }
const statusType = (s) => ({ 0: 'info', 1: 'success', 2: 'warning', 3: 'primary' }[s] || 'info')
</script>
