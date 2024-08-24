package lsfusion.server.logics.classes.data.file;

import lsfusion.base.file.RawFileData;

public abstract class HumanReadableFileClass extends StaticFormatFileClass {
    protected HumanReadableFileClass(boolean multiple, boolean storeName) {
        super(multiple, storeName);
    }

//    @Override
//    public RawFileData parseString(String s) {
//        return new RawFileData(s.getBytes());
//    }
//
//    @Override
//    public String formatString(RawFileData value, boolean ui) {
//        return value != null ? new String(value.getBytes()) : null;
//    }
}
