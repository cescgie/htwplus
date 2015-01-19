package controllers;

import com.fasterxml.jackson.databind.deser.Deserializers;
import models.Folder;
import models.Group;
import models.Media;
import play.Logger;
import play.Play;
import play.data.Form;
import play.db.jpa.JPA;
import play.db.jpa.Transactional;
import play.mvc.Result;
import play.mvc.Security;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static play.data.Form.form;

/**
 * Created by meichris on 06.12.14.
 */
@Security.Authenticated(Secured.class)
public class FolderController extends BaseController {

    final static Form<Folder> folderForm = form(Folder.class);
    final static String tempPrefix = "htwplus_temp";
    static final int PAGE = 1;

    public static Result createFolder(Long groupID, Long parentID) {
        Folder parent = Folder.findById(parentID);
        Form<Folder> filledForm = folderForm.bindFromRequest();
        String name = filledForm.data().get("name");
        if(Secured.viewFolder(parent) && groupID == parent.group.id) {
            Folder folder = new Folder();
            if (allowToCreate(name,parentID)) {
                folder.name = name;
                if (parentID != null) {
                    folder.group = parent.group;
                    folder.parent = parent;
                    folder.depth = parent.depth + 1;
                } else {
                    folder.group = null;
                    folder.parent = null;
                    folder.depth = 0;
                }
                folder.create();
            } else {
                flash("error", "Dazu hast du keine Berechtigung, ein Ordner mit diesem Namen existiert bereits!");
                return redirectFolder(groupID, parentID);
            }
            return redirectFolder(parent.group.id,parentID);
        } else {
            flash("error", "Dazu hast du keine Berechtigung!");
            return redirect(controllers.routes.Application.index());
        }
    }

    private static boolean allowToCreate(String argName, Long argParentId) {
        boolean allow = true;
        List<Folder> folders = Folder.findById(argParentId).childs;
        if (folders != null)
            for (Folder f: folders)
                if (f.name.equals(argName))
                    allow =false;
        return allow;
    }

    ///// TO CHANGE
    @Transactional
    public static Result listFolder(Long groupID,Long folderID) {
        Form<Media> mediaForm = Form.form(Media.class);
        Folder folder = Folder.findById(folderID);
        Folder groupFolder = getGroupFolder(groupID);
        if(Secured.viewFolder(folder) && groupID == folder.group.id) {
            Folder folderTemp = folder;
            List<Folder> path = new ArrayList<Folder>();
            List<Folder> pathTemp = new ArrayList<Folder>();
            while (folderTemp.depth > 0) {
                pathTemp.add(folderTemp);
                folderTemp = folderTemp.parent;
            }
            for (int i = pathTemp.size()-1; i >= 0; i--)
                path.add(pathTemp.get(i));

            Logger.debug("show Folder with ID:" + folderID);
            Navigation.set(Navigation.Level.GROUPS, "Media", groupFolder.group.title, controllers.routes.GroupController.view(groupFolder.group.id, PAGE));
            return ok(views.html.Folder.viewFolder.render(path, folder, folderForm, mediaForm));
        } else {
            flash("error", "Dazu hast du keine Berechtigung!");
            return redirect(controllers.routes.Application.index());
        }
    }

    @Transactional
    public static Result deleteFolder(Long groupID,Long folderID) {
        Logger.debug("use deleteFolder");
        Folder folder = Folder.findById(folderID);
        Folder parent = folder.parent;
        Folder groupFolder = getGroupFolder(groupID);
        if(Secured.deleteFolder(folder) && groupID == folder.group.id) {
            Logger.debug("Delete Folder[" + folder.id + "]");
            for (Media file : folder.files) {
                file.delete();
                Logger.debug("File [" + file.id + "] deleted");
            }
            folder.delete();
            Logger.debug("Folder[" + folder.id + "] deleted");
            Logger.debug("-> list Folder[" + parent.id + "]");
            return redirect(controllers.routes.FolderController.listFolder(groupID, parent.id));
        } else {
            flash("error", "Dazu hast du keine Berechtigung!");
            return redirect(controllers.routes.FolderController.listFolder(groupID, parent.id));
        }
    }


    public static Folder getGroupFolder(Long groupID) {
        return (Folder) JPA.em().createNamedQuery(Folder.QUERY_FIND_ROOT_OF_GROUP).setParameter(Folder.PARAM_GROUP_ID, groupID).getSingleResult();
    }


    public static Result redirectFolder(Long groupID, Long folderID) {
        return redirect("/group/" + groupID + "/folder/" + folderID);
    }
	
	public static Result createMedia(Long id) {
		return ok("OKAY");
	}

    private static String folderContentToString(List<Folder> path, Folder folder) {
        String ret = "";
        for (Folder f:path)
            ret = ret + f.name + "/";
        ret += "\n\n";
        List<Folder> childs = folder.childs;
        List<Media> files = folder.files;
        int file = 1;
        for (Folder f: childs)
            ret = ret + f.toAlternateString() + "\n";
        ret += "\n\n";
            ret = ret + "Files: " + files.size() + "\n";
        return ret;
    }

    @Transactional(readOnly=true)
    public static Result multiView(Long gID, Long fID) throws IOException {
        Logger.debug("use multiView");
        Group group = Group.findById(gID);
        Logger.debug("Group[" + group.id + "]");
        if(!Secured.viewGroup(group)){
            Logger.debug("Secured.viewGroup: false");
            return redirect(controllers.routes.Application.index());
        }

        String filename = group.title + "-" + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + ".zip";
        String filePath = "";
        String[] selection = request().body().asFormUrlEncoded().get("selection");
        Boolean selectionHasFiles = true;
        String selectetFolder = "";
        String tmpPath = Play.application().configuration().getString("media.tempPath");
        File file = File.createTempFile(tempPrefix, ".tmp", new File(tmpPath));

        ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(file));
        zipOut.setLevel(Deflater.NO_COMPRESSION);
        byte[] buffer = new byte[4092];
        int byteCount = 0;

        if(selection != null) {
            for (String s : selection) {
                Media media = Media.findById(Long.parseLong(s));
                Folder folder = Folder.findById(Long.parseLong(s));
                if (media != null) {
                    selectionHasFiles = false;
                    addFile2Zipp(media,filePath,zipOut);
                } else {
                    selectetFolder = folder.name;
                    addFolder2Zipp(filePath, folder, zipOut);
                }
            }
        } else {
            flash("error", "Bitte wähle mindestens eine Datei aus.");
            return redirect(controllers.routes.FolderController.listFolder(gID,fID));
        }
        zipOut.flush();
        zipOut.close();
        if(selectionHasFiles && selection.length == 1) {
            filename = selectetFolder + ".zip";
        }
        Logger.debug(filename + " created");
        response().setHeader("Content-disposition", "attachment; filename=\"" + filename + "\"");
        Logger.debug("return ok(file)");
        return ok(file);
    }

    private static void addFile2Zipp(Media media, String filePath, ZipOutputStream zipOut) throws IOException{
        Logger.debug("add Filename: " + media.file.getName());
        byte[] buffer = new byte[4092];
        int byteCount = 0;

        zipOut.putNextEntry(new ZipEntry(filePath + media.fileName));
        FileInputStream fis = new FileInputStream(media.file);
        while ((byteCount = fis.read(buffer)) != -1) {
            zipOut.write(buffer, 0, byteCount);
        }
        fis.close();
        zipOut.closeEntry();
    }

    private static void addFolder2Zipp(String filePath, Folder folder, ZipOutputStream zipOut) throws IOException{
        Logger.debug("add Folder: " + folder.name);
        filePath = filePath + folder.name + "/";
        zipOut.putNextEntry(new ZipEntry(filePath));
        zipOut.closeEntry();

        if(!folder.files.isEmpty()) {
            for (Media m : folder.files) {
                Media media = Media.findById(m.id);
                addFile2Zipp(media, filePath, zipOut);
            }
        }

        if(!folder.childs.isEmpty()) {
            for (Folder f : folder.childs) {
                addFolder2Zipp(filePath, f, zipOut);
            }
        }
    }
}
