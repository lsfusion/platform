package lsfusion.server.logics.property.actions.external;

import com.google.common.base.Throwables;
import lsfusion.base.ExternalUtils;
import lsfusion.base.Result;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.type.ParseException;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.NullValue;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.linear.LP;
import lsfusion.server.logics.property.ClassType;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.actions.flow.FlowResult;
import lsfusion.server.session.DataSession;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ExternalHTTPActionProperty extends ExternalActionProperty {
    private PropertyInterface query;

    public ExternalHTTPActionProperty(ImList<Type> params, ImList<LCP> targetPropList) {
        super(1, params, targetPropList);

        this.query = getOrderInterfaces().get(0);
    }

    @Override
    protected FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        try {

            Result<ImOrderSet<PropertyInterface>> rNotUsedParams = new Result<>();
            String replacedParams = replaceParams(context, getTransformedText(context, query), rNotUsedParams, ExternalUtils.TEXT_PLAIN.getMimeType());
            HttpResponse response = readHTTP(context, replacedParams, rNotUsedParams.result);
            HttpEntity responseEntity = response.getEntity();

            if (response.getStatusLine().getStatusCode() != 200)
                throw new RuntimeException(response.getStatusLine().toString());
            else {
                ContentType contentType = ContentType.get(responseEntity);
                List<Object> requestParams = ExternalUtils.getListFromInputStream(responseEntity.getContent(), contentType);
                fillResults(context, targetPropList, requestParams, ExternalUtils.getCharsetFromContentType(contentType)); // важно игнорировать параметры, так как иначе при общении с LSF пришлось бы всегда TO писать (так как он по умолчанию exportFile возвращает)
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }

        return FlowResult.FINISH;
    }

    private HttpResponse readHTTP(ExecutionContext<PropertyInterface> context, String connectionString, ImOrderSet<PropertyInterface> bodyParams) throws IOException {
        HttpClient httpClient = HttpClientBuilder.create().build();

        List<Object> paramList = new ArrayList<>();
        for (PropertyInterface i : bodyParams) {
            paramList.add(format(context, i, null)); // пока в body ничего не кодируем (так как content-type'ы другие)
        }

        if (!paramList.isEmpty()) {
            HttpPost httpPost = new HttpPost(connectionString);

            HttpEntity entity = ExternalUtils.getInputStreamFromList(paramList, null);
            httpPost.addHeader("Content-type", entity.getContentType().getValue());
            httpPost.setEntity(entity);
            return httpClient.execute(httpPost);
        } else {
            return httpClient.execute(new HttpGet(connectionString));
        }
    }

    public static ObjectValue[] getParams(DataSession session, LP property, Object[] params, Charset charset) throws ParseException, SQLException, SQLHandledException {
        ImOrderSet<PropertyInterface> interfaces = (ImOrderSet<PropertyInterface>) property.listInterfaces;
        ImMap<PropertyInterface, ValueClass> interfaceClasses = property.property.getInterfaceClasses(ClassType.editPolicy);
        ObjectValue[] objectValues = new ObjectValue[interfaces.size()];
        for (int i = 0; i < interfaces.size(); i++) {
            ValueClass valueClass = interfaceClasses.get(interfaces.get(i));

            Object value = null;
            if (i < params.length) // для лишних записываем null
                value = valueClass.getType().parse(params[i], charset);

            objectValues[i] = value == null ? NullValue.instance : session.getObjectValue(valueClass, value);
        }
        return objectValues;
    }

    public static void fillResults(ExecutionContext context, ImList<LCP> targetPropList, List<Object> results, Charset charset) throws ParseException, SQLException, SQLHandledException {
        for(int i = 0, size = targetPropList.size(); i < size; i++) {
            LCP targetProp = targetPropList.get(i);

            Object value = null;;
            if (i < results.size()) // для недостающих записываем null
                value = results.get(i);

            targetProp.change(value == null ? null : targetProp.property.getType().parse(value, charset), context);
        }
    }

}