package com.ltnc.auction.server.protocol;

public enum MessageType 
{
    // Client to Server
    LOGIN, REGISTER, AUCTION_BID, GET_ITEMS, CREATE_AUCTION,

    // Server to Client
    LOGIN_RESULT, REGISTER_RESULT, AUCTION_BID_RESULT, GET_ITEMS_RESULT, CREATE_AUCTION_RESULT,

    // Server to client broadcast
    AUCTION_STATE_UPDATE,

    // S -> C error message
    ERROR;
}
