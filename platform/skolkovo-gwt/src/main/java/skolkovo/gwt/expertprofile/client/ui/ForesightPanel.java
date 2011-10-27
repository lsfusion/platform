package skolkovo.gwt.expertprofile.client.ui;

import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.TitleOrientation;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.FormItemIfFunction;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.layout.Layout;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;
import com.smartgwt.client.widgets.layout.VLayout;
import platform.gwt.base.client.ui.VLayout100;
import skolkovo.api.gwt.shared.ForesightInfo;
import skolkovo.api.gwt.shared.ProfileInfo;
import skolkovo.gwt.expertprofile.client.ExpertProfileMessages;

import java.util.Stack;

public class ForesightPanel extends VLayout100 {
    private static ExpertProfileMessages messages = ExpertProfileMessages.Instance.get();

    private ProfileInfo pi;
    private SectionStack fSections;
    private DynamicForm form;

    public ForesightPanel(ProfileInfo PI) {
        this.pi = PI;

        setLayoutMargin(10);
        setOverflow(Overflow.AUTO);

        createForesightForm();
        createSectionStack();

        addMember(form);
        addMember(fSections);
    }

    private void createForesightForm() {
        final CheckboxItem cbTechnical = new CheckboxItem();
        cbTechnical.setTitle(messages.technical());
        cbTechnical.setWrapTitle(true);
        cbTechnical.setWidth("*");
        cbTechnical.setColSpan("*");
        cbTechnical.setShowTitle(false);
        cbTechnical.addChangedHandler(new ChangedHandler() {
            @Override
            public void onChanged(ChangedEvent event) {
                pi.technical = cbTechnical.getValueAsBoolean();
            }
        });

        final CheckboxItem cbBusiness = new CheckboxItem();
        cbBusiness.setTitle(messages.business());
        cbBusiness.setWrapTitle(true);
        cbBusiness.setWidth("*");
        cbBusiness.setColSpan("*");
        cbBusiness.setShowTitle(false);
        cbBusiness.addChangedHandler(new ChangedHandler() {
            @Override
            public void onChanged(ChangedEvent event) {
                pi.business = cbBusiness.getValueAsBoolean();
            }
        });

        form = new DynamicForm();
        form.setIsGroup(true);
        form.setGroupTitle(messages.expertiseClass());
        form.setMargin(5);
        form.setWidth100();
        form.setAutoHeight();
        form.setNumCols(1);
        form.setFields(cbTechnical, cbBusiness);
    }

    private void createSectionStack() {
        fSections = new SectionStack();
        fSections.setOverflow(Overflow.VISIBLE);
        fSections.setVisibilityMode(VisibilityMode.MULTIPLE);

        ForesightInfo[] fis = pi.foresightInfos;
        int cnt = fis.length;
        Stack<StackElement> s = new Stack<StackElement>();
        for (int i = 0; i < cnt; i++) {
            ForesightInfo fi = fis[i];

            int fLevel = popWithMoreEqLevel(s, fi);
            if (s.empty()) {
                Layout fComponent = createSectionElement(fi);

                s.push(new StackElement(fi, fLevel, fComponent));
            } else if (i < cnt - 1 && getForesightLevel(fis[i+1]) > getForesightLevel(fi)) {
                Layout fComponent = createGroupElement(fi);

                s.peek().layout.addMember(fComponent);

                s.push(new StackElement(fi, fLevel, fComponent));
            } else {
                Layout fComponent = createLeafElement(fi);

                s.peek().layout.addMember(fComponent);
            }
        }
    }

    private Layout createSectionElement(ForesightInfo fi) {
        VLayout layout = new VLayout(5);
        layout.setLayoutMargin(10);
        layout.setAutoHeight();

        SectionStackSection fSection = new SectionStackSection(fi.sID + " " + fi.name);
        fSection.setExpanded(false);
        fSection.setItems(layout);

        fSections.addSection(fSection);

        return layout;
    }

    private Layout createGroupElement(ForesightInfo fi) {
        VLayout layout = new VLayout();
        layout.setMargin(5);
        layout.setIsGroup(true);
        layout.setGroupTitle(fi.sID + " " + fi.name);
        return layout;
    }

    private Layout createLeafElement(final ForesightInfo fi) {
        final TextAreaItem taComment = new TextAreaItem();
        taComment.setTitleOrientation(TitleOrientation.TOP);
        taComment.setTitle(messages.commentHint());
        taComment.setEndRow(true);
        taComment.setStartRow(true);
        taComment.setWidth("*");
        taComment.setHeight(50);
        taComment.setValue(fi.comment);
        taComment.addChangedHandler(new ChangedHandler() {
            @Override
            public void onChanged(ChangedEvent event) {
                fi.comment = taComment.getValueAsString();
            }
        });

        final CheckboxItem cbSelected = new CheckboxItem();
        cbSelected.setTitle(fi.sID + " " + fi.name);
        cbSelected.setWrapTitle(true);
        cbSelected.setWidth("*");
        cbSelected.setColSpan("*");
        cbSelected.setShowTitle(false);
        cbSelected.setRedrawOnChange(true);

        cbSelected.addChangedHandler(new ChangedHandler() {
            @Override
            public void onChanged(ChangedEvent event) {
                fi.selected = cbSelected.getValueAsBoolean();
            }
        });

        taComment.setShowIfCondition(new FormItemIfFunction() {
            @Override
            public boolean execute(FormItem item, Object value, DynamicForm form) {
                return cbSelected.getValueAsBoolean();
            }
        });

        cbSelected.setValue(fi.selected);

        DynamicForm form = new DynamicForm();
        form.setWidth100();
        form.setMargin(5);
        form.setNumCols(1);
        form.setFields(cbSelected, taComment);

        VLayout vl = new VLayout();
        vl.setWidth100();
        vl.setAutoHeight();
        vl.addMember(form);

        return vl;
    }

    private int popWithMoreEqLevel(Stack<StackElement> s, ForesightInfo fi) {
        int fLevel = getForesightLevel(fi);
        while (!s.empty() && s.peek().level >= fLevel) {
            s.pop();
        }

        return fLevel;
    }

    private int getForesightLevel(ForesightInfo fi) {
        return fi.sID.replaceAll("[^\\.]", "").length();
    }

    public String validate() {
        for (ForesightInfo fi : pi.foresightInfos) {
            String comment = fi.comment;
            if (fi.selected && (comment == null || comment.trim().isEmpty())) {
                return messages.emptyForesightsError();
            }
        }
        return null;
    }

    public ProfileInfo populateProfileInfo() {
        return pi;
    }

    public static class StackElement {
        ForesightInfo info;
        int level;
        public Layout layout;

        public StackElement(ForesightInfo info, int level, Layout layout) {
            this.info = info;
            this.level = level;
            this.layout = layout;
        }
    }
}
