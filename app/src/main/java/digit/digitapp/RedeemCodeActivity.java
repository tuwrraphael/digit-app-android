package digit.digitapp;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.ClientSecretBasic;
import net.openid.appauth.ResponseTypeValues;
import net.openid.appauth.TokenRequest;
import net.openid.appauth.TokenResponse;

import java.util.logging.Logger;

public class RedeemCodeActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_redeem_code);
        final Context context = this;
        final AuthorizationResponse response = AuthorizationResponse.fromIntent(getIntent());
        final IDigitServiceClientConfig digitServiceClientConfig = DigitServiceClientConfig.Default;
        final AuthenticationOptions authenticationOptions = digitServiceClientConfig.getAuthenticationOptions();
        AuthorizationException exception = AuthorizationException.fromIntent(getIntent());
        if (null != response){
            final AuthStateManager authStateManager = AuthStateManager.getInstance(this);
            final AuthState authState = authStateManager.getCurrent();
            authStateManager.updateAfterAuthorization(response, exception);
            final AuthorizationService authService = new AuthorizationService(this);
            authService.performTokenRequest(
                    response.createTokenExchangeRequest(),
                    new ClientSecretBasic(authenticationOptions.getClientSecret()),
                    new AuthorizationService.TokenResponseCallback() {
                        @Override public void onTokenRequestCompleted(TokenResponse tokenResponse, AuthorizationException ex) {
                            authStateManager.updateAfterTokenResponse(tokenResponse, ex);
                            if (tokenResponse != null) {
                                String token = DigitFirebaseMessagingService.getToken(context);
                                new PushSubscriptionManager(context).sendToken(token);
                                Intent mainActivity = new Intent(context, MainActivity.class);
                                startActivity(mainActivity);
                                final SharedPreferences mPrefs = context.getSharedPreferences("DigitSettings", Context.MODE_PRIVATE);
                                mPrefs.edit().remove("authBroken");
                            } else {
                                Intent errorActivity = new Intent(context, LoginErrorActivity.class);
                                startActivity(errorActivity);
                            }
                            authService.dispose();
                            ((RedeemCodeActivity) context).finish();
                        }
                    });
        }
    }
}
