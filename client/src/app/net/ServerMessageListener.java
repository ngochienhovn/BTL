package app.net;

import com.ltnc.auction.shared.protocol.ClientToServerMessage;
import com.ltnc.auction.shared.protocol.ServerToClientMessage;
import com.ltnc.auction.shared.protocol.MessageType;


import app.service.NetworkAuctionService;
import java.util.function.Consumer;

/**
 * Tách luồng push từ server (broadcast) khỏi {@link SocketClient} — chỉ là adapter đăng ký listener.
 */
public final class ServerMessageListener {

    private ServerMessageListener() {}

    /**
     * Consumer truyền vào {@link SocketClient#addBroadcastListener} để {@link NetworkAuctionService}
     * xử lý {@link MessageType#AUCTION_UPDATE}, {@link MessageType#WALLET_UPDATE},
     * {@link MessageType#AUCTION_STATE_CHANGE}.
     */
    public static Consumer<ServerToClientMessage> forAuctionService(NetworkAuctionService svc) {
        return svc::onBroadcastMessage;
    }
}
