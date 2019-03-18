package lsfusion.client.classes;

public abstract class ClientStaticFormatFileClass extends ClientFileClass {

    protected ClientStaticFormatFileClass(boolean multiple, boolean storeName) {
        super(multiple, storeName);
    }
    
    public String getExtension() { // should be equal to StaticFormatFileClass.getExtension
        return getExtensions()[0];
    }

    public abstract String[] getExtensions();
}
