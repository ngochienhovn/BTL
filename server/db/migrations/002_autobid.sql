CREATE TABLE IF NOT EXISTS auto_bid_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    auction_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    max_bid DECIMAL(19, 2) NOT NULL,
    increment DECIMAL(19, 2) NOT NULL,
    UNIQUE KEY uq_auto_bid_auction_user (auction_id, user_id)
);