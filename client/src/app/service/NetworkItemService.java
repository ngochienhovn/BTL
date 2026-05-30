package app.service;

import app.model.Art;
import app.model.Electronics;
import app.model.Item;
import app.model.Vehicle;
import com.ltnc.auction.shared.dto.ItemDto;
import com.ltnc.auction.shared.protocol.MessageType;
import com.ltnc.auction.shared.protocol.ClientToServerMessage;
import com.ltnc.auction.shared.protocol.ServerToClientMessage;
import app.net.SocketClient;
import javafx.application.Platform;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class NetworkItemService implements IItemService {
    private static final NetworkItemService INSTANCE = new NetworkItemService();
    
    private final List<Item> localItems = new CopyOnWriteArrayList<>();
    private final List<ItemObserver> observers = new CopyOnWriteArrayList<>();

    private NetworkItemService() {
        SocketClient.getInstance().addBroadcastListener(this::onBroadcastMessage);
    }

    public static NetworkItemService getInstance() { return INSTANCE; }

    @Override
    public void registerObserver(ItemObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    @Override
    public void unregisterObserver(ItemObserver observer) {
        observers.remove(observer);
    }

    private void notifyObservers() {
        Platform.runLater(() -> {
            for (ItemObserver observer : observers) {
                try {
                    observer.onItemsUpdated();
                } catch (Exception ignored) {}
            }
        });
    }

    private void onBroadcastMessage(ServerToClientMessage resp) {
        if (resp == null || resp.type == null) return;
        
        if (resp.type == MessageType.ITEM_UPDATE && resp.item != null) {
            if (updateLocalItem(resp.item)) {
                notifyObservers();
            }
        } else if (resp.type == MessageType.ITEM_DELETED && resp.itemId != null) {
            String idStr = resp.itemId.toString();
            localItems.removeIf(i -> idStr.equals(i.getId()));
            notifyObservers();
        }
    }

    private boolean updateLocalItem(ItemDto info) {
        if (info == null || info.id == null) return false;
        String idStr = info.id.toString();
        Item newItem = mapItem(info);
        for (int i = 0; i < localItems.size(); i++) {
            Item current = localItems.get(i);
            if (idStr.equals(current.getId())) {
                if (current.getName().equals(newItem.getName()) &&
                    current.getDescription().equals(newItem.getDescription()) &&
                    current.getType().equalsIgnoreCase(newItem.getType()) &&
                    Double.compare(current.getStartingBid(), newItem.getStartingBid()) == 0 &&
                    java.util.Objects.equals(current.getImageUrl(), newItem.getImageUrl())) {
                    return false; // No change
                }
                localItems.set(i, newItem);
                return true;
            }
        }
        localItems.add(newItem);
        return true;
    }

    @Override
    public Item createItem(String type, String name, String description, double startingBid,
                           String imageUrl, String sellerEmail) {
        ClientToServerMessage req = new ClientToServerMessage();
        req.type = MessageType.CREATE_ITEM;
        req.itemType = type;
        req.itemName = name;
        req.itemDescription = description;
        req.itemStartingBid = startingBid;
        req.itemImageUrl = imageUrl;
        req.sellerEmail = sellerEmail;

        ServerToClientMessage resp = SocketClient.getInstance().sendAndReceive(req);
        if (resp == null || !resp.success || resp.item == null) return null;
        Item item = mapItem(resp.item);
        updateLocalItem(resp.item);
        notifyObservers();
        return item;
    }

    @Override
    public boolean updateItem(Item item, String name, String description, double startingBid, String imageUrl) {
        if (item == null) return false;
        ClientToServerMessage req = new ClientToServerMessage();
        req.type = MessageType.UPDATE_ITEM;
        try {
            req.itemId = Long.parseLong(item.getId());
        } catch (NumberFormatException e) {
            return false;
        }
        req.itemType = item.getType();
        req.itemName = name;
        req.itemDescription = description;
        req.itemStartingBid = startingBid;
        req.itemImageUrl = imageUrl;

        ServerToClientMessage resp = SocketClient.getInstance().sendAndReceive(req);
        if (resp != null && resp.success && resp.item != null) {
            if (updateLocalItem(resp.item)) {
                notifyObservers();
            }
        }
        return resp != null && resp.success;
    }

    @Override
    public boolean deleteItem(String itemId) {
        ClientToServerMessage req = new ClientToServerMessage();
        req.type = MessageType.DELETE_ITEM;
        try {
            req.itemId = Long.parseLong(itemId);
        } catch (NumberFormatException e) {
            return false;
        }
        ServerToClientMessage resp = SocketClient.getInstance().sendAndReceive(req);
        if (resp != null && resp.success) {
            localItems.removeIf(i -> itemId.equals(i.getId()));
            notifyObservers();
        }
        return resp != null && resp.success;
    }

    @Override
    public List<Item> getItemsBySeller(String sellerEmail) {
        ClientToServerMessage req = new ClientToServerMessage();
        req.type = MessageType.GET_ITEMS_BY_SELLER;
        req.sellerEmail = sellerEmail;
        ServerToClientMessage resp = SocketClient.getInstance().sendAndReceive(req);
        if (resp == null || !resp.success || resp.items == null) return getCachedItemsBySeller(sellerEmail);
        
        List<Item> fetched = new ArrayList<>();
        for (ItemDto info : resp.items) {
            Item item = mapItem(info);
            if (item != null) {
                fetched.add(item);
                // Update cache for each item
                updateLocalItem(info);
            }
        }
        return fetched;
    }

    @Override
    public List<Item> getCachedItemsBySeller(String sellerEmail) {
        if (sellerEmail == null) return new ArrayList<>(localItems);
        return localItems.stream()
                .filter(i -> sellerEmail.equalsIgnoreCase(i.getSellerEmail()))
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public List<Item> getAllItems() {
        ClientToServerMessage req = new ClientToServerMessage();
        req.type = MessageType.GET_ITEMS_BY_SELLER;
        // No sellerEmail = get all
        ServerToClientMessage resp = SocketClient.getInstance().sendAndReceive(req);
        if (resp == null || !resp.success || resp.items == null) return new ArrayList<>(localItems);
        
        List<Item> fetched = new ArrayList<>();
        for (ItemDto info : resp.items) {
            Item item = mapItem(info);
            if (item != null) fetched.add(item);
        }
        localItems.clear();
        localItems.addAll(fetched);
        return new ArrayList<>(localItems);
    }

    private Item mapItem(ItemDto info) {
        if (info == null) return null;
        String id = info.id != null ? info.id.toString() : UUID.randomUUID().toString();
        String type = info.type != null ? info.type : "Electronics";
        return switch (type.toLowerCase()) {
            case "art" -> new Art(id, info.name, info.description, info.startingBid, info.imageUrl, info.sellerEmail);
            case "vehicle" -> new Vehicle(id, info.name, info.description, info.startingBid, info.imageUrl, info.sellerEmail);
            default -> new Electronics(id, type, info.name, info.description, info.startingBid, info.imageUrl, info.sellerEmail);
        };
    }
}
