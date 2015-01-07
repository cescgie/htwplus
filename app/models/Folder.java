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
        @NamedQuery(name = Folder.QUERY_FIND_ROOT_OF_GROUP, query = "SELECT f FROM Folder f, Group g WHERE f.depth = 1 and g.id = :" + Folder.PARAM_GROUP_ID)
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

    @OneToMany(mappedBy = "inFolder")
    public List<Media> files;

    @OneToMany(mappedBy = "parent")
    public List<Folder> childs;

    public static Folder findById(long id) {
        return JPA.em().find(Folder.class, id);
    }

    @Override
    public void create() {
        try {
            JPA.em().persist(this);
        } catch (Exception e) {
            //Blabla
        }
    }

    @Override
    public void update() {
        JPA.em().merge(this);
    }

    @Override
    public void delete() {
        if (this.isEmpty())
            JPA.em().remove(this);
    }

    public String getPath() {
        String result = "";
        String tmp = "";
        Folder f = this;
        while (f.depth > 1) {
            tmp = f.name;
            result = tmp.concat("/").concat(result);
            f = f.parent;
        }
        return result;
    }

    public boolean isEmpty() {
        boolean result = false;
        if (this.files.isEmpty())
            result = true;
        return result;
    }

    public String toAlternateString() {
        StringBuilder sb = new StringBuilder();
        sb.append("folder { ").append("id :").append(id).append(", ")
                .append("name :").append(name).append(" }");
        return sb.toString();
    }
}

