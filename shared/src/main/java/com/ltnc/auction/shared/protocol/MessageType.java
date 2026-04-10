package com.ltnc.auction.shared.protocol;

public enum MessageType {
    // C -> S
    PING, LOGIN, REGISTER, CREATE_ITEM, UPDATE_ITEM, DELETE_ITEM, GET_ITEMS_BY_SELLER,
    
    // S -> C
    LOGIN_RESULT, REGISTER_RESULT, CREATE_ITEM_RESULT, UPDATE_ITEM_RESULT, DELETE_ITEM_RESULT, GET_ITEMS_BY_SELLER_RESULT,

    // S -> C error
    ERROR;
}
