# JMeter 200 并发压测说明

## 压测链路

`登录(JWT) → 进入考试(一次性 Exam-Token) → 5 次自动保存(Redis Hash 快照) → 交卷(Redisson 锁 + 客观题即时判分)`，
完整覆盖双 Token 鉴权、答题快照、防并发提交与即时出分四个关键路径。

## 压测前准备

1. 启动中间件：在项目根目录执行 `docker compose up -d`。
2. 启动后端：`java -jar exam-admin/target/exam-server.jar`（首次启动自动建表、初始化演示数据）。
3. 灌入 200 名压测学生（班级 1）：
   ```bash
   mysql -uroot -proot123456 campus_exam < jmeter/prepare-200-students.sql
   ```
   生成 stu001~stu200，密码均为 123456；`testdata/students.csv` 已同步生成。
4. 确认考试 id=1 的时间窗口覆盖当前时间（演示数据默认为前后各 1 天）。

## 执行

GUI 调试：用 JMeter 5.6+ 打开 `exam-200-concurrency.jmx`。

命令行正式压测：
```bash
jmeter -n -t jmeter/exam-200-concurrency.jmx -l jmeter/result/result.jtl -e -o jmeter/result/html
```

线程模型：200 线程、20 秒爬坡、每线程完整执行 1 次链路。

## 关注指标

| 接口 | 目标 |
|---|---|
| 登录 | P99 < 300ms，无失败 |
| 进入考试 | P99 < 200ms，Exam-Token 全部成功签发 |
| 自动保存 | P99 < 100ms（Redis Hash 写入） |
| 交卷 | P99 < 2s，错误率 0；Redisson 锁拦截重复提交 |
| 整体 | 200 并发下无 5xx，收卷 MQ 消息零丢失（本地消息表补偿可查） |

> 压测后如需重置：`UPDATE exam_record SET status=0 WHERE user_id BETWEEN ...` 并清空 `exam_answer`，
> 或直接重建库（`docker compose down -v && docker compose up -d`）。
