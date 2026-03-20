-- 重置 admin 密码为 123456
-- 在 MySQL 中执行: source v2_reset_admin_password.sql 或直接运行此脚本

USE medical_service;

-- 如果 admin 用户已存在，更新密码
UPDATE sys_user 
SET password = '$2a$10$rQbeBO6qTw.ha5Dy6RgaduzMUl9k5bvln4l6mLXTLd5rIdTz27WbO' 
WHERE username = 'admin';

-- 如果 admin 不存在则插入（先执行上面的 UPDATE，若影响行数为 0 再手动执行下面这行）
-- INSERT INTO sys_user (username, password, name, status, created_time) VALUES ('admin', '$2a$10$rQbeBO6qTw.ha5Dy6RgaduzMUl9k5bvln4l6mLXTLd5rIdTz27WbO', '系统管理员', 1, NOW());

-- 确保 admin 有 SUPER_ADMIN 角色
INSERT IGNORE INTO sys_user_role (user_id, role_id, created_time)
SELECT u.user_id, r.role_id, NOW()
FROM sys_user u, sys_role r
WHERE u.username = 'admin' AND r.role_code = 'SUPER_ADMIN';
