package lsfusion.interop.session;

import lsfusion.base.BaseUtils;
import lsfusion.base.MIMETypeUtils;
import lsfusion.base.Result;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.base.file.FileData;
import lsfusion.base.file.IOUtils;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.interop.logics.remote.RemoteLogicsInterface;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.http.entity.ContentType.APPLICATION_FORM_URLENCODED;

public class ExternalUtils {

    public static final String defaultCSVSeparator = ";";
    public static final String defaultCSVCharset = "UTF-8";
    public static final String defaultXMLJSONCharset = "UTF-8";
    public static final String defaultDBFCharset = "CP1251";

    public static final Charset stringCharset = Consts.UTF_8;
    public static final ContentType TEXT_PLAIN = ContentType.create(
            "text/plain", stringCharset);
    public static final ContentType MULTIPART_MIXED = ContentType.create(
            "multipart/mixed", stringCharset);

    private static final String ACTION_CN_PARAM = "action";
    private static final String SCRIPT_PARAM = "script";
    private static final String PARAMS_PARAM = "p";
    private static final String RETURN_PARAM = "return";
    private static final String RETURNMULTITYPE_PARAM = "returnmultitype";
    private static final String PROPERTY_PARAM = "property";
    
    public static ExecInterface getExecInterface(final AuthenticationToken token, final SessionInfo sessionInfo, final RemoteLogicsInterface remoteLogics) {
        return new ExecInterface() {
            public lsfusion.interop.session.ExternalResponse exec(String action, ExternalRequest request) throws RemoteException {
                return remoteLogics.exec(token, sessionInfo, action, request);
            }
            public lsfusion.interop.session.ExternalResponse eval(boolean action, Object paramScript, ExternalRequest request) throws RemoteException {
                return remoteLogics.eval(token, sessionInfo, action, paramScript, request);
            }
        };
    }

    public static ExternalResponse processRequest(ExecInterface remoteExec, InputStream is, ContentType requestContentType,
                                                  String[] headerNames, String[] headerValues, String[] cookieNames, String[] cookieValues, String logicsHost,
                                                  Integer logicsPort, String logicsExportName, String scheme, String webHost, Integer webPort,
                                                  String contextPath, String servletPath, String query) throws IOException, MessagingException {
        Charset charset = getCharsetFromContentType(requestContentType);
        List<NameValuePair> queryParams = URLEncodedUtils.parse(query, charset);

        ImList<String> queryActionParams = getParameterValues(queryParams, PARAMS_PARAM);
        ImList<Object> bodyActionParams = getListFromInputStream(is, requestContentType);
        ImList<Object> paramsList = ListFact.add(queryActionParams, bodyActionParams);
        
        ImList<String> returns = getParameterValues(queryParams, RETURN_PARAM);
        String returnMultiType = getParameterValue(queryParams, RETURNMULTITYPE_PARAM);
        boolean returnBodyUrl = returnMultiType != null && returnMultiType.equals("bodyurl");
        lsfusion.interop.session.ExternalResponse execResult = null;
        
        String filename = "export";

        ExternalRequest request = new ExternalRequest(returns.toArray(new String[0]), paramsList.toArray(new Object[paramsList.size()]),
                charset == null ? null : charset.toString(), headerNames, headerValues, cookieNames,
                cookieValues, logicsHost, logicsPort, logicsExportName, scheme, webHost, webPort, contextPath, servletPath, query);

        boolean isEvalAction = servletPath.endsWith("/eval/action");
        if (servletPath.endsWith("/eval") || isEvalAction) {
            Object script = getParameterValue(queryParams, SCRIPT_PARAM);
            if (script == null && !paramsList.isEmpty()) {
                int scriptParam = queryActionParams.size();
                if(paramsList.size() > scriptParam) {
                    script = paramsList.get(scriptParam);
                    paramsList = paramsList.remove(scriptParam);
                    request.params = paramsList.toArray(new Object[paramsList.size()]);
                }
            }
            execResult = remoteExec.eval(isEvalAction, script, request);
        } else if (servletPath.endsWith("/exec")) {
            String action = getParameterValue(queryParams, ACTION_CN_PARAM);
            execResult = remoteExec.exec(action, request);
        } else {
            Pattern p = Pattern.compile(".*/exec/(.*)");
            Matcher m = p.matcher(servletPath);
            if(m.matches()) {
                String action = m.group(1);
                execResult = remoteExec.exec(action, request);
            }
        }

        HttpEntity entity = null;
        String contentDisposition = null;

        if (execResult != null) {
            Result<String> singleFileExtension = new Result<>();
            entity = getInputStreamFromList(execResult.results, getBodyUrl(execResult.results, returnBodyUrl), singleFileExtension);

            if (singleFileExtension.result != null) // если возвращается один файл, задаем ему имя
                contentDisposition = "filename=" + (returns.isEmpty() ? filename : returns.get(0)).replace(',', '_') + "." + singleFileExtension.result;
            return new ExternalResponse(entity, contentDisposition, execResult.headerNames, execResult.headerValues, execResult.cookieNames, execResult.cookieValues);
        }
        return new ExternalResponse(null, null, null, null, null, null);
    }

    public static String getBodyUrl(Object[] results, boolean returnBodyUrl) {
        String bodyUrl = null;
        if (results.length > 1 && returnBodyUrl) {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < results.length; i++) {
                Object value = results[i];
                result.append(String.format("%s=%s", ((result.length() == 0) ? "" : "&") + "param" + i, value));
            }
            bodyUrl = result.toString();
        }
        return bodyUrl;
    }

    public static ContentType getContentType(String extension) {
        String mimeType = MIMETypeUtils.MIMETypeForFileExtension(extension);
        String charset = null;
        switch (extension) {
            case "csv":
                charset = defaultCSVCharset;
                break;
            case "json":
            case "xml":
                charset = defaultXMLJSONCharset;
                break;
        }
        return charset != null ? ContentType.create(mimeType, charset) : ContentType.create(mimeType);
    }

    public static String getExtensionFromContentType(ContentType contentType) {
        return MIMETypeUtils.fileExtensionForMIMEType(contentType.getMimeType());
    }

    private static String getParameterValue(List<NameValuePair> queryParams, String key) {
        ImList<String> params = getParameterValues(queryParams, key);
        return params.isEmpty() ? null : params.get(0);
    }

    private static ImList<String> getParameterValues(List<NameValuePair> queryParams, String key) {
        MList<String> mValues = ListFact.mListMax(queryParams.size());
        for(NameValuePair queryParam : queryParams) {
            if(queryParam.getName().equalsIgnoreCase(key))
                mValues.add(queryParam.getValue());
        }
        return mValues.immutableList();
    }

    private static Object getRequestParam(Object object, ContentType contentType, boolean convertedToString) {
        assert object instanceof byte[] || (object instanceof String && convertedToString);
        String mimeType = contentType.getMimeType();
        String extension = getExtensionFromContentType(contentType);
        Charset charset = getCharsetFromContentType(contentType);
        if(extension != null) { // FILE
            RawFileData file;
            if(mimeType.startsWith("text/") && convertedToString) //humanReadable
                file = new RawFileData(((String)object).getBytes(charset));
            else
                file = new RawFileData((byte[])object);
            return new FileData(file, extension);
        } else {
            if(!convertedToString)
                object = new String((byte[]) object, charset);
            return object;
        }
    }

    public static Charset getCharsetFromContentType(ContentType contentType) {
        Charset charset = null;
        if(contentType != null)
            charset = contentType.getCharset();
        if(charset == null)
            charset = Consts.ISO_8859_1; // HTTP spec, хотя тут может быть нюанс что по спецификации некоторых content-type'ов (например application/json) может быть другая default кодировка         
        return charset;
    }

    // returns String or FileData
    public static ImList<Object> getListFromInputStream(InputStream is, ContentType contentType) throws IOException, MessagingException {
        return getListFromInputStream(IOUtils.readBytesFromStream(is), contentType);
    }
    // returns FileData for FILE or String for other classes, contentType can be null if there are no parameters
    public static ImList<Object> getListFromInputStream(byte[] bytes, ContentType contentType) throws IOException, MessagingException {
        MList<Object> mParamsList = ListFact.mList();
        if (contentType != null) { // если есть параметры, теоретически можно было бы пытаться по другому угадать
            String mimeType = contentType.getMimeType();
            if (mimeType.startsWith("multipart/")) {
                MimeMultipart multipart = new MimeMultipart(new ByteArrayDataSource(bytes, mimeType));
                for (int i = 0; i < multipart.getCount(); i++) {
                    BodyPart bodyPart = multipart.getBodyPart(i);
                    Object param = bodyPart.getContent();
                    ContentType partContentType = ContentType.parse(bodyPart.getContentType());
                    if(param instanceof InputStream)
                        mParamsList.addAll(getListFromInputStream((InputStream)param, partContentType));
                    else
                        mParamsList.add(getRequestParam(param, partContentType, true)); // multipart автоматически text/* возвращает как String
                }
            } else if(mimeType.equalsIgnoreCase(APPLICATION_FORM_URLENCODED.getMimeType())) {
                Charset charset = getCharsetFromContentType(contentType);
                List<NameValuePair> params = URLEncodedUtils.parse(new String(bytes, charset), charset);
                for(NameValuePair param : params)
                    mParamsList.add(getRequestParam(param.getValue(), ContentType.create(TEXT_PLAIN.getMimeType(), charset), true));
            } else
                mParamsList.add(getRequestParam(bytes, contentType, false));
        }
        return mParamsList.immutableList();
    }

    // results byte[] || String, можно было бы попровать getRequestResult (по аналогии с getRequestParam) выделить общий, но там возвращаемые классы разные, нужны будут generic'и и оно того не стоит
    public static HttpEntity getInputStreamFromList(Object[] results, String bodyUrl, Result<String> singleFileExtension) {
        HttpEntity entity;
        int paramCount = results.length;
        if (paramCount > 1) {
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setContentType(ExternalUtils.MULTIPART_MIXED);
            for (int i = 0; i < paramCount; i++) {
                Object value = results[i];
                if (value instanceof FileData) {
                    String extension = ((FileData) value).getExtension();
                    builder.addPart("param" + i, new ByteArrayBody(((FileData) value).getRawFile().getBytes(), getContentType(extension), "filename"));
                } else {
                    builder.addPart("param" + i, new StringBody((String) value, ExternalUtils.TEXT_PLAIN));
                }
            }
            entity = builder.build();
        } else if(paramCount == 1) {
            Object value = BaseUtils.single(results);
            if (value instanceof FileData) {
                String extension = ((FileData) value).getExtension();
                entity = new ByteArrayEntity(((FileData) value).getRawFile().getBytes(), getContentType(extension));
                if(singleFileExtension != null)
                    singleFileExtension.set(extension);
            } else {
                entity = new StringEntity((String) value, ExternalUtils.TEXT_PLAIN);
            }
        } else {
            if (bodyUrl != null) {
                entity = new StringEntity(bodyUrl, APPLICATION_FORM_URLENCODED);
            } else {
                entity = new StringEntity("", ExternalUtils.TEXT_PLAIN);
            }
        }
        return entity;
    }

    public static class ExternalResponse {
        public final HttpEntity response;
        public final String contentDisposition;
        public final String[] headerNames;
        public final String[] headerValues;
        public final String[] cookieNames;
        public final String[] cookieValues;

        public ExternalResponse(HttpEntity response, String contentDisposition, String[] headerNames, String[] headerValues, String[] cookieNames, String[] cookieValues) {
            this.response = response;
            this.contentDisposition = contentDisposition;
            this.headerNames = headerNames;
            this.headerValues = headerValues;
            this.cookieNames = cookieNames;
            this.cookieValues = cookieValues;
        }
    }
}