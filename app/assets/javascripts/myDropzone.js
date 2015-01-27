/**
 * Created by Sophia on 21.01.2015.
 */

//window.Dropzone;
Dropzone.autoDiscover = true;
Dropzone.options.fileuploadForm = {
    init: function () {
        fileuploadForm = this;

        $("#send-button").click(function () {
            document.getElementById("fortschrittsspalte").style.display = "inline-block";
            var files_queued = fileuploadForm.getQueuedFiles();
            alert (files_queued.length);
            for (var i = 0; i < files_queued.length; i++){
                alert ("File "+i+ ": "+ files_queued[i]);
                /*var node, removeFileEvent, removeLink, _i, _j, _k, _len, _len1, _len2, _ref, _ref1, _ref2, _results;
                if (this.element === this.previewsContainer) {
                    this.element.classList.add("dz-started");
                }
                if (this.previewsContainer) {
                    file.previewElement = Dropzone.createElement(this.options.previewTemplate.trim());
                    file.previewTemplate = file.previewElement;
                    this.previewsContainer.appendChild(file.previewElement);
                    _ref = file.previewElement.querySelectorAll("[data-dz-name]");
                    for (_i = 0, _len = _ref.length; _i < _len; _i++) {
                        node = _ref[_i];
                        node.textContent = file.name;
                    }
                    _ref1 = file.previewElement.querySelectorAll("[data-dz-size]");
                    for (_j = 0, _len1 = _ref1.length; _j < _len1; _j++) {
                        node = _ref1[_j];
                        node.innerHTML = this.filesize(file.size);
                    }
                    if (this.options.addRemoveLinks) {
                        file._removeLink = Dropzone.createElement("<a class=\"dz-remove\" href=\"javascript:undefined;\" data-dz-remove>" + this.options.dictRemoveFile + "</a>");
                        file.previewElement.appendChild(file._removeLink);
                    }
                    removeFileEvent = (function (_this) {
                        return function (e) {
                            e.preventDefault();
                            e.stopPropagation();
                            if (file.status === Dropzone.UPLOADING) {
                                return Dropzone.confirm(_this.options.dictCancelUploadConfirmation, function () {
                                    return _this.removeFile(file);
                                });
                            } else {
                                if (_this.options.dictRemoveFileConfirmation) {
                                    return Dropzone.confirm(_this.options.dictRemoveFileConfirmation, function () {
                                        return _this.removeFile(file);
                                    });
                                } else {
                                    return _this.removeFile(file);
                                }
                            }
                        };
                    })(this);
                    _ref2 = file.previewElement.querySelectorAll("[data-dz-remove]");
                    _results = [];
                    for (_k = 0, _len2 = _ref2.length; _k < _len2; _k++) {
                        removeLink = _ref2[_k];
                        _results.push(removeLink.addEventListener("click", removeFileEvent));
                    }
                }*/
            }



            fileuploadForm.processQueue();

            fileuploadForm.on("complete", function (file) {
                files_queued = fileuploadForm.getQueuedFiles();
                var files_uploading = fileuploadForm.getUploadingFiles();
                if (files_uploading.length === 0 && files_queued.length === 0) {
                    document.getElementById("fortschrittsspalte").style.display = "none";
                }
            });


            /*@for(m <- mediaList) {
            <tr>
                <td><input name="selection" value="@m.id" type="checkbox"></td>
                <td><span class="glyphicon @{MediaController.glyph(m.id)}"></span> <a href="@routes.MediaController.view(m.id)" rel="tooltip" data-delay="500" data-original-title="von @m.owner.name">@m.title</a></td>
                <td><p class="size" data-dz-size></p>
                <div class="progress progress-striped active" role="progressbar" aria-valuemin="0" aria-valuemax="100" aria-valuenow="0">
                <div class="progress-bar progress-bar-success" style="width:0%;" data-dz-uploadprogress></div>
                </div></td>
                <td>@m.createdAt.format("dd.MM.yyyy")</td>
                <td>@{MediaController.bytesToString(m.size, false)}</td>
                <td class="hp-optionsMenu">
                @views.html.Media.snippets.optionMenu(m)
                </td>
                </tr>
            }*/
            document.getElementById("loadingMedia").style.display = "inline-block";
                $("#fileuploadForm").submit();
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