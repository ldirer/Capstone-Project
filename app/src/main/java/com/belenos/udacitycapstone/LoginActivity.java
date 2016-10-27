package com.belenos.udacitycapstone;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.Toast;

import com.belenos.udacitycapstone.data.DbContract;
import com.belenos.udacitycapstone.network.MySyncAdapter;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.belenos.udacitycapstone.GameFragment.ACTION_DATA_UPDATED;


/**
 * Source: https://github.com/googlesamples/google-services/blob/master/android/signin/app/src/main/java/com/google/samples/quickstart/signin/SignInActivity.java#L59-L64
 * Plus documentation.
 *
 *
 * Here we could check for internet and warn the user that logging in requires internet (Google does not do that apparently!?...)
 */
public class LoginActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

    public static final String ACTION_LOGOUT = "LOGOUT";

    public static final String KEY_GOOGLE_GIVEN_NAME = "GOOGLE_GIVEN_NAME";
    public static final String KEY_GOOGLE_ID = "GOOGLE_ID";
    public static final String KEY_USER_ID = "USER_ID";

    private static final String LOG_TAG = LoginActivity.class.getSimpleName();
    private static final String KEY_ALREADY_LOGGED_IN = "LOGGED_IN";

    private GoogleApiClient mGoogleApiClient;

    // Wat. From the Google sample. Quality code.
    private static final int RC_SIGN_IN = 9001;

    Tracker mTracker;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        Toolbar toolbar;
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Google Analytics stuff
        // Obtain the shared Tracker instance.
        MyApplication application = (MyApplication) getApplication();
        mTracker = application.getDefaultTracker();


        // If the user is already logged in we want to start the main activity straight away.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean alreadyLoggedIn = prefs.getBoolean(KEY_ALREADY_LOGGED_IN, false);

        Intent launchIntent = getIntent();
        boolean signingOut = launchIntent.getAction().equals(ACTION_LOGOUT);

        if (alreadyLoggedIn && !signingOut) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(KEY_GOOGLE_GIVEN_NAME, prefs.getString(KEY_GOOGLE_GIVEN_NAME, null));
            intent.putExtra(KEY_GOOGLE_ID, prefs.getString(KEY_GOOGLE_ID, null));
            startActivity(intent);
        } else {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .build();

            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .enableAutoManage(this, this)
                    .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                    .build();


            setContentView(R.layout.activity_login);
            ButterKnife.bind(this);
        }

        if (signingOut) {
            signOut();
        }
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

    /**
     * This method makes sure we sign out as soon as the google api client is ready.
     */
    private void signOut() {
        mGoogleApiClient.registerConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
            @Override
            public void onConnected(@Nullable Bundle bundle) {
                Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                        new ResultCallback<Status>() {
                            @Override
                            public void onResult(@NonNull Status status) {
                                Log.d(LOG_TAG, "in signOut.onResult");
                                Log.d(LOG_TAG, status.isSuccess() ? "success" : "not success");
                            }
                        });
            }

            @Override
            public void onConnectionSuspended(int i) {

            }
        });
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

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

            // apply is async for the writing-to-disk part but writes to in-memory SharedPref immediately.
            preferences.edit().putBoolean(KEY_ALREADY_LOGGED_IN, true).apply();

            if (acct == null) {
                Log.e(LOG_TAG, "Google Sign was a success but account is null!");
            } else {
                preferences.edit()
                        .putString(KEY_GOOGLE_GIVEN_NAME, acct.getDisplayName())
                        .putString(KEY_GOOGLE_ID, acct.getId())
                        .apply();


                // Note we could use AsyncQueryHandler to move the insert out of the UI thread.
                // Not sure the performance gain would be huge though.
                ContentValues userCV = new ContentValues();
                String displayName = acct.getDisplayName();
                if (displayName == null) {
                    displayName = "Mysterious user";
                }
                userCV.put(DbContract.UserEntry.COLUMN_NAME, displayName);
                userCV.put(DbContract.UserEntry.COLUMN_GOOGLE_ID, acct.getId());

                ContentResolver contentResolver = getContentResolver();
                Uri userUri;
                try {
                    userUri = contentResolver.insert(DbContract.UserEntry.CONTENT_URI, userCV);
                } catch (SQLException e) {
                    // Happens when we try to insert a user who already exists.
                    userUri = DbContract.UserEntry.buildUserByGoogleIdUri(acct.getId());
                }

                if (null != userUri) {
                    Cursor c = contentResolver.query(userUri, new String[]{DbContract.UserEntry._ID},
                            null,
                            null,
                            null);

                    if (c != null && c.moveToFirst()) {
                        long userId = c.getLong(0);
                        c.close();
                        preferences.edit().putLong(KEY_USER_ID, userId).apply();

                        Log.d(LOG_TAG, "Running MySyncAdapter (supposedly!)");
                        MySyncAdapter.syncImmediately(this);
                    } else {
                        Log.e(LOG_TAG, "Could not retrieve a user we just inserted in the db!");
                        if (c != null)
                            c.close();
                    }
                } else {
                    Log.e(LOG_TAG, "userUri is null! We could not insert nor retrieve a matching Google id...");
                    Toast.makeText(this, R.string.error_insert_user, Toast.LENGTH_LONG).show();
                    return;
                }

                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);

                // Update our widget since we logged in successfully
                Intent widgetIntent = new Intent(ACTION_DATA_UPDATED).setPackage(getPackageName());
                sendBroadcast(widgetIntent);
                Log.d(LOG_TAG, "Sending broadcast intent for widget");
            }
        } else {
            // We do nothing (=Still show the sign in button).
            Log.e(LOG_TAG, "Google Sign in is NOT a success.");
            // Signed out, show unauthenticated UI.
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        String screenName = LOG_TAG;
        Log.d(LOG_TAG, "Analytics: Setting screen name: " + screenName);
        mTracker.setScreenName("Image~" + screenName);
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }
}

