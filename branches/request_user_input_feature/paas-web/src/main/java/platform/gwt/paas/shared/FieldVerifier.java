package platform.gwt.paas.shared;

public class FieldVerifier {

    public static boolean isValidUserName(String name) {
        if (name == null) {
            return false;
        }
        return name.length() >= 1;
    }

    public static boolean isValidModuleName(String name) {
        if (name == null) {
            return false;
        }
        return name.trim().length() != 0;
    }

    public static boolean isValidPassword(String password) {
        if (password == null) {
            return false;
        }
        return password.length() >= 1;
    }
}
