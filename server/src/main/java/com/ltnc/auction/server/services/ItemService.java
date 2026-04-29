package com.ltnc.auction.server.services;

import com.ltnc.auction.server.dao.ItemDAO;
import com.ltnc.auction.server.model.Item;
import com.ltnc.auction.server.model.User;
import java.math.BigDecimal;
import java.util.List;

public class ItemService {
    private final ItemDAO itemDAO;
    private final AuthService authService;

    public ItemService(ItemDAO itemDAO, AuthService authService) {
        this.itemDAO = itemDAO;
        this.authService = authService;
    }

    public record ServiceResult(boolean success, String code, String message, Long itemId) {
    }

    public ServiceResult createItem(User actor, String type, String name, String description, BigDecimal startingBid,
                                    String imageUrl) {
        if (!authService.canCreateItem(actor)) {
            return new ServiceResult(false, "FORBIDDEN", "Only SELLER can create items", null);
        }
        Item item = new Item(null, actor.getId(), actor.getEmail(), type, name, description, startingBid, imageUrl);
        Long id = itemDAO.insert(item);
        return new ServiceResult(true, "CREATE_ITEM_SUCCESS", "Item created", id);
    }

    public ServiceResult updateItem(User actor, Long itemId, String type, String name, String description,
                                    BigDecimal startingBid, String imageUrl) {
        if (!authService.canCreateItem(actor)) {
            return new ServiceResult(false, "FORBIDDEN", "Only SELLER can update items", null);
        }
        Item item = new Item(itemId, actor.getId(), actor.getEmail(), type, name, description, startingBid, imageUrl);
        boolean updated = itemDAO.update(item);
        if (!updated) {
            return new ServiceResult(false, "NOT_FOUND", "Item not found or not owned by seller", null);
        }
        return new ServiceResult(true, "UPDATE_ITEM_SUCCESS", "Item updated", itemId);
    }

    public ServiceResult deleteItem(User actor, Long itemId) {
        if (!authService.canCreateItem(actor)) {
            return new ServiceResult(false, "FORBIDDEN", "Only SELLER can delete items", null);
        }
        boolean deleted = itemDAO.delete(itemId, actor.getId());
        if (!deleted) {
            return new ServiceResult(false, "NOT_FOUND", "Item not found or not owned by seller", null);
        }
        return new ServiceResult(true, "DELETE_ITEM_SUCCESS", "Item deleted", itemId);
    }

    public List<Item> findBySeller(User actor) {
        return itemDAO.findBySeller(actor.getEmail());
    }
}
