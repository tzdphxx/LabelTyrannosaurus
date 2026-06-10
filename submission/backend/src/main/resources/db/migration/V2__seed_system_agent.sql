INSERT IGNORE INTO users (username, email, user_type, login_enabled, display_name)
VALUES ('system_ai_agent', 'system_ai_agent@labelhub.local', 'SYSTEM', 0, 'System AI Agent');

INSERT IGNORE INTO user_roles (user_id, role_code)
SELECT id, 'SYSTEM_AGENT'
FROM users
WHERE username = 'system_ai_agent';
