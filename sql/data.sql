-- ============================================================
-- 演示数据：账号初始密码均为 123456（应用首次启动自动升级为 BCrypt）
-- 账号：admin / teacher1 / s1~s6
-- ============================================================
USE campus_exam;

-- 关键：显式指定客户端连接字符集为 utf8mb4。
-- 否则 MySQL 官方镜像执行本脚本时客户端默认 latin1，会把 UTF-8 字节误当 latin1 写入，
-- 造成中文双重编码乱码（形如 "ä¸­"）。
SET NAMES utf8mb4;

-- 班级
INSERT INTO sys_class (id, class_name) VALUES (1, '计算机2301'), (2, '计算机2302');

-- 用户（明文密码，DataInitializer 启动时加密）
INSERT INTO sys_user (id, username, password, real_name, role, class_id) VALUES
(1, 'admin',    '123456', '系统管理员', 'ADMIN',   NULL),
(2, 'teacher1', '123456', '张老师',     'TEACHER', NULL),
(3, 's1', '123456', '李明', 'STUDENT', 1),
(4, 's2', '123456', '王芳', 'STUDENT', 1),
(5, 's3', '123456', '刘洋', 'STUDENT', 1),
(6, 's4', '123456', '陈晨', 'STUDENT', 1),
(7, 's5', '123456', '赵强', 'STUDENT', 2),
(8, 's6', '123456', '孙丽', 'STUDENT', 2);

-- 知识点
INSERT INTO knowledge_point (id, name, parent_id) VALUES
(1, 'Java基础', 0), (2, '集合框架', 0), (3, 'JVM', 0), (4, 'MySQL', 0),
(5, 'Redis', 0), (6, '消息队列', 0), (7, 'Spring框架', 0), (8, '并发编程', 0);

-- ================= 单选题 12 道 =================
INSERT INTO question (id, type, stem, options, answer, analysis, difficulty, creator_id) VALUES
(101,1,'Java 中 int 类型占用多少位？','[{"key":"A","text":"8位"},{"key":"B","text":"16位"},{"key":"C","text":"32位"},{"key":"D","text":"64位"}]','C','int 为 32 位有符号整数',1,2),
(102,1,'下列哪个 JVM 内存区域是线程私有的？','[{"key":"A","text":"堆"},{"key":"B","text":"方法区"},{"key":"C","text":"虚拟机栈"},{"key":"D","text":"常量池"}]','C','虚拟机栈与程序计数器随线程私有',2,2),
(103,1,'InnoDB 索引的底层数据结构是？','[{"key":"A","text":"哈希表"},{"key":"B","text":"B+树"},{"key":"C","text":"红黑树"},{"key":"D","text":"跳表"}]','B','InnoDB 使用 B+树作为索引结构',1,2),
(104,1,'Redis 默认服务端口是？','[{"key":"A","text":"3306"},{"key":"B","text":"6379"},{"key":"C","text":"5672"},{"key":"D","text":"8080"}]','B','Redis 默认 6379',1,2),
(105,1,'下列哪个不是 RabbitMQ 内置的 Exchange 类型？','[{"key":"A","text":"direct"},{"key":"B","text":"topic"},{"key":"C","text":"fanout"},{"key":"D","text":"partition"}]','D','partition 是 Kafka 的概念',2,2),
(106,1,'MyBatis-Plus 中用于声明主键的注解是？','[{"key":"A","text":"@TableId"},{"key":"B","text":"@TableField"},{"key":"C","text":"@Id"},{"key":"D","text":"@PrimaryKey"}]','A','@TableId 标识主键',1,2),
(107,1,'HashMap 默认初始容量是？','[{"key":"A","text":"8"},{"key":"B","text":"16"},{"key":"C","text":"32"},{"key":"D","text":"64"}]','B','默认初始容量 16',2,2),
(108,1,'Redis 中 ZSet 的底层实现之一是？','[{"key":"A","text":"压缩表/跳表"},{"key":"B","text":"红黑树"},{"key":"C","text":"B树"},{"key":"D","text":"位图"}]','A','ZSet 由 ziplist/listpack + skiplist 实现',3,2),
(109,1,'MySQL InnoDB 默认的事务隔离级别是？','[{"key":"A","text":"读未提交"},{"key":"B","text":"读已提交"},{"key":"C","text":"可重复读"},{"key":"D","text":"串行化"}]','C','默认为 Repeatable Read',3,2),
(110,1,'下列哪个不能作为 GC Roots？','[{"key":"A","text":"虚拟机栈局部变量"},{"key":"B","text":"静态变量引用"},{"key":"C","text":"常量引用"},{"key":"D","text":"堆中对象的互相引用"}]','D','堆内循环引用不构成 GC Root',4,2),
(111,1,'RabbitMQ 中用于手动确认消息的方法是？','[{"key":"A","text":"basicAck"},{"key":"B","text":"basicPublish"},{"key":"C","text":"basicConsume"},{"key":"D","text":"basicQos"}]','A','basicAck 确认投递',2,2),
(112,1,'实现 Redis 分布式锁常用的原子命令组合是？','[{"key":"A","text":"GET + SET"},{"key":"B","text":"SET NX EX"},{"key":"C","text":"INCR"},{"key":"D","text":"HSET"}]','B','SET key value NX EX 保证加锁原子性',3,2);

-- ================= 多选题 8 道 =================
INSERT INTO question (id, type, stem, options, answer, analysis, difficulty, creator_id) VALUES
(201,2,'下列属于 Spring/Spring Boot 常用注解的有？','[{"key":"A","text":"@Component"},{"key":"B","text":"@Autowired"},{"key":"C","text":"@Transactional"},{"key":"D","text":"@synchronized"}]','ABC','D 不是 Spring 注解',2,2),
(202,2,'Redis 支持的基本数据类型包括？','[{"key":"A","text":"String"},{"key":"B","text":"Hash"},{"key":"C","text":"Set"},{"key":"D","text":"ZSet"}]','ABCD','五种基础类型还包括 List',1,2),
(203,2,'下列属于 MySQL 索引类型的有？','[{"key":"A","text":"主键索引"},{"key":"B","text":"唯一索引"},{"key":"C","text":"联合索引"},{"key":"D","text":"全文索引"}]','ABCD','InnoDB 支持以上索引',3,2),
(204,2,'JVM 主流的垃圾回收算法包括？','[{"key":"A","text":"标记-清除"},{"key":"B","text":"标记-整理"},{"key":"C","text":"复制算法"},{"key":"D","text":"分代收集"}]','ABCD','均为经典 GC 算法',3,2),
(205,2,'保证 RabbitMQ 消息不丢失可以采取的措施有？','[{"key":"A","text":"生产者 confirm 确认"},{"key":"B","text":"队列与消息持久化"},{"key":"C","text":"消费者手动 ack"},{"key":"D","text":"本地消息表补偿"}]','ABCD','生产、Broker、消费三端共同保障',4,2),
(206,2,'HashMap 在并发场景下可能出现的问题有？','[{"key":"A","text":"数据覆盖"},{"key":"B","text":"JDK7 扩容成环"},{"key":"C","text":"size 不准确"},{"key":"D","text":"自动加锁"}]','ABC','HashMap 不会自动加锁',4,2),
(207,2,'MyBatis-Plus 提供的能力有？','[{"key":"A","text":"条件构造器"},{"key":"B","text":"分页插件"},{"key":"C","text":"逻辑删除"},{"key":"D","text":"自动填充"}]','ABCD','均为 MP 内置能力',2,2),
(208,2,'下列 HTTP 方法中具备幂等性的有？','[{"key":"A","text":"GET"},{"key":"B","text":"PUT"},{"key":"C","text":"DELETE"},{"key":"D","text":"POST"}]','ABC','POST 一般不幂等',3,2);

-- ================= 判断题 8 道 =================
INSERT INTO question (id, type, stem, options, answer, analysis, difficulty, creator_id) VALUES
(301,3,'Redis 的命令处理核心是单线程模型（6.0 前网络 IO 与命令执行均单线程）。',NULL,'TRUE','避免多线程竞争，6.0 后网络 IO 多线程',2,2),
(302,3,'MySQL 中一张表可以存在多个主键。',NULL,'FALSE','一张表只能有一个主键',1,2),
(303,3,'RabbitMQ 的队列和消息都可以设置持久化。',NULL,'TRUE','durable + deliveryMode=2',1,2),
(304,3,'JVM 方法区（元空间）是所有线程共享的内存区域。',NULL,'TRUE','方法区为线程共享',2,2),
(305,3,'B+树的叶子节点之间通过指针相连，便于范围查询。',NULL,'TRUE','叶子节点形成有序链表',3,2),
(306,3,'Redis 的 Key 到期后会被立即全部删除。',NULL,'FALSE','采用惰性删除 + 定期删除组合策略',3,2),
(307,3,'Spring 容器中的 Bean 默认作用域是单例。',NULL,'TRUE','默认 singleton',1,2),
(308,3,'消息队列消费者的业务逻辑应当设计为幂等。',NULL,'TRUE','因为重复投递不可避免',2,2);

-- ================= 填空题 6 道（## 分隔多空，|| 分隔可接受答案） =================
INSERT INTO question (id, type, stem, options, answer, analysis, difficulty, creator_id) VALUES
(401,4,'JDK 动态代理基于____实现，CGLIB 动态代理基于____实现。',NULL,'接口||interface##继承||子类||ASM','两空分别为接口与继承(字节码生成)',3,2),
(402,4,'MySQL InnoDB 默认的事务隔离级别是____。',NULL,'可重复读||REPEATABLE READ||RR','默认可重复读',2,2),
(403,4,'Redis 五种基本数据类型分别是 String、____、Hash、Set、____。',NULL,'List||列表##ZSet||有序集合||SortedSet','List 与 ZSet',2,2),
(404,4,'RabbitMQ 中消息先到达____，再按路由键投递到____。',NULL,'交换机||Exchange##队列||Queue','Exchange -> Queue',2,2),
(405,4,'HashMap 的默认负载因子是____。',NULL,'0.75','负载因子 0.75',3,2),
(406,4,'Spring Boot 应用的启动类注解是____。',NULL,'@SpringBootApplication||SpringBootApplication','组合注解',1,2);

-- ================= 简答题 4 道 =================
INSERT INTO question (id, type, stem, options, answer, analysis, difficulty, creator_id) VALUES
(501,5,'简述什么是 Redis 缓存穿透，以及常见的解决方案。',NULL,'查询不存在的数据导致请求直达数据库；方案：缓存空值、布隆过滤器拦截、参数校验。','空值缓存+布隆过滤器',3,2),
(502,5,'简述使用 RabbitMQ 时如何保证消息不丢失。',NULL,'生产端 confirm/return 确认；交换机队列消息持久化；消费端手动 ack；本地消息表补偿。','三端保障+补偿',4,2),
(503,5,'简述 synchronized 与 ReentrantLock 的主要区别。',NULL,'synchronized 是关键字、JVM 层自动释放；ReentrantLock 是 JUC 类、需手动 unlock，支持公平锁、可中断、多条件变量。','层面/释放/功能灵活性',4,2),
(504,5,'为什么 MySQL InnoDB 选择 B+树而不是 B 树作为索引结构？',NULL,'B+树非叶子节点不存数据，单页可容纳更多索引、树更矮；叶子节点有序链表，范围查询与顺序扫描更高效。','矮树+叶子链表',4,2);

-- 题目-知识点关联
INSERT INTO question_knowledge (question_id, knowledge_id) VALUES
(101,1),(102,3),(103,4),(104,5),(105,6),(106,7),(107,2),(108,5),(109,4),(110,3),(111,6),(112,5),
(201,7),(202,5),(203,4),(204,3),(205,6),(206,2),(207,7),(208,1),
(301,5),(302,4),(303,6),(304,3),(305,4),(306,5),(307,7),(308,6),
(401,7),(401,8),(402,4),(403,5),(404,6),(405,2),(406,7),
(501,5),(502,6),(503,8),(504,4);

-- ================= 预置一张 100 分正式试卷 =================
INSERT INTO paper (id, name, total_score, suggest_duration, gen_type, status, creator_id) VALUES
(1, 'Java后端基础测验（演示卷）', 100, 45, 'MANUAL', 1, 2);

INSERT INTO paper_question (paper_id, question_id, group_type, sort_no, score) VALUES
(1,101,1,1,4),(1,103,1,2,4),(1,104,1,3,4),(1,106,1,4,4),(1,107,1,5,4),
(1,201,2,1,6),(1,202,2,2,6),(1,207,2,3,6),
(1,302,3,1,4),(1,303,3,2,4),(1,307,3,3,4),
(1,402,4,1,5),(1,404,4,2,5),
(1,501,5,1,20),(1,502,5,2,20);

-- 预置一场覆盖“当前时间”的考试（开始于 1 天前，结束于 1 天后），学生登录即可参加
INSERT INTO exam (id, exam_name, paper_id, class_id, start_time, end_time, monitor_config, status, creator_id) VALUES
(1, '2026秋季 Java 基础随堂测', 1, 1, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 1 DAY),
 '{"switchLimit":3}', 1, 2);
