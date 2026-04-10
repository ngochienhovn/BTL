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
}
