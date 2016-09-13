package com.belenos.udacitycapstone;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.auth.api.Auth;

import butterknife.ButterKnife;
import butterknife.OnClick;


/**
 * Source: https://github.com/googlesamples/google-services/blob/master/android/signin/app/src/main/java/com/google/samples/quickstart/signin/SignInActivity.java#L59-L64
 * Plus documentation.
 */
public class LoginActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

    public static final String KEY_GOOGLE_GIVEN_NAME = "GOOGLE_GIVEN_NAME";
    public static final String KEY_GOOGLE_ID = "GOOGLE_ID";

    private static final String LOG_TAG = LoginActivity.class.getSimpleName();
    private GoogleApiClient mGoogleApiClient;

    // Wat. From the Google sample. Quality code.
    private static final int RC_SIGN_IN = 9001;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();


        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(LOG_TAG, "An unresolvable error has occurred and Google APIs (including Sign-In) will not be available.");
        Log.e(LOG_TAG, "onConnectionFailed:" + connectionResult);
    }


    @OnClick(R.id.sign_in_button)
    public void signIn() {
        Log.d(LOG_TAG, "in signIn");
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }

    private void handleSignInResult(GoogleSignInResult result) {
        Log.d(LOG_TAG, "handleSignInResult:" + result.isSuccess());
        if (result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            GoogleSignInAccount acct = result.getSignInAccount();
//            mStatusTextView.setText(getString(R.string.signed_in_format, acct.getDisplayName()));
// Also: acct.getId();
            if (acct == null) {
                Log.e(LOG_TAG, "Google Sign was a success but account is null!");
            }
            else {
                //TODO: Start a new activity and pass it the account info. Using a bundle param in startActivity.
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra(KEY_GOOGLE_GIVEN_NAME, acct.getDisplayName());
                intent.putExtra(KEY_GOOGLE_ID, acct.getDisplayName());
                startActivity(intent);
            }
        } else {
            // We do nothing (=Still show the sign in button).
            Log.e(LOG_TAG, "Google Sign in is NOT a success.");
            // Signed out, show unauthenticated UI.
        }
    }
}

