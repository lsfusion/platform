package lsfusion.server.logics.form.interactive.action.lifecycle;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawView;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawExtraType;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.awt.*;

import static lsfusion.server.logics.property.PropertyFact.createAnd;
import static lsfusion.server.logics.property.PropertyFact.createTrue;

public abstract class FormToolbarAction extends InternalAction {
    public final static Dimension BUTTON_SIZE = new Dimension(25, 20);

    public FormToolbarAction(ScriptingLogicsModule lm) {
        this(lm, true);
    }

    public FormToolbarAction(ScriptingLogicsModule lm, final boolean showCaption) {
        super(lm);

        LP propertyCaption = getShowIf();
        LP readOnlyIf = getReadOnlyIf();
        drawOptions.addProcessor(new DefaultProcessor() {
            public void proceedDefaultDraw(PropertyDrawEntity entity, FormEntity form) {
                if (propertyCaption != null) {
                    entity.setPropertyExtra(form.addPropertyObject(propertyCaption), PropertyDrawExtraType.SHOWIF);
                }
                if(readOnlyIf != null) {
                    entity.setPropertyExtra(form.addPropertyObject(readOnlyIf), PropertyDrawExtraType.READONLYIF);
                }
            }
            public void proceedDefaultDesign(PropertyDrawView propertyView) {
                if (!showCaption) {
                    propertyView.caption = LocalizedString.NONAME;
                }
            }
        });
    }

    protected LP getShowIf() {
        return null;
    }

    protected LP getReadOnlyIf() {
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

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        if (type.isChange())
            return false;
        return super.hasFlow(type);
    }
}
