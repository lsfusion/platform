package lsfusion.interop.session;

import com.google.common.base.Throwables;
import lsfusion.base.MIMETypeUtils;
import lsfusion.base.Result;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.base.file.FileData;
import lsfusion.base.file.IOUtils;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.interop.connection.ConnectionInfo;
import lsfusion.interop.logics.remote.RemoteLogicsInterface;
import lsfusion.interop.session.remote.RemoteSessionInterface;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.entity.mime.*;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.net.URLEncodedUtils;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.ContentDisposition;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.ParseException;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static lsfusion.base.BaseUtils.*;

public class ExternalUtils {

    public static final String defaultCSVSeparator = ";";

    // CHARSETS

    // "bytes-to-string" charsets
    public static Charset fileCharset = StandardCharsets.UTF_8; // when "data" files are converted to strings (should match cast)
    public static Charset resourceCharset = StandardCharsets.UTF_8; // when "disk" (usually "resource") files are converted (read) to strings
    public static Charset resultCharset = StandardCharsets.UTF_8; // when result ("disk" or "printed") files are converted to / from string
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
    public static Charset serializeCharset = StandardCharsets.UTF_8;
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
    public static final String CONTENT_DISPOSITION_HEADER = "Content-Disposition";

    public static final String generate(Object actionParam, boolean script, Object[] params) {
        String paramsString = "";
        for(int i = 0; i < params.length; i++)
            paramsString += "," + params[i];
        return (script ? "1" : "0") + "," + actionParam + paramsString;
    }

    public static final List<String> PARAMS = Arrays.asList(ACTION_CN_PARAM, SCRIPT_PARAM, PARAMS_PARAM, RETURN_PARAM, RETURNMULTITYPE_PARAM, SIGNATURE_PARAM);

    public static ExecInterface getExecInterface(RemoteLogicsInterface remoteLogics, Result<String> sessionID, Result<Boolean> closeSession, SessionContainer sessionContainer, AuthenticationToken token, ConnectionInfo connectionInfo) {
        if(sessionID != null) {
            if(sessionID.result.endsWith("_close")) {
                closeSession.set(true);
                sessionID.set(sessionID.result.substring(0, sessionID.result.length() - "_close".length()));
            }

            return new ExecInterface() {
                private RemoteSessionInterface getOrCreateSession(AuthenticationToken token, RemoteLogicsInterface remoteLogics, ConnectionInfo connectionInfo, ExternalRequest request, String fSessionID) throws RemoteException {
                    return sessionContainer.getOrCreateSession(remoteLogics, token, new SessionInfo(connectionInfo, request), fSessionID);
                }

                @Override
                public lsfusion.interop.session.ExternalResponse exec(String action, ExternalRequest request) throws RemoteException {
                    return getOrCreateSession(token, remoteLogics, connectionInfo, request, sessionID.result).exec(action, request);
                }

                @Override
                public lsfusion.interop.session.ExternalResponse eval(boolean action, ExternalRequest.Param paramScript, ExternalRequest request) throws RemoteException {
                    return getOrCreateSession(token, remoteLogics, connectionInfo, request, sessionID.result).eval(action, paramScript, request);
                }
            };
        }

        return new ExecInterface() {
            public lsfusion.interop.session.ExternalResponse exec(String action, ExternalRequest request) throws RemoteException {
                return remoteLogics.exec(token, connectionInfo, action, request);
            }
            public lsfusion.interop.session.ExternalResponse eval(boolean action, ExternalRequest.Param paramScript, ExternalRequest request) throws RemoteException {
                return remoteLogics.eval(token, connectionInfo, action, paramScript, request);
            }
        };
    }

    public static class SessionContainer {

        private final Map<String, RemoteSessionInterface> currentSessions = new ConcurrentHashMap<>();

        public RemoteSessionInterface getOrCreateSession(RemoteLogicsInterface remoteLogics, AuthenticationToken token, SessionInfo sessionInfo, String sessionID) throws RemoteException {
            RemoteSessionInterface remoteSession = currentSessions.get(sessionID);
            if(remoteSession == null) {
                remoteSession = remoteLogics.createSession(token, sessionInfo);
                currentSessions.put(sessionID, remoteSession);
            }
            return remoteSession;
        }

        public void removeSession(String sessionID) throws RemoteException {
            currentSessions.remove(sessionID).close();
        }

        public void destroy() throws RemoteException {
            for(RemoteSessionInterface remoteSession : currentSessions.values())
                remoteSession.close();
        }
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

    public static ExternalResponse processRequest(ExecInterface remoteExec, Function<ExternalRequest, ConvertFileValue> convertFileValue, InputStream is, ContentType requestContentType,
                                                  String[] headerNames, String[] headerValues, String[] cookieNames, String[] cookieValues, String logicsHost,
                                                  Integer logicsPort, String logicsExportName, String scheme, String method, String webHost, Integer webPort,
                                                  String contextPath, String servletPath, String pathInfo, String query, String sessionId) throws IOException, MessagingException, FileUploadException {
        Charset urlCharset = ExternalUtils.defaultUrlCharset;
        String urlCharsetName = urlCharset.toString();
        List<NameValuePair> parsedQueryParams = URLEncodedUtils.parse(query, urlCharset);

        ImList<ExternalRequest.Param> queryParams = ListFact.mapList(parsedQueryParams, queryParam -> ExternalRequest.getUrlParam(queryParam.getValue(), urlCharsetName, queryParam.getName()));
        byte[] body = IOUtils.readBytesFromStream(is);
        ImList<ExternalRequest.Param> bodyParams = getListFromInputStream(body, requestContentType, headerNames, headerValues);

        ImList<ExternalRequest.Param> params = ListFact.add(queryParams, bodyParams);

        ImList<String> returns = getParameterValues(parsedQueryParams, RETURN_PARAM);
        String returnMultiType = getParameterValue(parsedQueryParams, RETURNMULTITYPE_PARAM);

        String signature = getParameterValue(parsedQueryParams, SIGNATURE_PARAM);

        boolean needNotificationId = getHeaderValue(headerNames, headerValues, NEED_NOTIFICATION_ID_HEADER) != null;

        boolean isInteractiveClient = false;
        if(sessionId != null) { // supports interactive client
            String secFetchMode = getHeaderValue(headerNames, headerValues, "sec-fetch-mode");
            isInteractiveClient = secFetchMode != null && secFetchMode.equals("navigate");            ;
        }

        ExternalRequest request = new ExternalRequest(returns.toArray(new String[0]), params.toArray(new ExternalRequest.Param[params.size()]),
                headerNames, headerValues, cookieNames, cookieValues, logicsHost, logicsPort, logicsExportName, scheme, method, webHost, webPort,
                contextPath, servletPath, pathInfo, query, requestContentType != null ? requestContentType.toString() : null, sessionId, body,
                signature, returnMultiType, needNotificationId, isInteractiveClient);

        lsfusion.interop.session.ExternalResponse execResult = null;
        String path = servletPath + pathInfo;
        boolean isEvalAction = path.endsWith("/eval/action");
        if (path.endsWith("/eval") || isEvalAction) {
            ExternalRequest.Param paramScript = getParameterValue(params, SCRIPT_PARAM);
            if (paramScript == null) { // if we don't have script param, we consider the first body param as script
                int scriptParam = queryParams.size();
                if(scriptParam < params.size()) {
                    paramScript = params.get(scriptParam);
                    params = params.remove(scriptParam);
                    request.params = params.toArray(new ExternalRequest.Param[params.size()]);;
                }
            }
            execResult = remoteExec.eval(isEvalAction, paramScript, request);
        } else if (path.endsWith("/exec")) {
            String action = getParameterValue(parsedQueryParams, ACTION_CN_PARAM);
            execResult = remoteExec.exec(action, request);
        } else {
            Pattern p = Pattern.compile(".*/exec/(.*)");
            Matcher m = p.matcher(path);
            if(m.matches()) {
                String action = m.group(1);
                execResult = remoteExec.exec(action, request);
            }
        }

        return getExternalResponse(execResult, returnMultiType, convertFileValue.apply(request));
    }

    public static ExternalResponse getExternalResponse(lsfusion.interop.session.ExternalResponse execResult, String returnMultiType, ConvertFileValue convertFileValue) {
        if(execResult instanceof lsfusion.interop.session.ResultExternalResponse) {
            lsfusion.interop.session.ResultExternalResponse resultExecResult = (lsfusion.interop.session.ResultExternalResponse) execResult;
            Result<String> contentDisposition = new Result<>();
            HttpEntity entity = getInputStreamFromList(resultExecResult, convertFileValue, returnMultiType, contentDisposition);

            return new ResultExternalResponse(entity, contentDisposition.result, resultExecResult.headerNames, resultExecResult.headerValues, resultExecResult.cookieNames, resultExecResult.cookieValues, resultExecResult.statusHttp);
        } else if(execResult instanceof lsfusion.interop.session.HtmlExternalResponse) {
            return new HtmlExternalResponse(((lsfusion.interop.session.HtmlExternalResponse) execResult).html);
        } else if(execResult instanceof lsfusion.interop.session.RedirectExternalResponse)
            return new RedirectExternalResponse(((lsfusion.interop.session.RedirectExternalResponse) execResult).url, ((lsfusion.interop.session.RedirectExternalResponse) execResult).notification, ((lsfusion.interop.session.RedirectExternalResponse) execResult).usedParams);

        return new ExternalUtils.ResultExternalResponse("Something went wrong", StandardCharsets.UTF_8, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    public static String getBodyUrl(ExternalRequest.Result[] results, boolean returnBodyUrl, Charset bodyUrlCharset) {
        String bodyUrl = null;
        if (results.length > 1 && returnBodyUrl) {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < results.length; i++) {
                ExternalRequest.Result value = results[i];
                String name = value.name;
                if(name == null)
                    name = "param" + i;
                result.append(String.format("%s=%s", ((result.length() == 0) ? "" : "&") + name, encodeUrlParam(bodyUrlCharset, value)));
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

    public static ContentType getMultipartContentType(Charset charset) {
        return ContentType.create(ContentType.MULTIPART_MIXED.getMimeType(), charset);
    }

    public static ContentType getContentType(String extension, Charset charset) {
        return ContentType.create(MIMETypeUtils.MIMETypeForFileExtension(extension), charset);
    }

    public static String getExtensionFromContentType(ContentType contentType) {
        return MIMETypeUtils.fileExtensionForMIMEType(contentType.getMimeType());
    }

    public static boolean isTextExtension(String extension) {
        return isTextMimeType(MIMETypeUtils.MIMETypeForFileExtension(extension));
    }

    public static boolean isTextContentType(ContentType contentType) {
        return isTextMimeType(contentType.getMimeType());
    }

    private static boolean isTextMimeType(String mimeType) {
        return mimeType.startsWith("text/");
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

    public static ExternalRequest.Param getParameterValue(ImList<ExternalRequest.Param> queryParams, String key) {
        for(int i = 0, size = queryParams.size(); i < size; i++) {
            ExternalRequest.Param queryParam = queryParams.get(i);
            if(queryParam.name.equalsIgnoreCase(key))
                return queryParam;
        }
        return null;
    }

    public static ExternalRequest.Param getParameterValue(ExternalRequest.Param[] queryParams, String key) {
        for(int i = 0, size = queryParams.length; i < size; i++) {
            ExternalRequest.Param queryParam = queryParams[i];
            if(queryParam.name.equalsIgnoreCase(key))
                return queryParam;
        }
        return null;
    }

    private static String getHeaderValue(String[] headerNames, String[] headerValues, String header) {
        for(int i = 0; i < headerNames.length; i++) {
            if(headerNames[i].equalsIgnoreCase(header))
                return headerValues[i];
        }
        return null;
    }

    private static ExternalRequest.Param getRequestParam(String paramName, String fileName, Object object, ContentType contentType, boolean convertedToString) {
        assert object instanceof byte[] || (object instanceof String && convertedToString);
        Charset charset = getBodyCharset(contentType);

        String extension = contentType != null ? getExtensionFromContentType(contentType) : null;
        boolean isText = contentType != null && isTextContentType(contentType);
        Object value;
        if (isText && extension == null) { // in fact we can remove this branch, because even for text/plain we can use FileData
            if(!convertedToString)
                object = new String((byte[]) object, charset);
            value = object;
        } else { // FILE
            RawFileData file;
            if(isText && convertedToString) //humanReadable
                file = new RawFileData((String)object, charset);
            else
                file = new RawFileData((byte[])object);
            value = new FileData(file, extension != null ? extension : "file");
        }
        return ExternalRequest.getBodyParam(value, charset.toString(), paramName, fileName);
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

    // body
    public static ImList<ExternalRequest.Param> getListFromInputStream(byte[] bytes, ContentType contentType, String[] headerNames, String[] headerValues) throws MessagingException, IOException, FileUploadException {
        String contentDispositionHeader = getHeaderValue(headerNames, headerValues, CONTENT_DISPOSITION_HEADER);
        String name = null; String filename = null;
        if(contentDispositionHeader != null) {
            ContentDisposition contentDisposition;
           try {
                contentDisposition = new ContentDisposition(contentDispositionHeader);
            } catch (ParseException e) { // backward compatibility to parse filename=x.f
                contentDisposition = new ContentDisposition("attachment; " + contentDispositionHeader);
            }
            name = contentDisposition.getParameter("name");
            filename = contentDisposition.getParameter("filename");
        }
        return getListFromInputStream(name != null ? name : ExternalRequest.SINGLEBODYPARAMNAME, filename, bytes, contentType);
    }

    // returns FileData for FILE or String for other classes, contentType can be null if there are no parameters
    public static ImList<ExternalRequest.Param> getListFromInputStream(String paramName, String fileName, byte[] bytes, ContentType contentType) throws IOException, MessagingException, FileUploadException {
        MList<ExternalRequest.Param> mParamsList = ListFact.mList();

        String mimeType = contentType != null ? contentType.getMimeType() : null;

        if (mimeType != null && mimeType.startsWith("multipart/")) {
            if(mimeType.startsWith(FileUploadBase.MULTIPART_FORM_DATA)) { // using Apache Commons FileUpload to get the param names
                DiskFileItemFactory factory = new DiskFileItemFactory();
                ServletFileUpload upload = new ServletFileUpload(factory);

                RequestContext requestContext = new ByteArrayRequestContext(bytes, contentType);

                List<FileItem> fileItems = upload.parseRequest(requestContext);
                for (int i = 0, size = fileItems.size(); i < size; i++) {
                    FileItem fileItem = fileItems.get(i);
                    String fieldName = fileItem.getFieldName();
                    if(fieldName == null)
                        fieldName = paramName;
                    String fieldFileName = fileItem.getName();
                    if(fieldFileName == null)
                        fieldFileName = fieldName;
                    ContentType partContentType = parseContentType(fileItem.getContentType());
                    if (!fileItem.isFormField()) // actually it seems that simple true can be here (but apparently it doesn't matter)
                        mParamsList.addAll(getListFromInputStream(fieldName, fieldFileName, fileItem.get(), partContentType));
                    else
                        mParamsList.add(getRequestParam(fieldName, fieldFileName, fileItem.getString(), partContentType, true)); // multipart автоматически text/* возвращает как String
                }
            } else { // using javax.mail
                MimeMultipart multipart = new MimeMultipart(new ByteArrayDataSource(bytes, mimeType));
                for (int i = 0; i < multipart.getCount(); i++) {
                    BodyPart bodyPart = multipart.getBodyPart(i);
                    Object param = bodyPart.getContent();
                    ContentType partContentType = parseContentType(bodyPart.getContentType());
                    if (param instanceof InputStream)
                        mParamsList.addAll(getListFromInputStream(paramName, fileName, IOUtils.readBytesFromStream((InputStream) param), partContentType));
                    else
                        mParamsList.add(getRequestParam(paramName, fileName, param, partContentType, true)); // multipart автоматически text/* возвращает как String
                }
            }
        } else if(mimeType != null && mimeType.equalsIgnoreCase(ContentType.APPLICATION_FORM_URLENCODED.getMimeType())) {
            Charset charset = getBodyUrlCharset(contentType);
            List<NameValuePair> params = URLEncodedUtils.parse(new String(bytes, charset), charset);
            for(NameValuePair param : params)
                mParamsList.add(ExternalRequest.getBodyUrlParam(param.getValue(), charset.toString(), param.getName()));
        } else if (mimeType != null || bytes.length > 0)
            mParamsList.add(getRequestParam(paramName, fileName, bytes, contentType, false));

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

    public static ResponseType getResponseType(String returnMultiType, String[] headerNames, String[] headerValues) {
        ContentType forceContentType = parseContentType(getHeaderValue(headerNames, headerValues, "Content-Type"));

        boolean returnBodyUrl = returnMultiType != null && returnMultiType.equals("bodyurl");

        Charset charset = returnBodyUrl ? getBodyUrlCharset(forceContentType) : getBodyCharset(forceContentType);
        return new ResponseType(forceContentType, returnBodyUrl, charset);
    }

    public static HttpEntity getInputStreamFromList(lsfusion.interop.session.ResultExternalResponse response, ConvertFileValue convertFileValue, String returnMultiType, Result<String> contentDisposition) {
        ExternalRequest.Result[] results = convertFileValue(convertFileValue, response.results);

        ResponseType responseType = getResponseType(returnMultiType, response.headerNames, response.headerValues);
        return getInputStreamFromList(results, getBodyUrl(results, responseType.returnBodyUrl, responseType.charset), new ArrayList<>(), contentDisposition, responseType.forceContentType, responseType.charset);
    }

    private static ExternalRequest.Result[] convertFileValue(ConvertFileValue convertFileValue, ExternalRequest.Result[] results) {
        ExternalRequest.Result[] convertedResults = new ExternalRequest.Result[results.length];
        for(int i = 0; i < results.length; i++)
            convertedResults[i] = results[i].convertFileValue(convertFileValue);
        return convertedResults;
    }

    // results byte[] || String, можно было бы попровать getRequestResult (по аналогии с getRequestParam) выделить общий, но там возвращаемые классы разные, нужны будут generic'и и оно того не стоит
    public static HttpEntity getInputStreamFromList(ExternalRequest.Result[] results, String bodyUrl, List<Map<String, String>> bodyParamHeadersList, Result<String> contentDisposition, ContentType forceContentType, Charset charset) {
        HttpEntity entity;
        int paramCount = results.length;
        if (forceContentType == null ? paramCount > 1 : forceContentType.getMimeType().startsWith("multipart/")) {
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            //Default mode is STRICT, which uses only US-ASCII. EXTENDED mode allows UTF-8.
            builder.setMode(HttpMultipartMode.EXTENDED);
            builder.setContentType(nvl(forceContentType, getMultipartContentType(charset)));
            for (int i = 0; i < paramCount; i++) {
                ExternalRequest.Result result = results[i];
                Object value = result.value;
                String paramName = result.name;
                if(paramName == null)
                    paramName = "param" + i;

                ContentBody contentBody;
                if (value instanceof FileData) {
                    String fileName = result.fileName;
                    if(fileName == null)
                        fileName = "file" + i;
                    String extension = ((FileData) value).getExtension();
                    contentBody = new ByteArrayBody(((FileData) value).getRawFile().getBytes(), getContentType(extension, charset), fileName);
                } else {
                    contentBody = new StringBody((String) value, getStringContentType(charset));
                }

                FormBodyPart formBodyPart = FormBodyPartBuilder.create(paramName, contentBody).build();;
                Map<String, String> bodyParamHeaders = bodyParamHeadersList.size() > i ? bodyParamHeadersList.get(i) : null;
                if(bodyParamHeaders != null)
                    for (Map.Entry<String, String> bodyParamHeader : bodyParamHeaders.entrySet())
                        formBodyPart.addField(bodyParamHeader.getKey(), bodyParamHeader.getValue());
                builder.addPart(formBodyPart);
            }
            entity = builder.build();
        } else if(paramCount == 1) {
            ExternalRequest.Result result = single(results);

            Object value = result.value;
            if (value instanceof FileData) {
                String extension = ((FileData) value).getExtension();
                entity = new ByteArrayEntity(((FileData) value).getRawFile().getBytes(), nvl(forceContentType, getContentType(extension, charset)));

                String fileName = result.fileName;
                if(contentDisposition != null && fileName != null)
                    contentDisposition.set(getContentDisposition(fileName, extension, charset));
            } else
                entity = new StringEntity((String) value, nvl(forceContentType, getStringContentType(charset)));
        } else {
            entity = bodyUrl != null ? new StringEntity(bodyUrl, nvl(forceContentType, getUrlEncodedContentType(charset))) : null;
        }
        return entity;
    }

    public static String getContentDisposition(String name, String extension, Charset charset) {
        String result = "inline";

//        if(paramName != null)
//            result += "; name=\"" + paramName + "\"";

        if(StringUtils.isAsciiPrintable(name))
            result += "; filename=\"" + name +"\"";

        result += "; filename*=" + charset.name() + "''" + getContentFileName(name, extension);

        return result;
    }

    public static byte[] sendTCP(byte[] fileBytes, String host, Integer port, Integer timeout, boolean externalTCPWaitForByteMinusOne) throws IOException {
        try (Socket socket = new Socket(host, port)) {
            return sendTCP(fileBytes, socket, timeout, externalTCPWaitForByteMinusOne);
        }
    }

    public static byte[] sendTCP(byte[] fileBytes, Socket socket, Integer timeout, boolean externalTCPWaitForByteMinusOne) throws IOException {
        if (timeout != null) {
            socket.setSoTimeout(timeout);
        }

        if (externalTCPWaitForByteMinusOne) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (OutputStream os = socket.getOutputStream(); InputStream is = socket.getInputStream()) {
                os.write(fileBytes);

                //reading finishes when the sender closes the stream or by SO timeout
                int b;
                while ((b = is.read()) != -1) {
                    baos.write((byte) b);
                }
            } catch (SocketTimeoutException e) {
                //timeout exception, finish reading
            }
            return baos.toByteArray();
        } else {
            socket.getOutputStream().write(fileBytes);
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[1024 * 1024 * 10]; //10MB
                out.write(buffer, 0, socket.getInputStream().read(buffer));
                return out.toByteArray();
            }
        }
    }

    public static void sendUDP(byte[] fileBytes, String host, Integer port) throws IOException {
        try (DatagramSocket socket = new DatagramSocket()) {
            DatagramPacket packet = new DatagramPacket(fileBytes, fileBytes.length, InetAddress.getByName(host), port);
            socket.send(packet);
        }
    }

    public static String encodeUrlParam(Charset urlEncodeCharset, ExternalRequest.Result result) {
        String stringValue;
        String charsetName = urlEncodeCharset.name();
        Object value = result.value;
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
        public final String[] usedParams;

        public RedirectExternalResponse(String url, Integer notification, String[] usedParams) {
            this.url = url;
            this.notification = notification;
            this.usedParams = usedParams;
        }
    }
}