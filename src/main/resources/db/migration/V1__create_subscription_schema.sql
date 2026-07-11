CREATE TABLE channel (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(50) NOT NULL,
    subscribable BOOLEAN NOT NULL,
    unsubscribable BOOLEAN NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_channel_code (code)
) ENGINE=InnoDB;

CREATE TABLE subscription_member (
    id BIGINT NOT NULL AUTO_INCREMENT,
    phone_number VARCHAR(20) NOT NULL,
    status TINYINT NOT NULL COMMENT '구독 상태: 0=NONE, 1=BASIC, 2=PREMIUM',
    PRIMARY KEY (id),
    UNIQUE KEY uk_subscription_member_phone_number (phone_number)
) ENGINE=InnoDB;

CREATE TABLE subscription_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    channel_id BIGINT NOT NULL,
    action TINYINT NOT NULL COMMENT '구독 행위: 0=SUBSCRIBE, 1=UNSUBSCRIBE',
    previous_status TINYINT NOT NULL COMMENT '변경 전 상태: 0=NONE, 1=BASIC, 2=PREMIUM',
    changed_status TINYINT NOT NULL COMMENT '변경 후 상태: 0=NONE, 1=BASIC, 2=PREMIUM',
    changed_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_subscription_history_member FOREIGN KEY (member_id) REFERENCES subscription_member (id),
    CONSTRAINT fk_subscription_history_channel FOREIGN KEY (channel_id) REFERENCES channel (id),
    KEY idx_subscription_history_member_changed_at (member_id, changed_at, id)
) ENGINE=InnoDB;

INSERT INTO channel (code, name, subscribable, unsubscribable)
VALUES
    ('WEB', '홈페이지', TRUE, TRUE),
    ('MOBILE_APP', '모바일앱', TRUE, TRUE),
    ('NAVER', '네이버', TRUE, FALSE),
    ('SKT', 'SKT', TRUE, FALSE),
    ('CALL_CENTER', '콜센터', FALSE, TRUE),
    ('EMAIL', '이메일', FALSE, TRUE);
