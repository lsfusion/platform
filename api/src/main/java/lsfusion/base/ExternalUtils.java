package lsfusion.base;

import lsfusion.interop.ExecInterface;
import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.remote.AuthenticationToken;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.apache.http.entity.ContentType.APPLICATION_FORM_URLENCODED;
import java.util.Map;

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
            public lsfusion.base.ExternalResponse exec(String action, ExternalRequest request) throws RemoteException {
                return remoteLogics.exec(token, sessionInfo, action, request);
            }
            public lsfusion.base.ExternalResponse eval(boolean action, Object paramScript, ExternalRequest request) throws RemoteException {
                return remoteLogics.eval(token, sessionInfo, action, paramScript, request);
            }
        };
    }

    public static ExternalResponse processRequest(ExecInterface remoteExec, String url, String uri, String query, InputStream is, Map<String, String[]> requestParams, ContentType requestContentType, String[] headerNames, String[] headerValues, 
                                                  String logicsHost, Integer logicsPort, String logicsExportName) throws IOException, MessagingException {
        Charset charset = getCharsetFromContentType(requestContentType);
        List<NameValuePair> queryParams = URLEncodedUtils.parse(query, charset);

        List<Object> paramsList = BaseUtils.mergeList(getParameterValues(queryParams, PARAMS_PARAM), getListFromInputStream(is, requestContentType));
        List<String> returns = getParameterValues(queryParams, RETURN_PARAM);
        String returnMultiType = getParameterValue(queryParams, RETURNMULTITYPE_PARAM);
        boolean returnBodyUrl = returnMultiType != null && returnMultiType.equals("bodyurl");
        lsfusion.base.ExternalResponse execResult = null;
        
        String filename = "export";

        ExternalRequest request = new ExternalRequest(returns.toArray(new String[0]), paramsList.toArray(), 
                charset == null ? null : charset.toString(), url, query, headerNames, headerValues, logicsHost, logicsPort, logicsExportName);
        
        if (uri.endsWith("/exec")) {
            String action = getParameterValue(queryParams, ACTION_CN_PARAM);
            execResult = remoteExec.exec(action, request);
        } else {
            boolean isEvalAction = uri.endsWith("/eval/action");
            if (uri.endsWith("/eval") || isEvalAction) {
                Object script = getParameterValue(queryParams, SCRIPT_PARAM);
                if (!requestParams.isEmpty()) {
                    script = script != null ? script : getScriptRequestParam(requestParams);
                    paramsList = getRequestParams(requestParams);
                } else if (script == null && !paramsList.isEmpty()) {
                    //Первый параметр считаем скриптом
                    script = paramsList.get(0);
                    request.params = paramsList.subList(1, paramsList.size()).toArray();
                }
                execResult = remoteExec.eval(isEvalAction, script, request);
            }
        }

        HttpEntity entity = null;
        String contentDisposition = null;

        if (execResult != null) {
            Result<String> singleFileExtension = new Result<>();
            entity = getInputStreamFromList(execResult.results, getBodyUrl(execResult.results, returnBodyUrl), singleFileExtension);

            if (singleFileExtension.result != null) // если возвращается один файл, задаем ему имя
                contentDisposition = "filename=" + (returns.isEmpty() ? filename : returns.get(0)).replace(',', '_') + "." + singleFileExtension.result;
            return new ExternalResponse(entity, contentDisposition, execResult.headerNames, execResult.headerValues);
        }
        return new ExternalResponse(null, null, null, null);
    }

    private static String getScriptRequestParam(Map requestParams) {
        Object params = requestParams.get(SCRIPT_PARAM);
        if (params instanceof List && !((List) params).isEmpty()) {
            return String.valueOf(((List) params).get(0));
        } else return null;
    }

    private static List<Object> getRequestParams(Map<String, String[]> requestParams) {
        List<Object> params = new ArrayList<>();
        for (Map.Entry<String, String[]> entry : requestParams.entrySet()) {
            if (!entry.getKey().equals(SCRIPT_PARAM)) {
                params.addAll(Arrays.asList(entry.getValue()));
            }
        }
        return params;
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
        String mimeType = MIMETypeUtil.MIMETypeForFileExtension(extension);
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
        return MIMETypeUtil.fileExtensionForMIMEType(contentType.getMimeType());
    }

    private static String getParameterValue(List<NameValuePair> queryParams, String key) {
        List<String> params = getParameterValues(queryParams, key);
        return params.isEmpty() ? null : params.get(0);
    }

    private static List<String> getParameterValues(List<NameValuePair> queryParams, String key) {
        List<String> values = new ArrayList<>();
        for(NameValuePair queryParam : queryParams) {
            if(queryParam.getName().equalsIgnoreCase(key))
                values.add(queryParam.getValue());
        }
        return values;
    }

    private static Object getRequestParam(Object object, ContentType contentType, boolean convertedToString) throws IOException {
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
    public static List<Object> getListFromInputStream(InputStream is, ContentType contentType) throws IOException, MessagingException {
        return getListFromInputStream(IOUtils.readBytesFromStream(is), contentType);
    }
    // returns FileData for FILE or String for other classes, contentType can be null if there are no parameters
    public static List<Object> getListFromInputStream(byte[] bytes, ContentType contentType) throws IOException, MessagingException {
        List<Object> paramsList = new ArrayList<>();
        if (contentType != null) { // если есть параметры, теоретически можно было бы пытаться по другому угадать
            String mimeType = contentType.getMimeType();
            if (mimeType.startsWith("multipart/")) {
                MimeMultipart multipart = new MimeMultipart(new ByteArrayDataSource(bytes, mimeType));
                for (int i = 0; i < multipart.getCount(); i++) {
                    BodyPart bodyPart = multipart.getBodyPart(i);
                    Object param = bodyPart.getContent();
                    ContentType partContentType = ContentType.parse(bodyPart.getContentType());
                    if(param instanceof InputStream)
                        paramsList.addAll(getListFromInputStream((InputStream)param, partContentType));
                    else
                        paramsList.add(getRequestParam(param, partContentType, true)); // multipart автоматически text/* возвращает как String
                }
            } else if(mimeType.equalsIgnoreCase(APPLICATION_FORM_URLENCODED.getMimeType())) {
                Charset charset = getCharsetFromContentType(contentType);
                List<NameValuePair> params = URLEncodedUtils.parse(new String(bytes, charset), charset);
                for(NameValuePair param : params)
                    paramsList.add(getRequestParam(param.getValue(), ContentType.create(TEXT_PLAIN.getMimeType(), charset), true));                    
            } else
                paramsList.add(getRequestParam(bytes, contentType, false));
        }
        return paramsList;
    }

    // results byte[] || String, можно было бы попровать getRequestResult (по аналогии с getRequestParam) выделить общий, но там возвращаемые классы разные, нужны будут generic'и и оно того не стоит
    public static HttpEntity getInputStreamFromList(Object[] results, String bodyUrl, Result<String> singleFileExtension) {
        HttpEntity entity;
        int paramCount = results.length;
        if (paramCount > 1) {
            if(bodyUrl != null) {
                entity = new StringEntity(bodyUrl, APPLICATION_FORM_URLENCODED);
            } else {
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
            }
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
            entity = new StringEntity("", ExternalUtils.TEXT_PLAIN);
        }
        return entity;
    }

    public static class ExternalResponse {
        public final HttpEntity response;
        public final String contentDisposition;
        public final String[] headerNames;
        public final String[] headerValues;

        public ExternalResponse(HttpEntity response, String contentDisposition, String[] headerNames, String[] headerValues) {
            this.response = response;
            this.contentDisposition = contentDisposition;
            this.headerNames = headerNames;
            this.headerValues = headerValues;
        }
    }
}