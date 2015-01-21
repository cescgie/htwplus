/**
 * Created by Sophia on 21.01.2015.
 */

//window.Dropzone;
Dropzone.autoDiscover = true;
Dropzone.options.fileuploadForm = {
    init: function () {
        fileuploadForm = this;

        $("#send-button").click(function () {
            alert ("here");
            fileuploadForm.processQueue();
            setTimeout(mediaUpload(), 3000);
        });
    },
    previewTemplate: '<div class="dz-preview dz-file-preview">' +
    '<div class="dz-filename"><span data-dz-name></span></div>' +
    '<div class="dz-size" data-dz-size></div>' +
    '<div class="dz-progress"><span class="dz-upload" data-dz-uploadprogress></span></div></div>',
    paramName: "file",
    maxFilesize: 256,
    parallelUploads: 10,
    autoProcessQueue: false,
    addRemoveLinks: true
};