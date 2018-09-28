package digit.digitapp;

public class AuthenticationOptions {
    private String identityUrl;
    private String scopes;
    private String clientId;
    private String clientSecret;
    private String redirectUrl;

    public AuthenticationOptions(String identityUrl, String scopes, String clientId, String clientSecret, String redirectUrl) {
        this.identityUrl = identityUrl;
        this.scopes = scopes;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUrl = redirectUrl;
    }

    public String getIdentityUrl() {
        return identityUrl;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getClientId() {
        return clientId;
    }

    public String getScopes() {
        return scopes;
    }
}
