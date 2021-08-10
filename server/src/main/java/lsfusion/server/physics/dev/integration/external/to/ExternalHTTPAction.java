package lsfusion.server.physics.dev.integration.external.to;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.file.IOUtils;
import lsfusion.interop.session.*;
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
import org.apache.http.HttpEntity;
import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.BasicCookieStore;

import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExternalHTTPAction extends ExternalAction {
    boolean clientAction;
    private ExternalHttpMethod method;
    private PropertyInterface queryInterface;
    private PropertyInterface bodyUrlInterface;
    private List<String> bodyParamNames;
    private ImList<LP> bodyParamHeadersPropertyList;
    private LP<?> headersProperty;
    private LP<?> cookiesProperty;
    private LP headersToProperty;
    private LP cookiesToProperty;

    public ExternalHTTPAction(boolean clientAction, ExternalHttpMethod method, ImList<Type> params, ImList<LP> targetPropList,
                              List<String> bodyParamNames, ImList<LP> bodyParamHeadersPropertyList,
                              LP headersProperty, LP cookiesProperty, LP headersToProperty, LP cookiesToProperty, boolean hasBodyUrl) {
        super(hasBodyUrl ? 2 : 1, params, targetPropList);

        this.clientAction = clientAction;
        this.method = method;
        this.queryInterface = getOrderInterfaces().get(0);
        this.bodyUrlInterface = hasBodyUrl ? getOrderInterfaces().get(1) : null;
        this.bodyParamNames = bodyParamNames;
        this.bodyParamHeadersPropertyList = bodyParamHeadersPropertyList;
        this.headersProperty = headersProperty;
        this.cookiesProperty = cookiesProperty;
        this.headersToProperty = headersToProperty;
        this.cookiesToProperty = cookiesToProperty;
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
                
                List<Map<String, String>> bodyParamHeadersList = new ArrayList<>();

                for (LP bodyParamHeadersProperty : bodyParamHeadersPropertyList) {
                    Map<String, String> bodyParamHeaders = new HashMap<>();
                    MapFact.addJavaAll(bodyParamHeaders, readPropertyValues(context.getEnv(), bodyParamHeadersProperty));
                    bodyParamHeadersList.add(bodyParamHeaders);
                }

                Map<String, String> headers = new HashMap<>();
                if(headersProperty != null) {
                    MapFact.addJavaAll(headers, readPropertyValues(context.getEnv(), headersProperty));
                }
                Map<String, String> cookies = new HashMap<>();
                if(cookiesProperty != null) {
                    MapFact.addJavaAll(cookies, readPropertyValues(context.getEnv(), cookiesProperty));
                }

                CookieStore cookieStore = new BasicCookieStore();

                Object[] paramList = new Object[rNotUsedParams.result.size()];
                for (int i=0,size=rNotUsedParams.result.size();i<size;i++)
                    paramList[i] = format(context, rNotUsedParams.result.get(i), null); // пока в body ничего не кодируем (так как content-type'ы другие)


                byte[] body = null;
                if (method.hasBody()) {
                    String contentType = headers.get("Content-Type");
                    HttpEntity entity = ExternalUtils.getInputStreamFromList(paramList, bodyUrl, bodyParamNames, bodyParamHeadersList, null, contentType != null ? ContentType.create(contentType) : null);
                    body = IOUtils.readBytesFromHttpEntity(entity);
                    headers.put("Content-Type", entity.getContentType().getValue());
                }

                Integer timeout = (Integer) context.getBL().LM.timeoutHttp.read(context);

                ExternalHttpResponse response;
                if (clientAction) {
                    response = (ExternalHttpResponse) context.requestUserInteraction(new HttpClientAction(method, connectionString, timeout, body, headers, cookies, cookieStore));
                } else {
                    response = ExternalHttpUtils.sendRequest(method, connectionString, timeout, body, headers, cookies, cookieStore);
                }

                ContentType contentType = response.contentType != null ? ContentType.parse(response.contentType) : ExternalUtils.APPLICATION_OCTET_STREAM;
                ImList<Object> requestParams = response.responseBytes != null ? ExternalUtils.getListFromInputStream(response.responseBytes, contentType) : ListFact.EMPTY();
                fillResults(context, targetPropList, requestParams, ExternalUtils.getCharsetFromContentType(contentType)); // важно игнорировать параметры, так как иначе при общении с LSF пришлось бы всегда TO писать (так как он по умолчанию exportFile возвращает)

                if(headersToProperty != null) {
                    Map<String, List<String>> responseHeaders = response.responseHeaders;
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
                context.getBL().LM.statusHttp.change(response.statusCode, context);
                if (response.statusCode < 200 || response.statusCode >= 300) {
                    throw new RuntimeException(response.statusCode + " " + response.statusText);
                }
            } else {
                throw new RuntimeException("connectionString not specified");
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }

        return FlowResult.FINISH;
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