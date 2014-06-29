package lib.media.interfaces;

/**
 * Created by FKI on 29.06.2014.
 */

import lib.exceptions.MediaException;

import java.io.File;
import java.util.List;

/**
 * General Interface for managing files, respective any media.
 * Files are identified by an unique URL.
 */
public interface MediaManager {

    /**
     * Creating a new file
     * @param url
     * @param file
     */
    public void create(String url, File file) throws MediaException;

    /**
     * Reading a file
     * @param url
     * @return
     */
    public File read(String url) throws MediaException;

    /**
     * Reading multiple files and returning them compressed in one file
     * @param urls
     * @return
     */
    public File read(List<String> urls) throws MediaException;

    /**
     * Updating an exisiting file
     * @param url
     * @param file
     */
    public void update(String url, File file) throws MediaException;

    /**
     * Deleting a file
     * @param url
     */
    public void delete(String url) throws MediaException;

    /**
     * Deleting multiple files
     * @param urls
     */
    public void delete(List<String> urls) throws MediaException;
}
