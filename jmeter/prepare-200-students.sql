-- =============================================================
-- JMeter 200 并发压测造数脚本
-- 为预置考试（exam_id=1，班级 id=1）批量创建 200 名学生账号
-- 初始密码 123456 的 BCrypt 哈希（由项目同款 Hutool BCrypt 生成，可直接登录）
INSERT INTO sys_user(username, password, real_name, role, class_id)
SELECT CONCAT('stu', LPAD(n, 3, '0')),
       '$2a$10$RBt1J3MsF2ASFU1Wfsi7NOwZOgFsJFXf7UsVfMsr5RFYkT8tg9oxi',
       CONCAT('压测学生', LPAD(n, 3, '0')),
       'STUDENT', 1
FROM (
  SELECT a.N + b.N*10 + c.N*100 + 1 AS n
  FROM (SELECT 0 N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
        UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a,
       (SELECT 0 N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
        UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) b,
       (SELECT 0 N UNION SELECT 1) c
) seq
WHERE n BETWEEN 1 AND 200
  AND NOT EXISTS (SELECT 1 FROM sys_user u WHERE u.username = CONCAT('stu', LPAD(n, 3, '0')));
