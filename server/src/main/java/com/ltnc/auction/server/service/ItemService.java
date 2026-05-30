package com.ltnc.auction.server.service;

import com.ltnc.auction.server.dao.ItemDAO;
import com.ltnc.auction.server.dao.UserDAO;
import com.ltnc.auction.server.model.Item;
import com.ltnc.auction.server.model.ItemFactory;
import com.ltnc.auction.server.model.User;
import com.ltnc.auction.shared.dto.ItemDto;
import com.ltnc.auction.shared.protocol.MessageType;
import com.ltnc.auction.shared.protocol.ServerToClientMessage;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItemService {
    private static final Logger LOG = LoggerFactory.getLogger(ItemService.class);
    private static volatile ItemService instance;
    private final ItemDAO itemDAO = new ItemDAO();
    private final UserDAO userDAO = new UserDAO();

    private ItemService() {}

    public static ItemService getInstance() {
        if (instance == null) {
            synchronized (ItemService.class) {
                if (instance == null) {
                    instance = new ItemService();
                }
            }
        }
        return instance;
    }

    public ItemDto createItem(String type, String name, String description, double startingBid,
                               String imageUrl, String sellerEmail) {
        try {
            com.ltnc.auction.server.db.DBConnection.startTransaction();
            
            Item item = ItemFactory.create(type, name, description,
                    BigDecimal.valueOf(startingBid), imageUrl, sellerEmail);
            
            // Find seller id
            Long sellerId = null;
            User seller = userDAO.findByEmail(sellerEmail);
            if (seller != null) {
                sellerId = seller.getId();
                item.setSellerId(sellerId);
            } else {
                LOG.warn("Seller not found by email: {}", sellerEmail);
            }
            
            Long id = itemDAO.insert(item, sellerId);
            if (id == null) {
                com.ltnc.auction.server.db.DBConnection.rollbackTransaction();
                LOG.error("Failed to insert item into database for seller: {}", sellerEmail);
                return null;
            }
            
            item.setId(id);
            com.ltnc.auction.server.db.DBConnection.commitTransaction();
            
            ItemDto dto = toDto(item);
            broadcastItemUpdate(dto);
            return dto;
        } catch (Exception e) {
            com.ltnc.auction.server.db.DBConnection.rollbackTransaction();
            LOG.error("Exception occurred while creating item for seller: " + sellerEmail, e);
            return null;
        }
    }

    public List<ItemDto> getItemsBySeller(String sellerEmail) {
        return itemDAO.findBySeller(sellerEmail).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<ItemDto> getAllItems() {
        return itemDAO.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public Item getItemById(Long itemId) {
        return itemDAO.findById(itemId);
    }

    public ItemDto updateItem(Long itemId, String type, String name, String description,
                               double startingBid, String imageUrl) {
        Item item = itemDAO.findById(itemId);
        if (item == null) return null;
        item.setName(name);
        item.setDescription(description);
        item.setStartingBid(BigDecimal.valueOf(startingBid));
        item.setImageUrl(imageUrl);
        itemDAO.update(item);
        
        // Sync active auctions referencing this item
        AuctionService.getInstance().syncAuctionWithItem(itemId, name, description, item.getType(), item.getStartingBid(), imageUrl);
        
        ItemDto dto = toDto(item);
        broadcastItemUpdate(dto);
        return dto;
    }

    public boolean deleteItem(Long itemId) {
        AuctionService.getInstance().deleteAuctionByItem(itemId);
        boolean ok = itemDAO.delete(itemId);
        if (ok) {
            broadcastItemDeleted(itemId);
        }
        return ok;
    }

    private void broadcastItemUpdate(ItemDto item) {
        ServerToClientMessage msg = new ServerToClientMessage();
        msg.type = MessageType.ITEM_UPDATE;
        msg.item = item;
        msg.success = true;
        BroadcastManager.getInstance().broadcastAll(msg);
    }

    private void broadcastItemDeleted(Long itemId) {
        ServerToClientMessage msg = new ServerToClientMessage();
        msg.type = MessageType.ITEM_DELETED;
        msg.itemId = itemId;
        msg.success = true;
        BroadcastManager.getInstance().broadcastAll(msg);
    }

    private ItemDto toDto(Item item) {
        ItemDto dto = new ItemDto();
        dto.id = item.getId();
        dto.type = item.getType();
        dto.name = item.getName();
        dto.description = item.getDescription();
        dto.startingBid = item.getStartingBid() != null ? item.getStartingBid().doubleValue() : 0;
        dto.imageUrl = item.getImageUrl();
        dto.sellerEmail = item.getSellerEmail();
        return dto;
    }
}
