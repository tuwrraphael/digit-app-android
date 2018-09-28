package digit.digitapp;

public interface IDigitServiceClientConfig {
    AuthenticationOptions getAuthenticationOptions();
    String getDigitServiceUrl();
    String getPushServiceUrl();
}
