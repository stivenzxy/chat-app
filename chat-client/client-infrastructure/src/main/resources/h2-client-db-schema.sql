CREATE TABLE IF NOT EXISTS channels (
    channel_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    owner_id VARCHAR(255) NOT NULL,
    visibility VARCHAR(10) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS channel_members (
    channel_id INT NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (channel_id, user_id)
);

CREATE TABLE IF NOT EXISTS channel_invites (
    invite_id INT AUTO_INCREMENT PRIMARY KEY,
    channel_id INT NOT NULL,
    inviter_user_id VARCHAR(255) NOT NULL,
    invited_user_id VARCHAR(255) NOT NULL,
    status VARCHAR(10) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS messages (
    message_id INT AUTO_INCREMENT PRIMARY KEY,
    sender_id VARCHAR(255) NOT NULL,
    recipient_id VARCHAR(255) NULL,
    recipient_channel_id INT NULL,
    content TEXT,
    message_type VARCHAR(20) NOT NULL,
    audio_content BLOB,
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT check_recipient CHECK (
        (recipient_id IS NOT NULL AND recipient_channel_id IS NULL) OR 
        (recipient_id IS NULL AND recipient_channel_id IS NOT NULL)
    )
);