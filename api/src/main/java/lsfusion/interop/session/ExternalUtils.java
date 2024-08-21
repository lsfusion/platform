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
import org.apache.hc.client5.http.entity.mime.*;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.net.URLEncodedUtils;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static lsfusion.base.BaseUtils.*;

public class ExternalUtils {

    public static final String defaultCSVSeparator = ";";

    // CHARSETS

    // "bytes-to-string" charsets
    public static Charset fileCharset = StandardCharsets.UTF_8; // when "data" files are converted to strings (should match cast)
    public static Charset resourceCharset = StandardCharsets.UTF_8; // when "disk" (usually "resource") files are converted (read) to strings
    public static Charset printCharset = StandardCharsets.UTF_8; // when "printed" files are converted to string
    public static Charset downloadCharset = StandardCharsets.UTF_8; // when file ("disk" or "data" or "printed") is downloaded to the client
    public static Charset jsonCharset = StandardCharsets.UTF_8; // when json file comes from the "unknown" place (i.e javascript client, url.openStream, etc.)

    // reversed ("string-to/from-bytes") charsets
    public static final String defaultCSVCharset = "UTF-8";
    public static final String defaultXMLJSONCharset = "UTF-8";
    public static final String defaultDBFCharset = "CP1251";

    public static Charset defaultUrlCharset = StandardCharsets.UTF_8;
    public static Charset defaultCookieCharset = StandardCharsets.UTF_8;
    public static Charset defaultBodyUrlCharset = StandardCharsets.UTF_8;
    public static Charset defaultBodyCharset = StandardCharsets.UTF_8;

    public static Charset hashCharset = StandardCharsets.UTF_8;
    public static Charset imageCharset = StandardCharsets.UTF_8;
    public static Charset fileDataNameCharset = StandardCharsets.UTF_8; // file types name encoding

    // "string-to-bytes" charsets
    public static Charset javaCharset = StandardCharsets.UTF_8;
    public static Charset emailCharset = StandardCharsets.UTF_8;

    public static final String ACTION_CN_PARAM = "action";
    public static final String SCRIPT_PARAM = "script";
    public static final String PARAMS_PARAM = "p";
    public static final String RETURN_PARAM = "return";
    public static final String RETURNMULTITYPE_PARAM = "returnmultitype";

    public static final String SIGNATURE_PARAM = "signature";

    public static final String NEED_NOTIFICATION_ID_HEADER = "Need-Notification-Id";

    public static final String generate(Object actionParam, boolean script, Object[] params) {
        String paramsString = "";
        for(int i = 0; i < params.length; i++)
            paramsString += "," + params[i];
        return (script ? "1" : "0") + "," + actionParam + paramsString;
    }

    public static final List<String> PARAMS = Arrays.asList(ACTION_CN_PARAM, SCRIPT_PARAM, PARAMS_PARAM, RETURN_PARAM, RETURNMULTITYPE_PARAM, SIGNATURE_PARAM);

    public static ExecInterface getExecInterface(final AuthenticationToken token, final SessionInfo sessionInfo, final RemoteLogicsInterface remoteLogics) {
        return new ExecInterface() {
            public lsfusion.interop.session.ExternalResponse exec(String action, ExternalRequest request) throws RemoteException {
                return remoteLogics.exec(token, sessionInfo, action, request);
            }
            public lsfusion.interop.session.ExternalResponse eval(boolean action, ExternalRequest.Param paramScript, ExternalRequest request) throws RemoteException {
                return remoteLogics.eval(token, sessionInfo, action, paramScript, request);
            }
        };
    }

    public static int DEFAULT_COOKIE_VERSION = 0;
    public static String encodeCookie(String cookie, int version) {
        if(version == 0) {
            try {
                return URLEncoder.encode(cookie, defaultCookieCharset.name());
            } catch (UnsupportedEncodingException e) {
                throw Throwables.propagate(e);
            }
        }
        return cookie;
    }
    public static String decodeCookie(String cookie, int version) {
        if(version == 0) {
            try {
                return URLDecoder.decode(cookie, defaultCookieCharset.name());
            } catch (UnsupportedEncodingException e) {
                throw Throwables.propagate(e);
            }
        }
        return cookie;
    }

    public static ExternalResponse processRequest(ExecInterface remoteExec, ConvertFileValue convertFileValue, InputStream is, ContentType requestContentType,
                                                  String[] headerNames, String[] headerValues, String[] cookieNames, String[] cookieValues, String logicsHost,
                                                  Integer logicsPort, String logicsExportName, String scheme, String method, String webHost, Integer webPort,
                                                  String contextPath, String servletPath, String pathInfo, String query, String sessionId) throws IOException, MessagingException {
        Charset urlCharset = ExternalUtils.defaultUrlCharset;
        String urlCharsetName = urlCharset.toString();
        List<NameValuePair> queryParams = URLEncodedUtils.parse(query, urlCharset);

        ImList<String> queryActionParams = getParameterValues(queryParams, PARAMS_PARAM);
        byte[] body = IOUtils.readBytesFromStream(is);
        ImList<ExternalRequest.Param> bodyActionParams = getListFromInputStream(body, requestContentType);
        ImList<ExternalRequest.Param> paramsList = ListFact.add(queryActionParams.mapListValues(value -> ExternalRequest.getUrlParam(value, urlCharsetName)), bodyActionParams);
        
        ImList<String> returns = getParameterValues(queryParams, RETURN_PARAM);

        String signature = getParameterValue(queryParams, SIGNATURE_PARAM);

        boolean needNotificationId = getHeaderValue(headerNames, headerValues, NEED_NOTIFICATION_ID_HEADER) != null;

        ExternalRequest request = new ExternalRequest(returns.toArray(new String[0]), paramsList.toArray(new ExternalRequest.Param[paramsList.size()]),
                queryParams, urlCharsetName, headerNames, headerValues, cookieNames,
                cookieValues, logicsHost, logicsPort, logicsExportName, scheme, method, webHost, webPort, contextPath,
                servletPath, pathInfo, query, requestContentType != null ? requestContentType.toString() : null, sessionId, body, signature, needNotificationId);

        lsfusion.interop.session.ExternalResponse execResult = null;
        String path = servletPath + pathInfo;
        boolean isEvalAction = path.endsWith("/eval/action");
        if (path.endsWith("/eval") || isEvalAction) {
            ExternalRequest.Param paramScript = null;
            String script = getParameterValue(queryParams, SCRIPT_PARAM);
            if (script == null) { // if we don't have script param, we consider the first body param as script
                int scriptParam = queryActionParams.size();
                if(scriptParam < paramsList.size()) {
                    paramScript = paramsList.get(scriptParam);
                    paramsList = paramsList.remove(scriptParam);
                    request.params = paramsList.toArray(new ExternalRequest.Param[paramsList.size()]);
                }
            } else
                paramScript = ExternalRequest.getUrlParam(script, urlCharsetName);
            execResult = remoteExec.eval(isEvalAction, paramScript, request);
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

        return getExternalResponse(execResult, queryParams, (returns.isEmpty() ? "export" : returns.get(0)), convertFileValue);
    }

    public static ExternalResponse getExternalResponse(lsfusion.interop.session.ExternalResponse execResult, List<NameValuePair> queryParams, String singleFileName, ConvertFileValue convertFileValue) {
        if(execResult instanceof lsfusion.interop.session.ResultExternalResponse) {
            lsfusion.interop.session.ResultExternalResponse resultExecResult = (lsfusion.interop.session.ResultExternalResponse) execResult;
            Result<String> singleFileExtension = singleFileName != null ? new Result<>() : null;
            HttpEntity entity = getInputStreamFromList(resultExecResult, convertFileValue, queryParams, singleFileExtension);

            String contentDisposition = null;
            if (singleFileName != null && singleFileExtension.result != null) // если возвращается один файл, задаем ему имя
                contentDisposition = "filename=" + singleFileName.replace(',', '_') + "." + singleFileExtension.result;
            return new ResultExternalResponse(entity, contentDisposition, resultExecResult.headerNames, resultExecResult.headerValues, resultExecResult.cookieNames, resultExecResult.cookieValues, resultExecResult.statusHttp);
        } else if(execResult instanceof lsfusion.interop.session.HtmlExternalResponse) {
            return new HtmlExternalResponse(((lsfusion.interop.session.HtmlExternalResponse) execResult).html);
        } else if(execResult instanceof lsfusion.interop.session.RedirectExternalResponse)
            return new RedirectExternalResponse(((lsfusion.interop.session.RedirectExternalResponse) execResult).url, ((lsfusion.interop.session.RedirectExternalResponse) execResult).notification);

        return new ExternalUtils.ResultExternalResponse("Something went wrong", StandardCharsets.UTF_8, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    public static String getBodyUrl(Object[] results, boolean returnBodyUrl, Charset bodyUrlCharset) {
        String bodyUrl = null;
        if (results.length > 1 && returnBodyUrl) {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < results.length; i++) {
                Object value = results[i];
                result.append(String.format("%s=%s", ((result.length() == 0) ? "" : "&") + "param" + i, encodeUrlParam(bodyUrlCharset, value)));
            }
            bodyUrl = result.toString();
        }
        return bodyUrl;
    }

    private static ContentType getUrlEncodedContentType(Charset charset) {
        return ContentType.create(ContentType.APPLICATION_FORM_URLENCODED.getMimeType(), charset);
    }

    private static ContentType getStringContentType(Charset charset) {
        return ContentType.create(ContentType.TEXT_PLAIN.getMimeType(), charset);
    }

    public static ContentType getHtmlContentType(Charset charset) {
        return ContentType.create(ContentType.TEXT_HTML.getMimeType(), charset);
    }

    private static ContentType getMultipartContentType(Charset charset) {
        return ContentType.create(ContentType.MULTIPART_MIXED.getMimeType(), charset);
    }

    public static ContentType getContentType(String extension, Charset charset) {
        return ContentType.create(MIMETypeUtils.MIMETypeForFileExtension(extension), charset);
    }

    public static String getExtensionFromContentType(ContentType contentType) {
        return MIMETypeUtils.fileExtensionForMIMEType(contentType.getMimeType());
    }

    public static String getParameterValue(List<NameValuePair> queryParams, String key) {
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

    private static String getHeaderValue(String[] headerNames, String[] headerValues, String header) {
        for(int i = 0; i < headerNames.length; i++) {
            if(headerNames[i].equalsIgnoreCase(header))
                return headerValues[i];
        }
        return null;
    }

    private static ExternalRequest.Param getRequestParam(Object object, ContentType contentType, boolean convertedToString) {
        assert object instanceof byte[] || (object instanceof String && convertedToString);
        Charset charset = getBodyCharset(contentType);

        String extension = contentType != null ? getExtensionFromContentType(contentType) : null;
        Object value;
        if(extension != null) { // FILE
            RawFileData file;
            if(contentType.getMimeType().startsWith("text/") && convertedToString) //humanReadable
                file = new RawFileData((String)object, charset);
            else
                file = new RawFileData((byte[])object);
            value = new FileData(file, extension);
        } else {
            if(!convertedToString)
                object = new String((byte[]) object, charset);
            value = object;
        }
        return ExternalRequest.getBodyParam(value, charset.toString());
    }

    public static Charset getLoggingCharsetFromContentType(String contentType) {
        return getBodyCharset(parseContentType(contentType));
    }

    public static ContentType parseContentType(String contentType) {
        return contentType != null ? ContentType.parse(contentType) : null;
    }

    // REQUEST BODY
    public static Charset getBodyUrlCharset(ContentType contentType) {
        if(contentType != null) {
            Charset charset = contentType.getCharset();
            if(charset != null)
                return charset;
        }
        return defaultBodyUrlCharset;
    }
    public static Charset getBodyCharset(ContentType contentType) {
        if(contentType != null) {
            Charset charset = contentType.getCharset();
            if(charset != null)
                return charset;
        }
        return defaultBodyCharset;
    }

    // returns FileData for FILE or String for other classes, contentType can be null if there are no parameters
    public static ImList<ExternalRequest.Param> getListFromInputStream(byte[] bytes, ContentType contentType) throws IOException, MessagingException {
        MList<ExternalRequest.Param> mParamsList = ListFact.mList();

        String mimeType = contentType != null ? contentType.getMimeType() : null;

        if (mimeType != null && mimeType.startsWith("multipart/")) {
            MimeMultipart multipart = new MimeMultipart(new ByteArrayDataSource(bytes, mimeType));
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                Object param = bodyPart.getContent();
                ContentType partContentType = parseContentType(bodyPart.getContentType());
                if(param instanceof InputStream)
                    mParamsList.addAll(getListFromInputStream(IOUtils.readBytesFromStream((InputStream) param), partContentType));
                else
                    mParamsList.add(getRequestParam(param, partContentType, true)); // multipart автоматически text/* возвращает как String
            }
        } else if(mimeType != null && mimeType.equalsIgnoreCase(ContentType.APPLICATION_FORM_URLENCODED.getMimeType())) {
            Charset charset = getBodyUrlCharset(contentType);
            List<NameValuePair> params = URLEncodedUtils.parse(new String(bytes, charset), charset);
            for(NameValuePair param : params)
                mParamsList.add(ExternalRequest.getBodyUrlParam(param.getValue(), charset.toString()));
        } else
            mParamsList.add(getRequestParam(bytes, contentType, false));

        return mParamsList.immutableList();
    }

    public static class ResponseType {
        public final ContentType forceContentType;
        public final boolean returnBodyUrl;
        public final Charset charset;

        public ResponseType(ContentType forceContentType, boolean returnBodyUrl, Charset charset) {
            this.forceContentType = forceContentType;
            this.returnBodyUrl = returnBodyUrl;
            this.charset = charset;
        }
    }

    public static ResponseType getResponseType(List<NameValuePair> queryParams, String[] headerNames, String[] headerValues) {
        ContentType forceContentType = parseContentType(getHeaderValue(headerNames, headerValues, "Content-Type"));

        boolean returnBodyUrl = false;
        if(queryParams != null) {
            String returnMultiType = getParameterValue(queryParams, RETURNMULTITYPE_PARAM);
            returnBodyUrl = returnMultiType != null && returnMultiType.equals("bodyurl");
        }

        Charset charset = returnBodyUrl ? getBodyUrlCharset(forceContentType) : getBodyCharset(forceContentType);
        return new ResponseType(forceContentType, returnBodyUrl, charset);
    }

    public static HttpEntity getInputStreamFromList(lsfusion.interop.session.ResultExternalResponse response, ConvertFileValue convertFileValue, List<NameValuePair> queryParams, Result<String> singleFileExtension) {
        Object[] results = convertFileValue(convertFileValue, response.results);

        ResponseType responseType = getResponseType(queryParams, response.headerNames, response.headerValues);
        return getInputStreamFromList(results, getBodyUrl(results, responseType.returnBodyUrl, responseType.charset), null, new ArrayList<>(), singleFileExtension, responseType.forceContentType, responseType.charset);
    }

    private static Object[] convertFileValue(ConvertFileValue convertFileValue, Object[] results) {
        Object[] convertedResults = new Object[results.length];
        for(int i = 0; i < results.length; i++)
            convertedResults[i] = convertFileValue.convertFileValue(results[i]);
        return convertedResults;
    }

    // results byte[] || String, можно было бы попровать getRequestResult (по аналогии с getRequestParam) выделить общий, но там возвращаемые классы разные, нужны будут generic'и и оно того не стоит
    public static HttpEntity getInputStreamFromList(Object[] results, String bodyUrl, ImList<String> bodyParamNames, List<Map<String, String>> bodyParamHeadersList, Result<String> singleFileExtension, ContentType forceContentType, Charset charset) {
        HttpEntity entity;
        int paramCount = results.length;
        if (paramCount > 1 || (bodyParamNames != null && !bodyParamNames.isEmpty())) {
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            //Default mode is STRICT, which uses only US-ASCII. EXTENDED mode allows UTF-8.
            builder.setMode(HttpMultipartMode.EXTENDED);
            builder.setContentType(nvl(forceContentType, getMultipartContentType(charset)));
            for (int i = 0; i < paramCount; i++) {
                Object value = results[i];
                String[] bodyParamName = trimToEmpty(bodyParamNames != null && i < bodyParamNames.size() ? bodyParamNames.get(i) : null).split(";");
                String bodyPartName = isEmpty(bodyParamName[0]) ? ("param" + i) : bodyParamName[0];
                FormBodyPart formBodyPart;
                if (value instanceof FileData) {
                    String fileName = bodyParamName.length < 2 || isEmpty(bodyParamName[1]) ? "filename" : bodyParamName[1];
                    String extension = ((FileData) value).getExtension();
                    formBodyPart = FormBodyPartBuilder.create(bodyPartName, new ByteArrayBody(((FileData) value).getRawFile().getBytes(), getContentType(extension, charset), fileName)).build();
                } else {
                    formBodyPart = FormBodyPartBuilder.create(bodyPartName, new StringBody((String) value, getStringContentType(charset))).build();
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
                entity = new ByteArrayEntity(((FileData) value).getRawFile().getBytes(), nvl(forceContentType, getContentType(extension, charset)));
                if(singleFileExtension != null)
                    singleFileExtension.set(extension);
            } else {
                entity = new StringEntity((String) value, nvl(forceContentType, getStringContentType(charset)));
            }
        } else {
            entity = bodyUrl != null ? new StringEntity(bodyUrl, nvl(forceContentType, getUrlEncodedContentType(charset))) : null;
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

    public static String encodeUrlParam(Charset urlEncodeCharset, Object value) {
        String stringValue;
        String charsetName = urlEncodeCharset.name();
        if(value instanceof FileData)
            stringValue = encodeFileData((FileData) value, charsetName);
        else
            stringValue = (String) value;

        try {
            return URLEncoder.encode(stringValue, charsetName);
        } catch (UnsupportedEncodingException e) {
            throw Throwables.propagate(e);
        }
    }

    public static String encodeFileData(FileData fileData, String encodeCharset) {
        return fileData.getRawFile().getString(encodeCharset);
    }

    public static FileData decodeFileData(String string, String encodeCharset, String extension) {
        return new FileData(new RawFileData(string, encodeCharset), extension);
    }

    // should correspond PValue.convertFileValue
    public static String convertFileValue(String[] prefixes, String[] urls) {
        StringBuilder result = new StringBuilder();
        for (int j = 0; j < prefixes.length; j++) {
            result.append(prefixes[j]);
            if(j < urls.length) {
                Serializable url = urls[j];
                if(url != null) // file
                    result.append((String) url);
            }
        }
        return result.toString();
    }

    public static class ExternalResponse {
    }
    public static class ResultExternalResponse extends ExternalResponse {
        public final HttpEntity response;
        public final String contentDisposition;
        public final String[] headerNames;
        public final String[] headerValues;
        public final String[] cookieNames;
        public final String[] cookieValues;
        public final int statusHttp;

        public ResultExternalResponse(String message, Charset charset, int statusHttp) {
            this(new StringEntity(message, charset), null, null, null, null, null, statusHttp);
        }
        public ResultExternalResponse(HttpEntity response, String contentDisposition, String[] headerNames, String[] headerValues, String[] cookieNames, String[] cookieValues, Integer statusHttp) {
            this.response = response;
            this.contentDisposition = contentDisposition;
            this.headerNames = headerNames;
            this.headerValues = headerValues;
            this.cookieNames = cookieNames;
            this.cookieValues = cookieValues;
            this.statusHttp = statusHttp;
        }
    }
    public static class HtmlExternalResponse extends ExternalResponse {

        public final String html;

        public HtmlExternalResponse(String html) {
            this.html = html;
        }
    }
    public static class RedirectExternalResponse extends ExternalResponse {

        public final String url;
        public final Integer notification;

        public RedirectExternalResponse(String url, Integer notification) {
            this.url = url;
            this.notification = notification;
        }
    }
}