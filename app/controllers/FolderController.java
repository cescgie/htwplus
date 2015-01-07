package controllers;

import com.fasterxml.jackson.databind.deser.Deserializers;
import models.Folder;
import models.Group;
import models.Media;
import play.db.jpa.JPA;
import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.main;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by meichris on 06.12.14.
 */
public class FolderController extends Controller {


    public static Long createFolder(String argName, Long argParentId) {
        Folder f = new Folder();
        if (allowToCreate(argName,argParentId)) {
            f.name = argName;
            f.group = Folder.findById(argParentId).group;
            if (argParentId != null) {
                f.parent = Folder.findById(argParentId);
                f.depth = f.parent.depth + 1;
            }
            f.create();
        }
        return f.id;
    }

    public static List<Media> getAllMediaInFolder(Long argId){
        List<Media> medias = new ArrayList<Media>();
        try {
            Folder f = Folder.findById(argId);
            medias = f.files;
        } catch (NullPointerException e) {
            //Blabla
        }
        return medias;
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

    public static Long getGroupFolder(String argGroupName) {
        List<Folder> f = JPA.em().createNamedQuery(Folder.QUERY_FIND_ALL_GROUPFOLDER).getResultList();
        return f.get(0).id;
    }
    
    ///// TO CHANGE
    

	@Transactional
	public static Result listFolder(Long folderID) {
		Folder f = Folder.findById(folderID);
		Folder folder = f;
		List<Folder> path = new ArrayList<Folder>();
		List<Folder> pathTemp = new ArrayList<Folder>();
		if (f == null)
			return redirectFolderError("Ordner nicht vorhanden, wählen Sie einen anderen!");
		while (f.depth > 1) {
			pathTemp.add(f);
			f = f.parent;
		}
		for (int i = pathTemp.size()-1; i >= 0; i--)
			path.add(pathTemp.get(i));
		return ok(folder.toAlternateString());
	//	return ok(folder.render(path,folder));
	}
    

    public static Result redirectFolder(Long folderID) {
		return redirect("/folder/" + folderID);
	}

	public static Result redirectFolderError(String error) {
		return ok("NOT OKAY");
		//return ok(test.render(error));
	}
	
	public static Result createMedia(Long id) {
		return ok("OKAY");
	}
	

	@Transactional
	public static Result createFolder(Long folderID) {

//		String uri = request().uri();
//		int index = uri.lastIndexOf("/");
//		Long folder = Long.valueOf(uri.substring(index));

        FolderController.createFolder("Ordner xyz", folderID);
        return redirectFolder(folderID);
    }


}
