package digit.digitapp;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.ResponseTypeValues;

public class LoginActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Context context = this;
        final AuthStateManager authStateManager = AuthStateManager.getInstance(context);
        final  IDigitServiceClientConfig digitServiceClientConfig = DigitServiceClientConfig.Default;
        final  AuthenticationOptions authenticationOptions = digitServiceClientConfig.getAuthenticationOptions();
        AuthorizationServiceConfiguration.fetchFromIssuer(
                Uri.parse(authenticationOptions.getIdentityUrl()),
                new AuthorizationServiceConfiguration.RetrieveConfigurationCallback() {
                    @Override
                    public void onFetchConfigurationCompleted(@Nullable AuthorizationServiceConfiguration serviceConfiguration, @Nullable AuthorizationException ex) {
                        AuthState authState = new AuthState(serviceConfiguration);
                        authStateManager.replace(authState);
                        AuthorizationRequest.Builder authRequestBuilder =
                                new AuthorizationRequest.Builder(
                                        serviceConfiguration,
                                        authenticationOptions.getClientId(),
                                        ResponseTypeValues.CODE,
                                        Uri.parse(authenticationOptions.getRedirectUrl()));
                        AuthorizationRequest authRequest = authRequestBuilder
                                .setScope(authenticationOptions.getScopes())
                                .build();
                        final AuthorizationService authService = new AuthorizationService(context);
                        authService.performAuthorizationRequest(
                                authRequest,
                                PendingIntent.getActivity(context, 0, new Intent(context, RedeemCodeActivity.class), 0),
                                PendingIntent.getActivity(context, 0, new Intent(context, LoginErrorActivity.class), 0));
                        authService.dispose();
                        ((LoginActivity) context).finish();
                    }
                });
    }
}
