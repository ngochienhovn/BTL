public class ServerMessageListener implements Runnable {

    private final Socket socket;
    private final Gson gson;
    private final List<ServerMessageCallback> callbacks;
    private volatile boolean running = true;

    public ServerMessageListener(Socket socket) {
        this.socket = socket;
        this.gson = new Gson();
        this.callbacks = new CopyOnWriteArrayList<>();
    }

    public void registerCallback(ServerMessageCallback callback) {
        callbacks.add(callback);
    }

    public void unregisterCallback(ServerMessageCallback callback) {
        callbacks.remove(callback);
    }

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream()))) {

            String line;

            while (running && (line = reader.readLine()) != null) {
                try {
                    ServerToClientMessage message =
                            gson.fromJson(line, ServerToClientMessage.class);

                    for (ServerMessageCallback cb : callbacks) {
                        cb.onMessageReceived(message);
                    }

                } catch (Exception e) {
                    System.err.println("Failed to parse message: - ServerMessageListener.java:39" + line);
                }
            }

        } catch (IOException e) {
            System.out.println("Connection lost. - ServerMessageListener.java:44");
        } finally {
            notifyConnectionLost();
        }
    }

    public void stop() {
        running = false;
        try {
            socket.close(); // force unblock readLine()
        } catch (IOException ignored) {}
    }

    private void notifyConnectionLost() {
        for (ServerMessageCallback cb : callbacks) {
            cb.onConnectionLost();
        }
    }
}