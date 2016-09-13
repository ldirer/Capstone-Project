package com.belenos.udacitycapstone;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements OnboardingFragment.OnFragmentInteractionListener,
    HomeFragment.OnFragmentInteractionListener {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOG_TAG, "in onCreate");
        setContentView(R.layout.activity_main);

        android.support.v4.app.FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

        // TODO: move fragment id to a variable (or delete it altogether).
        OnboardingFragment fragment = OnboardingFragment.newInstance();
        // We just forward the extras we got as arguments to the fragment.
        fragment.setArguments(getIntent().getExtras());
        ft.replace(R.id.fragment_frame_layout, fragment, "onboarding");
        ft.commit();

        // TODO: remove if not used
//        ButterKnife.bind(this);
    }


    @Override
    public void onFragmentInteraction(Uri uri) {
        // Do smt.
    }

    @Override
    public void onLanguageSelected(Integer languageId) {
        // TODO; make sure we replace the fragment cleanly
        android.support.v4.app.FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        HomeFragment fragment = HomeFragment.newInstance(0);
        // We just forward the extras we got as arguments to the fragment.
        fragment.setArguments(getIntent().getExtras());
        ft.replace(R.id.fragment_frame_layout, fragment, "home");
        ft.commit();
    }
}
