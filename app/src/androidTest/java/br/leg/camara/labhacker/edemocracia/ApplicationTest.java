package br.leg.camara.labhacker.edemocracia;

import android.app.Application;
import android.test.ApplicationTestCase;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import br.leg.camara.labhacker.edemocracia.liferay.AuthenticationHelper;
import br.leg.camara.labhacker.edemocracia.liferay.CustomService;
import br.leg.camara.labhacker.edemocracia.liferay.Session;

class Helper {
    private static Properties properties = null;

    public static String getProperty(String name) throws IOException {
        return getProperty(name, null);
    }

    public static String getProperty(String name, String defaultValue) throws IOException {
        if (properties == null) {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            InputStream stream = loader.getResourceAsStream("test.properties");
            if (stream == null) {
                throw new IOException("Resource `test.properties` doesn't exist");
            }
            try {
                properties = new Properties();
                properties.load(stream);
            } catch (IOException | NullPointerException e) {
                throw new IOException("Failed to load testing properties: " + e.toString());
            }
        }
        return properties.getProperty(name, defaultValue);
    }
}

public class ApplicationTest extends ApplicationTestCase<Application> {
    public ApplicationTest() {
        super(Application.class);
    }

    private String getUsername() throws IOException {
        return Helper.getProperty("username");
    }

    private String getPassword() throws IOException {
        return Helper.getProperty("password");
    }

    private int getCompanyId() throws IOException {
        return Integer.parseInt(Helper.getProperty("test.companyId"));
    }

    public void testItAll() throws Exception {
        createApplication();

        String username = getUsername();
        String password = getPassword();

        assertNotNull(username);
        assertNotNull(password);

        Session session = ApplicationSession.authenticate(getApplication(), username, password);

        assertNotNull(session);
        assertTrue(AuthenticationHelper.isAuthenticated(session));

        CustomService service = new CustomService(session);

        JSONArray groups = service.listGroups(getCompanyId());

        assertNotNull(groups);

        if (groups.length() < 1) {
            throw new AssertionError("We expected to receive at least one group");
        }

        int groupId = -1;
        for (int i = 0; i < groups.length(); i++) {
            JSONObject group = groups.getJSONObject(i);

            if (group.getString("friendlyURL").contains("hackathon-de-genero")) {
                groupId = group.getInt("groupId");
            }
        }

        if (groupId < 0) {
            throw new AssertionError("We couldn't find the a group containing hackathon-de-genero");
        }

        JSONArray threads = service.listGroupThreads(groupId);

        assertNotNull(threads);

        if (threads.length() < 1) {
            throw new AssertionError("We expected to receive at least one thread");
        }

        assertNotNull(threads.getJSONObject(0).getJSONObject("rootMessage"));
    }
}