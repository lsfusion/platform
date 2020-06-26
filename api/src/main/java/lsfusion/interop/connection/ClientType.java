package lsfusion.interop.connection;

public enum ClientType {
    NATIVE_DESKTOP, NATIVE_MOBILE, WEB_DESKTOP, WEB_MOBILE;

    @Override
    public String toString() {
        switch (this) {
            case NATIVE_DESKTOP:
                return "nativeDesktop";
            case NATIVE_MOBILE:
                return "nativeMobile";
            case WEB_DESKTOP:
                return "webDesktop";
            case WEB_MOBILE:
                return "webMobile";
            default:
                throw new UnsupportedOperationException("Unsupported ClientType");
        }
    }
}