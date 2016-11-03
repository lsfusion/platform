package lsfusion.server.logics.property.actions.form;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.PropertyDrawEntity;
import lsfusion.server.form.view.PropertyDrawView;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.awt.*;

import static lsfusion.server.logics.property.derived.DerivedProperty.createAnd;
import static lsfusion.server.logics.property.derived.DerivedProperty.createTrue;

public abstract class FormToolbarActionProperty extends ScriptingActionProperty {
    public final static Dimension BUTTON_SIZE = new Dimension(25, 20);

    @Override
    protected boolean isVolatile() { // проще чем разбираться, что используется
        return true;
    }

    public FormToolbarActionProperty(ScriptingLogicsModule lm) {
        this(lm, true);
    }

    public FormToolbarActionProperty(ScriptingLogicsModule lm, final boolean showCaption) {
        super(lm);

        final LCP propertyCaption = getShowIf();
        drawOptions.addProcessor(new DefaultProcessor() {
            public void proceedDefaultDraw(PropertyDrawEntity entity, FormEntity<?> form) {
                if (propertyCaption != null) {
                    entity.propertyShowIf = form.addPropertyObject(propertyCaption);
                }
            }
            public void proceedDefaultDesign(PropertyDrawView propertyView) {
                if (!showCaption) {
                    propertyView.caption = LocalizedString.create("");
                }
            }
        });
    }

    protected CalcProperty getEnableIf() {
        return null;
    }

    protected LCP getShowIf() {
        return null;
    }

    @Override
    public CalcPropertyMapImplement<?, ClassPropertyInterface> getWhereProperty(boolean recursive) {
        CalcProperty enableIf = getEnableIf();
        return enableIf == null ? super.getWhereProperty(recursive) : enableIf.getImplement();
    }

    static LCP createShowIfProperty(final CalcProperty showIfs[], boolean showIfNots[]) {
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
}
