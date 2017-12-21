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
    public static String applicationOctetStreamType = "application/octet-stream";
    public static String multipartMixedType = "multipart/mixed";

    public static HttpEntity processRequest(RemoteLogicsInterface remoteLogics, String uri, String action, String script, List<String> returns, List<String> getParams, InputStream is, String requestContentType) throws IOException, MessagingException {
        List<Object> returnList = new ArrayList<>();

        List<Object> paramsList = getRequestParams(getParams, is, requestContentType);

        if (uri.startsWith("/exec")) {
            returnList = remoteLogics.exec(action, returns.toArray(new String[returns.size()]), paramsList.toArray());
        } else if (uri.startsWith("/eval")) {
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
        }

        HttpEntity entity = null;

        if (!returnList.isEmpty()) {
            if (returnList.size() > 1) {
                MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
                builder.setContentType(ContentType.MULTIPART_FORM_DATA);
                for (int i = 0; i < returnList.size(); i++) {
                    Object returnEntry = returnList.get(i);
                    if (returnEntry instanceof byte[])
                        builder.addPart("param" + i, new ByteArrayBody((byte[]) returnEntry, ContentType.APPLICATION_OCTET_STREAM, "filename"));
                    else
                        builder.addPart("param" + i, new StringBody((String) returnEntry, ContentType.TEXT_PLAIN));
                }
                entity = builder.build();
            } else {
                Object returnEntry = returnList.get(0);
                if (returnEntry instanceof byte[]) {
                    entity = new ByteArrayEntity((byte[]) returnEntry, ContentType.APPLICATION_OCTET_STREAM);
                } else {
                    entity = new StringEntity((String) returnEntry, ContentType.TEXT_PLAIN);
                }
            }
        }
        return entity;
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
}