INSERT INTO sys_user (username, password, nickname, email) VALUES
('admin', '$2a$10$P1UK1iryZJXe9T5ZpfCHF.6BzLBokxBYzQfHx1P99d/snxM4KFETe', '管理员', 'admin@example.com'),
('zhangsan', '$2a$10$P1UK1iryZJXe9T5ZpfCHF.6BzLBokxBYzQfHx1P99d/snxM4KFETe', '张三', 'zhangsan@example.com'),
('lisi', '$2a$10$P1UK1iryZJXe9T5ZpfCHF.6BzLBokxBYzQfHx1P99d/snxM4KFETe', '李四', 'lisi@example.com');

INSERT INTO sys_role (name, code, description) VALUES
('超级管理员', 'ADMIN', '拥有所有权限'),
('编辑者', 'EDITOR', '可查看、新增、编辑用户'),
('浏览者', 'VIEWER', '仅可查看用户列表');

INSERT INTO sys_permission (name, code, type, description) VALUES
('查看用户列表', 'user:list', 'BUTTON', '查看用户列表权限'),
('新增用户', 'user:add', 'BUTTON', '新增用户权限'),
('编辑用户', 'user:edit', 'BUTTON', '编辑用户权限'),
('删除用户', 'user:delete', 'BUTTON', '删除用户权限'),
('切换用户状态', 'user:status', 'BUTTON', '启用/禁用用户权限'),
('查看审计日志', 'audit:list', 'BUTTON', '查看操作审计日志权限'),
('查看部门列表', 'dept:list', 'BUTTON', '查看部门列表权限'),
('新增部门', 'dept:add', 'BUTTON', '新增部门权限'),
('编辑部门', 'dept:edit', 'BUTTON', '编辑部门权限'),
('删除部门', 'dept:delete', 'BUTTON', '删除部门权限');

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p WHERE r.code = 'ADMIN';

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.code = 'EDITOR' AND p.code IN ('user:list', 'user:add', 'user:edit', 'user:status');

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.code = 'VIEWER' AND p.code IN ('user:list');

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r WHERE u.username = 'admin' AND r.code = 'ADMIN';

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r WHERE u.username = 'zhangsan' AND r.code = 'EDITOR';

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r WHERE u.username = 'lisi' AND r.code = 'VIEWER';

INSERT INTO sys_dept (name, code, parent_id, sort_order, leader, phone, email) VALUES
('总公司', 'HQ', 0, 1, '张总', '010-88888888', 'hq@company.com'),
('技术部', 'TECH', 1, 1, '李技术', '010-88888801', 'tech@company.com'),
('市场部', 'MARKET', 1, 2, '王市场', '010-88888802', 'market@company.com'),
('人事部', 'HR', 1, 3, '赵人事', '010-88888803', 'hr@company.com'),
('财务部', 'FINANCE', 1, 4, '孙财务', '010-88888804', 'finance@company.com'),
('前端开发组', 'TECH-FE', 2, 1, '周前端', '010-88888811', 'tech-fe@company.com'),
('后端开发组', 'TECH-BE', 2, 2, '吴后端', '010-88888812', 'tech-be@company.com'),
('测试组', 'TECH-QA', 2, 3, '郑测试', '010-88888813', 'tech-qa@company.com'),
('运营组', 'MARKET-OP', 3, 1, '冯运营', '010-88888821', 'market-op@company.com'),
('销售组', 'MARKET-SALE', 3, 2, '陈销售', '010-88888822', 'market-sale@company.com');

INSERT INTO sys_user_dept (user_id, dept_id)
SELECT u.id, 2 FROM sys_user u WHERE u.username = 'admin';

INSERT INTO sys_user_dept (user_id, dept_id)
SELECT u.id, 6 FROM sys_user u WHERE u.username = 'zhangsan';

INSERT INTO sys_user_dept (user_id, dept_id)
SELECT u.id, 9 FROM sys_user u WHERE u.username = 'lisi';
