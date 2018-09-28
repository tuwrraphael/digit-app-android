package digit.digitapp.pushService;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface PushServiceClient {
    @GET("api/me/pushchannels")
    Call<PushChannelConfiguration[]> GetPushChannels();

    @Headers({"Content-Type: application/vnd+pushserver.firebase+json"})
    @PUT("api/me/pushchannels/{configurationid}")
    Call<PushChannelConfiguration[]> UpdateChannel(@Path("configurationid") String configurationId, @Body FirebasePushChannelRegistration payload);

    @Headers({"Content-Type: application/vnd+pushserver.firebase+json"})
    @POST("api/me/pushchannels")
    Call<PushChannelConfiguration[]> CreateChannel(@Body FirebasePushChannelRegistration payload);
}
