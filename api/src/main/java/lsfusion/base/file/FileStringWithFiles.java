package lsfusion.base.file;

import lsfusion.interop.session.ExternalUtils;

import java.io.Serializable;
import java.util.function.Function;

public class FileStringWithFiles implements Serializable {

    public final StringWithFiles stringData;

    public final String extension;

    public FileStringWithFiles(StringWithFiles stringData, String extension) {
        this.stringData = stringData;
        this.extension = extension;
    }

    public FileData convertFileValue(Function<StringWithFiles, String> convert) {
        return new FileData(new RawFileData(convert.apply(stringData), ExternalUtils.fileCharset), extension);
    }
}
