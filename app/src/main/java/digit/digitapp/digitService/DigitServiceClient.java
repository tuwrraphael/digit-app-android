package digit.digitapp.digitService;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface DigitServiceClient {
    @POST("api/device/12345/log")
    Call<ResponseBody> Log(@Body LogEntry entry);
}
