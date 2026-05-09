package com.ltnc.auction.server.services;

import com.google.gson.Gson;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * BroadcastManager dùng để quản lý các client đang kết nối
 * và gửi thông báo realtime từ server xuống tất cả client.
 *
 * Ví dụ:
 * - Có người đặt giá mới
 * - Auction đổi trạng thái
 * - Server muốn cập nhật dữ liệu realtime cho client
 */
public class BroadcastManager {
    private static final BroadcastManager instance = new BroadcastManager();

    /**
     * Danh sách các client đang kết nối.
     *
     * Mỗi client sẽ có một PrintWriter riêng để server gửi dữ liệu xuống.
     * Dùng Collections.synchronizedSet để an toàn hơn khi nhiều client
     * kết nối hoặc ngắt kết nối cùng lúc.
     */
    private final Set<PrintWriter> clients = Collections.synchronizedSet(new HashSet<>());

    /**
     * Gson dùng để chuyển object Java thành chuỗi JSON.
     */
    private final Gson gson = new Gson();

    /**
     * Constructor private để không cho class khác tự tạo nhiều BroadcastManager.
     * Toàn bộ server sẽ dùng chung một instance thông qua getInstance().
     */
    private BroadcastManager() {
    }

    public static BroadcastManager getInstance() {
        return instance;
    }

    /**
     * Thêm client vào danh sách nhận broadcast.
     *
     * Hàm này thường được gọi trong ClientHandler
     * sau khi client kết nối thành công.
     */
    public void addClient(PrintWriter out) {
        if (out == null) {
            return;
        }

        clients.add(out);
    }

    /**
     * Xóa client khỏi danh sách nhận broadcast.
     *
     * Hàm này thường được gọi khi client thoát
     * hoặc bị mất kết nối.
     */
    public void removeClient(PrintWriter out) {
        if (out == null) {
            return;
        }

        clients.remove(out);
    }

    /**
     * Gửi message xuống tất cả client đang kết nối.
     *
     * Object msg sẽ được chuyển thành JSON trước khi gửi.
     */
    public void broadcast(Object msg) {
        if (msg == null) {
            return;
        }

        String json = gson.toJson(msg);

        /*
         * Vì clients là synchronizedSet, khi duyệt bằng iterator
         * ta nên bọc trong synchronized để tránh lỗi khi vừa duyệt vừa thêm/xóa client.
         */
        synchronized (clients) {
            Iterator<PrintWriter> iterator = clients.iterator();

            while (iterator.hasNext()) {
                PrintWriter out = iterator.next();

                try {
                    out.println(json);
                    out.flush();

                    /*
                     * PrintWriter thường không ném IOException trực tiếp.
                     * checkError() giúp phát hiện client đã lỗi hoặc mất kết nối.
                     */
                    if (out.checkError()) {
                        iterator.remove();
                    }

                } catch (Exception e) {
                    iterator.remove();
                }
            }
        }
    }

    /**
     * Hàm này dùng để test hoặc debug.
     * Ví dụ: kiểm tra hiện tại có bao nhiêu client đang kết nối.
     */
    public int getClientCount() {
        return clients.size();
    }
}