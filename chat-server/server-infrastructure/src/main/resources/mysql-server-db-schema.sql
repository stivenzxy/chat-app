CREATE DATABASE IF NOT EXISTS server_db;

CREATE TABLE IF NOT EXISTS users (
                                     user_id VARCHAR(36) PRIMARY KEY,
                                     username VARCHAR(50) NOT NULL UNIQUE,
                                     email VARCHAR(100) NOT NULL UNIQUE,
                                     password_hash VARCHAR(255) NOT NULL,
                                     photo_data LONGBLOB,
                                     ip_address VARCHAR(45),
                                     is_replicated BOOLEAN DEFAULT FALSE,
                                     origin_server_id VARCHAR(50),
                                     last_sync_at TIMESTAMP NULL,
                                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS channels (
                                        channel_id INT AUTO_INCREMENT PRIMARY KEY,
                                        name VARCHAR(100) NOT NULL,
                                        owner_id VARCHAR(36) NOT NULL,
                                        visibility ENUM('PUBLIC','PRIVATE') NOT NULL DEFAULT 'PUBLIC',
                                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                        FOREIGN KEY (owner_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS channel_members (
                                               channel_id INT NOT NULL,
                                               user_id VARCHAR(36) NOT NULL,
                                               PRIMARY KEY (channel_id, user_id),
                                               FOREIGN KEY (channel_id) REFERENCES channels(channel_id) ON DELETE CASCADE,
                                               FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS channel_invites (
                                               invite_id INT AUTO_INCREMENT PRIMARY KEY,
                                               channel_id INT NOT NULL,
                                               inviter_user_id VARCHAR(36) NOT NULL,
                                               invited_user_id VARCHAR(36) NOT NULL,
                                               status ENUM('PENDING','ACCEPTED','REJECTED') NOT NULL DEFAULT 'PENDING',
                                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                               FOREIGN KEY (channel_id) REFERENCES channels(channel_id) ON DELETE CASCADE,
                                               FOREIGN KEY (inviter_user_id) REFERENCES users(user_id) ON DELETE CASCADE,
                                               FOREIGN KEY (invited_user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS messages (
                                        message_id INT AUTO_INCREMENT PRIMARY KEY,
                                        author_id VARCHAR(36) NOT NULL,
                                        recipient_user_id VARCHAR(36),
                                        recipient_channel_id INT,
                                        content TEXT,
                                        message_type ENUM('TEXT', 'AUDIO') NOT NULL,
                                        audio_content LONGBLOB,
                                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                        FOREIGN KEY (author_id) REFERENCES users(user_id) ON DELETE CASCADE,
                                        FOREIGN KEY (recipient_user_id) REFERENCES users(user_id) ON DELETE SET NULL,
                                        FOREIGN KEY (recipient_channel_id) REFERENCES channels(channel_id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS audio_transcriptions (
                                                    message_id INT PRIMARY KEY,
                                                    audio_format VARCHAR(20),
                                                    transcribed_text TEXT,
                                                    FOREIGN KEY (message_id) REFERENCES messages(message_id) ON DELETE CASCADE
);


CREATE TABLE IF NOT EXISTS peer_registry (
                                             peer_id VARCHAR(50) PRIMARY KEY,
                                             ip_address VARCHAR(45) NOT NULL,
                                             port INT NOT NULL,
                                             last_seen_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                             is_active BOOLEAN DEFAULT TRUE,
                                             discovered_from VARCHAR(50),
                                             created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                             INDEX idx_active (is_active),
                                             INDEX idx_last_seen (last_seen_at)
);
