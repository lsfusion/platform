package skolkovo.gwt.claimer.client.ui;

import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.FormItemIfFunction;
import com.smartgwt.client.widgets.form.fields.*;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.form.fields.events.FormItemInitHandler;
import com.smartgwt.client.widgets.layout.VLayout;
import gwtupload.client.*;
import platform.gwt.base.client.GwtClientUtils;
import skolkovo.gwt.claimer.client.ClaimerMessages;

import java.util.ArrayList;

public class ProjectApplicationForm extends DynamicForm {
    private static final String DOC_TEMPLATE_URL = "documents/roadmap.xls";

    private static ClaimerMessages messages = ClaimerMessages.Instance.get();
    private ArrayList<FormItem> formItems = new ArrayList<FormItem>();
    private ArrayList<FormItem> currentItems = new ArrayList<FormItem>();

    public ProjectApplicationForm() {
        createProjectInfoSection();

        createInnovationSection();

        createExecutiveSection();

        createFundingSection();

        createEquipmentSection();

        createPatentsSection();

        createDocumentsSection();

        setWidth100();
        setHeight100();
        setColWidths("*", "*");
        setNumCols(2);
        setFields(formItems.toArray(new FormItem[formItems.size()]));
    }

    private void createProjectInfoSection() {
        createLabelItem(messages.projectName());
        createConstraintHintItem(messages.maximumValueLength(170));

        TextAreaItem taProjectNameRu = createRuTextAreaItem(170);
        TextAreaItem taProjectNameEn = createEnTextAreaItem(170);

        addRowSpacerItem();

        createLabelItem(messages.projectManagerName());
        createHintItem(messages.projectManagerHint());
        createSymbolsConstraintHintItem();

        TextItem tProjectManagerNameRu = createRuTextItem();
        TextItem tProjectManagerNameEn = createEnTextItem();

        addRowSpacerItem();

        createSectionItem(messages.projectInformation()).setSectionExpanded(false);
    }

    private void createInnovationSection() {
        createLabelItem(messages.innovationProblem());
        createHintItem(messages.innovationProblemHint());
        createSymbolsConstraintHintItem();

        TextAreaItem taProblemRu = createRuTextAreaItem();
        TextAreaItem taProblemEn = createEnTextAreaItem();

        addRowSpacerItem();

        createLabelItem(messages.innovationDescription());
        createHintItem(messages.innovationDescriptionHint());
        createSymbolsConstraintHintItem();

        TextAreaItem taProblemDescriptionRu = createRuTextAreaItem();
        TextAreaItem taProblemDescriptionEn = createEnTextAreaItem();

        addRowSpacerItem();

        createLabelItem(messages.projectTechnology());

        final RadioGroupItem rgTechnologies = createRadioGroupItem(
                messages.projectTechComparable(),
                messages.projectTechSurpass(),
                messages.projectTechDemanded(),
                messages.projectTechAdvantages(),
                messages.projectTechOutperforms(),
                messages.projectTechNoBenchmarks()
        );

        StaticTextItem lbTechDescription = createLabelItem(messages.projectTechDescription());
        StaticTextItem lbTechDescriptionConstraint = createSymbolsConstraintHintItem();

        TextAreaItem taTechDescriptionRu = createRuTextAreaItem();
        TextAreaItem taTechDescriptionEn = createEnTextAreaItem();

        setShowIfCondition(new FormItemIfFunction() {
            @Override
            public boolean execute(FormItem item, Object value, DynamicForm form) {
                return rgTechnologies.getValue() != null;
            }
        }, lbTechDescription, lbTechDescriptionConstraint, taTechDescriptionRu, taTechDescriptionEn);

        addRowSpacerItem();

        createLabelItem(messages.projectArea());

        final CheckboxItem cbAreaEnergy = addCheckBoxItem(messages.projectAreaEnergy());
        StaticTextItem lbEnergyJustification = createLabelItem(messages.justification());
        TextAreaItem taEnergyDescriptionRu = createRuTextAreaItem();
        TextAreaItem taEnergyDescriptionEn = createEnTextAreaItem();

        setShowIfCondition(new BooleanIfFunction() {
            @Override
            public Boolean booleanResult() {
                return cbAreaEnergy.getValueAsBoolean();
            }
        }, lbEnergyJustification, taEnergyDescriptionRu, taEnergyDescriptionEn);


        final CheckboxItem cbAreaNuclear = addCheckBoxItem(messages.projectAreaNuclear());
        StaticTextItem lbNuclearJustification = createLabelItem(messages.justification());
        TextAreaItem taNuclearDescriptionRu = createRuTextAreaItem();
        TextAreaItem taNuclearDescriptionEn = createEnTextAreaItem();

        setShowIfCondition(new BooleanIfFunction() {
            @Override
            public Boolean booleanResult() {
                return cbAreaNuclear.getValueAsBoolean();
            }
        }, lbNuclearJustification, taNuclearDescriptionRu, taNuclearDescriptionEn);


        final CheckboxItem cbAreaSpace = addCheckBoxItem(messages.projectAreaSpace());
        StaticTextItem lbSpaceJustification = createLabelItem(messages.justification());
        TextAreaItem taSpaceDescriptionRu = createRuTextAreaItem();
        TextAreaItem taSpaceDescriptionEn = createEnTextAreaItem();

        setShowIfCondition(new BooleanIfFunction() {
            @Override
            public Boolean booleanResult() {
                return cbAreaSpace.getValueAsBoolean();
            }
        }, lbSpaceJustification, taSpaceDescriptionRu, taSpaceDescriptionEn);


        final CheckboxItem cbAreaMedical = addCheckBoxItem(messages.projectAreaMedical());
        StaticTextItem lbMedicalJustification = createLabelItem(messages.justification());
        TextAreaItem taMedicalDescriptionRu = createRuTextAreaItem();
        TextAreaItem taMedicalDescriptionEn = createEnTextAreaItem();

        setShowIfCondition(new BooleanIfFunction() {
            @Override
            public Boolean booleanResult() {
                return cbAreaMedical.getValueAsBoolean();
            }
        }, lbMedicalJustification, taMedicalDescriptionRu, taMedicalDescriptionEn);


        final CheckboxItem cbAreaIT = addCheckBoxItem(messages.projectAreaIT());
        StaticTextItem lbITJustification = createLabelItem(messages.justification());
        TextAreaItem taITDescriptionRu = createRuTextAreaItem();
        TextAreaItem taITDescriptionEn = createEnTextAreaItem();

        setShowIfCondition(new BooleanIfFunction() {
            @Override
            public Boolean booleanResult() {
                return cbAreaIT.getValueAsBoolean();
            }
        }, lbITJustification, taITDescriptionRu, taITDescriptionEn);


        final CheckboxItem cbAreaNone = addCheckBoxItem(messages.projectAreaNone());
        StaticTextItem lbNoneJustification = createLabelItem(messages.justification());
        TextAreaItem taNoneDescriptionRu = createRuTextAreaItem();
        TextAreaItem taNoneDescriptionEn = createEnTextAreaItem();

        setShowIfCondition(new BooleanIfFunction() {
            @Override
            public Boolean booleanResult() {
                return cbAreaNone.getValueAsBoolean();
            }
        }, lbNoneJustification, taNoneDescriptionRu, taNoneDescriptionEn);

        cbAreaNone.addChangedHandler(new ChangedHandler() {
            @Override
            public void onChanged(ChangedEvent event) {
                boolean isNoneChecked = isChecked((CheckboxItem) event.getItem());

                cbAreaEnergy.setDisabled(isNoneChecked);
                cbAreaIT.setDisabled(isNoneChecked);
                cbAreaMedical.setDisabled(isNoneChecked);
                cbAreaSpace.setDisabled(isNoneChecked);
                cbAreaNuclear.setDisabled(isNoneChecked);
            }
        });

        ChangedHandler cbAreaHandler = new ChangedHandler() {
            @Override
            public void onChanged(ChangedEvent event) {
                boolean disabled = isChecked(cbAreaEnergy)
                                   || isChecked(cbAreaIT)
                                   || isChecked(cbAreaMedical)
                                   || isChecked(cbAreaSpace)
                                   || isChecked(cbAreaNuclear);
                cbAreaNone.setDisabled(disabled);
            }
        };

        cbAreaEnergy.addChangedHandler(cbAreaHandler);
        cbAreaIT.addChangedHandler(cbAreaHandler);
        cbAreaMedical.addChangedHandler(cbAreaHandler);
        cbAreaSpace.addChangedHandler(cbAreaHandler);
        cbAreaNuclear.addChangedHandler(cbAreaHandler);

        addRowSpacerItem();

        createSectionItem(messages.innovation()).setSectionExpanded(false);
    }

    private void createExecutiveSection() {
        createLabelItem(messages.executiveSummaryFile());
        createHintItem(messages.executiveSummaryHint());
        createConstraintHintItem(messages.availableFileFormatsPDF());
        createSymbolsConstraintHintItem();

        CustomUploadItem fExecutiveRu = (CustomUploadItem) createRuUploadItem();
        CustomUploadItem fExecutiveEn = (CustomUploadItem) createEnUploadItem();

        addRowSpacerItem();

        createSectionItem(messages.executiveSummary()).setSectionExpanded(false);
    }

    private void createFundingSection() {
        CheckboxItem cbFundingReturns = addCheckBoxItem(messages.fundingReturns());
        CheckboxItem cbFundingNonReturns = addCheckBoxItem(messages.fundingNonReturns());
        CheckboxItem cbFundingOwn = addCheckBoxItem(messages.fundingOwn());
        CheckboxItem cbFundingSearch = addCheckBoxItem(messages.fundingSearch());
        CheckboxItem cbFundingOther = addCheckBoxItem(messages.fundingOther());

        addRowSpacerItem();

        createSectionItem(messages.fundingSources()).setSectionExpanded(true);
    }

    private void createEquipmentSection() {
        createHintItem(messages.equipmentHint());

        CheckboxItem cbEquipmentOwned = addCheckBoxItem(messages.equipmentOwned());
        CheckboxItem cbEquipmentBuy = addCheckBoxItem(messages.equipmentBuy());
        CheckboxItem cbEquipmentAgreement = addCheckBoxItem(messages.equipmentAgreement());
        CheckboxItem cbEquipmentSkolkovo = addCheckBoxItem(messages.equipmentSkolkovo());
        CheckboxItem cbEquipmentSeek = addCheckBoxItem(messages.equipmentSeek());
        CheckboxItem cbEquipmentOther = addCheckBoxItem(messages.equipmentOther());

        addRowSpacerItem();

        createSectionItem(messages.equipment()).setSectionExpanded(false);
    }

    private void createPatentsSection() {
        createHintItem(messages.patentsHint());

        addRowSpacerItem();

        createSectionItem(messages.patents()).setSectionExpanded(false);
    }

    private void createDocumentsSection() {
        createLabelItem(messages.documentsRoadmap());
        createHintItem(messages.documentsRoadmapHint(makeTemplateLink()));
        createConstraintHintItem(messages.availableFileFormatsPDF());
        createConstraintHintItem(messages.maximumFileSize4Mb());

        createRuUploadItem();
        createEnUploadItem();

        createLabelItem(messages.documentsDescription());
        createConstraintHintItem(messages.availableFileFormatsPDF());
        createConstraintHintItem(messages.maximumFileSize4Mb());

        createRuUploadItem();
        createEnUploadItem();

        createLabelItem(messages.documentsPreliminarySertificate());
        createHintItem(messages.documentsPreliminaryHint());
        createConstraintHintItem(messages.availableFileFormatsPDF());
        createConstraintHintItem(messages.maximumFileSize4Mb());

        createRuUploadItem().setIcons();

        addRowSpacerItem();

        createSectionItem(messages.documents()).setSectionExpanded(false);
    }

    private String makeTemplateLink() {
        return "<a href=" + GwtClientUtils.getWebAppBaseURL() + DOC_TEMPLATE_URL + ">" + messages.documentsTemplate() + "</a>";
    }

    private boolean isChecked(CheckboxItem cb) {
        Boolean val = cb.getValueAsBoolean();
        return val != null && val;
    }

    private void setShowIfCondition(FormItemIfFunction showIfcondition, FormItem... items) {
        for (FormItem item : items) {
            item.setShowIfCondition(showIfcondition);
        }
    }

    private CheckboxItem addCheckBoxItem(String title) {
        CheckboxItem cbItem = new CheckboxItem();
        cbItem.setColSpan("*");
        cbItem.setEndRow(true);
        cbItem.setStartRow(true);
        cbItem.setWrapTitle(true);
        cbItem.setShowTitle(false);
        cbItem.setTitle(title);
        cbItem.setRedrawOnChange(true);

        addFormItem(cbItem);

        return cbItem;
    }

    private FormItem createRuUploadItem() {
        return createUploadItem(true);
    }

    private FormItem createEnUploadItem() {
        return createUploadItem(false);
    }

    private FormItem createUploadItem(boolean ruItem) {
        CustomUploadItem customItem = new CustomUploadItem();
        customItem.setIcons(createLocaleIcon(ruItem));

        addFormItem(customItem);

        StaticTextItem uploadStatusLabel = createUploadStatusItem();
        customItem.setValueField(uploadStatusLabel.getName());

        return customItem;
    }

    private void addFormItem(FormItem customItem) {
        currentItems.add(customItem);
    }

    private RadioGroupItem createRadioGroupItem(String... values) {
        RadioGroupItem rgItem = new RadioGroupItem();
        rgItem.setShowTitle(false);
        rgItem.setColSpan("*");
        rgItem.setValueMap(values);
        rgItem.setRedrawOnChange(true);

        addFormItem(rgItem);

        return rgItem;
    }

    private void addRowSpacerItem() {
        addFormItem(new RowSpacerItem());
    }

    private FormItem[] getCurrentItems() {
        return currentItems.toArray(new FormItem[currentItems.size()]);
    }

    private SectionItem createSectionItem(String title) {
        SectionItem sectionItem = new SectionItem();
        sectionItem.setValue(title);
        sectionItem.setItemIds(itemNames(getCurrentItems()));

        formItems.add(sectionItem);
        formItems.addAll(currentItems);
        currentItems.clear();

        return sectionItem;
    }

    private String[] itemNames(FormItem... items) {
        String res[] = new String[items.length];
        for (int i = 0; i < items.length; ++i) {
            res[i] = items[i].getName();
        }

        return res;
    }

    private StaticTextItem createLabelItem(String title) {
        return createLabelItem(title, true);
    }

    private StaticTextItem createLabelItem(String title, boolean required) {
        StaticTextItem labelItem = new StaticTextItem();
        labelItem.setValue(title + " :");
        labelItem.setShowTitle(false);
        labelItem.setColSpan("*");
        labelItem.setTextBoxStyle("label");
        labelItem.setEndRow(true);

        addFormItem(labelItem);

        return labelItem;
    }

    private StaticTextItem createHintItem(String hint) {
        StaticTextItem hintItem = new StaticTextItem();
        hintItem.setValue(hint);
        hintItem.setShowTitle(false);
        hintItem.setColSpan("*");
        hintItem.setTextBoxStyle("hint");
        hintItem.setEndRow(true);

        addFormItem(hintItem);

        return hintItem;
    }

    private StaticTextItem createUploadStatusItem() {
        StaticTextItem hintItem = new StaticTextItem();
        hintItem.setShowTitle(false);
        hintItem.setColSpan("*");
        hintItem.setTextBoxStyle("uploadStatusLabel");
        hintItem.setEndRow(true);

        addFormItem(hintItem);

        return hintItem;
    }

    private StaticTextItem createSymbolsConstraintHintItem() {
        return createConstraintHintItem(messages.maximumValueLength(2000));
    }

    private StaticTextItem createConstraintHintItem(String hint) {
        StaticTextItem hintItem = new StaticTextItem();
        hintItem.setValue(hint);
        hintItem.setShowTitle(false);
        hintItem.setTextBoxStyle("constraintHint");
        hintItem.setEndRow(true);

        addFormItem(hintItem);

        return hintItem;
    }

    private TextAreaItem createRuTextAreaItem() {
        return createTextAreaItem(true);
    }

    private TextAreaItem createEnTextAreaItem() {
        return createTextAreaItem(false);
    }

    private TextAreaItem createRuTextAreaItem(int length) {
        return createTextAreaItem(length, true);
    }

    private TextAreaItem createEnTextAreaItem(int length) {
        return createTextAreaItem(length, false);
    }

    private TextAreaItem createTextAreaItem(boolean ruItem) {
        return createTextAreaItem(2000, ruItem);
    }

    private TextAreaItem createTextAreaItem(int length, boolean ruItem) {
        TextAreaItem textAreaItem = new TextAreaItem();
        textAreaItem.setLength(length);
        textAreaItem.setShowTitle(false);
        textAreaItem.setRequired(true);
        textAreaItem.setWidth("*");
        textAreaItem.setIcons(createLocaleIcon(ruItem));

        addFormItem(textAreaItem);

        return textAreaItem;
    }

    private TextItem createRuTextItem() {
        return createTextItem(true);
    }

    private TextItem createEnTextItem() {
        return createTextItem(false);
    }

    private TextItem createTextItem(boolean ruItem) {
        return createTextItem(2000, ruItem);
    }

    private TextItem createTextItem(int length, boolean ruItem) {
        TextItem textItem = new TextItem();
        textItem.setLength(length);
        textItem.setWidth("*");
        textItem.setShowTitle(false);
        textItem.setIcons(createLocaleIcon(ruItem));

        addFormItem(textItem);

        return textItem;
    }

    private FormItemIcon createLocaleIcon(boolean ruItem) {
        FormItemIcon itemIcon = new FormItemIcon();
        itemIcon.setSrc(ruItem ? "ru.png" : "en.png");
        itemIcon.setShowOver(false);
        return itemIcon;
    }

    public static class CustomUploadItem extends CanvasItem {
        private String valueFieldName;

        public CustomUploadItem(String name) {
            this();
            if (name != null) {
                setName(name);
            }
        }

        public CustomUploadItem() {
            setWidth(250);
            setColSpan("*");
            setEndRow(true);
            setStartRow(true);
            setShowTitle(false);

            setShouldSaveValue(true);

            setInitHandler(new FormItemInitHandler() {
                @Override
                public void onInit(FormItem item) {
                    SingleUploader uploadItem = new SingleUploader(IFileInput.FileInputType.ANCHOR, new BaseUploadStatus() {
                        @Override
                        public void setError(String msg) {
                            setStatus(Status.ERROR);
                        }
                    });
                    uploadItem.setHeight("1");
                    uploadItem.addOnFinishUploadHandler(new IUploader.OnFinishUploaderHandler() {
                        @Override
                        public void onFinish(IUploader uploader) {
                            if (uploader.getStatus() == IUploadStatus.Status.SUCCESS) {
                                String fileName = uploader.getBasename();
                                if (valueFieldName != null && fileName != null && !fileName.isEmpty()) {
                                    getForm().setValue(valueFieldName, uploader.getBasename());
                                }
                            }
                        }
                    });
                    uploadItem.setAutoSubmit(true);

                    VLayout uploadContainer = new VLayout();
                    uploadContainer.setHeight(1);
                    uploadContainer.setOverflow(Overflow.VISIBLE);
                    uploadContainer.addMember(uploadItem);

                    setCanvas(uploadContainer);
                }
            });
        }

        public void setValueField(String valueFieldName) {
            this.valueFieldName = valueFieldName;
        }
    }

    public static abstract class BooleanIfFunction implements FormItemIfFunction {
        @Override
        public final boolean execute(FormItem item, Object value, DynamicForm form) {
            Boolean b = booleanResult();
            return b != null && b;
        }

        public abstract Boolean booleanResult();
    }
}
