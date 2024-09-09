package lsfusion.server.logics.classes.data.file;

import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.classes.DataType;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.form.stat.struct.FormIntegrationType;

import java.util.ArrayList;
import java.util.Collection;

public class VideoClass extends RenderedClass {

    protected String getFileSID() {
        return "VIDEOFILE";
    }

    private static Collection<VideoClass> instances = new ArrayList<>();

    public static VideoClass get() {
        return get(false, false);
    }
    public static VideoClass get(boolean multiple, boolean storeName) {
        for (VideoClass instance : instances)
            if (instance.multiple == multiple && instance.storeName == storeName)
                return instance;

        VideoClass instance = new VideoClass(multiple, storeName);
        instances.add(instance);
        DataClass.storeClass(instance);
        return instance;
    }

    private VideoClass(boolean multiple, boolean storeName) {
        super(multiple, storeName);
    }

    public byte getTypeID() {
        return DataType.VIDEO;
    }

    @Override
    public String getExtension() {
        return "mp4";
    }

    @Override
    public FormIntegrationType getIntegrationType() {
        throw new UnsupportedOperationException();
    }
}
