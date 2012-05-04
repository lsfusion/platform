package platform.server.logics.property.actions;

import platform.base.BaseUtils;
import platform.base.Result;
import platform.server.classes.StringClass;
import platform.server.classes.ValueClass;
import platform.server.logics.DataObject;
import platform.server.logics.linear.LP;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class TranslateActionProperty extends CustomActionProperty {
    private LP sourceProperty;
    private LP targetProperty;
    private LP translationDictionaryTerm;

    public TranslateActionProperty(String sID, String caption, LP translationDictionaryTerm, LP sourceProperty, LP targetProperty, ValueClass dictionary) {
        super(sID, caption, getValueClasses(sourceProperty, dictionary));

        this.translationDictionaryTerm = translationDictionaryTerm;
        this.sourceProperty = sourceProperty;
        this.targetProperty = targetProperty;
    }

    private static ValueClass[] getValueClasses(LP sourceProperty, ValueClass dictionary) {
        List<ValueClass> result = new ArrayList<ValueClass>();
        result.add(dictionary);
        result.addAll(BaseUtils.toList(sourceProperty.getCommonClasses(new Result<ValueClass>(){})));
        return result.toArray(new ValueClass[result.size()]);
    }

    public void execute(ExecutionContext context) throws SQLException {
        List<ClassPropertyInterface> interfacesList = new ArrayList<ClassPropertyInterface>(interfaces);
        DataObject dictionary = context.getKeyValue(interfacesList.remove(0));
        List<DataObject> inputObjects = BaseUtils.mapList(interfacesList, context.getKeys());

        String source = (String) sourceProperty.read(context, inputObjects.toArray(new DataObject[inputObjects.size()]));

        if (source != null) {
            String delim = ", .:;%#$@/\\|<>=+-_)(*&?^!~{}[]\"1234567890'";
            StringTokenizer st = new StringTokenizer(source, delim, true);
            String result = "";
            String fullLineTranslation = (String) translationDictionaryTerm.read(context, dictionary, new DataObject(source, StringClass.get(50)));
            if (fullLineTranslation != null) {
                result = fullLineTranslation.trim();
            } else {
                while (st.hasMoreTokens()) {
                    String token = st.nextToken();
                    if (!delim.contains(token.subSequence(0, token.length()))) {
                        String translation = (String) translationDictionaryTerm.read(context, dictionary, new DataObject(token, StringClass.get(50)));
                        if (translation != null) {
                            token = translation.trim();
                        }
                    }
                    result += token;
                }
            }
            targetProperty.execute(result, context, inputObjects.toArray(new DataObject[inputObjects.size()]));
        }
    }
}
