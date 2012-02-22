package roman;

import platform.interop.ClassViewType;
import platform.server.classes.DataClass;
import platform.server.classes.FileActionClass;
import platform.server.classes.ValueClass;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.actions.CustomActionProperty;

import java.util.Iterator;

/**
 * User: DAle
 * Date: 25.02.11
 * Time: 15:40
 */

public abstract class BaseImportActionProperty extends CustomActionProperty {
    protected final ClassPropertyInterface supplierInterface;
    protected RomanLogicsModule LM;
    protected FileActionClass valueClass;

    public BaseImportActionProperty(RomanLogicsModule LM, String caption, ValueClass supplierClass) {
        this(LM, caption, supplierClass, "xls");
    }

    public BaseImportActionProperty(RomanLogicsModule LM, String caption, ValueClass supplierClass, String extensions) {
        super(LM.baseLM.genSID(), caption, new ValueClass[]{supplierClass});

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        supplierInterface = i.next();
        this.LM = LM;

        String[] extArray = extensions.split(" ");
        String extString = "";
        for (String ext : extArray) {
            if (extString.length() > 0) {
                extString = extString + ", ";
            }
            extString = extString + "*." + ext;
        }
        valueClass = FileActionClass.getDefinedInstance(true, "Файлы c данными (" + extString + ")", extensions);
    }

    @Override
    public void proceedDefaultDraw(PropertyDrawEntity<ClassPropertyInterface> entity, FormEntity<?> form) {
        super.proceedDefaultDraw(entity, form);
        entity.shouldBeLast = true;
        entity.forceViewType = ClassViewType.PANEL;
    }

    @Override
    public DataClass getValueClass() {
        return valueClass;
    }
}
