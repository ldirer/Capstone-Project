package com.belenos.udacitycapstone;

import android.*;
import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.belenos.udacitycapstone.data.DbContract;
import com.belenos.udacitycapstone.network.FetchLanguageTask;
import com.belenos.udacitycapstone.network.MySyncAdapter;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements OnboardingFragment.OnFragmentInteractionListener,
        HomeFragment.OnFragmentInteractionListener, GameFragment.OnFragmentInteractionListener {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    public static final String FRAGMENT_ARG_LANGUAGE_NAME = "LANGUAGE_NAME";
    public static final String FRAGMENT_ARG_LANGUAGE_ID = "LANGUAGE_ID";

    public boolean mLanguageDataLoaded = false;

    @BindView(R.id.sign_in_different_account_button)
    Button mSwitchAccountButton;
    @BindView(R.id.toolbar)
    Toolbar mToolbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOG_TAG, "in onCreate");
        setContentView(R.layout.activity_main);

        MySyncAdapter.initializeSyncAdapter(this);
        MySyncAdapter.syncImmediately(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[] {"android.permissions.GET_ACCOUNTS"}, 123);
            }

            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        Long userId = preferences.getLong(LoginActivity.KEY_USER_ID, 0);


        Cursor languagesCursor = getContentResolver().query(DbContract.UserEntry.buildLanguagesForUserUri(userId), null, null, null, null);
        if(languagesCursor != null && languagesCursor.getCount() > 0) {
            launchHomeFragment();
            languagesCursor.close();
        }
        else {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            OnboardingFragment fragment = OnboardingFragment.newInstance();
            ft.replace(R.id.fragment_frame_layout, fragment);
            ft.addToBackStack(null);
            ft.commit();
        }

        ButterKnife.bind(this);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        mToolbar.setNavigationIcon(R.drawable.ic_menu_home);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        AccountManager am = AccountManager.get(this);
        Account account = am.getAccountsByType("udacitycapstone.belenos.com")[0];
        boolean isYourAccountSyncEnabled = ContentResolver.getSyncAutomatically(account, getString(R.string.content_authority));
        boolean isMasterSyncEnabled = ContentResolver.getMasterSyncAutomatically();
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @OnClick(R.id.sign_in_different_account_button)
    public void switchAccount() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setAction(LoginActivity.ACTION_LOGOUT);
        startActivity(intent);
    }


    @Override
    public void onFragmentInteraction(Uri uri) {
        // Do smt.
    }


    public void launchHomeFragment() {
        android.support.v4.app.FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        HomeFragment fragment = HomeFragment.newInstance();
        ft.replace(R.id.fragment_frame_layout, fragment, "home");
        ft.addToBackStack(null);
        ft.commit();
    }

    /**
     * Offer a screen to start learning a new language.
     */
    @Override
    public void onAddLanguage() {
        Log.d(LOG_TAG, "in onAddLanguage");
        android.support.v4.app.FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

        OnboardingFragment fragment = OnboardingFragment.newInstance();
        Bundle args = new Bundle();
        args.putBoolean(HomeFragment.FRAGMENT_ARG_ALREADY_ONBOARDED, true);
        fragment.setArguments(args);

        ft.replace(R.id.fragment_frame_layout, fragment, "onboarding");
        ft.commit();
    }

    @Override
    public void onLanguageSelected(String languageName, long languageId) {
        // TODO; make sure we replace the fragment cleanly
        startGameFragment(languageName, languageId);
    }


    public void startGameFragment(String languageName, long languageId) {
        android.support.v4.app.FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        mSwitchAccountButton.setVisibility(View.GONE);
        GameFragment fragment = GameFragment.newInstance();
        Bundle args = new Bundle();
        args.putString(FRAGMENT_ARG_LANGUAGE_NAME, languageName);
        args.putLong(FRAGMENT_ARG_LANGUAGE_ID, languageId);
        fragment.setArguments(args);
        ft.replace(R.id.fragment_frame_layout, fragment, "game");
        ft.addToBackStack(null);
        ft.commit();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == android.R.id.home) {
            Log.d(LOG_TAG, "action bar clicked");
            mSwitchAccountButton.setVisibility(View.VISIBLE);
            launchHomeFragment();
        }

        return super.onOptionsItemSelected(item);
    }
}
