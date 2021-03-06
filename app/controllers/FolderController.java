package controllers;

import com.fasterxml.jackson.databind.deser.Deserializers;
import models.Folder;
import models.Group;
import models.Media;
import org.omg.CORBA.Current;
import play.Logger;
import play.Play;
import play.data.Form;
import play.db.jpa.JPA;
import play.db.jpa.Transactional;
import play.mvc.Call;
import play.mvc.Result;
import play.mvc.Security;

import javax.persistence.NoResultException;
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

/***
 * Created by meichris on 06.12.14.
 */
@Security.Authenticated(Secured.class)
public class FolderController extends BaseController {

    final static Form<Folder> folderForm = form(Folder.class);
    final static String tempPrefix = "htwplus_temp";
    public static final int DEPTH = 6;
    private static final int MAX_NAME_LENGTH = 30;
    final static int PAGE = 1;

/**
    Creates a folder and returns a Result

    @param groupID The Group ID
    @param parentId The folders parent Folder ID

    @return redirectFolder Result, consisting of the groupID and the parentID<br>
            redirect to the index page, if access is not allowed <br>
            redirect to the current folders parent folder if no folder name was delivered <br>
            redirect to the current folders parent folder if the deserved folder doesnt exist <br>

    **/
    @Transactional
    public static Result createFolder(Long groupID, Long parentID) {
        Folder parent = Folder.findById(parentID);
        Group group = Group.findById(groupID);
        Folder groupFolder = getGroupFolder(group);
        Form<Folder> filledForm = folderForm.bindFromRequest();
        String name = filledForm.data().get("name");
        if (parent != null) {
            if (!name.isEmpty()) {
                if (Secured.isMemberOfGroup(group, Component.currentAccount()) && groupID.equals(parent.group.id)) {
                    if (allowToCreate(name, parent)) {
                        Folder folder = new Folder();
                        folder.name = name;
                        folder.group = parent.group;
                        folder.parent = parent;
                        folder.depth = parent.depth + 1;
                        folder.owner = Component.currentAccount();
                        folder.create();
                    } else {
                        if (name.length() > MAX_NAME_LENGTH) {
                            flash("error", "Der eingegebene Ordnername ist zu lang (max. 30 Zeichen)");
                        } else {
                            flash("error", "Ein Ordner mit diesem Namen existiert bereits!");
                        }
                        return redirectFolder(groupID, parentID);
                    }
                    flash("success", "Der Ordner \"" + name + "\" wurde erfolgreich angelegt");
                    return redirectFolder(parent.group.id, parentID);
                } else {
                    flash("error", "Dazu hast Du keine Berechtigung!");
                    return redirect(controllers.routes.Application.index());
                }
            } else {
                flash("error", "Du musst den Ordner benennen!");
                return redirectFolder(parent.group.id, parentID);
            }
        } else {
            flash("error", "Der Ordner, auf den Du zugreifen wolltest, existiert nicht mehr!");
            return redirect(controllers.routes.FolderController.listFolder(group.id, groupFolder.id));
        }
    }
    /**
     Renames a Folder
     @param argName the delivered Name
     @param folder the delivered Folder
     @return listFolder Result, consisting of the parent folders group id an the parent folders id<br>
              redirect to the index page, if access is not allowed <br>
              redirect to the current folders parent folder if the deserved folder doesnt exist 
     **/
    @Transactional
    public static Result renameFolder(Long groupID, Long folderID) {
        Group group = Group.findById(groupID);
        Folder folder = Folder.findById(folderID);
        Folder groupFolder = getGroupFolder(group);
        if (folder != null) {
            if (Secured.renameFolder(folder)) {
                String oldName = folder.name;
                Form<Folder> filledForm = folderForm.bindFromRequest();
                String newName = filledForm.data().get("name");
                if (allowToCreate(newName, folder.parent)) {
                    folder.name = newName;
                    flash("success", "Der Ordner \"" + oldName + "\" wurde erfolgreich in \"" + newName + "\" umbenannt");
                    return listFolder(group.id, folder.parent.id);
                } else {
                    if (newName.length() > MAX_NAME_LENGTH) {
                        flash("error", "Der eingegebene Ordnername ist zu lang (max. 30 Zeichen)");
                    } else {
                        flash("error", "Ein Ordner mit diesem Namen existiert bereits!");
                    }
                    return listFolder(folder.parent.group.id, folder.parent.id);
                }
            } else {
                flash("error", "Dazu hast Du keine Berechtigung!");
                return redirect(controllers.routes.Application.index());
            }
        } else {
            flash("error", "Der Ordner, auf den Du zugreifen wolltest, existiert nicht mehr!");
            return redirect(controllers.routes.FolderController.listFolder(group.id, groupFolder.id));
        }
    }
     /**
    Lists existing Folders
    
     @param groupID the groupID
     @param folderID the folderID
    
     @return ok Result, invoking a rendered view if access allowed <br>
             Redirects to the Index Page if access isn't allowed
    
    
    **/
    @Transactional
    public static Result listFolder(Long groupID, Long folderID) {
        Form<Media> mediaForm = Form.form(Media.class);
        Folder folder = Folder.findById(folderID);
        Group group = Group.findById(groupID);
        Folder groupFolder = getGroupFolder(group);

        if (Secured.viewFolder(groupFolder) && groupID.equals(groupFolder.group.id)) {

            if (Secured.viewFolder(folder) && groupID.equals(folder.group.id)) {
                List<Folder> path = getPathOfThisFolder(folder);
                Logger.debug("show Folder with ID:" + folderID);
                Navigation.set(Navigation.Level.GROUPS, "Media", groupFolder.group.title, controllers.routes.GroupController.view(groupFolder.group.id, PAGE));
                return ok(views.html.Folder.viewFolder.render(path, folder, folderForm, mediaForm));
            } else {
                Logger.debug("show Group-Folder with ID:" + folderID);
                flash("error", "Der Ordner, auf den Du zugreifen wolltest, existiert nicht mehr!");
                return redirect(controllers.routes.FolderController.listFolder(group.id, groupFolder.id));
            }

        } else {
            Logger.debug("show Index()");
            flash("error", "Für den Zugang zu diesem Ordner hast Du keine Berechtigung!");
            return redirect(controllers.routes.Application.index());
        }
    }

    /**
    Deletes a certain folder
    @param groupID the groupID
    @param folderID the folderID
    @return redirect Result, redirecting to the current folder <br>
            Redirects to the current folders parent folder because it is impossible to access a deletet folder
    **/
    @Transactional
    public static Result deleteFolder(Long groupID,Long folderID) {
        Logger.debug("use deleteFolder");
        Folder folder = Folder.findById(folderID);
        Group group = Group.findById(groupID);
        Folder groupFolder = getGroupFolder(group);
        if(folder != null) {
            Folder parent = folder.parent;
            if (Secured.deleteFolder(folder) && groupID.equals(folder.group.id)) {
                Logger.debug("Delete Folder[" + folder.id + "]...");
                deleteFolderContent(folder);
                Logger.debug("Folder[" + folder.id + "] -> deleted");
            } else {
                flash("error", "Dazu hast Du keine Berechtigung!");
            }
            flash("success", "Der Ordner \"" + folder.name + "\" wurde erfolgreich gelöscht");
            return redirect(controllers.routes.FolderController.listFolder(groupID, parent.id));
        } else {
            flash("error", "Der Ordner, auf den Du zugreifen wolltest, existiert nicht mehr!");
            return redirect(controllers.routes.FolderController.listFolder(group.id, groupFolder.id));
        }
    }
    /**
    Creates a GroupFolder (as a first child in tree hirarchy of the root folder)
    @param group the delivered group
    @return the groups "root" folder
    **/
    @Transactional
    public static Folder createGroupFolder(Group group) {
        Folder rootFolder = getRootFolder();
        Folder groupFolder = null;
        if(Secured.viewGroup(group)) {
            Logger.debug("Create Group Folder...");
            groupFolder = new Folder();
            groupFolder.name = group.title;
            groupFolder.depth = rootFolder.depth + 1;
            groupFolder.parent = rootFolder;
            groupFolder.group = group;
            groupFolder.owner =  group.owner;
            groupFolder.create();
            Logger.debug("Group Folder -> created");
        }
        return groupFolder;
    }
    /**
    Creates and returns a groop folder folder if none exists
    @param group the group for which a folder shall be created
    @return a group folder
    **/
    @Transactional
    public static Folder getGroupFolder(Group group) {
        Folder groupFolder = null;
        if(Secured.viewGroup(group)) {
            try {
                groupFolder = (Folder) JPA.em().createNamedQuery(Folder.QUERY_FIND_ROOT_OF_GROUP).setParameter(Folder.PARAM_GROUP_ID, group.id).getSingleResult();
            } catch (NoResultException e) {
                // catch to create Groupfolder if not exists ! For old Projekt !!
                groupFolder = createGroupFolder(group);
            }
        }
        return groupFolder;
    }
    /**
    Provides the possibility to download a folder with its contents
    @param groupID the groupID
    @param folderID the folderID
    @return the zipped file, <br>
            redirect to the current folders parent folder if the deserved folder doesnt exist <br>
            redirect to the index page if access is not allowed
    */
    @Transactional(readOnly=true)
    public static Result singleDownload(Long groupID, Long folderID) throws IOException {
        Logger.debug("use multiView");
        Folder folder = Folder.findById(folderID);
        Group group = Group.findById(groupID);
        Folder groupFolder = getGroupFolder(group);
        if (Secured.viewGroup(group)) {
            Logger.debug("Group[" + group.id + "]");
            if (folder != null) {
                String tmpPath = Play.application().configuration().getString("media.tempPath");
                File file = File.createTempFile(tempPrefix, ".tmp", new File(tmpPath));
                ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(file));
                zipOut.setLevel(Deflater.NO_COMPRESSION);
                addFolder2Zip("", folder, zipOut);
                zipOut.flush();
                zipOut.close();
                String filename = folder.name + ".zip";
                Logger.debug(filename + " created");
                response().setHeader("Content-disposition", "attachment; filename=\"" + filename + "\"");
                Logger.debug("return ok(file)");
                return ok(file);
            } else {
                flash("error", "Der Ordner, auf den Du zugreifen wolltest, existiert nicht mehr!");
                return redirect(controllers.routes.FolderController.listFolder(group.id, groupFolder.id));
            }
        } else {
            Logger.debug("Secured.viewGroup: false");
            return redirect(controllers.routes.Application.index());
        }
    }
    /**

    
    Provides the possibility to download selected files and/or folders (with its contents)
    @param groupID the groupID
    @param folderID the folderID
    @return the zipped file, <br>
            redirect to the current folders parent folder <br>
            if one of the selected folders is empty or not a single file has been selected<br>
            redirect to the index page if access is not allowed
    
    */
    @Transactional(readOnly=true)
    public static Result multiView(Long groupID, Long folderID) throws IOException {

        Logger.debug("use multiView");
        Folder currentfolder = Folder.findById(folderID);
        Group group = Group.findById(groupID);
        Folder groupFolder = getGroupFolder(group);
        String fehlercode = "";
        if (Secured.viewGroup(group)) {
            Logger.debug("Group[" + group.id + "]");
            String[] selection = request().body().asFormUrlEncoded().get("selection");
            if (selection != null) {
                String filename = group.title + "-" + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + ".zip";
                String filePath = "";
                Boolean selectionHasNoFiles = true;
                String selectetFolder = "";
                String tmpPath = Play.application().configuration().getString("media.tempPath");
                File file = File.createTempFile(tempPrefix, ".tmp", new File(tmpPath));

                ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(file));
                zipOut.setLevel(Deflater.NO_COMPRESSION);

                for (String s : selection) {
                    Logger.debug("Selection[" + s + "]");
                    Media media = Media.findById(Long.parseLong(s));
                    Folder folder = Folder.findById(Long.parseLong(s));
                    if (media != null) {
                        Logger.debug("Selection[" + s + "] media");
                        selectionHasNoFiles = false;
                        addFile2Zip(media, filePath, zipOut);
                    } else if(folder != null){
                        Logger.debug("Selection[" + s + "] folder");
                        selectetFolder = folder.name;
                        addFolder2Zip(filePath, folder, zipOut);
                    } else if(media == null && folder == null) {
                        Logger.debug("Selection[" + s + "] failed");
                        fehlercode = "Achtung, eine/mehrere der gewählten Dateien bzw. Ordner wurden in der Zwischenzeit gelöscht !! Triff bitte eine neue Auswahl.";
                        }
                    }
                zipOut.flush();
                zipOut.close();
                if (selectionHasNoFiles && selection.length == 1) {
                    filename = selectetFolder + ".zip";
                }
                Logger.debug(filename + " created");
                response().setHeader("Content-disposition", "attachment; filename=\"" + filename + "\"");
                Logger.debug("return ok(file)");

                if (!fehlercode.isEmpty()) {
                    flash("info", fehlercode);
                    return redirect(controllers.routes.FolderController.listFolder(group.id, currentfolder.id));
                }
                return ok(file);
            } else {
                flash("error", "Bitte wähle mindestens eine Datei aus.");
                return redirect(controllers.routes.FolderController.listFolder(group.id, currentfolder.id));
            }
        } else {
                Logger.debug("Secured.viewGroup: false");
            return redirect(controllers.routes.Application.index());
        }
    }
    /**
    Redirects to the current folders parent folder
    @param groupID the groupID
    @param folderID the folderID
    @return redirect to the current folders parent folder <br>

    */
    public static Result redirectFolder(Long groupID, Long folderID) {
        return redirect("/group/" + groupID + "/folder/" + folderID);
    }
    /**
    Checks whether there is content in a folder 
    @param m the Media
    @param f the Folder
    @return true, if content exists, false if not
    */
    public static boolean mediaExistInFolder(Media m, Folder f) {
        boolean exist = false;
        List<Media> mediaList = f.files;
        for (Media media: mediaList) {
            if (media.title.equals(m.title))
                exist = true;
        }
        return exist;
    }
    /**
    Adds a file to the delivered ZipOutputstream
    @param media the delivered media
    @param filePath the delivered file path
    @param zipOut the delivered Zip Output Stream
    */
    private static void addFile2Zip(Media media, String filePath, ZipOutputStream zipOut) throws IOException{
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
    /**
    Adds a folder to the delivered ZipOutputstream
    
    @param filePath the delivered file path
    @param folder the delivered folder
    @param zipOut the delivered Zip Output Stream
    */
    private static void addFolder2Zip(String filePath, Folder folder, ZipOutputStream zipOut) throws IOException{
        Logger.debug("add Folder: " + folder.name);
        filePath = filePath + folder.name + "/";
        zipOut.putNextEntry(new ZipEntry(filePath));
        zipOut.closeEntry();

        if(!folder.files.isEmpty()) {
            for (Media m : folder.files) {
                Media media = Media.findById(m.id);
                addFile2Zip(media, filePath, zipOut);
            }
        }

        if(!folder.childs.isEmpty()) {
            for (Folder f : folder.childs) {
                addFolder2Zip(filePath, f, zipOut);
            }
        }
    }
    /**
    Returns the path of the delivered folder
    @param folder the delivered folder
    @return the path of the delivered folder
    */
    private static List<Folder> getPathOfThisFolder(Folder folder) {
        List<Folder> pathTemp = new ArrayList<Folder>();
        List<Folder> path = new ArrayList<Folder>();
        while (folder.depth > 0) {
            pathTemp.add(folder);
            folder = folder.parent;
        }
        for (int i = pathTemp.size()-1; i >= 0; i--) {
            path.add(pathTemp.get(i));
        }
        return path;
    }
    /**
    Checks whether it is allowed to create a folder
    @param argName the delivered name (which the new folder should get)
    @param parent the folders parent folder
    @return true if its allowed to create a folder, false if not
    */
    private static boolean allowToCreate(String argName, Folder parent) {
        boolean allow = false;
        if (argName.length() <= MAX_NAME_LENGTH && parent.depth < DEPTH) {
            if (parent.childs == null) {
                allow = true;
            } else {
                allow = true;
                for (Folder f: parent.childs)
                    if (f.name.equals(argName))
                        allow =false;
            }
        }
        return allow;
    }

    /**
    Deletes the delivered folders content
    @param folder the delivered folder
    */
    private static void deleteFolderContent(Folder folder){
        for (Media m: folder.files) {
            m.delete();
        }
        for (Folder f: folder.childs) {
            deleteFolderContent(f);
        }
        folder.delete();
    }
    /**
    Creates a root folder
    @return the root folder
    */
    private static Folder getRootFolder() {
        Folder rootFolder = null;
        try {
            rootFolder = (Folder) JPA.em().createNamedQuery(Folder.QUERY_FIND_ROOT).getSingleResult();
        } catch (NoResultException e) {
            // catch to create Rootfolder if not exist ! For old Projekt !!
            Logger.debug("Create Root Folder...");
            rootFolder = new Folder();
            rootFolder.name = "root";
            rootFolder.depth = 0;
            rootFolder.parent = null;
            rootFolder.group = null;
            rootFolder.owner = null;
            rootFolder.create();
            Logger.debug("Root Folder -> created");
        }
        return rootFolder;
    }
}
