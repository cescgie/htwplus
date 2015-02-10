package models;

import javax.persistence.*;
import models.base.BaseModel;
import play.db.jpa.JPA;
import java.util.List;

/**
 * Created by meichris on 02.12.14.
 */

@Entity
@NamedQueries({
        @NamedQuery(name = Folder.QUERY_FETCH_ALL, query = "SELECT f FROM Folder f ORDER BY f.name"),
        @NamedQuery(name = Folder.QUERY_FIND_ROOT, query = "SELECT f FROM Folder f, Group WHERE f.depth = 0 AND f.parent IS NULL"),
        @NamedQuery(name = Folder.QUERY_FIND_ALL_GROUPFOLDER, query = "SELECT f FROM Folder f, Group WHERE f.depth = 0"),
        @NamedQuery(name = Folder.QUERY_FIND_ROOT_OF_GROUP, query = "SELECT f FROM Folder f WHERE f.depth = 1 and f.group.id = :" + Folder.PARAM_GROUP_ID)
})
public class Folder extends BaseModel{

	public static final String QUERY_FETCH_ALL = "Folder.fetchAll";
	public static final String QUERY_FIND_ROOT = "Folder.findRoot";
	
    public static final String QUERY_FIND_ALL_GROUPFOLDER = "Folder.findAllGroupFolder";
    public static final String QUERY_FIND_ROOT_OF_GROUP = "Folder.findRootOfGroup";
    
    public static final String PARAM_GROUP_ID = "id";

    /*The folders name*/
    @Column(name = "name")
    public String name;
    /*The folders depth*/
    @Column(name = "depth")
    public int depth;
    /*The folders parent*/
    @ManyToOne
    public Folder parent;
    /*The folders group*/
    @ManyToOne
    public Group group;
    /*The folders owner*/
    @ManyToOne
    public Account owner;
    /*Lists the folders files*/
    @OneToMany(mappedBy = "inFolder"/*, cascade=CascadeType.REMOVE*/)
    @OrderBy("createdAt DESC")
    public List<Media> files;
    /*Lists the folders childs*/
    @OneToMany(mappedBy = "parent"/*, cascade=CascadeType.REMOVE*/)
    @OrderBy("createdAt DESC")
    public List<Folder> childs;
    /*Searches in the database for the delivered folder id and returns the folder 
    @param id the folders id
    @return the folder
    */
    public static Folder findById(long id) {
        return JPA.em().find(Folder.class, id);
    }
    /*
    Creates a databse entry (of type folder)
    */
    @Override
    public void create() {
            JPA.em().persist(this);
    }
    /*
    Updates a databse entry (of type folder)
    */
    @Override
    public void update() {
        JPA.em().merge(this);
    }
    /*
    Deletes a databse entry (of type folder)
    */
    @Override
    public void delete() {
            JPA.em().remove(this);
    }

    /*
    Returns a folder defined by its delivered name
    @param title the folders title
    @return the folder
    */
    public static Folder findByTitle(String title) {
        @SuppressWarnings("unchecked")
        List<Folder> folders = (List<Folder>) JPA.em()
                .createQuery("FROM Folder f WHERE f.name = ?1")
                .setParameter(1, title).getResultList();

        if (folders.isEmpty()) {
            return null;
        } else {
            return folders.get(0);
        }
    }

}

