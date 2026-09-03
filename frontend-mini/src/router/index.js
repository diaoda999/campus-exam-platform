import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  { path: '/login', component: () => import('../views/Login.vue') },
  {
    path: '/',
    component: () => import('../layout/MainLayout.vue'),
    redirect: '/exam/list',
    children: [
      { path: 'question', meta: { roles: ['TEACHER', 'ADMIN'], title: '题库管理' }, component: () => import('../views/teacher/QuestionManage.vue') },
      { path: 'paper', meta: { roles: ['TEACHER', 'ADMIN'], title: '试卷与组卷' }, component: () => import('../views/teacher/PaperManage.vue') },
      { path: 'exam/manage', meta: { roles: ['TEACHER', 'ADMIN'], title: '考试管理' }, component: () => import('../views/teacher/ExamManage.vue') },
      { path: 'grade', meta: { roles: ['TEACHER', 'ADMIN'], title: '阅卷批改' }, component: () => import('../views/teacher/GradeCenter.vue') },
      { path: 'stat', meta: { roles: ['TEACHER', 'ADMIN'], title: '成绩统计' }, component: () => import('../views/teacher/StatBoard.vue') },
      { path: 'exam/list', meta: { roles: ['STUDENT', 'TEACHER', 'ADMIN'], title: '我的考试' }, component: () => import('../views/student/ExamList.vue') },
      { path: 'exam/room/:examId', meta: { roles: ['STUDENT'], title: '答题中', fullscreen: true }, component: () => import('../views/student/ExamRoom.vue') },
      { path: 'exam/result/:examId', meta: { roles: ['STUDENT'], title: '我的成绩' }, component: () => import('../views/student/MyResult.vue') }
    ]
  }
]

const router = createRouter({ history: createWebHashHistory(), routes })

router.beforeEach((to, from, next) => {
  const role = localStorage.getItem('role')
  if (to.path !== '/login' && !role) return next('/login')
  if (to.meta.roles && role && !to.meta.roles.includes(role)) return next('/exam/list')
  next()
})

export default router
