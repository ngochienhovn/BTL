package app.service;

public enum BidResult {
    OK,
    NOT_LOGGED_IN,
    INVALID_AUCTION,
    INVALID_AMOUNT,
    BID_TOO_LOW,
    AUCTION_CLOSED,
    INSUFFICIENT_FUNDS,
    FORBIDDEN_SELF_BID
}
