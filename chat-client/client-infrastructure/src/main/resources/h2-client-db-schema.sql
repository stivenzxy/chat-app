CREATE TABLE IF NOT EXISTS messages (
                                        message_id INT AUTO_INCREMENT PRIMARY KEY,
                                        sender_id VARCHAR(255) NOT NULL,
    recipient_id VARCHAR(255) NOT NULL,
    content TEXT,
    message_type VARCHAR(20) NOT NULL,
    audio_content BLOB,
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );