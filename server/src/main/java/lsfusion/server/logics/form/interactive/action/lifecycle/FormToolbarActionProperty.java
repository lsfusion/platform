package lsfusion.server.logics.form.interactive.action.lifecycle;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawView;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.implement.CalcPropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.CalcPropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.language.linear.LCP;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.language.ScriptingAction;
import lsfusion.server.language.ScriptingLogicsModule;

import java.awt.*;

import static lsfusion.server.logics.property.derived.DerivedProperty.createAnd;
import static lsfusion.server.logics.property.derived.DerivedProperty.createTrue;

public abstract class FormToolbarActionProperty extends ScriptingAction {
    public final static Dimension BUTTON_SIZE = new Dimension(25, 20);

    public FormToolbarActionProperty(ScriptingLogicsModule lm) {
        this(lm, true);
    }

    public FormToolbarActionProperty(ScriptingLogicsModule lm, final boolean showCaption) {
        super(lm);

        final LCP propertyCaption = getShowIf();
        drawOptions.addProcessor(new DefaultProcessor() {
            public void proceedDefaultDraw(PropertyDrawEntity entity, FormEntity form) {
                if (propertyCaption != null) {
                    entity.propertyShowIf = form.addPropertyObject(propertyCaption);
                }
            }
            public void proceedDefaultDesign(PropertyDrawView propertyView) {
                if (!showCaption) {
                    propertyView.caption = LocalizedString.NONAME;
                }
            }
        });
    }

    protected Property getEnableIf() {
        return null;
    }

    protected LCP getShowIf() {
        return null;
    }

    @Override
    public CalcPropertyMapImplement<?, ClassPropertyInterface> getWhereProperty(boolean recursive) {
        Property enableIf = getEnableIf();
        return enableIf == null ? super.getWhereProperty(recursive) : enableIf.getImplement();
    }

    static LCP createShowIfProperty(final Property showIfs[], boolean showIfNots[]) {
        assert showIfs != null && showIfNots != null && showIfs.length == showIfNots.length;

        MList<CalcPropertyInterfaceImplement<PropertyInterface>> mAnds = ListFact.mList(showIfs.length);
        MList<Boolean> mNots = ListFact.mList(showIfs.length);

        for (int i = 0; i < showIfs.length; ++i) {
            mAnds.add(showIfs[i].getImplement());
            mNots.add(showIfNots[i]);
        }

        CalcPropertyMapImplement showIfImplement = createAnd(SetFact.<PropertyInterface>EMPTY(), createTrue(), mAnds.immutableList(), mNots.immutableList());
        return new LCP(showIfImplement.property);
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        if (type.isChange())
            return false;
        return super.hasFlow(type);
    }
}
