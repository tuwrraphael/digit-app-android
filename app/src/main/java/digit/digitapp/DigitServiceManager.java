package digit.digitapp;

import android.content.Context;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.openid.appauth.AppAuthConfiguration;
import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.ClientSecretBasic;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import digit.digitapp.digitService.DigitServiceClient;
import digit.digitapp.digitService.Location;
import digit.digitapp.digitService.LocationResponse;
import digit.digitapp.digitService.LogEntry;
import digit.digitapp.pushService.FirebasePushChannelRegistration;
import digit.digitapp.pushService.FirebasePushChannelRegistrationOptions;
import digit.digitapp.pushService.PushChannelConfiguration;
import digit.digitapp.pushService.PushServiceClient;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class DigitServiceManager {

    private interface DigitServiceAction {
        void execute(DigitServiceClient digitServiceClient);
    }

    private Context context;

    public DigitServiceManager(Context context) {
        this.context = context;
    }

    public void sendLocation(final Location location, final Callback<LocationResponse> finished) {
        executeAuthorized(new DigitServiceAction() {
            @Override
            public void execute(DigitServiceClient digitServiceClient) {
                digitServiceClient.AddLocation(location).enqueue(finished);
            }
        });
    }

    public void log(String msg, int code) {
        final ActionFinished finished = new ActionFinished() {
            @Override
            public void finished() {

            }
        };
        log(msg,code,finished);
    }

    public void log(String msg, int code, final ActionFinished finished) {
        final LogEntry entry = new LogEntry(null, new Date(), code, msg, "digitAppAndroid");
        executeAuthorized(new DigitServiceAction() {
            @Override
            public void execute(DigitServiceClient digitServiceClient) {
                digitServiceClient.Log(entry).enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                        finished.finished();
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        finished.finished();
                    }
                });
            }
        });
    }


    private void executeAuthorized(final DigitServiceAction action) {
        final AuthStateManager authStateManager = AuthStateManager.getInstance(context);
        final AuthState authState = authStateManager.getCurrent();
        if (null == authState.getAccessToken() || null == authState.getRefreshToken()) {
            return;
        }
        final  IDigitServiceClientConfig digitServiceClientConfig = DigitServiceClientConfig.Default;
        final  AuthenticationOptions authenticationOptions = digitServiceClientConfig.getAuthenticationOptions();
        final AppAuthConfiguration config =  new AppAuthConfiguration.Builder()
                .setConnectionBuilder(DigitAuthConnectionBuilder.INSTANCE)
                .build();
        final AuthorizationService authorizationService = new AuthorizationService(context, config);
        authState.performActionWithFreshTokens(authorizationService, new ClientSecretBasic(authenticationOptions.getClientSecret()),
                new AuthState.AuthStateAction() {
                    @Override
                    public void execute(@Nullable final String accessToken, @Nullable String idToken, @Nullable AuthorizationException ex) {
                        authStateManager.replace(authState);
                        authorizationService.dispose();
                        OkHttpClient client = new OkHttpClient.Builder()
                                .readTimeout(60, TimeUnit.SECONDS)
                                .connectTimeout(10,TimeUnit.SECONDS)
                                .addInterceptor(new Interceptor() {
                            @Override
                            public Response intercept(Chain chain) throws IOException {
                                Request newRequest  = chain.request().newBuilder()
                                        .addHeader("Authorization", "Bearer " + accessToken)
                                        .build();
                                return chain.proceed(newRequest);
                            }
                        }).build();
                        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").create();
                        Retrofit retrofit = new Retrofit.Builder()
                                .client(client)
                                .baseUrl(digitServiceClientConfig.getDigitServiceUrl())
                                .addConverterFactory(GsonConverterFactory.create(gson))
                                .build();

                        final DigitServiceClient digitServiceClient = retrofit.create(DigitServiceClient.class);
                       action.execute(digitServiceClient);
                    }
                });
    }
}
