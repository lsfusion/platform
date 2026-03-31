package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GProgressBar;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.view.*;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.classes.GFilesDTO;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.controller.KeepCellEditor;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.GUserInputResult;
import org.moxieapps.gwt.uploader.client.File;
import org.moxieapps.gwt.uploader.client.Uploader;
import org.moxieapps.gwt.uploader.client.events.UploadErrorEvent;

import java.util.ArrayList;
import java.util.List;

public class FileCellEditor extends ARequestValueCellEditor implements KeepCellEditor {

    private static final ClientMessages messages = ClientMessages.Instance.get();
    private final boolean multipleInput;
    private final boolean storeName;
    private final List<String> validExtensions; // null if FILE (with any extension/contenttype)
    private final boolean named;

    private Uploader newVersionUploader;
    private final List<FileInfo> fileInfos = new ArrayList<>();

    public FileCellEditor(EditManager editManager, boolean multipleInput, boolean storeName, List<String> validExtensions, boolean named) {
        super(editManager);
        this.multipleInput = multipleInput;
        this.storeName = storeName;
        this.validExtensions = validExtensions;
        this.named = named;
    }

    private void addFilesToUploader(JsArray files, Element parent, Widget popupOwnerWidget) {
        resetUploadedFiles();
        JsArray validFiles = JsArray.createArray().cast();
        for (int i = 0; i < files.length(); i++) {
            File file = files.get(i).cast();
            String extension = GwtClientUtils.getFileExtension(file.getName()).toLowerCase();
            if ((validExtensions == null || emptyValidExtension() || validExtensions.contains(extension)) && (multipleInput || validFiles.length() == 0)) {
                validFiles.push(file);
            }
        }

        if(validFiles.length() == 0) {
            cancel();
            return;
        }

        newVersionUploader = createUploader(parent, popupOwnerWidget);
        newVersionUploader.addFilesToQueue(validFiles);
        newVersionUploader.startUpload();
    }

    private boolean emptyValidExtension() {
        return validExtensions.size() == 1 && validExtensions.get(0).isEmpty();
    }

    @Override
    public void stop(Element parent, boolean cancel, boolean blurred) {
        if(cancel && newVersionUploader != null)
            newVersionUploader.cancelUpload();
        newVersionUploader = null;
    }

    @Override
    public PValue getCommitValue(Element parent, Integer contextAction) throws InvalidEditException {
        throw new UnsupportedOperationException();
    }

    @Override
    public GUserInputResult getCommitResult(Element parent, Integer contextAction) throws InvalidEditException {
        if(!isUploaded())
            throw new InvalidEditException();

        PValue[] values = new PValue[fileInfos.size()];
        for (int i = 0, size = fileInfos.size(); i < size; i++) {
            FileInfo fileInfo = fileInfos.get(i);
            values[i] = PValue.getPValue(new GFilesDTO(fileInfo.filePrefix + "_" + fileInfo.fileName, fileInfo.fileName, storeName, validExtensions == null, named));
        }
        return new GUserInputResult(false, values, contextAction);
    }

    @Override
    public void start(EventHandler handler, Element parent, RenderContext renderContext, boolean notFocusable, PValue oldValue) {
        Event event;
        Widget popupOwnerWidget = renderContext.getPopupOwnerWidget();
        if(handler != null && GKeyStroke.isDropEvent(event = handler.event)) {
            drop(event, parent, popupOwnerWidget);
        } else {
            click(parent, createFileInputElement(), popupOwnerWidget);
        }
    }

    private void drop(Event editEvent, Element parent, Widget popupOwnerWidget) {
        JsArray droppedFiles = Uploader.getDroppedFiles(editEvent);
        addFilesToUploader(droppedFiles, parent, popupOwnerWidget);
    }

    private native void click(Element parent, Element inputElement, Widget popupOwnerWidget) /*-{
        var instance = this;

        inputElement.onchange = function () {
            instance.@FileCellEditor::addFilesToUploader(*)(this.files, parent, popupOwnerWidget);
        }

        inputElement.oncancel = function () {
            instance.@FileCellEditor::cancel()();
        }

        inputElement.click();
    }-*/;

    private InputElement createFileInputElement() {
        InputElement inputElement = Document.get().createFileInputElement();
        inputElement.setPropertyBoolean("multiple", multipleInput);
        if(validExtensions != null) {
            String accept = "";
            for(String type : validExtensions)
                accept += (accept.isEmpty() ? "" : ",") + "." + type;
            inputElement.setAccept(accept);
        }
        return inputElement;
    }

    private boolean isCurrentUploader(Uploader uploader) {
        return uploader == newVersionUploader;
    }

    private Uploader createUploader(Element parent, Widget popupOwnerWidget) {
        LoadingBox loadingBox = new LoadingBox(this::cancel);

        Uploader newVersionUploader = new Uploader();
        newVersionUploader.setUploadURL(GwtClientUtils.getUploadURL(null)) // not sure that is needed
                .setUploadErrorHandler(uploadErrorEvent -> {
                    if (!isCurrentUploader(newVersionUploader))
                        return true;
                    if (!uploadErrorEvent.getErrorCode().equals(UploadErrorEvent.ErrorCode.FILE_CANCELLED)) { // this check is necessary because loadingBox.hideLoadingBox() throws FILE_CANCELLED error
                        String errorMessage = uploadErrorEvent.getMessage().endsWith(String.valueOf(-UploadErrorEvent.ErrorCode.FILE_VALIDATION_FAILED.toInt())) ?
                                ClientMessages.Instance.get().fileNameTooLong() : uploadErrorEvent.getMessage();
                        loadingBox.hideLoadingBox(true);
                        throw new RuntimeException(errorMessage);
                    }
                    return false;
                })
                .setFileQueuedHandler(fileQueuedEvent -> {
                    if (!isCurrentUploader(newVersionUploader))
                        return true;
                    final File file = fileQueuedEvent.getFile();
                    fileInfos.add(new FileInfo(file.getId(), GwtSharedUtils.randomString(15), file.getName()));
                    if(fileInfos.size() == 1)
                        loadingBox.showLoadingBox(new PopupOwner(popupOwnerWidget, parent));
                    return true;
                })
                .setUploadProgressHandler(uploadProgressEvent -> {
                    if (!isCurrentUploader(newVersionUploader))
                        return true;
                    loadingBox.setProgress(new GProgressBar(uploadProgressEvent.getFile().getName(), (int) uploadProgressEvent.getBytesComplete(), (int) uploadProgressEvent.getBytesTotal()));
                    return true;
                })
                .setUploadStartHandler(uploadStartEvent -> {
                    if (!isCurrentUploader(newVersionUploader))
                        return true;
                    FileInfo fileInfo = getFileInfo(uploadStartEvent.getFile().getId());
                    newVersionUploader.setUploadURL(GwtClientUtils.getUploadURL(fileInfo.filePrefix));
                    return true;
                })
                .setUploadSuccessHandler(uploadSuccessEvent -> {
                    if (!isCurrentUploader(newVersionUploader))
                        return true;
                    FileInfo fileInfo = getFileInfo(uploadSuccessEvent.getFile().getId());
                    fileInfo.uploaded = true;
                    if (isUploaded()) {
                        loadingBox.hideLoadingBox(false);
                        commit(parent);
                    }
                    return true;
                })
                .setUploadCompleteHandler(uploadCompleteEvent -> {
                    if (!isCurrentUploader(newVersionUploader))
                        return true;
                    if (!isUploaded()) {
                        newVersionUploader.startUpload();
                    }
                    return true;
                });

        return newVersionUploader;
    }

    private void resetUploadedFiles() {
        fileInfos.clear();
    }

    private boolean isUploaded() {
        if(fileInfos.isEmpty())
            return false;
        for (FileInfo fileInfo : fileInfos) {
            if(!fileInfo.uploaded)
                return false;
        }
        return true;
    }

    private FileInfo getFileInfo(String fileId) {
        for (FileInfo fileInfo : fileInfos) {
            if(fileInfo.fileId.equals(fileId))
                return fileInfo;
        }
        throw new IllegalStateException("File not found in upload queue: " + fileId);
    }

    private class FileInfo {
        public final String fileId;
        public final String filePrefix;
        public final String fileName;
        public boolean uploaded;

        private FileInfo(String fileId, String filePrefix, String fileName) {
            this.fileId = fileId;
            this.filePrefix = filePrefix;
            this.fileName = fileName;
        }
    }

    private class LoadingBox extends DialogModalWindow {

        private final SimplePanel progressPane;
        private GProgressBar prevProgress;
        private Timer timer;
        private final Runnable cancelAction;

        public LoadingBox(Runnable cancelAction) {
            super(messages.loading(), false, ModalWindowSize.FIT_CONTENT);
            this.cancelAction = cancelAction;

            progressPane = new SimplePanel();
            GwtClientUtils.addClassName(progressPane, "dialog-loading-progress");
            setBodyWidget(progressPane);

            addFooterWidget(new FormButton(messages.cancel(), FormButton.ButtonStyle.SECONDARY, clickEvent -> hideLoadingBox(true)));
        }

        private boolean isShowing = false;
        public void showLoadingBox(PopupOwner popupOwner) {
            timer = new Timer() {
                @Override
                public void run() {
                    isShowing = true;
                    show(popupOwner);
                }
            };
            timer.schedule(1000);
        }

        public void hideLoadingBox(boolean cancel) {
            if(isShowing) {
                isShowing = false;
                hide();
            }
            timer.cancel();

            if (cancel)
                cancelAction.run();
        }

        public void setProgress(GProgressBar progress) {
            if (prevProgress == null || !prevProgress.equals(progress)) {
                progressPane.setWidget(new ProgressBar(0, progress.total, progress.progress, new ProgressBar.TextFormatter() {
                    @Override
                    protected String getText(ProgressBar bar, double curProgress) {
                        return progress.message;
                    }
                }));
                prevProgress = progress;
            }
        }
    }
}
