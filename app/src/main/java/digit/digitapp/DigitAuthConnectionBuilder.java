package digit.digitapp;

import android.net.Uri;
import android.support.annotation.NonNull;

import net.openid.appauth.Preconditions;
import net.openid.appauth.connectivity.ConnectionBuilder;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class DigitAuthConnectionBuilder implements ConnectionBuilder {
    private static final String HTTPS_SCHEME = "https";
    public static final DigitAuthConnectionBuilder INSTANCE = new DigitAuthConnectionBuilder();

    @NonNull
    @Override
    public HttpURLConnection openConnection(@NonNull Uri uri) throws IOException {
        Preconditions.checkNotNull(uri, "url must not be null");
        Preconditions.checkArgument(HTTPS_SCHEME.equals(uri.getScheme()),
                "only https connections are permitted");
        HttpURLConnection conn = (HttpURLConnection) new URL(uri.toString()).openConnection();
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(60000);
        conn.setInstanceFollowRedirects(false);
        return conn;
    }
}
