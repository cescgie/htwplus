import java.util.concurrent.TimeUnit;

import controllers.FolderController;
import models.Folder;
import models.services.EmailService;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;

import controllers.Component;
import controllers.MediaController;
import controllers.routes;
import models.Account;
import models.Group;
import models.Post;
import models.enums.AccountRole;
import models.enums.GroupType;
import org.joda.time.DateTime;
import org.joda.time.Seconds;
import play.Play;
import play.Application;
import play.GlobalSettings;
import play.Logger;
import play.db.jpa.JPA;
import play.libs.Akka;
import play.libs.F.*;
import play.mvc.Http.RequestHeader;
import play.mvc.Result;
import scala.concurrent.duration.Duration;

@SuppressWarnings("unused")
public class Global extends GlobalSettings {
	
	@Override
	public void beforeStart(Application app) {
		Logger.info(" Global beforeStart");
		super.beforeStart(app);
	}

	@Override
	public void onStart(Application app) {
		Logger.info("Application - onStart: "+"Prod? "+Play.isProd()+" Test? "+Play.isTest()+" Dev? "+Play.isDev());
		super.onStart(app);

        // Sets the schedule for cleaning the media temp directory
		Akka.system().scheduler().schedule(
            Duration.create(0, TimeUnit.MILLISECONDS),
            Duration.create(30, TimeUnit.MINUTES),
            new Runnable() {
                public void run() {
                    MediaController.cleanUpTemp();
                }
            },
            Akka.system().dispatcher()
        );

        // set the email schedule to next full hour clock for sending daily and hourly emails
        Akka.system().scheduler().schedule(
            Duration.create(nextExecutionInSeconds(getNextHour(), 0), TimeUnit.SECONDS),
            Duration.create(1, TimeUnit.HOURS),
            new Runnable() {
                public void run() {
                    EmailService.getInstance().sendDailyHourlyNotificationsEmails();
                }
            },
            Akka.system().dispatcher()
        );
		
		JPA.withTransaction(new play.libs.F.Callback0() {
			@Override
			public void invoke() throws Throwable {
                FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(JPA.em());
                try {
                    fullTextEntityManager.createIndexer(Group.class).startAndWait();
                    fullTextEntityManager.createIndexer(Account.class).startAndWait();
                } catch (InterruptedException e) {
                    Logger.error(e.getMessage());
                }
			}
		});
		InitialData.insert(app);
	}

    /**
     * Returns the next full hour of the current time
     *
     * @return Next hour of current time
     */
    public static int getNextHour() {
        if ((new DateTime()).getHourOfDay() < 23){
            return (new DateTime()).getHourOfDay() + 1;
        }
        else{
            return (new DateTime()).getHourOfDay();
        }

    }

    /**
     * Calculates seconds between now and a time for hour and minute.
     *
     * @param hour Hour
     * @param minute Minute
     * @return Seconds
     */
    public static int nextExecutionInSeconds(int hour, int minute) {
        return Seconds.secondsBetween(
                new DateTime(),
                nextExecution(hour, minute)
        ).getSeconds();
    }

    /**
     * Returns a DateTime for hour and minute.
     *
     * @param hour Hour
     * @param minute Minute
     * @return DateTime
     */
    public static DateTime nextExecution(int hour, int minute) {
        DateTime next = new DateTime()
                .withHourOfDay(hour)
                .withMinuteOfHour(minute)
                .withSecondOfMinute(0)
                .withMillisOfSecond(0);

        return (next.isBeforeNow())
                ? next.plusHours(24)
                : next;
    }
	
	@Override
	public Promise<Result> onError(final RequestHeader rh, final Throwable t) {
		Logger.error("onError "+ rh + " " + t);
				
		JPA.withTransaction(new play.libs.F.Callback0() {
			@Override
			public void invoke() throws Throwable {
                Group group = Group.findByTitle(play.Play.application().configuration().getString("htwplus.admin.group"));
                if(group != null){
                    Post p = new Post();
                    p.content = "Request: "+rh+"\nError: "+t;
                    p.owner = Account.findByEmail(play.Play.application().configuration().getString("htwplus.admin.mail"));
                    p.group = group;
                    p.create();
                }
			}
		});
		
		
		// prod mode? return 404 page
		if(Play.isProd()){
			return Promise.pure(play.mvc.Results.redirect(routes.Application.error()));
		}

		return super.onError(rh, t);
	}
	
	static class InitialData {
		public static void insert(Application app) {
			
			final String adminGroupTitle = app.configuration().getString("htwplus.admin.group");
			final String adminMail = app.configuration().getString("htwplus.admin.mail");
			final String adminPassword = app.configuration().getString("htwplus.admin.pw");
			
			// Do some inital db stuff
			JPA.withTransaction(new play.libs.F.Callback0() {
				@Override
				public void invoke() throws Throwable {

                    //create root-Folder if not exists
                    Folder rootFolder = Folder.findByTitle("root");
                    if (rootFolder == null) {
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

                    // create Admin account if none exists
                    Account admin = Account.findByEmail(adminMail);
                    if (admin == null) {
                        admin = new Account();
                        admin.email = adminMail;
                        admin.firstname = "Admin";
                        admin.lastname = "@HTWplus";
                        admin.role = AccountRole.ADMIN;
                        admin.avatar = "a1";
                        admin.password = Component.md5(adminPassword);
                        admin.create();
                    }

                    // create Admin group if none exists
                    Group group = Group.findByTitle(adminGroupTitle);
                    if (group == null) {
                        group = new Group();
                        group.title = adminGroupTitle;
                        group.groupType = GroupType.close;
                        group.description = "for HTWplus Admins only";
                        group.createWithGroupAccount(admin);

                        //create Group-Folder if not exists
                        Folder groupFolder = Folder.findByTitle(group.title);
                        if (groupFolder == null) {
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
                    }

                    // create Feedback group if none exists
                    Group feedbackGroup = Group.findByTitle("HTWplus Feedback");
                    if (feedbackGroup == null) {
                        group = new Group();
                        group.title = "HTWplus Feedback";
                        group.groupType = GroupType.open;
                        group.description = "Du hast Wünsche, Ideen, Anregungen, Kritik oder Probleme mit der Seite? Hier kannst du es loswerden!";
                        group.createWithGroupAccount(admin);
                        //create Group-Folder if not exists
                        Folder groupFolder = Folder.findByTitle(group.title);
                        if (groupFolder == null) {
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
                    }
                    // Generate indexes
				}
			});
		}
	}
}
