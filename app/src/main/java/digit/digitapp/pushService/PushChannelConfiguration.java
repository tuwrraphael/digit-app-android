package digit.digitapp.pushService;

import java.util.Map;

public class PushChannelConfiguration {
    private String id;
    private Map<String,String> options;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, String> getOptions() {
        return options;
    }

    public void setOptions(Map<String, String> options) {
        this.options = options;
    }
}
