package lsfusion.interop.session;

import org.apache.commons.fileupload.RequestContext;
import org.apache.hc.core5.http.ContentType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

class ByteArrayRequestContext implements RequestContext {
    private final byte[] data;
    private final String contentType;
    private final String encoding;

    public ByteArrayRequestContext(byte[] data, ContentType contentType) {
        this.data = data;
        this.contentType = contentType.toString();
        this.encoding = ExternalUtils.getBodyUrlCharset(contentType).name();
    }

    @Override
    public String getCharacterEncoding() {
        return encoding;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public int getContentLength() {
        return data.length;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(data);
    }
}
