package lsfusion.server.physics.dev.integration.external.to;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.heavy.OrderedMap;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.file.IOUtils;
import lsfusion.interop.session.*;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.NullValue;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.property.LP;
import lsfusion.server.language.property.oraction.LAP;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.controller.context.ExecutionEnvironment;
import lsfusion.server.logics.action.flow.FlowResult;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.ParseException;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.property.classes.infer.ClassType;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.authentication.controller.remote.RemoteConnection;
import lsfusion.server.physics.admin.log.ServerLoggers;
import org.apache.commons.lang.StringUtils;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.Cookie;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;

import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExternalHTTPAction extends CallAction {
    boolean clientAction;
    private ExternalHttpMethod method;
    private PropertyInterface queryInterface;
    private PropertyInterface bodyUrlInterface;
    private ImList<PropertyInterface> bodyParamNamesInterfaces;
    private ImList<LP> bodyParamHeadersPropertyList;
    private LP<?> headersProperty;
    private LP<?> cookiesProperty;
    private LP headersToProperty;
    private LP cookiesToProperty;

    public ExternalHTTPAction(boolean clientAction, ExternalHttpMethod method, ImList<Type> params, ImList<LP> targetPropList,
                              int bodyParamNamesSize, ImList<LP> bodyParamHeadersPropertyList,
                              LP headersProperty, LP cookiesProperty, LP headersToProperty, LP cookiesToProperty, boolean hasBodyUrl) {
        super((hasBodyUrl ? 2 : 1) + bodyParamNamesSize, params, targetPropList);

        this.clientAction = clientAction;
        this.method = method;
        this.queryInterface = getOrderInterfaces().get(0);
        this.bodyUrlInterface = hasBodyUrl ? getOrderInterfaces().get(1) : null;

        int startIndex = hasBodyUrl ? 2 : 1;
        bodyParamNamesInterfaces = getOrderInterfaces().subList(startIndex, startIndex + bodyParamNamesSize);

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

    private OrderedMap<String, String> getResponseCookies(CookieStore cookieStore) {
        OrderedMap<String, String> responseCookies = new OrderedMap<>();
        for(Cookie cookie : cookieStore.getCookies())
            ExternalHttpUtils.formatCookie(responseCookies, cookie);
        return responseCookies;
    }

    @Override
    protected FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) {
        String connectionString = getTransformedText(context, queryInterface);
        String requestLogMessage = Settings.get().isLogToExternalSystemRequests() ?
                RemoteConnection.getExternalSystemRequestsLog(ThreadLocalContext.getLogInfo(Thread.currentThread()), connectionString, method.name(), null) : null;
        boolean successfulResponse = false;
        try {
            Result<ImOrderSet<PropertyInterface>> rNotUsedParams = new Result<>();
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
                    ImList<String> bodyParamNames = bodyParamNamesInterfaces.mapListValues(bodyParamNamesInterface -> (String) context.getKeyObject(bodyParamNamesInterface));
                    HttpEntity entity = ExternalUtils.getInputStreamFromList(paramList, bodyUrl, bodyParamNames, bodyParamHeadersList, null, contentType != null ? ContentType.parse(contentType) : null);
                    if (entity != null) {
                        body = IOUtils.readBytesFromHttpEntity(entity);
                        headers.put("Content-Type", entity.getContentType());
                    }
                }

                Integer timeout = (Integer) context.getBL().LM.timeoutHttp.read(context);
                boolean insecureSSL = context.getBL().LM.insecureSSL.read(context) != null;

                ExternalHttpResponse response;
                if (clientAction) {
                    response = (ExternalHttpResponse) context.requestUserInteraction(new HttpClientAction(method, connectionString, timeout, insecureSSL, body, headers, cookies, cookieStore));
                } else {
                    response = ExternalHttpUtils.sendRequest(method, connectionString, timeout, insecureSSL, body, headers, cookies, cookieStore);
                }

                ContentType contentType = response.contentType != null ? ContentType.parse(response.contentType) : ExternalUtils.APPLICATION_OCTET_STREAM;
                byte[] responseBytes = response.responseBytes;
                ImList<Object> requestParams = responseBytes != null ? ExternalUtils.getListFromInputStream(responseBytes, contentType) : ListFact.EMPTY();
                Charset charset = ExternalUtils.getCharsetFromContentType(contentType);
                fillResults(context, targetPropList, requestParams, charset); // важно игнорировать параметры, так как иначе при общении с LSF пришлось бы всегда TO писать (так как он по умолчанию exportFile возвращает)

                Map<String, List<String>> responseHeaders = response.responseHeaders;
                String[] headerNames = responseHeaders.keySet().toArray(new String[0]);
                String[] headerValues = getResponseHeaderValues(responseHeaders, headerNames);
                if(headersToProperty != null) {
                    writePropertyValues(context, headersToProperty, headerNames, headerValues);
                }
                Map<String, String> responseCookies = getResponseCookies(cookieStore);
                if(cookiesToProperty != null) {
                    String[] cookieNames = responseCookies.keySet().toArray(new String[0]);
                    String[] cookieValues = responseCookies.values().toArray(new String[0]);
                    writePropertyValues(context, cookiesToProperty, cookieNames, cookieValues);
                }
                int statusCode = response.statusCode;
                context.getBL().LM.statusHttp.change(statusCode, context);

                String responseEntity = responseBytes != null ? new String(responseBytes, charset) : null;
                String responseStatus = statusCode + " " + response.statusText;
                successfulResponse = RemoteConnection.successfulResponse(statusCode);

                if (requestLogMessage != null && Settings.get().isLogToExternalSystemRequestsDetail()) {
                    requestLogMessage += RemoteConnection.getExternalSystemRequestsLogDetail(headers,
                            cookies,
                            null,
                            (bodyUrl != null ? "\tREQUEST_BODYURL: " + bodyUrl : null),
                            BaseUtils.toStringMap(headerNames, headerValues),
                            responseCookies,
                            responseStatus,
                            (responseEntity != null ? "\tRESPONSE_BODY:\n" + responseEntity : null));
                }
                if (!successfulResponse)
                    throw new RuntimeException(responseStatus + (responseEntity == null ? "" : "\n" + responseEntity));
            } else {
                throw new RuntimeException("connectionString not specified");
            }
        } catch (Exception e) {
            if (requestLogMessage != null)
                requestLogMessage += "\n\tERROR: " + e.getMessage() + "\n";

            throw Throwables.propagate(e);
        } finally {
            RemoteConnection.logExternalSystemRequest(ServerLoggers.httpToExternalSystemRequestsLogger, requestLogMessage, successfulResponse);
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

    public static <P extends PropertyInterface> void writePropertyValues(ExecutionContext context, LP<P> property, String[] names, String[] values) throws SQLException, SQLHandledException {
        writePropertyValues(context.getSession(), context.getEnv(), property, names, values);
    }
    public static <P extends PropertyInterface> void writePropertyValues(DataSession session, ExecutionEnvironment env, LP<P> property, String[] names, String[] values) throws SQLException, SQLHandledException {
        property.change(session, env, MapFact.toMap(names, values), StringClass.instance, StringClass.instance);
    }

}