package com.belenos.udacitycapstone;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements OnboardingFragment.OnFragmentInteractionListener,
    HomeFragment.OnFragmentInteractionListener, GameFragment.OnFragmentInteractionListener {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    public static final String FRAGMENT_ARG_LANGUAGE_NAME = "LANGUAGE_NAME";

    @BindView(R.id.sign_in_different_account_button) Button mSwitchAccountButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOG_TAG, "in onCreate");
        setContentView(R.layout.activity_main);

        android.support.v4.app.FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

        // TODO: move fragment id to a variable (or delete it altogether).
        OnboardingFragment fragment = OnboardingFragment.newInstance();
        ft.replace(R.id.fragment_frame_layout, fragment, "onboarding");
        ft.addToBackStack(null);
        ft.commit();

        ButterKnife.bind(this);
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
        android.support.v4.app.FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        GameFragment fragment = GameFragment.newInstance(mSwitchAccountButton);
        Bundle args = new Bundle();
        args.putString(FRAGMENT_ARG_LANGUAGE_NAME, languageName);
        fragment.setArguments(args);
        ft.replace(R.id.fragment_frame_layout, fragment, "game");
        ft.addToBackStack(null);
        ft.commit();
    }
}
