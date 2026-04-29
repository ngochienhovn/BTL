package com.ltnc.auction.shared.protocol;

import java.util.List;
import java.util.Map;

public class ServerToClientMessage {
    public MessageType type;
    public boolean success;
    public String code;
    public String error;
    public String message;
    public Map<String, Object> data;
    public List<Map<String, Object>> items;
    public List<Map<String, Object>> auctions; // danh sách phiên đấu giá
    public Map<String, Object> auction; // thông tin 1 phiên cụ thể
    public Double balance;   // tổng tiền
    public Double reserved;  // tiền đang giữ lại khi bid
    public Double available; // tiền còn dùng được
    public Double requiredTopUp; // thiếu bao nhiêu tiền nếu bid fail
}
