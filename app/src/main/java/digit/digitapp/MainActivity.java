package digit.digitapp;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.ClientSecretBasic;
import net.openid.appauth.ResponseTypeValues;

import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

import digit.digitapp.digitService.DigitServiceClient;
import digit.digitapp.digitService.LogEntry;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Context context = this;
        final AuthStateManager authStateManager = AuthStateManager.getInstance(context);
        final AuthState authState = authStateManager.getCurrent();
        if (null == authState.getRefreshToken() ||
                null == authState.getAccessToken() ||
                null == authState.getAuthorizationServiceConfiguration()){
            Intent loginActivity = new Intent(context, LoginActivity.class);
            startActivity(loginActivity);
            this.finish();
        }
        final  IDigitServiceClientConfig digitServiceClientConfig = DigitServiceClientConfig.Default;
        final  AuthenticationOptions authenticationOptions = digitServiceClientConfig.getAuthenticationOptions();
        AuthorizationService authorizationService = new AuthorizationService(this);
        authState.performActionWithFreshTokens(authorizationService, new ClientSecretBasic(authenticationOptions.getClientSecret()),
                new AuthState.AuthStateAction() {
                    @Override
                    public void execute(@Nullable final String accessToken, @Nullable String idToken, @Nullable AuthorizationException ex) {
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
                                .baseUrl("https://digit-svc.azurewebsites.net")
                                .addConverterFactory(GsonConverterFactory.create())
                                .build();

                        DigitServiceClient digitServiceClient = retrofit.create(DigitServiceClient.class);
                        digitServiceClient.Log(new LogEntry(null, new Date(), 123, "Test from android", "digitAppAndroid")).enqueue(new Callback<ResponseBody>() {
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
