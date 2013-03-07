package roman;

import platform.interop.ClassViewType;
import platform.server.classes.CustomStaticFormatFileClass;
import platform.server.classes.DataClass;
import platform.server.classes.ValueClass;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.actions.CustomReadValueActionProperty;

import java.util.Iterator;

/**
 * User: DAle
 * Date: 25.02.11
 * Time: 15:40
 */

public abstract class BaseImportActionProperty extends CustomReadValueActionProperty {
    protected final ClassPropertyInterface supplierInterface;
    protected RomanLogicsModule RomanLM;
    protected CustomStaticFormatFileClass valueClass;

    public BaseImportActionProperty(RomanLogicsModule RomanLM, String caption, ValueClass supplierClass) {
        this(RomanLM, caption, supplierClass, "xls xlsx");
    }

    public BaseImportActionProperty(RomanLogicsModule RomanLM, String caption, ValueClass supplierClass, String extensions) {
        super(RomanLM.baseLM.genSID(), caption, new ValueClass[]{supplierClass});

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        supplierInterface = i.next();
        this.RomanLM = RomanLM;

        String[] extArray = extensions.split(" ");
        String extString = "";
        for (String ext : extArray) {
            if (extString.length() > 0) {
                extString = extString + ", ";
            }
            extString = extString + "*." + ext;
        }
        valueClass = CustomStaticFormatFileClass.getDefinedInstance(true, "Файлы c данными (" + extString + ")", extensions);
    }

    @Override
    public void proceedDefaultDraw(PropertyDrawEntity<ClassPropertyInterface> entity, FormEntity<?> form) {
        super.proceedDefaultDraw(entity, form);
        entity.shouldBeLast = true;
        entity.forceViewType = ClassViewType.PANEL;
    }

    protected DataClass getReadType() {
        return valueClass;
    }
}
