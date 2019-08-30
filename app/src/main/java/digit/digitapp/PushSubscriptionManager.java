package digit.digitapp;

import android.content.Context;
import android.support.annotation.Nullable;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.ClientSecretBasic;

import java.io.IOException;
import java.util.HashMap;

import digit.digitapp.pushService.FirebasePushChannelRegistration;
import digit.digitapp.pushService.PushChannelConfiguration;
import digit.digitapp.pushService.PushServiceClient;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class PushSubscriptionManager {

    private Context context;

    public PushSubscriptionManager(Context context) {
        this.context = context;
    }

    public void sendToken(String s) {
        final String token = s;
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
                        authStateManager.replace(authState);
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
                                .baseUrl(digitServiceClientConfig.getPushServiceUrl())
                                .addConverterFactory(GsonConverterFactory.create())
                                .build();

                        final PushServiceClient pushServiceClient = retrofit.create(PushServiceClient.class);
                        pushServiceClient.GetPushChannels().enqueue(new Callback<PushChannelConfiguration[]>() {
                            @Override
                            public void onResponse(Call<PushChannelConfiguration[]> call, retrofit2.Response<PushChannelConfiguration[]> response) {
                                FirebasePushChannelRegistration registration = new FirebasePushChannelRegistration();
                                registration.setToken(token);
                                boolean channelExists = false;
                                for(PushChannelConfiguration config:response.body()){
                                    channelExists = config.getOptions().containsKey("digitLocationRequest");
                                    if (channelExists) {
                                        registration.setOptions(config.getOptions());
                                        registration.getOptions().put("digitLocationRequest", "supported");
                                        pushServiceClient.UpdateChannel(config.getId(), registration).enqueue(new Callback<PushChannelConfiguration[]>() {
                                            @Override
                                            public void onResponse(Call<PushChannelConfiguration[]> call, retrofit2.Response<PushChannelConfiguration[]> response) {

                                            }

                                            @Override
                                            public void onFailure(Call<PushChannelConfiguration[]> call, Throwable t) {

                                            }
                                        });
                                        break;
                                    }
                                }
                                if (!channelExists) {
                                    registration.setDeviceInfo("Android");
                                    registration.setOptions(new HashMap<>());
                                    registration.getOptions().put("digitLocationRequest", "supported");
                                    registration.setToken(token);
                                    pushServiceClient.CreateChannel(registration).enqueue(new Callback<PushChannelConfiguration[]>() {
                                        @Override
                                        public void onResponse(Call<PushChannelConfiguration[]> call, retrofit2.Response<PushChannelConfiguration[]> response) {

                                        }

                                        @Override
                                        public void onFailure(Call<PushChannelConfiguration[]> call, Throwable t) {

                                        }
                                    });
                                }
                            }
                            @Override
                            public void onFailure(Call<PushChannelConfiguration[]> call, Throwable t) {

                            }
                        });
                    }
                });
    }
}
