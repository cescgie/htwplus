package controllers;

import com.fasterxml.jackson.databind.deser.Deserializers;
import models.Folder;
import models.Group;
import models.Media;
import play.Logger;
import play.db.jpa.JPA;
import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import views.html.main;
import views.html.Folder.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by meichris on 06.12.14.
 */
@Security.Authenticated(Secured.class)
public class FolderController extends BaseController {

    public static Result createFolder(Long groupID, Long parentID, String name) {
        Folder parent = Folder.findById(parentID);
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
        Folder folder = Folder.findById(folderID);
        Folder groupFolder = (Folder) JPA.em().createNamedQuery(Folder.QUERY_FIND_ROOT_OF_GROUP).setParameter(Folder.PARAM_GROUP_ID, groupID).getSingleResult();
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

//            return ok(folderContentToString(path,folder));
            return ok(viewFolder.render(path,folder));
        } else {
            flash("error", "Dazu hast du keine Berechtigung!");
            return redirect(controllers.routes.Application.index());
        }
    }

    public static Result deleteFolder(Long groupID,Long folderID) {
        Folder folder = Folder.findById(folderID);
        Folder groupFolder = (Folder) JPA.em().createNamedQuery(Folder.QUERY_FIND_ROOT_OF_GROUP).setParameter(Folder.PARAM_GROUP_ID, groupID).getSingleResult();
        if(Secured.viewFolder(folder) && groupID == folder.group.id) {

            return listFolder(groupID, folderID);
        } else {
            flash("error", "Dazu hast du keine Berechtigung!");
            return redirect(controllers.routes.Application.index());
        }
    }


    public static Result redirectFolder(Long groupID, Long folderID) {
        return redirect("/group/" + groupID + "/folder/" + folderID);
    }

    public static Result redirectGroupFolder(Long groupID, Long folderID) {
        return redirect("/group/" + groupID + "/folder/" + folderID);
    }

	public static Result redirectFolderError(String error) {
		return ok("NOT OKAY");
		//return ok(test.render(error));
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
}
