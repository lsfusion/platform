package lsfusion.server.logics.classes.data.file;

import lsfusion.base.file.FileData;
import lsfusion.interop.session.ExternalRequest;
import lsfusion.interop.session.ExternalUtils;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.ParseException;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.nio.charset.Charset;

public abstract class FileBasedClass<T> extends DataClass<T> {

    public FileBasedClass(LocalizedString caption) {
        super(caption);
    }

    @Override
    public T parseHTTP(ExternalRequest.Param param) throws ParseException {
        Object value = param.value;
        String charsetName = param.charsetName;
        if(value instanceof String)
            value = ExternalUtils.decodeFileData((String) value, charsetName, "file");

        FileData file = (FileData) value;
        if (file.isNull())
            return null;
        return parseHTTPNotNull(file, charsetName);
    }

    @Override
    public Object formatHTTP(T value, Charset charset) {
        if(value == null)
            return FileData.NULL;
        return formatHTTPNotNull(value, charset);
    }

    protected abstract T parseHTTPNotNull(FileData b, String charsetName);

    protected abstract FileData formatHTTPNotNull(T b, Charset charset);
}
