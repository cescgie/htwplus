package models.base;


import models.Account;
import models.Group;
import models.GroupAccount;
import models.NewNotification;
import models.enums.LinkType;
import play.Logger;
import play.api.mvc.Content;
import play.api.templates.Html;
import play.db.jpa.JPA;
import play.libs.F;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class as extension to BaseModel for classes, that are notifiable.
 */
public abstract class BaseNotifiable extends BaseModel implements INotifiable {
    /**
     * Type of this notifiable instance. This might be usable, if the render() method, needs to differentiate
     * between more than one possible states of this notifiable object (e.g. a post can be profile, stream
     * or group post).
     */
    public String type;

    @Override
    public BaseModel getReference() {
        return this;
    }

    /**
     * Returns the fully qualified path to the compiled template class desired for this notification as string.
     * Example:
     * - For post:  There are types like PROFILE or STREAM posts, therefore the fully qualified
     *              string for a profile post is: views.html.NewNotification.post.profile
     *
     * @return The desired template class as string
     */
    protected String getTemplateClass() {
        return this.type.equals("")
            ? "views.html.NewNotification." + this.getClass().getSimpleName().toLowerCase()
            : "views.html.NewNotification." + this.getClass().getSimpleName().toLowerCase() + "."
                + this.type.toLowerCase();
    }

    /**
     * Determines the fully qualified path to the rendered template class,
     * invokes the static render() method and returns the rendered content.
     *
     * @return Rendered HTML content
     * @throws Exception
     */
    protected Html getRendered(NewNotification notification) throws Exception {
        Class<?> templateClass = Class.forName(this.getTemplateClass());
        Class[] parameterClasses = { NewNotification.class, this.getClass() };

        Method renderMethod = templateClass.getDeclaredMethod("render", parameterClasses);

        return (Html)renderMethod.invoke(null, notification, this);
    }

    @Override
    public String render(NewNotification notification) {
        try {
            Content html = this.getRendered(notification);

            return html.toString().trim();
        } catch (Exception e) {
            Logger.error(e.getMessage());

            return "";
        }
    }

    /**
     * Helper method to return one account as a List<Account> as required by getRecipients()
     * in the INotifiable interface.
     *
     * @param account One account to be returned as list
     * @return List of account instances
     */
    public List<Account> getAsAccountList(Account account) {
        List<Account> accounts = new ArrayList<Account>();
        accounts.add(account);

        return accounts;
    }

    /**
     * Returns all accounts that are assigned to a group as list.
     *
     * @param group Group to retrieve a list of accounts from
     * @return List of accounts of group
     */
    public List<Account> getGroupAsAccountList(final Group group) {
        try {
            return JPA.withTransaction(new F.Function0<List<Account>>() {
                @Override
                public List<Account> apply() throws Throwable {
                    return GroupAccount.findAccountsByGroup(group, LinkType.establish);
                }
            });
        } catch (Throwable ex) {
            Logger.error("Could not get list of accounts by group \"" + group.getTitle()
                    + "\", returning empty list: " + ex.getMessage());
            return new ArrayList<Account>();
        }
    }
}
