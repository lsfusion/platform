package lsfusion.server.logics.property.actions;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.IOUtils;
import lsfusion.server.classes.DynamicFormatFileClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.actions.flow.FlowResult;
import org.apache.commons.httpclient.util.URIUtil;
import org.codehaus.plexus.util.FileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExternalHTTPActionProperty extends ExternalActionProperty {

    public ExternalHTTPActionProperty(int paramsCount, String query, List<LCP> targetPropList) {
        super(paramsCount, query, targetPropList);
    }

    @Override
    protected FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        try {

            connectionString = replaceParams(context, connectionString);
            byte[] result = readHTTP(connectionString);

            for(LCP targetProp : targetPropList) {
                if (targetProp.property.getType() instanceof DynamicFormatFileClass) {
                    //с расширением могут быть проблемы
                    targetProp.change(BaseUtils.mergeFileAndExtension(result, FileUtils.getExtension(connectionString).getBytes()), context);
                } else {
                    targetProp.change(result, context);
                }
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
        
        return FlowResult.FINISH;
    }

    private byte[] readHTTP(String path) throws IOException {
        final List<String> properties = parseHTTPPath(path);
        if(properties != null) {
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
            return IOUtils.readBytesFromStream(new URL(path).openStream());
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