package platform.server.net;

public class ServerInstanceLocatorSettings {
    private int port;

    public ServerInstanceLocatorSettings(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }
}
