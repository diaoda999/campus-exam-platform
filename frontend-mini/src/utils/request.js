import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'
import { useUserStore } from '../store/user'

const request = axios.create({ baseURL: '/', timeout: 15000 })

// 考试进行中的一次性 Token（进入考试后设置，交卷后清除）
let examToken = ''
export const setExamToken = (t) => { examToken = t || '' }

request.interceptors.request.use(config => {
  const store = useUserStore()
  if (store.token) config.headers.Authorization = `Bearer ${store.token}`
  if (examToken) config.headers['Exam-Token'] = examToken
  return config
})

request.interceptors.response.use(
  resp => {
    const body = resp.data
    if (body.code === 0) return body.data
    ElMessage.error(body.message || '请求失败')
    if (body.code === 10002) {
      useUserStore().logout()
      router.push('/login')
    }
    return Promise.reject(new Error(body.message || 'fail'))
  },
  err => {
    ElMessage.error(err.message || '网络异常')
    return Promise.reject(err)
  }
)

export default request
