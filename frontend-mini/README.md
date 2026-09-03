# 前端演示端（frontend-mini）

面向演示的最小可用前端，覆盖平台全部主流程，不做过度工程化。

## 技术栈

Vue 3（Composition API）· Vite 5 · Element Plus · Pinia · Vue Router 4 · Axios

## 启动

```bash
npm install
npm run dev       # http://localhost:5173 ，/api 代理到 http://localhost:8080
npm run build     # 产物在 dist/，纯静态文件
```

## 页面清单

| 角色 | 页面 | 关键交互 |
|---|---|---|
| 公共 | 登录 | 登录态写入 Pinia + localStorage |
| 教师 | 题库管理 | 五题型 CRUD、动态选项编辑、知识点维护、难度/题型/知识点筛选 |
| 教师 | 试卷与组卷 | 智能组卷对话框（题型/难度/知识点覆盖约束）、手动选题、预览、发布 |
| 教师 | 考试管理 | 选正式试卷建考试、时间窗口、切屏上限、交卷进度 |
| 教师 | 阅卷批改 | 待批列表、逐题打分评语、异步汇总、发布成绩 |
| 教师 | 成绩统计 | 指标卡、分数段、逐题得分率、缓存命中标识 |
| 学生 | 我的考试 | 时间窗判断、状态展示、进入/续考/查看成绩 |
| 学生 | 答题房间 | 服务器剩余时间倒计时、3 秒防抖自动保存、答题卡、切屏上报、到点自动交卷 |
| 学生 | 成绩单 | 客观/主观/总分、违规次数、批改与发布状态 |

## 约定

- Axios 请求拦截器自动注入 `Authorization: Bearer <jwt>`；进入考试后由 `setExamToken` 注入 `Exam-Token`，离开房间清除。
- 后端统一返回 `{code,message,data}`，`code===0` 为成功，拦截器直接解包 `data`，其余弹错并在登录失效时跳登录页。
