package lsfusion.server.logics.property.actions;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.IOUtils;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.classes.DynamicFormatFileClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.actions.flow.FlowResult;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.HttpClientBuilder;

import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExternalHTTPActionProperty extends ExternalActionProperty {
    final int bodyParamsCount;

    public ExternalHTTPActionProperty(int getParamsCount, int bodyParamsCount, String query, List<LCP> targetPropList) {
        super(getParamsCount + bodyParamsCount, query, targetPropList);
        this.bodyParamsCount = bodyParamsCount;
    }

    @Override
    protected FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        try {

            String replacedParams = replaceParams(context, connectionString);
            byte[] response = readHTTP(context, replacedParams, bodyParamsCount);

            if (targetPropList.size() == 1) {
                LCP targetProp = targetPropList.get(0);
                Type type = targetProp.property.getType();

                Object result;
                if (type instanceof DynamicFormatFileClass) {
                    result = IOUtils.readBytesFromStream(new ByteArrayInputStream(response));
                } else
                    result = type.parseString(new String(response));
                targetProp.change(result, context);

            } else if (targetPropList.size() >= 2) {
                ByteArrayDataSource ds = new ByteArrayDataSource(response, "multipart/mixed");
                MimeMultipart multipart = new MimeMultipart(ds);

                if (multipart.getCount() != targetPropList.size())
                    throw new RuntimeException(String.format("Expected return params: %s, got: %s", targetPropList.size(), multipart.getCount()));
                else {
                    for (int i = 0; i < targetPropList.size(); i++) {
                        LCP targetProp = targetPropList.get(i);
                        Type type = targetProp.property.getType();
                        Object result = multipart.getBodyPart(i).getContent();
                        if (type instanceof DynamicFormatFileClass) {
                            if (result instanceof ByteArrayInputStream)
                                result = IOUtils.readBytesFromStream((ByteArrayInputStream) result);
                            else
                                result = null;
                        } else
                            result = type.parseString((String) result);
                        targetProp.change(result, context);
                    }
                }
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }

        return FlowResult.FINISH;
    }

    private byte[] readHTTP(ExecutionContext context, String connectionString, int bodyParamsCount) throws IOException {
        final List<String> properties = parseHTTPPath(connectionString);
        if (properties != null) {
            //пока вариант с аутентификацией не работает
            String type = properties.get(0);
            final String username = properties.get(1);
            final String password = properties.get(2);
            String pathToFile = properties.get(3);
            Authenticator.setDefault(new Authenticator() {
                public PasswordAuthentication getPasswordAuthentication() {
                    return (new PasswordAuthentication(username, password.toCharArray()));
                }
            });
            URL httpUrl = new URL(URIUtil.encodeQuery(type + "://" + pathToFile));

            InputStream inputStream = httpUrl.openConnection().getInputStream();

            return IOUtils.readBytesFromStream(inputStream);
        } else {

            ImOrderSet<PropertyInterface> orderInterfaces = getOrderInterfaces();

            HttpPost httpPost = new HttpPost(connectionString);
            HttpEntity entity = null;
            if (bodyParamsCount > 1) {
                MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                for (int i = orderInterfaces.size() - bodyParamsCount; i < orderInterfaces.size(); i++) {
                    Object value = context.getKeyValue(orderInterfaces.get(i)).getValue();
                    String name = "param" + (i + 1);
                    if (value instanceof byte[]) {
                        builder.addPart(name, new ByteArrayBody(BaseUtils.getFile((byte[]) value), name));
                    } else {
                        builder.addPart(name, new StringBody(value == null ? "" : String.valueOf(value), ContentType.create("text/plain", Charset.forName("UTF-8"))));
                    }
                }
                entity = builder.build();
                httpPost.addHeader("Content-type", "multipart/mixed");

            } else if (bodyParamsCount == 1) {
                Object value = context.getKeyValue(orderInterfaces.get(orderInterfaces.size() - 1)).getValue();
                if (value instanceof byte[]) {
                    entity = new ByteArrayEntity(BaseUtils.getFile((byte[]) value));
                } else {
                    entity = new StringEntity(value == null ? "" : String.valueOf(value));
                }
            }
            httpPost.addHeader("Content-type", "text/plain");

            HttpClient httpClient = HttpClientBuilder.create().build();
            if (entity != null)
                httpPost.setEntity(entity);
            HttpResponse response = httpClient.execute(httpPost);
            if (response.getStatusLine().getStatusCode() == 200)
                return IOUtils.readBytesFromStream(response.getEntity().getContent());
            else
                throw new RuntimeException(response.getStatusLine().getReasonPhrase());
        }
    }

    private List<String> parseHTTPPath(String path) {
        /*http|https://username:password@path_to_file*/
        Pattern connectionStringPattern = Pattern.compile("(http|https):\\/\\/(.*):(.*)@(.*)");
        Matcher connectionStringMatcher = connectionStringPattern.matcher(path);
        if (connectionStringMatcher.matches()) {
            String type = connectionStringMatcher.group(1);
            String username = connectionStringMatcher.group(2);
            String password = connectionStringMatcher.group(3);
            String pathToFile = connectionStringMatcher.group(4);
            return Arrays.asList(type, username, password, pathToFile);
        } else return null;
    }
}