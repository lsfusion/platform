package lsfusion.interop.connection;

public enum ClientType {
    DESKTOP, WEB, MOBILE;

    @Override
    public String toString() {
        switch (this) {
            case DESKTOP:
                return "desktop";
            case WEB:
                return "web";
            case MOBILE:
                return "mobile";
            default:
                throw new UnsupportedOperationException("Unsupported ClientType");
        }
    }
}