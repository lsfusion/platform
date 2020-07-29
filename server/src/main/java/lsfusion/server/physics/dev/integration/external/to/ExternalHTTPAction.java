package lsfusion.server.physics.dev.integration.external.to;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.DateConverter;
import lsfusion.base.Result;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.interop.session.ExternalUtils;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.NullValue;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.property.LP;
import lsfusion.server.language.property.oraction.LAP;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.controller.context.ExecutionEnvironment;
import lsfusion.server.logics.action.flow.FlowResult;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.action.session.table.SingleKeyPropertyUsage;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.ParseException;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.infer.ClassType;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;

import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

import static lsfusion.server.physics.dev.integration.external.to.ExternalHttpMethod.PUT;

public class ExternalHTTPAction extends ExternalAction {
    private ExternalHttpMethod method;
    private PropertyInterface queryInterface;
    private PropertyInterface bodyUrlInterface;
    private LP<?> headersProperty;
    private LP<?> cookiesProperty;
    private LP headersToProperty;
    private LP cookiesToProperty;

    public ExternalHTTPAction(ExternalHttpMethod method, ImList<Type> params, ImList<LP> targetPropList,
                              LP headersProperty, LP cookiesProperty, LP headersToProperty, LP cookiesToProperty, boolean hasBodyUrl) {
        super(hasBodyUrl ? 2 : 1, params, targetPropList);

        this.method = method;
        this.queryInterface = getOrderInterfaces().get(0);
        this.bodyUrlInterface = hasBodyUrl ? getOrderInterfaces().get(1) : null;
        this.headersProperty = headersProperty;
        this.cookiesProperty = cookiesProperty;
        this.headersToProperty = headersToProperty;
        this.cookiesToProperty = cookiesToProperty;
    }

    @Override
    protected FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) {
        try {

            Result<ImOrderSet<PropertyInterface>> rNotUsedParams = new Result<>();
            String connectionString = getTransformedText(context, queryInterface);
            String bodyUrl = bodyUrlInterface != null ? getTransformedText(context, bodyUrlInterface) : null;
            if(connectionString != null) {
                connectionString = replaceParams(context, connectionString, rNotUsedParams, ExternalUtils.getCharsetFromContentType(ExternalUtils.TEXT_PLAIN));
                bodyUrl = bodyUrl != null ? replaceParams(context, bodyUrl, rNotUsedParams, ExternalUtils.getCharsetFromContentType(ExternalUtils.TEXT_PLAIN)) : null;
                if(bodyUrl != null && !rNotUsedParams.result.isEmpty()) {
                    throw new RuntimeException("All params should be used in BODYURL");
                }
                ImMap<String, String> headers = headersProperty != null ? readPropertyValues(context.getEnv(), headersProperty) : MapFact.EMPTY();
                ImMap<String, String> cookies = cookiesProperty != null ? readPropertyValues(context.getEnv(), cookiesProperty) : MapFact.EMPTY();
                CookieStore cookieStore = new BasicCookieStore();
                Integer timeout = (Integer) context.getBL().LM.timeoutHttp.read(context);
                HttpResponse response = readHTTP(context, connectionString, timeout, bodyUrl, rNotUsedParams.result, headers, cookies, cookieStore);
                HttpEntity responseEntity = response.getEntity();

                ContentType contentType = ContentType.get(responseEntity);
                ImList<Object> requestParams = responseEntity != null ? ExternalUtils.getListFromInputStream(responseEntity.getContent(), contentType) : ListFact.EMPTY();
                fillResults(context, targetPropList, requestParams, ExternalUtils.getCharsetFromContentType(contentType)); // важно игнорировать параметры, так как иначе при общении с LSF пришлось бы всегда TO писать (так как он по умолчанию exportFile возвращает)

                if(headersToProperty != null) {
                    Map<String, List<String>> responseHeaders = getResponseHeaders(response);
                    String[] headerNames = responseHeaders.keySet().toArray(new String[0]);
                    String[] headerValues = getResponseHeaderValues(responseHeaders, headerNames);

                    writePropertyValues(context.getSession(), headersToProperty, headerNames, headerValues);
                }
                if(cookiesToProperty != null) {
                    Map<String, String> responseCookies = getResponseCookies(cookieStore);
                    String[] cookieNames = responseCookies.keySet().toArray(new String[0]);
                    String[] cookieValues = responseCookies.values().toArray(new String[0]);

                    writePropertyValues(context.getSession(), cookiesToProperty, cookieNames, cookieValues);
                }
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

    private Map<String, List<String>> getResponseHeaders(HttpResponse response) {
        Map<String, List<String>> responseHeaders = new HashMap<>();
        for(Header header : response.getAllHeaders()) {
            String headerName = header.getName();
            List<String> headerValues = responseHeaders.get(headerName);
            if (headerValues == null) {
                headerValues = new ArrayList<>();
                responseHeaders.put(headerName, headerValues);
            }
            headerValues.add(header.getValue());
        }
        return responseHeaders;
    }

    private String[] getResponseHeaderValues(Map<String, List<String>> responseHeaders, String[] headerNames) {
        String[] headerValuesArray = new String[headerNames.length];
        for (int i = 0; i < headerNames.length; i++) {
            headerValuesArray[i] = StringUtils.join(responseHeaders.get(headerNames[i]).iterator(), ",");
        }
        return headerValuesArray;
    }

    private Map<String, String> getResponseCookies(CookieStore cookieStore) {
        Map<String, String> responseCookies = new HashMap<>();
        for(Cookie cookie : cookieStore.getCookies()) {
            responseCookies.put(cookie.getName(), cookie.getValue());
        }
        return responseCookies;
    }

    private HttpResponse readHTTP(ExecutionContext<PropertyInterface> context, String connectionString, Integer timeout, String bodyUrl, ImOrderSet<PropertyInterface> bodyParams, ImMap<String, String> headers, ImMap<String, String> cookies, CookieStore cookieStore) throws IOException {
        Object[] paramList = new Object[bodyParams.size()];
        for (int i=0,size=bodyParams.size();i<size;i++)
            paramList[i] = format(context, bodyParams.get(i), null); // пока в body ничего не кодируем (так как content-type'ы другие)

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
        for(int i=0,size=headers.size();i<size;i++)
            httpRequest.addHeader(headers.getKey(i), headers.getValue(i));
        for(int i=0,size=cookies.size();i<size;i++) {
            BasicClientCookie cookie = parseRawCookie(cookies.getKey(i), cookies.getValue(i));
            cookieStore.addCookie(cookie);
        }

        HttpClientBuilder requestBuilder = HttpClientBuilder.create().setDefaultCookieStore(cookieStore).useSystemProperties();

        if(timeout != null) {
            RequestConfig.Builder configBuilder = RequestConfig.custom();
            configBuilder.setConnectTimeout(timeout);
            configBuilder.setConnectionRequestTimeout(timeout);
            requestBuilder.setDefaultRequestConfig(configBuilder.build());
        }

        return requestBuilder.build().execute(httpRequest);
    }

    private BasicClientCookie parseRawCookie(String cookieName, String rawCookie) {
        BasicClientCookie cookie;
        String[] rawCookieParams = rawCookie.split(";");

        String cookieValue = rawCookieParams[0];

        cookie = new BasicClientCookie(cookieName, cookieValue);

        for (int i = 1; i < rawCookieParams.length; i++) {

            String[] rawCookieParam = rawCookieParams[i].split("=");
            String paramName = rawCookieParam[0].trim();

            if (paramName.equalsIgnoreCase("secure")) {
                cookie.setSecure(true);
            } else if (rawCookieParam.length == 2) {
                String paramValue = rawCookieParam[1].trim();

                if (paramName.equalsIgnoreCase("expires")) {
                    cookie.setExpiryDate(parseDate(paramValue));
                } else if (paramName.equalsIgnoreCase("max-age")) {
                    long maxAge = Long.parseLong(paramValue);
                    Date expiryDate = new Date(System.currentTimeMillis() + maxAge);
                    cookie.setExpiryDate(expiryDate);
                } else if (paramName.equalsIgnoreCase("domain")) {
                    cookie.setDomain(paramValue);
                } else if (paramName.equalsIgnoreCase("path")) {
                    cookie.setPath(paramValue);
                } else if (paramName.equalsIgnoreCase("comment")) {
                    cookie.setPath(paramValue);
                }
            }
        }
        return cookie;
    }

    private Date parseDate(String value) {
        try {
            return new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ssZZZ").parse(value);
        } catch (java.text.ParseException e) {
            return null;
        }
    }

    public static ObjectValue[] getParams(DataSession session, LAP property, Object[] params, Charset charset) throws ParseException, SQLException, SQLHandledException {
        ImOrderSet<PropertyInterface> interfaces = (ImOrderSet<PropertyInterface>) property.listInterfaces;
        ImMap<PropertyInterface, ValueClass> interfaceClasses = property.getActionOrProperty().getInterfaceClasses(ClassType.parsePolicy);
        ObjectValue[] objectValues = new ObjectValue[interfaces.size()];
        for (int i = 0; i < interfaces.size(); i++) {
            ValueClass valueClass = interfaceClasses.get(interfaces.get(i));

            Object value = null;
            if (i < params.length && valueClass != null) // all incorrect params will consider to be nulls
                value = valueClass.getType().parseHTTP(params[i], charset);

            objectValues[i] = value == null ? NullValue.instance : session.getObjectValue(valueClass, value);
        }
        return objectValues;
    }

    public static void fillResults(ExecutionContext context, ImList<LP> targetPropList, ImList<Object> results, Charset charset) throws ParseException, SQLException, SQLHandledException {
        for(int i = 0, size = targetPropList.size(); i < size; i++) {
            LP<?> targetProp = targetPropList.get(i);

            Object value = null;
            if (i < results.size()) // для недостающих записываем null
                value = results.get(i);

            targetProp.change(value == null ? null : targetProp.property.getType().parseHTTP(value, charset), context);
        }
    }

    public static ImMap<String, String> readPropertyValues(ExecutionEnvironment env, LP<?> property) throws SQLException, SQLHandledException {
        return BaseUtils.immutableCast(property.readAll(env).mapKeys(value -> (String) value.single()));
    }

    public static <P extends PropertyInterface> void writePropertyValues(DataSession session, LP<P> property, String[] names, String[] values) throws SQLException, SQLHandledException {
        Property<P> prop = property.property;
        P name = property.listInterfaces.get(0);

        SingleKeyPropertyUsage table = new SingleKeyPropertyUsage("writePropertyValues", prop.interfaceTypeGetter.getType(name), prop.getType());

        MExclMap<DataObject, ObjectValue> mRows = MapFact.mExclMap();
        for (int i = 0; i < names.length; i++)
            mRows.exclAdd(new DataObject(names[i]), new DataObject(values[i]));

        try {
            table.writeRows(session.sql, session.getOwner(), mRows.immutable());
            session.change(prop, SingleKeyPropertyUsage.getChange(table, name));
        } finally {
            table.drop(session.sql, session.getOwner());
        }
    }

}