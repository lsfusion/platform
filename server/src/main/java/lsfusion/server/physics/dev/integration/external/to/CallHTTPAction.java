package lsfusion.server.physics.dev.integration.external.to;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.heavy.OrderedMap;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.MOrderExclSet;
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
import lsfusion.server.logics.action.implement.ActionImplement;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.ParseException;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.classes.data.file.CustomStaticFormatFileClass;
import lsfusion.server.logics.property.classes.infer.ClassType;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.authentication.controller.remote.RemoteConnection;
import lsfusion.server.physics.admin.log.ServerLoggers;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.Cookie;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.NameValuePair;

import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static lsfusion.base.BaseUtils.nvl;

public abstract class CallHTTPAction extends CallAction {
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
    private boolean noEncode;

    protected CallHTTPAction(boolean clientAction, ExternalHttpMethod method, ImList<Type> params, ImList<LP> targetPropList,
                          int bodyParamNamesSize, ImList<LP> bodyParamHeadersPropertyList,
                          LP headersProperty, LP cookiesProperty, LP headersToProperty, LP cookiesToProperty,
                          boolean noEncode, boolean hasBodyUrl) {
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
        this.noEncode = noEncode;
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

    private String getTransformedEncodedText(ExecutionContext<PropertyInterface> context, PropertyInterface param, Charset charset) {
        if (param == null)
            return null;

        String transformedText = getTransformedText(context, param);
        if(transformedText != null && !noEncode) {
            try {
                transformedText = URIUtil.encodeQuery(transformedText, charset.toString());
            } catch (URIException e) {
                throw Throwables.propagate(e);
            }
        }

        return transformedText;
    }

    @Override
    protected FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) {
        Charset defaultUrlCharset = ExternalUtils.defaultUrlCharset;
        String connectionString = getTransformedEncodedText(context, queryInterface, defaultUrlCharset);
        if(connectionString == null)
            throw new RuntimeException("connectionString not specified");

        boolean noExec = isNoExec(connectionString);

        String requestLogMessage = !noExec && Settings.get().isLogToExternalSystemRequests() ? RemoteConnection.getExternalSystemRequestsLog(ThreadLocalContext.getLogInfo(Thread.currentThread()), connectionString, method.name(), null) : null;
        boolean successfulResponse = false;
        try {
            Result<ImOrderSet<PropertyInterface>> rNotUsedParams = new Result<>();
            connectionString = replaceParams(context, createUrlProcessor(connectionString, noExec), rNotUsedParams, defaultUrlCharset);

            if(noExec)
                targetPropList.single().change(connectionString, context);
            else {
                String bodyUrl = null;

                Map<String, String> headers = new HashMap<>();
                if (headersProperty != null) {
                    MapFact.addJavaAll(headers, readPropertyValues(context.getEnv(), headersProperty));
                }
                Map<String, String> cookies = new HashMap<>();
                if (cookiesProperty != null) {
                    MapFact.addJavaAll(cookies, readPropertyValues(context.getEnv(), cookiesProperty));
                }

                CookieStore cookieStore = new BasicCookieStore();

                byte[] body = null;
                if (method.hasBody()) {
                    ContentType forceContentType = ExternalUtils.parseContentType(headers.get("Content-Type"));

                    Charset bodyUrlCharset = ExternalUtils.getBodyUrlCharset(forceContentType);
                    bodyUrl = getTransformedEncodedText(context, bodyUrlInterface, bodyUrlCharset);
                    if(bodyUrl != null) {
                        bodyUrl = replaceParams(context, createUrlProcessor(bodyUrl, noExec), rNotUsedParams, bodyUrlCharset);
                        if (!rNotUsedParams.result.isEmpty()) {
                            throw new RuntimeException("All params should be used in BODYURL");
                        }
                    }

                    Charset bodyCharset = ExternalUtils.getBodyCharset(forceContentType);
                    Object[] paramList = new Object[rNotUsedParams.result.size()];
                    for (int i = 0, size = rNotUsedParams.result.size(); i < size; i++)
                        paramList[i] = formatHTTP(context, rNotUsedParams.result.get(i), bodyCharset);

                    ImList<String> bodyParamNames = bodyParamNamesInterfaces.mapListValues(bodyParamNamesInterface -> (String) context.getKeyObject(bodyParamNamesInterface));
                    List<Map<String, String>> bodyParamHeadersList = new ArrayList<>();
                    for (LP bodyParamHeadersProperty : bodyParamHeadersPropertyList) {
                        Map<String, String> bodyParamHeaders = new HashMap<>();
                        MapFact.addJavaAll(bodyParamHeaders, readPropertyValues(context.getEnv(), bodyParamHeadersProperty));
                        bodyParamHeadersList.add(bodyParamHeaders);
                    }

                    HttpEntity entity = ExternalUtils.getInputStreamFromList(paramList, bodyUrl, bodyParamNames, bodyParamHeadersList, null, forceContentType, bodyCharset);
                    if (entity != null) {
                        body = IOUtils.readBytesFromHttpEntity(entity);
                        headers.put("Content-Type", entity.getContentType());
                    }
                }

                Integer timeout = nvl((Integer) context.getBL().LM.timeoutHttp.read(context), getDefaultTimeout());
                boolean insecureSSL = context.getBL().LM.insecureSSL.read(context) != null;

                ExternalHttpResponse response;
                if (clientAction) {
                    response = (ExternalHttpResponse) context.requestUserInteraction(new HttpClientAction(method, connectionString, timeout, insecureSSL, body, headers, cookies, cookieStore));
                } else {
                    response = ExternalHttpUtils.sendRequest(method, connectionString, timeout, insecureSSL, body, headers, cookies, cookieStore);
                }

                byte[] responseBytes = response.responseBytes;
                ImList<ExternalRequest.Param> requestParams = responseBytes != null ? ExternalUtils.getListFromInputStream(responseBytes, ExternalUtils.parseContentType(response.contentType)) : ListFact.EMPTY();
                fillResults(context, targetPropList, requestParams); // важно игнорировать параметры, так как иначе при общении с LSF пришлось бы всегда TO писать (так как он по умолчанию exportFile возвращает)

                Map<String, List<String>> responseHeaders = response.responseHeaders;
                String[] headerNames = responseHeaders.keySet().toArray(new String[0]);
                String[] headerValues = getResponseHeaderValues(responseHeaders, headerNames);
                if (headersToProperty != null) {
                    writePropertyValues(context, headersToProperty, headerNames, headerValues);
                }
                Map<String, String> responseCookies = getResponseCookies(cookieStore);
                if (cookiesToProperty != null) {
                    String[] cookieNames = responseCookies.keySet().toArray(new String[0]);
                    String[] cookieValues = responseCookies.values().toArray(new String[0]);
                    writePropertyValues(context, cookiesToProperty, cookieNames, cookieValues);
                }
                int statusCode = response.statusCode;
                context.getBL().LM.statusHttp.change(statusCode, context);

                // LOGGING
                String responseEntity = responseBytes != null ? new String(responseBytes, ExternalUtils.getLoggingCharsetFromContentType(response.contentType)) : null;
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

    private static boolean isNoExec(String connectionString) {
        return connectionString.isEmpty() || connectionString.startsWith("/");
    }

    public static ObjectValue[] getParams(DataSession session, LAP<?, ?> property, ExternalRequest.Param[] params, List<NameValuePair> queryParams, String queryParamsCharsetName) throws ParseException, SQLException, SQLHandledException {
        ValueClass[] classes = property.getInterfaceClasses(ClassType.parsePolicy);
        String[] names = property.getInterfaceNames();
        ObjectValue[] values = new ObjectValue[classes.length];

        int interfacesSize = classes.length;
        int prmUsed = 0;
        for (int i = 0; i < interfacesSize; i++) {
            ValueClass valueClass = classes[i];
            String paramName = names[i];

            ExternalRequest.Param param = null;
            // if there are not enough parameters - looking for some in the query
            String queryParamValue;
            if(queryParams != null && paramName != null && interfacesSize - i > params.length - prmUsed && (queryParamValue = ExternalUtils.getParameterValue(queryParams, paramName)) != null)
                param = ExternalRequest.getUrlParam(queryParamValue, queryParamsCharsetName);

            // if we have not found one - using the next in the list
            if(param == null && prmUsed < params.length)
                param = params[prmUsed++];

            Object value = null;
            if (param != null) // all incorrect params will consider to be nulls
                value = (valueClass != null ? valueClass.getType().parseHTTP(param) : param.value);

            if(valueClass == null && value != null)
                valueClass = value instanceof String ? StringClass.instance : CustomStaticFormatFileClass.get();

            values[i] = value == null ? NullValue.instance : session.getObjectValue(valueClass, value);
        }
        return values;
    }

    public static void fillResults(ExecutionContext context, ImList<LP> targetPropList, ImList<ExternalRequest.Param> results) throws ParseException, SQLException, SQLHandledException {
        for(int i = 0, size = targetPropList.size(); i < size; i++) {
            LP<?> targetProp = targetPropList.get(i);

            ExternalRequest.Param value = null;
            if (i < results.size()) // для недостающих записываем null
                value = results.get(i);

            targetProp.change(value == null ? null : targetProp.property.getType().parseHTTP(value), context);
        }
    }

    public static ImMap<String, String> readPropertyValues(ExecutionEnvironment env, LP<?> property) throws SQLException, SQLHandledException {
        return BaseUtils.immutableCast(property.readAll(env).mapKeys(value -> (String) value.single()));
    }

    public static <P extends PropertyInterface> void writePropertyValues(ExecutionContext context, LP<P> property, String[] names, String[] values) throws SQLException, SQLHandledException {
        writePropertyValues(context.getSession(), context.getEnv(), property, names, values);
    }
    public static <P extends PropertyInterface> void writePropertyValues(DataSession session, ExecutionEnvironment env, LP<P> property, String[] names, String[] values) throws SQLException, SQLHandledException {
        property.change(session, env, MapFact.toMap(names, values));
    }

    public static <P extends PropertyInterface> void writePropertyValues(DataSession session, ExecutionEnvironment env, LP<P> property, ImMap<ImList<Object>, String> params) throws SQLException, SQLHandledException {
        property.changeList(session, env, params);
    }

    protected Integer getDefaultTimeout() {
        return null;
    };

    protected abstract UrlProcessor createUrlProcessor(String connectionString, boolean noExec);
    interface UrlProcessor {
        boolean proceed(int number, Object value, String encodedValue);

        String finish(ExecutionContext<PropertyInterface> context);
    }

    public <T extends PropertyInterface> ActionImplement<PropertyInterface, PropertyInterfaceImplement<T>> getActionImplement(PropertyInterfaceImplement<T> query, ImList<PropertyInterfaceImplement<T>> params) {
        return new ActionImplement<>(this, paramInterfaces.mapList(params).addExcl(queryInterface, query));
    }

    protected Object formatHTTP(ExecutionContext<PropertyInterface> context, PropertyInterface paramInterface, Charset charset) {
        ObjectValue value = context.getKeyValue(paramInterface);
        return context.convertFileValue(getParamType(paramInterface, value).formatHTTP(value.getValue(), charset));
    }

    protected String replaceParams(ExecutionContext<PropertyInterface> context, CallHTTPAction.UrlProcessor urlProcessor, Result<ImOrderSet<PropertyInterface>> rNotUsedParams, Charset urlCharset) {
        ImOrderSet<PropertyInterface> orderInterfaces = paramInterfaces;
        MOrderExclSet<PropertyInterface> mNotUsedParams = rNotUsedParams != null ? SetFact.mOrderExclSetMax(orderInterfaces.size()) : null;
        for (int i = 0, size = orderInterfaces.size(); i < size ; i++) {
            PropertyInterface paramInterface = orderInterfaces.get(i);
            Object value = formatHTTP(context, paramInterface, urlCharset);

            if (!urlProcessor.proceed(i, value, ExternalUtils.encodeUrlParam(urlCharset, value))) {
                if(mNotUsedParams != null)
                    mNotUsedParams.exclAdd(paramInterface);
            }
        }
        if(rNotUsedParams != null)
            rNotUsedParams.set(mNotUsedParams.immutableOrder());
        return urlProcessor.finish(context);
    }
}