package lsfusion.server.logics.classes.data.file;

import lsfusion.server.language.action.LA;
import lsfusion.server.logics.BaseLogicsModule;

public abstract class AbstractDynamicFormatFileClass<T> extends FileClass<T> {

    protected AbstractDynamicFormatFileClass(boolean multiple, boolean storeName) {
        super(multiple, storeName);
    }

    @Override
    public LA getDefaultOpenAction(BaseLogicsModule baseLM) {
        return baseLM.openFile;
    }
}