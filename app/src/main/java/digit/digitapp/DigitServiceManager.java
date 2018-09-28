package digit.digitapp;

import android.content.Context;
import android.support.annotation.Nullable;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.ClientSecretBasic;

import java.io.IOException;

import digit.digitapp.digitService.DigitServiceClient;
import digit.digitapp.digitService.Location;
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

    private Context context;

    public DigitServiceManager(Context context) {
        this.context = context;
    }

    public void sendLocation(Location s) {
        final Location location = s;
        final AuthStateManager authStateManager = AuthStateManager.getInstance(context);
        final AuthState authState = authStateManager.getCurrent();
        if (null == authState.getAccessToken() || null == authState.getRefreshToken()) {
            return;
        }
        final  IDigitServiceClientConfig digitServiceClientConfig = DigitServiceClientConfig.Default;
        final  AuthenticationOptions authenticationOptions = digitServiceClientConfig.getAuthenticationOptions();
        final AuthorizationService authorizationService = new AuthorizationService(context);
        authState.performActionWithFreshTokens(authorizationService, new ClientSecretBasic(authenticationOptions.getClientSecret()),
                new AuthState.AuthStateAction() {
                    @Override
                    public void execute(@Nullable final String accessToken, @Nullable String idToken, @Nullable AuthorizationException ex) {
                        authorizationService.dispose();
                        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(new Interceptor() {
                            @Override
                            public Response intercept(Chain chain) throws IOException {
                                Request newRequest  = chain.request().newBuilder()
                                        .addHeader("Authorization", "Bearer " + accessToken)
                                        .build();
                                return chain.proceed(newRequest);
                            }
                        }).build();
                        Retrofit retrofit = new Retrofit.Builder()
                                .client(client)
                                .baseUrl("https://digit-svc.azurewebsites.net/")
                                .addConverterFactory(GsonConverterFactory.create())
                                .build();

                        final DigitServiceClient digitServiceClient = retrofit.create(DigitServiceClient.class);
                        digitServiceClient.AddLocation(location).enqueue(new Callback<ResponseBody>() {
                            @Override
                            public void onResponse(Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {

                            }

                            @Override
                            public void onFailure(Call<ResponseBody> call, Throwable t) {

                            }
                        });
                    }
                });
    }
}
