package lsfusion.server.classes;

public abstract class StaticFormatFileClass extends FileClass {

    public abstract String getOpenExtension(byte[] file);

    protected StaticFormatFileClass(boolean multiple, boolean storeName) {
        super(multiple, storeName);
    }
}
