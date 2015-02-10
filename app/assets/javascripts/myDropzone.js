Dropzone.autoDiscover = false;

var flashmessage;

Dropzone.options.fileuploadForm = {
    init: function () {
        if(localStorage.flash) {
            //alert(localStorage.flash);
             flashmessage = localStorage.flash;

            if(flashmessage.indexOf("erfolgreich hinzugefügt") > -1) {
                $("#usermessage").prepend("<div class='alert alert-success fade in'>"+
                "<button type='button' class='close fade out' data-dismiss='alert'>&times;</button>"+
                flashmessage + "</div>");
            }

			else {
                $("#usermessage").prepend("<div class='alert alert-danger fade in'>"+
                "<button type='button' class='close fade out' data-dismiss='alert'>&times;</button>"+
                flashmessage + "</div>");
            }

            localStorage.setItem('flash','');
            flashmessage = "";
        }


        fileuploadForm = this;
        fileuploadForm.on("queuecomplete", function(file) {
            localStorage.setItem('flash',fileuploadForm.getLastMessage());
            window.location.assign(window.location.pathname);
        });

        $("#send-button").click(function (e) {
            e.preventDefault();
            e.stopPropagation();
            $(".delete-button").hide();
            fileuploadForm.processQueue();
        });
    },
    previewTemplate: '<div class="dz-preview dz-file-preview">' +
    '<div class="dz-filename"><span data-dz-name></span></div>' +
    '<div class="dz-size" data-dz-size></div>' +
    '<div class="dz-progress"><div class="dz-upload" data-dz-uploadprogress></div></div>' +
    '<div class="delete-button"> <button data-dz-remove>Entfernen</button></div>' +
    '</div>',
    paramName: "form[files][]",
    maxFilesize: 5,
    uploadMultiple: true,
    parallelUploads: 20,
    autoProcessQueue: false,
    dictFileTooBig: "Die Datei ist zu groß ({{filesize}}MiB). Die maximale Dateigröße beträgt: {{maxFilesize}}MiB.",
    dictDefaultMessage: "Hier klicken oder Hineinziehen um Dateien hochzuladen"

};
var myDropzone = new Dropzone(".dropzone");