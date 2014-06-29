package lib.media;

import lib.exceptions.MediaException;
import lib.media.interfaces.MediaManager;

import java.io.File;
import java.util.List;
import play.Play;

/**
 * Created by FKI on 29.06.2014.
 */
public class FilesystemMediaManager implements MediaManager {

    private String path;
    private String tempPath;

    public FilesystemMediaManager(){
        this.path = Play.application().configuration().getString("media.path");
        this.tempPath = Play.application().configuration().getString("media.tempPath");
        // ToDO Error when configuration is missing
    }

    @Override
    public void create(String url, File file) throws MediaException {
        File newFile = new File(this.path + "/" + url);
        if(newFile.exists()){
            throw new MediaException("File exists already");
        }
        newFile.getParentFile().mkdirs();
        file.renameTo(newFile);
        if(!newFile.exists()) {
            throw new MediaException("Could not upload file");
        }
    }

    @Override
    public File read(String url) {
        return null;
    }

    @Override
    public File read(List<String> urls) {
        return null;
    }

    @Override
    public void update(String url, File file) {

    }

    @Override
    public void delete(String url) {

    }

    @Override
    public void delete(List<String> urls) {

    }
}
