package app.service;

import app.model.Item;
import java.util.List;

public interface IItemService {
    Item createItem(String type, String name, String description, double startingBid, String imageUrl, String sellerEmail);
    boolean updateItem(Item item, String name, String description, double startingBid, String imageUrl);
    boolean deleteItem(String itemId);
    List<Item> getItemsBySeller(String sellerEmail);
    List<Item> getCachedItemsBySeller(String sellerEmail);
    List<Item> getAllItems();
    void registerObserver(ItemObserver observer);
    void unregisterObserver(ItemObserver observer);
}
