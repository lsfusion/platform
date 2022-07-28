package lsfusion.interop.session;

import com.google.common.base.Throwables;
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
import org.apache.http.entity.mime.FormBodyPart;
import org.apache.http.entity.mime.FormBodyPartBuilder;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.log4j.Logger;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static lsfusion.base.BaseUtils.*;
import static org.apache.http.entity.ContentType.APPLICATION_FORM_URLENCODED;

public class ExternalUtils {

    public static final Logger httpServerLogger = Logger.getLogger("HttpServerLogger");

    public static final String defaultCSVSeparator = ";";
    public static final String defaultCSVCharset = "UTF-8";
    public static final String defaultXMLJSONCharset = "UTF-8";
    public static final String defaultDBFCharset = "CP1251";

    public static final Charset stringCharset = Consts.UTF_8;
    public static final ContentType TEXT_PLAIN = ContentType.create(
            "text/plain", stringCharset);
    public static final ContentType MULTIPART_MIXED = ContentType.create(
            "multipart/mixed", stringCharset);
    public static final ContentType APPLICATION_OCTET_STREAM = ContentType.create(
            "application/octet-stream");

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

    public static int DEFAULT_COOKIE_VERSION = 0;
    public static String encodeCookie(String cookie, int version) {
        if(version == 0) {
            try {
                return URLEncoder.encode(cookie, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw Throwables.propagate(e);
            }
        }
        return cookie;
    }
    public static String decodeCookie(String cookie, int version) {
        if(version == 0) {
            try {
                return URLDecoder.decode(cookie, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw Throwables.propagate(e);
            }
        }
        return cookie;
    }

    //web request, compatibility to not change apiVersion
    public static ExternalResponse processRequest(ExecInterface remoteExec, InputStream is, ContentType requestContentType,
                                                  String[] headerNames, String[] headerValues, String[] cookieNames, String[] cookieValues, String logicsHost,
                                                  Integer logicsPort, String logicsExportName, String scheme, String method, String webHost, Integer webPort,
                                                  String contextPath, String servletPath, String pathInfo, String query) throws IOException, MessagingException {
        return processRequest(remoteExec, is, requestContentType, headerNames, headerValues, cookieNames, cookieValues, logicsHost, logicsPort, logicsExportName,
                scheme, method, webHost, webPort, contextPath, servletPath, pathInfo, query, false);
    }

    public static ExternalResponse processRequest(ExecInterface remoteExec, InputStream is, ContentType requestContentType,
                                                  String[] headerNames, String[] headerValues, String[] cookieNames, String[] cookieValues, String logicsHost,
                                                  Integer logicsPort, String logicsExportName, String scheme, String method, String webHost, Integer webPort,
                                                  String contextPath, String servletPath, String pathInfo, String query, boolean logBody) throws IOException, MessagingException {
        Charset charset = getCharsetFromContentType(requestContentType);
        List<NameValuePair> queryParams = URLEncodedUtils.parse(query, charset);

        ImList<String> queryActionParams = getParameterValues(queryParams, PARAMS_PARAM);
        byte[] body = IOUtils.readBytesFromStream(is);
        if(logBody) {
            httpServerLogger.info("request body: " + new String(body, charset));
        }
        ImList<Object> bodyActionParams = getListFromInputStream(body, requestContentType);
        ImList<Object> paramsList = ListFact.add(queryActionParams, bodyActionParams);
        
        ImList<String> returns = getParameterValues(queryParams, RETURN_PARAM);
        String returnMultiType = getParameterValue(queryParams, RETURNMULTITYPE_PARAM);
        boolean returnBodyUrl = returnMultiType != null && returnMultiType.equals("bodyurl");
        lsfusion.interop.session.ExternalResponse execResult = null;
        
        String filename = "export";

        ExternalRequest request = new ExternalRequest(returns.toArray(new String[0]), paramsList.toArray(new Object[paramsList.size()]),
                charset == null ? null : charset.toString(), headerNames, headerValues, cookieNames,
                cookieValues, logicsHost, logicsPort, logicsExportName, scheme, method, webHost, webPort, contextPath,
                servletPath, pathInfo, query, requestContentType != null ? requestContentType.toString() : null, body);

        String path = servletPath + pathInfo;
        boolean isEvalAction = path.endsWith("/eval/action");
        if (path.endsWith("/eval") || isEvalAction) {
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
        } else if (path.endsWith("/exec")) {
            String action = getParameterValue(queryParams, ACTION_CN_PARAM);
            execResult = remoteExec.exec(action, request);
        } else {
            Pattern p = Pattern.compile(".*/exec/(.*)");
            Matcher m = p.matcher(path);
            if(m.matches()) {
                String action = m.group(1);
                execResult = remoteExec.exec(action, request);
            }
        }

        HttpEntity entity = null;
        String contentDisposition = null;

        if (execResult != null) {
            Result<String> singleFileExtension = new Result<>();
            entity = getInputStreamFromList(execResult.results, getBodyUrl(execResult.results, returnBodyUrl), new ArrayList<>(), new ArrayList<>(), singleFileExtension, null);

            if (singleFileExtension.result != null) // если возвращается один файл, задаем ему имя
                contentDisposition = "filename=" + (returns.isEmpty() ? filename : returns.get(0)).replace(',', '_') + "." + singleFileExtension.result;
            return new ExternalResponse(entity, contentDisposition, execResult.headerNames, execResult.headerValues, execResult.cookieNames, execResult.cookieValues, execResult.statusHttp);
        }
        return new ExternalResponse(null, null, null, null, null, null, null);
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
    public static HttpEntity getInputStreamFromList(Object[] results, String bodyUrl, List<String> bodyParamNames, List<Map<String, String>> bodyParamHeadersList, Result<String> singleFileExtension, ContentType forceContentType) {
        HttpEntity entity;
        int paramCount = results.length;
        if (paramCount > 1 || (bodyParamNames != null && !bodyParamNames.isEmpty())) {
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setContentType(nvl(forceContentType, ExternalUtils.MULTIPART_MIXED));
            for (int i = 0; i < paramCount; i++) {
                Object value = results[i];
                String[] bodyParamName = trimToEmpty(bodyParamNames != null && i < bodyParamNames.size() ? bodyParamNames.get(i) : null).split(";");
                String bodyPartName = isEmpty(bodyParamName[0]) ? ("param" + i) : bodyParamName[0];
                FormBodyPart formBodyPart;
                if (value instanceof FileData) {
                    String fileName = bodyParamName.length < 2 || isEmpty(bodyParamName[1]) ? "filename" : bodyParamName[1];
                    String extension = ((FileData) value).getExtension();
                    formBodyPart = FormBodyPartBuilder.create(bodyPartName, new ByteArrayBody(((FileData) value).getRawFile().getBytes(), getContentType(extension), fileName)).build();
                } else {
                    formBodyPart = FormBodyPartBuilder.create(bodyPartName, new StringBody((String) value, ExternalUtils.TEXT_PLAIN)).build();
                }
                Map<String, String> bodyParamHeaders = bodyParamHeadersList.size() > i ? bodyParamHeadersList.get(i) : null;
                if(bodyParamHeaders != null) {
                    for (Map.Entry<String, String> bodyParamHeader : bodyParamHeaders.entrySet()) {
                        formBodyPart.addField(bodyParamHeader.getKey(), bodyParamHeader.getValue());
                    }
                }
                builder.addPart(formBodyPart);
            }
            entity = builder.build();
        } else if(paramCount == 1) {
            Object value = BaseUtils.single(results);
            if (value instanceof FileData) {
                String extension = ((FileData) value).getExtension();
                entity = new ByteArrayEntity(((FileData) value).getRawFile().getBytes(), nvl(forceContentType, getContentType(extension)));
                if(singleFileExtension != null)
                    singleFileExtension.set(extension);
            } else {
                entity = new StringEntity((String) value, nvl(forceContentType, ExternalUtils.TEXT_PLAIN));
            }
        } else {
            if (bodyUrl != null) {
                entity = new StringEntity(bodyUrl, nvl(forceContentType, APPLICATION_FORM_URLENCODED));
            } else {
                entity = new StringEntity("", nvl(forceContentType, ExternalUtils.TEXT_PLAIN));
            }
        }
        return entity;
    }

    public static byte[] sendTCP(byte[] fileBytes, String host, Integer port, Integer timeout) throws IOException {
        try (Socket socket = new Socket(host, port)) {
            if (timeout != null) {
                socket.setSoTimeout(timeout);
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (OutputStream os = socket.getOutputStream(); InputStream is = socket.getInputStream()) {
                os.write(fileBytes);

                //reading finishes when the sender closes the stream or by SO timeout
                int b;
                while((b = is.read()) != -1) {
                    baos.write((byte) b);
                }
            } catch (SocketTimeoutException e) {
                //timeout exception, finish reading
            }
            return baos.toByteArray();
        }
    }

    public static void sendUDP(byte[] fileBytes, String host, Integer port) throws IOException {
        try (DatagramSocket socket = new DatagramSocket()) {
            DatagramPacket packet = new DatagramPacket(fileBytes, fileBytes.length, InetAddress.getByName(host), port);
            socket.send(packet);
        }
    }

    public static class ExternalResponse {
        public final HttpEntity response;
        public final String contentDisposition;
        public final String[] headerNames;
        public final String[] headerValues;
        public final String[] cookieNames;
        public final String[] cookieValues;
        public final Integer statusHttp;

        public ExternalResponse(HttpEntity response, String contentDisposition, String[] headerNames, String[] headerValues, String[] cookieNames, String[] cookieValues, Integer statusHttp) {
            this.response = response;
            this.contentDisposition = contentDisposition;
            this.headerNames = headerNames;
            this.headerValues = headerValues;
            this.cookieNames = cookieNames;
            this.cookieValues = cookieValues;
            this.statusHttp = statusHttp;
        }
    }
}