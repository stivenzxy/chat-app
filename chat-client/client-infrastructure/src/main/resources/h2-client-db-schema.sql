CREATE TABLE IF NOT EXISTS channels (
    channel_id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    owner_id VARCHAR(255) NOT NULL,
    visibility VARCHAR(10) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS channel_members (
    channel_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (channel_id, user_id)
);

CREATE TABLE IF NOT EXISTS channel_invites (
    invite_id VARCHAR(36) PRIMARY KEY,
    channel_id VARCHAR(36) NOT NULL,
    inviter_user_id VARCHAR(255) NOT NULL,
    invited_user_id VARCHAR(255) NOT NULL,
    status VARCHAR(10) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS messages (
    message_id INT AUTO_INCREMENT PRIMARY KEY,
    sender_id VARCHAR(255) NOT NULL,
    recipient_id VARCHAR(255) NOT NULL,
    content TEXT,
    message_type VARCHAR(20) NOT NULL,
    audio_content BLOB,
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS channel_messages (
    message_id INT AUTO_INCREMENT PRIMARY KEY,
    channel_id VARCHAR(36) NOT NULL,
    sender_user_id VARCHAR(255) NOT NULL,
    content TEXT,
    message_type VARCHAR(20) NOT NULL,
    audio_content BLOB,
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);