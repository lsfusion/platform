package lsfusion.server.logics.classes.data.file;

import lsfusion.base.file.RawFileData;

public abstract class HumanReadableFileClass extends StaticFormatFileClass {
    protected HumanReadableFileClass(boolean multiple, boolean storeName) {
        super(multiple, storeName);
    }

    @Override
    public RawFileData parseString(String s) {
        // when we send file as string in the request body, we don't expect it to be base64 encoded
//        return new RawFileData(s.getBytes());
        return super.parseString(s);
    }

    @Override
    public String formatString(RawFileData value, boolean ui) {
//        return value != null ? new String(value.getBytes()) : null;
        return super.formatString(value, ui);
    }
}
