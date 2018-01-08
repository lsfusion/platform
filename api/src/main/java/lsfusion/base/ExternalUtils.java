package lsfusion.base;

import lsfusion.interop.RemoteLogicsInterface;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExternalUtils {

    public static String textPlainType = "text/plain";
    public static String multipartMixedType = "multipart/mixed";
    public static String nullString = "XJ4P3DGG6Z71MI72G2HF3H0UEM14D17A";

    private static final String ACTION_CN_PARAM = "action";
    private static final String SCRIPT_PARAM = "script";
    private static final String PARAMS_PARAM = "p";
    private static final String RETURNS_PARAM = "returns";
    private static final String PROPERTY_PARAM = "property";

    public static ExternalResponse processRequest(RemoteLogicsInterface remoteLogics, String uri, String query, InputStream is, String requestContentType) throws IOException, MessagingException {
        List<Object> paramsList = getRequestParams(getParameterValues(query, PARAMS_PARAM), is, requestContentType);
        List<String> returns = getParameterValues(query, RETURNS_PARAM);
        List<Object> returnList = new ArrayList<>();

        if (uri.startsWith("/exec")) {
            String action = getParameterValue(query, ACTION_CN_PARAM);
            returnList = remoteLogics.exec(action, returns.toArray(new String[returns.size()]), paramsList.toArray());
        } else if (uri.startsWith("/eval")) {
            String script = getParameterValue(query, SCRIPT_PARAM);
            if (script == null && !paramsList.isEmpty()) {
                //Первый параметр считаем скриптом
                script = formatParam(paramsList.get(0));
                paramsList = paramsList.subList(1, paramsList.size());
            }
            if (script != null) {
                //оборачиваем в run без параметров
                Pattern p = Pattern.compile("run\\((.*)\\)\\s*?=\\s*?\\{.*\\}");
                Matcher m = p.matcher(script);
                if (!m.matches())
                    script = "run() = {" + script + ";\n};";
            }
            returnList = remoteLogics.eval(script, returns.toArray(new String[returns.size()]), paramsList.toArray());
        } else if (uri.startsWith("/read")) {
            String property = getParameterValue(query, PROPERTY_PARAM);
            if (property != null) {
                returnList.addAll(remoteLogics.read(property, paramsList.toArray()));
            }
        }

        HttpEntity entity = null;
        String contentDisposition = null;

        if (!returnList.isEmpty()) {
            if (returnList.size() > 1) {
                MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
                builder.setContentType(ContentType.MULTIPART_FORM_DATA);
                for (int i = 0; i < returnList.size(); i++) {
                    Object returnEntry = returnList.get(i);
                    if (returnEntry instanceof byte[])
                        builder.addPart("param" + i, new ByteArrayBody(BaseUtils.getFile((byte[]) returnEntry), getContentType(BaseUtils.getExtension((byte[]) returnEntry)), "filename"));
                    else
                        builder.addPart("param" + i, new StringBody(returnEntry == null ? nullString : (String) returnEntry, ContentType.TEXT_PLAIN));
                }
                entity = builder.build();
            } else {
                Object returnEntry = returnList.get(0);
                if (returnEntry instanceof byte[]) {
                    String extension = BaseUtils.getExtension((byte[]) returnEntry);
                    entity = new ByteArrayEntity(BaseUtils.getFile((byte[]) returnEntry), getContentType(extension));
                    contentDisposition = "filename=" + (returns.isEmpty() ? "file" : returns.get(0).replace(',', '_')) + "." + extension;
                } else {
                    entity = new StringEntity(returnEntry == null ? nullString : (String) returnEntry, ContentType.TEXT_PLAIN);
                }
            }
        }
        return new ExternalResponse(entity, contentDisposition);
    }

    public static ContentType getContentType(String extension) {
        switch (extension) {
            case "jpg":
            case "jpeg":
            case "png":
            case "bmp":
                return ContentType.create("image/" + extension);
            case "":
                return ContentType.APPLICATION_OCTET_STREAM;
                default:
                    return ContentType.create("application/" + extension);
        }
    }

    public static byte[] getExtensionFromContentType(String contentType) {
        Pattern p = Pattern.compile("(application|image)/(.*)");
        Matcher m = p.matcher(contentType);
        return m.matches() ? m.group(2).getBytes() : null;
    }

    private static String getParameterValue(String query, String key) {
        List<String> params = getParameterValues(query, key);
        return params.isEmpty() ? null : params.get(0);
    }

    private static List<String> getParameterValues(String query, String key) {
        List<String> values = new ArrayList<>();
        if (query != null) {
            for (String entry : query.split("&")) {
                if (entry.contains("=") && entry.substring(0, entry.indexOf("=")).equals(key))
                    values.add(entry.substring(Math.min(entry.indexOf("=") + 1, entry.length() - 1)));
            }
        }
        return values;
    }

    private static List<Object> getRequestParams(List<String> getParams, InputStream is, String contentType) throws IOException, MessagingException {
        List<Object> paramsList = getParams != null ? new ArrayList<Object>(getParams) : new ArrayList<>();
        boolean multipartPost = contentType != null && contentType.contains("multipart");
        byte[] postParams = IOUtils.readBytesFromStream(is);
        if (postParams.length > 0) {
            if (multipartPost) {
                MimeMultipart multipart = new MimeMultipart(new ByteArrayDataSource(postParams, multipartMixedType));
                for (int i = 0; i < multipart.getCount(); i++) {
                    Object param = multipart.getBodyPart(i).getContent();
                    paramsList.add(param instanceof ByteArrayInputStream ? IOUtils.readBytesFromStream((ByteArrayInputStream) param) : param);
                }
            } else {
                paramsList.add(postParams);
            }
        }
        return paramsList;
    }

    private static String formatParam(Object param) {
        if (param instanceof byte[])
            param = new String((byte[]) param);
        if (param instanceof String)
            return ((String) param).isEmpty() ? null : (String) param;
        else return null;
    }

    public static class ExternalResponse {
        public HttpEntity response;
        public String contentDisposition;

        public ExternalResponse(HttpEntity response, String contentDisposition) {
            this.response = response;
            this.contentDisposition = contentDisposition;
        }
    }
}