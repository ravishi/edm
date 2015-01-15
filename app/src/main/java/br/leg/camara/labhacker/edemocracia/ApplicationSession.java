package br.leg.camara.labhacker.edemocracia;

import android.app.Application;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.WeakHashMap;

import br.leg.camara.labhacker.edemocracia.liferay.AuthenticationHelper;
import br.leg.camara.labhacker.edemocracia.liferay.CookieCredentials;
import br.leg.camara.labhacker.edemocracia.liferay.LiferaySession;
import br.leg.camara.labhacker.edemocracia.liferay.Session;


public class ApplicationSession {

    public static final int DEFAULT_COMPANY_ID = 10131;

    private static final URL SERVICE_URL;
    private static final URL SERVICE_LOGIN_URL;

    static {
        try {
            SERVICE_URL = new URL("http://edemocracia.camara.gov.br/api/jsonws/invoke");
            SERVICE_LOGIN_URL = new URL("http://edemocracia.camara.gov.br/cadastro");
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Map<Application, Session> sessions = new WeakHashMap<>();

    public static Session getSession(Application application) {
        return sessions.get(application);
    }

    public static Session authenticate(Application application, String username, String password) throws IOException {
        CookieCredentials credentials = AuthenticationHelper.authenticate(SERVICE_LOGIN_URL, username, password);

        if (credentials == null) {
            return null;
        }

        PersistentCredentials.store(application.getApplicationContext(), credentials);

        Session session = new LiferaySession(SERVICE_URL, credentials);

        sessions.put(application, session);

        return session;
    }
}
