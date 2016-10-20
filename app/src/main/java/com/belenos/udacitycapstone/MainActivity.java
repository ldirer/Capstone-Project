package com.belenos.udacitycapstone;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.belenos.udacitycapstone.data.DbContract;
import com.belenos.udacitycapstone.network.MySyncAdapter;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements OnboardingFragment.OnFragmentInteractionListener,
        HomeFragment.OnFragmentInteractionListener {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    public static final String FRAGMENT_ARG_LANGUAGE_NAME = "LANGUAGE_NAME";
    public static final String FRAGMENT_ARG_LANGUAGE_ID = "LANGUAGE_ID";
    private static final String GAME_FRAGMENT_TAG = "GAME_FRAGMENT_TAG";
    private static final String HOME_FRAGMENT_TAG = "HOME_FRAGMENT_TAG";
    private static final String ONBOARDING_FRAGMENT_TAG = "ONBOARDING_FRAGMENT_TAG";

    public boolean mLanguageDataLoaded = false;

    @BindView(R.id.sign_in_different_account_button)
    TextView mSwitchAccountTextview;
    @BindView(R.id.toolbar)
    Toolbar mToolbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // We can launch the game directly if the intent has the right parameters.
        Intent launchingIntent = getIntent();
        // It's a bit weird that we're passing both the id and the name. Could pbbly use a refactor.
        long languageId = launchingIntent.getLongExtra(FRAGMENT_ARG_LANGUAGE_ID, 0);
        String languageName = launchingIntent.getStringExtra(FRAGMENT_ARG_LANGUAGE_NAME);

        Log.d(LOG_TAG, "in onCreate");
        setContentView(R.layout.activity_main);

        MySyncAdapter.initializeSyncAdapter(this);
        MySyncAdapter.syncImmediately(this);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        Long userId = preferences.getLong(LoginActivity.KEY_USER_ID, 0);

        ButterKnife.bind(this);

        setSupportActionBar(mToolbar);

        //noinspection ConstantConditions
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        mToolbar.setNavigationIcon(R.drawable.ic_menu_home);

        // Check if fragment creation *has been done already* (e.g on rotation)!
        // Nothing to do in that case, right?
        // Nooo... We have some lame fragment-specific code to run here (show/hide a button) ;).
        // Retrospectively this is pbbly poor design: we could just check for fragment existence and return if it weren't for this specific bit of code.
        Fragment gameFragment = getSupportFragmentManager().findFragmentByTag(GAME_FRAGMENT_TAG);
        Fragment homeFragment = getSupportFragmentManager().findFragmentByTag(HOME_FRAGMENT_TAG);
        Fragment onboardingFragment = getSupportFragmentManager().findFragmentByTag(ONBOARDING_FRAGMENT_TAG);

        if (gameFragment != null) {
            startGameFragment(gameFragment, languageName, languageId);
            return;
        }
        if (homeFragment != null) {
            startHomeFragment(homeFragment);
            return;
        }
        if (onboardingFragment != null) {
            startOnboardingFragment(onboardingFragment);
            return;
        }

        if (languageId != 0 && languageName != null) {
            //We have everything we need to start the game right away
            startGameFragment(null, languageName, languageId);
        } else {
            Cursor languagesCursor = getContentResolver().query(DbContract.UserEntry.buildLanguagesForUserUri(userId), null, null, null, null);
            if (languagesCursor != null && languagesCursor.getCount() > 0) {
                startHomeFragment(null);
                languagesCursor.close();
            } else {
                startOnboardingFragment(null);
            }
        }
    }

    private void startOnboardingFragment(Fragment existing) {
        mSwitchAccountTextview.setVisibility(View.VISIBLE);
        if (existing == null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            OnboardingFragment fragment = OnboardingFragment.newInstance();
            ft.replace(R.id.fragment_frame_layout, fragment, ONBOARDING_FRAGMENT_TAG);
            ft.addToBackStack(null);
            ft.commit();
        }
    }



    @OnClick(R.id.sign_in_different_account_button)
    public void switchAccount() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setAction(LoginActivity.ACTION_LOGOUT);
        startActivity(intent);
    }


    public void startHomeFragment(Fragment existing) {
        mSwitchAccountTextview.setVisibility(View.VISIBLE);
        if (existing == null) {
        HomeFragment fragment = HomeFragment.newInstance();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_frame_layout, fragment, HOME_FRAGMENT_TAG)
                .addToBackStack(null)
                .commit();
        }
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
        startGameFragment(null, languageName, languageId);
    }


    public void startGameFragment(Fragment existing, String languageName, long languageId) {
        mSwitchAccountTextview.setVisibility(View.GONE);
        if (existing == null) {
            // The fragment is already created. Already. Created.
            android.support.v4.app.FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            GameFragment fragment = GameFragment.newInstance();
            Bundle args = new Bundle();
            args.putString(FRAGMENT_ARG_LANGUAGE_NAME, languageName);
            args.putLong(FRAGMENT_ARG_LANGUAGE_ID, languageId);
            fragment.setArguments(args);
            ft.replace(R.id.fragment_frame_layout, fragment, GAME_FRAGMENT_TAG);
            ft.addToBackStack(null);
            ft.commit();
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == android.R.id.home) {
            Log.d(LOG_TAG, "action bar clicked");
            startHomeFragment(null);
        }

        return super.onOptionsItemSelected(item);
    }
}
