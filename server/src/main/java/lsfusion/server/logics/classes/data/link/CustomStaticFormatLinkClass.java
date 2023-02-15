package lsfusion.server.logics.classes.data.link;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.classes.DataType;
import lsfusion.server.logics.classes.data.DataClass;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CustomStaticFormatLinkClass extends StaticFormatLinkClass {

    private final String filterDescription;
    private final ImSet<String> filterExtensions;

    protected String getFileSID() {
        return isRawLink() ? "RAWLINK" : "LINK";
    }

    public static CustomStaticFormatLinkClass get(boolean multiple, String description, String extensions) {
        return get(multiple, description, SetFact.toExclSet(extensions.split(" ")));
    }

    private static final List<CustomStaticFormatLinkClass> instances = new ArrayList<>();

    public static CustomStaticFormatLinkClass get() { // RAWLINK
        return get(false);
    }
    public static CustomStaticFormatLinkClass get(boolean multiple) {
        return get(multiple, "", SetFact.singleton(""));
    }
    public static CustomStaticFormatLinkClass get(boolean multiple, String description, ImSet<String> extensions) {
        if(extensions.contains("")) // если есть RAWLINK то и результат считаем RAWLINK
            extensions = SetFact.singleton("");

        for(CustomStaticFormatLinkClass instance : instances)
            if(instance.multiple == multiple && instance.filterDescription.equals(description) && instance.filterExtensions.equals(extensions))
                return instance;

        CustomStaticFormatLinkClass instance = new CustomStaticFormatLinkClass(multiple, description, extensions);
        instances.add(instance);
        DataClass.storeClass(instance);
        return instance;
    }

    private CustomStaticFormatLinkClass(boolean multiple, String filterDescription, ImSet<String> filterExtensions) {
        super(multiple);
        this.filterDescription = filterDescription;
        this.filterExtensions = filterExtensions;
    }

    @Override
    protected ImSet<String> getExtensions() {
        return filterExtensions;
    }

    @Override
    public String getSID() {
        if (isRawLink()) {
            return super.getSID();
        } else {
            return super.getSID() + "_filterDescription=" + filterDescription + "_" + Arrays.toString(filterExtensions.toArray(new String[filterExtensions.size()]));
        }
    }

    private boolean isRawLink() {
        return filterExtensions.isEmpty() || filterExtensions.size() == 1 && filterExtensions.get(0).isEmpty();
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);

        outStream.writeUTF(filterDescription);
        outStream.writeInt(filterExtensions.size());
        for (String extension : filterExtensions) {
            outStream.writeUTF(extension);
        }
    }

    @Override
    public byte getTypeID() {
        return DataType.CUSTOMSTATICFORMATLINK;
    }
}