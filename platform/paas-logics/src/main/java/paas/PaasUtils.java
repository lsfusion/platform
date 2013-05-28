package paas;

public class PaasUtils {
    public static boolean isPortValid(Integer port) {
        return port == null || (port >= 0 && port < 65536);
    }

    public static void checkPortExceptionally(Integer port) {
        if (!PaasUtils.isPortValid(port)) {
            throw new RuntimeException("Port should be set to correct number between 0 and 65536.");
        }
    }
}
