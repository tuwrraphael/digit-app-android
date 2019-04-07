package digit.digitapp.digitService;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface DigitServiceClient {
    @POST("api/device/12345/log")
    Call<ResponseBody> Log(@Body LogEntry entry);

    @POST("api/me/location")
    Call<LocationResponse> AddLocation(@Body Location location);

    @GET ("api/me/sync")
    Call<List<SyncAction>> GetSyncActions();

    @PUT("api/me/sync/{id}")
    Call<ResponseBody> SetSynced(@Path("id") String id);

    @GET("api/devices/{id}/data")
    Call<DeviceData> GetDeviceData(@Path("id") String id);
}
