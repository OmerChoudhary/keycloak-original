package org.keycloak.theme;

import org.jboss.logging.Logger;
import org.keycloak.freemarker.Theme;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Locale;
import java.util.Properties;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClassLoaderTheme implements Theme {

    private static final Logger LOGGER = Logger.getLogger(ClassLoaderTheme.class);

    private String name;

    private String parentName;

    private String importName;

    private Type type;

    private ClassLoader classLoader;

    private String templateRoot;

    private String resourceRoot;

    private String messageRoot;

    private Properties properties;

    public ClassLoaderTheme(String name, Type type, ClassLoader classLoader) throws IOException {
        init(name, type, classLoader);
    }

    public void init(String name, Type type, ClassLoader classLoader) throws IOException {
        this.name = name;
        this.type = type;
        this.classLoader = classLoader;

        String themeRoot = "theme/" + type.toString().toLowerCase() + "/" + name + "/";

        this.templateRoot = themeRoot;
        this.resourceRoot = themeRoot + "resources/";
        this.messageRoot = themeRoot + "messages/";
        this.properties = new Properties();

        URL p = classLoader.getResource(themeRoot + "theme.properties");
        if (p != null) {
            properties.load(p.openStream());
            this.parentName = properties.getProperty("parent");
            this.importName = properties.getProperty("import");
        } else {
            this.parentName = null;
            this.importName = null;
        }
    }

    public ClassLoaderTheme() {

    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getParentName() {
        return parentName;
    }

    @Override
    public String getImportName() {
        return importName;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public URL getTemplate(String name) {
        return classLoader.getResource(templateRoot + name);
    }

    @Override
    public InputStream getTemplateAsStream(String name) {
        return classLoader.getResourceAsStream(templateRoot + name);
    }

    @Override
    public URL getResource(String path) {
        return classLoader.getResource(resourceRoot + path);
    }

    @Override
    public InputStream getResourceAsStream(String path) {
        return classLoader.getResourceAsStream(resourceRoot + path);
    }

    @Override
    public Properties getMessages(Locale locale) throws IOException {
        Properties m = new Properties();

        URL url = null;
        if(locale != null) {
            URL messageFile = classLoader.getResource(this.messageRoot + "messages_" + locale.toString() + ".properties");
            if(messageFile != null){
                url = messageFile;
            }else{
                LOGGER.warnf("Can not find message file %s", messageFile);
            }
        }

        if(url == null){
           url = classLoader.getResource(this.messageRoot + "messages.properties");
        }

        if (url != null) {
            m.load(url.openStream());
        }
        return m;
    }

    @Override
    public Properties getProperties() {
        return properties;
    }

}
