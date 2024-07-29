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
        if(value instanceof String)
            value = ExternalUtils.decodeFileData((String) value, Charset.forName(param.charsetName));

        FileData file = (FileData) value;
        if (file.isNull())
            return null;
        return parseHTTPNotNull(file);
    }

    @Override
    public Object formatHTTP(T value) {
        if(value == null)
            return FileData.NULL;
        return formatHTTPNotNull(value);
    }

    protected abstract T parseHTTPNotNull(FileData b);

    protected abstract FileData formatHTTPNotNull(T b);
}
