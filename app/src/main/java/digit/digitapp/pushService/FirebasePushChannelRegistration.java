package digit.digitapp.pushService;

import java.util.Map;

public class FirebasePushChannelRegistration {
    private String token;
    private String deviceInfo;
    private  FirebasePushChannelRegistrationOptions options;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public FirebasePushChannelRegistrationOptions getOptions() {
        return options;
    }

    public void setOptions(FirebasePushChannelRegistrationOptions options) {
        this.options = options;
    }
}

