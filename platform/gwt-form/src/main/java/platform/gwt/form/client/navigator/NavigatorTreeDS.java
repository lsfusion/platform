package platform.gwt.form.client.navigator;

import com.google.gwt.core.client.GWT;
import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.data.fields.DataSourceBooleanField;
import com.smartgwt.client.data.fields.DataSourceImageField;
import com.smartgwt.client.data.fields.DataSourceTextField;

public class NavigatorTreeDS extends DataSource {

    private static NavigatorTreeDS instance = null;

    public static NavigatorTreeDS getInstance() {
        if (instance == null) {
            instance = new NavigatorTreeDS("navigatorDS");
        }
        return instance;
    }

    public NavigatorTreeDS(String dsID) {

        setID(dsID);

        setTitleField("caption");
        setRecordXPath("/nodes/node");

        DataSourceTextField captionField = new DataSourceTextField("caption", "Caption", 128);

        DataSourceTextField elementIdField = new DataSourceTextField("elementSid", "Element SID");
        elementIdField.setPrimaryKey(true);
        elementIdField.setRequired(true);

        DataSourceTextField parentIdField = new DataSourceTextField("parentSid", "Parent SID");
        parentIdField.setRequired(true);
        parentIdField.setForeignKey(dsID + ".elementSid");
        parentIdField.setRootValue("baseElement");

        DataSourceBooleanField isFormField = new DataSourceBooleanField("isForm", "Is Form");
        DataSourceImageField iconField = new DataSourceImageField("icon", "Icon", 128);

        setFields(captionField, elementIdField, parentIdField, isFormField, iconField);

        setDataURL(GWT.getHostPageBaseURL() + "navigatorDS");
        setClientOnly(true);
    }
}