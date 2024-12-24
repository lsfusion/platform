package lsfusion.server.logics.classes.data.file;

import lsfusion.base.Result;
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
        return parseHTTPNotNull(file, charsetName, param.fileName);
    }

    @Override
    public ExternalRequest.Result formatHTTP(T value, Charset charset) {
        if(value == null)
            return new ExternalRequest.Result(FileData.NULL);
        Result<String> fileName = new Result<>();
        FileData result = formatHTTPNotNull(value, charset, fileName);
        return new ExternalRequest.Result(result, fileName.result);
    }

    protected abstract T parseHTTPNotNull(FileData b, String charsetName, String fileName);

    protected abstract FileData formatHTTPNotNull(T b, Charset charset, Result<String> fileName);
}
