package digit.digitapp;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.openid.appauth.AppAuthConfiguration;
import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.ClientSecretBasic;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import digit.digitapp.digitService.DeviceData;
import digit.digitapp.digitService.DigitServiceClient;
import digit.digitapp.digitService.Location;
import digit.digitapp.digitService.LocationResponse;
import digit.digitapp.digitService.LogEntry;
import digit.digitapp.digitService.SyncAction;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class DigitServiceManager {

    private interface DigitServiceAction {
        void execute(DigitServiceClient digitServiceClient);
        void authFailed(Throwable t);
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

            @Override
            public void authFailed(Throwable t) {
                finished.onFailure(null,t);
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

    public void getPendingSyncActions( final Callback<List<SyncAction>> cb) {
        executeAuthorized(new DigitServiceAction() {
            @Override
            public void execute(DigitServiceClient digitServiceClient) {
                digitServiceClient.GetSyncActions().enqueue(cb);
            }

            @Override
            public void authFailed(Throwable t) {
                cb.onFailure(null,t);
            }
        });
    }

    public void getDeviceData(final String id, final Callback<DeviceData> cb) {
        executeAuthorized(new DigitServiceAction() {
            @Override
            public void execute(DigitServiceClient digitServiceClient) {
                digitServiceClient.GetDeviceData(id).enqueue(cb);
            }

            @Override
            public void authFailed(Throwable t) {
                cb.onFailure(null,t);
            }
        });
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

            @Override
            public void authFailed(Throwable t) {
                finished.finished(); //TODO log without auth to unauthorized endpoint
            }
        });
    }

    public void setSynched(final String id, final Callback<ResponseBody> cb) {
        executeAuthorized(new DigitServiceAction() {
            @Override
            public void execute(DigitServiceClient digitServiceClient) {
                    digitServiceClient.SetSynced(id).enqueue(cb);
            }

            @Override
            public void authFailed(Throwable t) {
                cb.onFailure(null,t);
            }
        });
    }

    private void executeAuthorized(final DigitServiceAction action) {
        final SharedPreferences mPrefs = context.getSharedPreferences("DigitSettings", Context.MODE_PRIVATE);
        if (mPrefs.contains("authBroken")) {
            action.authFailed(new Exception("Token refresh failure"));
        }
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
                        if (null != ex) {
                            mPrefs.edit().putBoolean("authBroken",true);
                            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                            Intent intent = new Intent(context, LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
                            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "default")
                                    .setSmallIcon(R.drawable.ic_stat_onesignal_default)
                                    .setContentTitle(context.getText(R.string.authError_notification_title))
                                    .setContentText(context.getText(R.string.authError_notification_content))
                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                    .setContentIntent(pendingIntent)
                                    .setAutoCancel(true);
                            notificationManager.notify(14, builder.build());
                            action.authFailed(new Exception("Token refresh failure"));
                        } else {
                            authStateManager.replace(authState);
                            authorizationService.dispose();
                            OkHttpClient client = new OkHttpClient.Builder()
                                    .readTimeout(60, TimeUnit.SECONDS)
                                    .connectTimeout(10, TimeUnit.SECONDS)
                                    .addInterceptor(new Interceptor() {
                                        @Override
                                        public okhttp3.Response intercept(Chain chain) throws IOException {
                                            Request newRequest = chain.request().newBuilder()
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
                    }
                });
    }
}
