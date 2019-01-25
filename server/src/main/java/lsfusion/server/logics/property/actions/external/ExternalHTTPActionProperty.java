package lsfusion.server.logics.property.actions.external;

import com.google.common.base.Throwables;
import lsfusion.base.ExternalUtils;
import lsfusion.base.Result;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.data.type.ParseException;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.NullValue;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.linear.LP;
import lsfusion.server.logics.property.ClassType;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.ExternalHttpMethod;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.actions.flow.FlowResult;
import lsfusion.server.session.DataSession;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static lsfusion.server.logics.property.ExternalHttpMethod.PUT;

public class ExternalHTTPActionProperty extends ExternalActionProperty {
    private ExternalHttpMethod method;
    private PropertyInterface queryInterface;
    private PropertyInterface bodyUrlInterface;
    private LCP headersProperty;

    public ExternalHTTPActionProperty(ExternalHttpMethod method, ImList<Type> params, ImList<LCP> targetPropList, LCP headersProperty, boolean hasBodyUrl) {
        super(hasBodyUrl ? 2 : 1, params, targetPropList);

        this.method = method;
        this.queryInterface = getOrderInterfaces().get(0);
        this.bodyUrlInterface = hasBodyUrl ? getOrderInterfaces().get(1) : null;
        this.headersProperty = headersProperty;
    }

    @Override
    protected FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        try {

            Result<ImOrderSet<PropertyInterface>> rNotUsedParams = new Result<>();
            String connectionString = getTransformedText(context, queryInterface);
            String bodyUrl = bodyUrlInterface != null ? getTransformedText(context, bodyUrlInterface) : null;
            if(connectionString != null) {
                connectionString = replaceParams(context, connectionString, rNotUsedParams, ExternalUtils.getCharsetFromContentType(ExternalUtils.TEXT_PLAIN));
                bodyUrl = bodyUrl != null ? replaceParams(context, bodyUrl, rNotUsedParams, ExternalUtils.getCharsetFromContentType(ExternalUtils.TEXT_PLAIN)) : null;
                Map<String, String> headers = getHeaders(context);
                HttpResponse response = readHTTP(context, connectionString, bodyUrl, rNotUsedParams.result, headers);
                HttpEntity responseEntity = response.getEntity();

                ContentType contentType = ContentType.get(responseEntity);
                List<Object> requestParams = ExternalUtils.getListFromInputStream(responseEntity.getContent(), contentType);
                fillResults(context, targetPropList, requestParams, ExternalUtils.getCharsetFromContentType(contentType)); // важно игнорировать параметры, так как иначе при общении с LSF пришлось бы всегда TO писать (так как он по умолчанию exportFile возвращает)
                context.getBL().LM.statusHttp.change(response.getStatusLine().getStatusCode(), context);
                if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() >= 300) {
                    throw new RuntimeException(response.getStatusLine().toString());
                }
            } else {
                throw new RuntimeException("connectionString not specified");
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }

        return FlowResult.FINISH;
    }

    private HttpResponse readHTTP(ExecutionContext<PropertyInterface> context, String connectionString, String bodyUrl, ImOrderSet<PropertyInterface> bodyParams, Map<String, String> headers) throws IOException {
        HttpClient httpClient = HttpClientBuilder.create().build();

        List<Object> paramList = new ArrayList<>();
        for (PropertyInterface i : bodyParams) {
            paramList.add(format(context, i, null)); // пока в body ничего не кодируем (так как content-type'ы другие)
        }

        HttpUriRequest httpRequest;
        switch (method) {
            case GET: {
                httpRequest = new HttpGet(connectionString);
                break;
            }
            case DELETE: {
                httpRequest = new HttpDelete(connectionString);
                break;
            }
            case PUT:
            case POST:
            default: {
                if(method.equals(PUT))
                    httpRequest = new HttpPut(connectionString);
                else
                    httpRequest = new HttpPost(connectionString);
                HttpEntity entity = ExternalUtils.getInputStreamFromList(paramList, bodyUrl, null);
                if (!headers.containsKey("Content-Type"))
                    httpRequest.addHeader("Content-Type", entity.getContentType().getValue());
                ((HttpEntityEnclosingRequestBase) httpRequest).setEntity(entity);
                break;
            }
        }
        for(Map.Entry<String, String> header : headers.entrySet()) {
            httpRequest.addHeader(header.getKey(), header.getValue());
        }
        return httpClient.execute(httpRequest);
    }

    public static ObjectValue[] getParams(DataSession session, LP property, Object[] params, Charset charset) throws ParseException, SQLException, SQLHandledException {
        ImOrderSet<PropertyInterface> interfaces = (ImOrderSet<PropertyInterface>) property.listInterfaces;
        ImMap<PropertyInterface, ValueClass> interfaceClasses = property.property.getInterfaceClasses(ClassType.parsePolicy);
        ObjectValue[] objectValues = new ObjectValue[interfaces.size()];
        for (int i = 0; i < interfaces.size(); i++) {
            ValueClass valueClass = interfaceClasses.get(interfaces.get(i));

            Object value = null;
            if (i < params.length) // для лишних записываем null
                value = valueClass.getType().parseHTTP(params[i], charset);

            objectValues[i] = value == null ? NullValue.instance : session.getObjectValue(valueClass, value);
        }
        return objectValues;
    }

    public static void fillResults(ExecutionContext context, ImList<LCP> targetPropList, List<Object> results, Charset charset) throws ParseException, SQLException, SQLHandledException {
        for(int i = 0, size = targetPropList.size(); i < size; i++) {
            LCP<?> targetProp = targetPropList.get(i);

            Object value = null;;
            if (i < results.size()) // для недостающих записываем null
                value = results.get(i);

            targetProp.change(value == null ? null : targetProp.property.getType().parseHTTP(value, charset), context);
        }
    }

    private Map<String, String> getHeaders(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        Map<String, String> headers = new HashMap<>();
        if(headersProperty != null) {
            KeyExpr stringExpr = new KeyExpr("string");
            ImRevMap<Object, KeyExpr> keys = MapFact.singletonRev((Object) "string", stringExpr);
            QueryBuilder<Object, Object> query = new QueryBuilder<>(keys);
            Expr headersExpr = headersProperty.getExpr(context.getModifier(), stringExpr);
            query.addProperty("header", headersExpr);
            query.and(headersExpr.getWhere());
            ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> result = query.execute(context);
            for (int i = 0; i < result.size(); i++) {
                headers.put(((String) result.getKey(i).get("string")).trim(), ((String) result.getValue(i).get("header")).trim());
            }
        }
        return headers;
    }

}