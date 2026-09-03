import request from './request'

export const login = (data) => request.post('/api/auth/login', data)

// 题库
export const pageQuestion = (data) => request.post('/api/question/page', data)
export const saveQuestion = (data) => request.post('/api/question', data)
export const questionDetail = (id) => request.get('/api/question/detail', { params: { id } })
export const deleteQuestion = (id) => request.delete('/api/question', { params: { id } })
export const knowledgeTree = () => request.get('/api/knowledge/tree')
export const saveKnowledge = (data) => request.post('/api/knowledge', data)
export const deleteKnowledge = (id) => request.delete('/api/knowledge', { params: { id } })
export const classList = () => request.get('/api/classes/list')

// 试卷
export const autoGenerate = (data) => request.post('/api/paper/auto-generate', data)
export const savePaper = (data) => request.post('/api/paper/save', data)
export const paperDetail = (id) => request.get('/api/paper/detail', { params: { id } })
export const paperPage = (params) => request.post('/api/paper/page', null, { params })
export const publishPaper = (id) => request.post('/api/paper/publish', null, { params: { id } })
export const deletePaper = (id) => request.delete('/api/paper', { params: { id } })

// 考试
export const createExam = (data) => request.post('/api/exam', data)
export const examPage = (params) => request.post('/api/exam/page', null, { params })
export const studentExams = () => request.get('/api/exam/student/list')
export const enterExam = (examId) => request.get('/api/exam/enter', { params: { examId } })
export const autoSave = (data) => request.post('/api/exam/autosave', data)
export const heartbeat = (examId) => request.post('/api/exam/heartbeat', null, { params: { examId } })
export const reportViolation = (data) => request.post('/api/exam/violation', data)
export const submitExam = (data) => request.post('/api/exam/submit', data)
export const deleteExam = (id) => request.delete('/api/exam', { params: { id } })

// 阅卷
export const gradeTodo = (examId) => request.get('/api/grade/todo', { params: { examId } })
export const gradeDetail = (examId, userId) => request.get('/api/grade/detail', { params: { examId, userId } })
export const gradeScore = (data) => request.post('/api/grade/score', data)
export const gradeFinish = (data) => request.post('/api/grade/finish', data)
export const gradePublish = (examId) => request.post('/api/grade/publish', null, { params: { examId } })
export const myResult = (examId) => request.get('/api/grade/my-result', { params: { examId } })

// 统计
export const classStat = (examId) => request.get('/api/stat/class', { params: { examId } })
