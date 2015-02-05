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

    @Column(name = "name")
    public String name;

    @Column(name = "depth")
    public int depth;

    @ManyToOne
    public Folder parent;

    @ManyToOne
    public Group group;

    @OneToMany(mappedBy = "inFolder"/*, cascade=CascadeType.REMOVE*/)
    public List<Media> files;

    @OneToMany(mappedBy = "parent"/*, cascade=CascadeType.REMOVE*/)
    public List<Folder> childs;

    public static Folder findById(long id) {
        return JPA.em().find(Folder.class, id);
    }

    @Override
    public void create() {
            JPA.em().persist(this);
    }

    @Override
    public void update() {
        JPA.em().merge(this);
    }

    @Override
    public void delete() {
            JPA.em().remove(this);
    }

}

