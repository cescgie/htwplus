package models.services;

import play.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Helper service to render templates.
 */
public class TemplateService {
    /**
     * Singleton instance
     */
    private static TemplateService instance = null;

    /**
     * Private constructor for singleton instance
     */
    private TemplateService() { }

    /**
     * Returns the singleton instance.
     *
     * @return EmailHandler instance
     */
    public static TemplateService getInstance() {
        if (TemplateService.instance == null) {
            TemplateService.instance = new TemplateService();
        }

        return TemplateService.instance;
    }

    /**
     * Returns the classes as array of the template parameters as this information
     * is required for method reflection.
     *
     * @param templateParameters Template parameters
     * @return Array of classes
     */
    protected Class[] getParameterClasses(Object... templateParameters) {
        Class[] parameterClasses = new Class[templateParameters.length];
        for (int i = 0; i < parameterClasses.length; i++) {
            // template render methods usually use List interfaces, therefore we need to add List.class instead of
            // realizing classes like ArrayList which might be given in templateParameters
            if (templateParameters[i] instanceof List) {
                parameterClasses[i] = List.class;
            } else {
                parameterClasses[i] = templateParameters[i].getClass();
            }
        }

        return parameterClasses;
    }

    /**
     * Renders a HTML template and returns the content as a String. If an exception appears (e.g. template
     * path is not valid), an empty String is returned.
     *
     * @param templatePath Template path
     * @param templateParameters Template parameters
     * @return Rendered content as String or empty String on exception
     */
    public String getRenderedTemplate(String templatePath, Object... templateParameters) {
        try {
            // determine class of template, retrieve render method of template and invoke render()
            Class<?> templateClass = Class.forName(templatePath);
            Method renderMethod = templateClass.getDeclaredMethod("render", this.getParameterClasses(templateParameters));

            return renderMethod.invoke(null, templateParameters).toString().trim();
        } catch (ClassNotFoundException e) {
            Logger.error("Could not get template class for template path: " + templatePath + "(" + e.getMessage() + ")");
        } catch (NoSuchMethodException e) {
            Logger.error("Could not get render method for template path: " + templatePath + "(" + e.getMessage() + ")");
        } catch (InvocationTargetException e) {
            Logger.error("Invocation exception while render() template path: " + templatePath + "(" + e.getMessage() + ")");
        } catch (IllegalAccessException e) {
            Logger.error("IllegalAccessException exception while render() template path: " + templatePath + "(" + e.getMessage() + ")");
        } catch (Exception e) {
            Logger.error("Exception while trying to render template path: " + templatePath + "(" + e.getMessage() + ")");
        }

        // previous exception is logged, return empty String
        return "";
    }
}
