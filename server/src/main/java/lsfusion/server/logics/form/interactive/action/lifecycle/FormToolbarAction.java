package lsfusion.server.logics.form.interactive.action.lifecycle;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.server.base.version.Version;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawView;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawExtraType;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.cases.CalcCase;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import static lsfusion.server.logics.property.PropertyFact.*;

public abstract class FormToolbarAction extends InternalAction {

    public FormToolbarAction(ScriptingLogicsModule lm) {
        super(lm);

        drawOptions.addProcessor(new DefaultProcessor() {
            public void proceedDefaultDraw(PropertyDrawEntity entity, FormEntity form, Version version) {
                LP propertyCaption = getShowIf();
                if (propertyCaption != null) {
                    entity.setPropertyExtra(form.addPropertyObject(propertyCaption), PropertyDrawExtraType.SHOWIF, version);
                }
                LP readOnlyIf = getReadOnlyIf();
                if(readOnlyIf != null) {
                    entity.setPropertyExtra(form.addPropertyObject(readOnlyIf), PropertyDrawExtraType.READONLYIF, version);
                }
            }
            public void proceedDefaultDesign(PropertyDrawView propertyView) {
                if (!isShowCaption()) {
                    propertyView.caption = LocalizedString.NONAME;
                }

                String valueElementClass = getValueElementClass();
                if(valueElementClass != null)
                    propertyView.valueElementClass = valueElementClass;
            }
        });
    }

    protected LP getShowIf() {
        return null;
    }

    protected LP getReadOnlyIf() {
        return null;
    }

    protected boolean isShowCaption() {
        return true;
    }

    protected String getValueElementClass() {
        return null;
    }

    public static LP createIfProperty(final Property ifs[], boolean ifNots[]) {
        assert ifs != null && ifNots != null && ifs.length == ifNots.length;

        MList<PropertyInterfaceImplement<PropertyInterface>> mAnds = ListFact.mList(ifs.length);
        MList<Boolean> mNots = ListFact.mList(ifs.length);

        for (int i = 0; i < ifs.length; ++i) {
            mAnds.add(ifs[i].getImplement());
            mNots.add(ifNots[i]);
        }

        PropertyMapImplement showIfImplement = createAnd(SetFact.EMPTY(), createTrue(), mAnds.immutableList(), mNots.immutableList());
        return new LP(showIfImplement.property);
    }

    protected static <C extends PropertyInterface> LP createDisableIfNotProperty(Property disableIf) {
        ImList<CalcCase<C>> cases = ListFact.singleton(new CalcCase<>(createNot(disableIf.getImplement()).property.getImplement(), PropertyFact.createTTrue()));
        return new LP(PropertyFact.createCaseProperty(SetFact.EMPTY(), false, cases).mapEntityObjects(MapFact.EMPTYREV()).property);
    }

    @Override
    protected boolean hasNoChange() {
        return true;
    }
}
