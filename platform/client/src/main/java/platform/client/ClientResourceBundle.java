package platform.client;


import java.util.Locale;
import java.util.ResourceBundle;

public class ClientResourceBundle {

    private static ResourceBundle clientResourceBundle = Utf8ResourceBundle.getBundle("ClientResourceBundle", Locale.getDefault());
    //private static ResourceBundle clientResourceBundle = Utf8ResourceBundle.getBundle("ClientResourceBundle", Locale.US);

    public static String getString(String key) {
        return clientResourceBundle.getString(key);
    }
    //public static void load(){
    //        clientResourceBundle = ResourceBundle.getBundle("LabelsBundle", new Locale("ru"));
    //}
}
