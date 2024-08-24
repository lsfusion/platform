package lsfusion.server.logics.classes.data.link;

import lsfusion.interop.classes.DataType;
import lsfusion.server.logics.classes.data.DataClass;

import java.util.ArrayList;
import java.util.Collection;

public class VideoLinkClass extends RenderedLinkClass {

    protected String getFileSID() {
        return "VIDEOLINK";
    }

    private static Collection<VideoLinkClass> instances = new ArrayList<>();

    public static VideoLinkClass get(boolean multiple) {
        for (VideoLinkClass instance : instances)
            if (instance.multiple == multiple)
                return instance;

        VideoLinkClass instance = new VideoLinkClass(multiple);
        instances.add(instance);
        DataClass.storeClass(instance);
        return instance;
    }

    private VideoLinkClass(boolean multiple) {
        super(multiple);
    }

    public byte getTypeID() {
        return DataType.VIDEOLINK;
    }

    @Override
    public String getDefaultCastExtension() {
        return "video";
    }
}