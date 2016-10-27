package com.belenos.udacitycapstone;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.aigestudio.wheelpicker.WheelPicker;
import com.belenos.udacitycapstone.data.DbContract;
import com.belenos.udacitycapstone.network.FetchLanguageTask;
import com.belenos.udacitycapstone.utils.TrackedFragment;
import com.belenos.udacitycapstone.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnboardingFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link OnboardingFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class OnboardingFragment extends TrackedFragment implements WheelPicker.OnItemSelectedListener, LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = OnboardingFragment.class.getSimpleName();
    private OnFragmentInteractionListener mListener;

    // We'll use this to slightly adjust the layout and wording when it's a proper onboarding vs when the user just wants to add a language.
    private boolean mUserAlreadyOnboarded;

    @BindView(R.id.user_name_text_view) TextView mNameTextView;
    @BindView(R.id.call_to_action_textview) TextView mCallToActionTextView;
    @BindView(R.id.wheel_picker) WheelPicker mWheelPicker;
    @BindView(R.id.empty_wheel_textview) TextView mEmptyWheelTextview;
    @BindView(R.id.flag_image_view) ImageView mFlagImageView;
    @BindView(R.id.progress_bar) ProgressBar mProgressBar;
    @BindView(R.id.start_learning_button) Button mStartLearningButton;

    private String mDisplayName = "";


    // A list of icon names (and another one for ids) that we'll maintain in sync with wheel selector language names.
    private List<String> mWheelDataIconNames;
    private List<Long> mWheelDataIds;

    // Butterknife stuff
    private Unbinder mUnbinder;


    // Loader stuff
    private static final int LANGUAGES_LOADER = 0;
    private static final String[] LANGUAGES_COLUMNS = {
            // In this case the id needs to be fully qualified with a table name, since
            // the content provider joins several tables.
            DbContract.LanguageEntry.TABLE_NAME + "." + DbContract.LanguageEntry._ID,
            DbContract.LanguageEntry.COLUMN_NAME,
            DbContract.LanguageEntry.COLUMN_ICON_NAME
    };

    public static final int COL_LANGUAGE_ID = 0;
    public static final int COL_LANGUAGE_NAME = 1;
    public static final int COL_LANGUAGE_ICON_NAME = 2;
    private long mUserId;

    public OnboardingFragment() {
        // Required empty public constructor
    }

    public static OnboardingFragment newInstance() {
        return new OnboardingFragment();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        getLoaderManager().initLoader(LANGUAGES_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        mUserAlreadyOnboarded = args != null && args.getBoolean(HomeFragment.FRAGMENT_ARG_ALREADY_ONBOARDED, false);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        mDisplayName = preferences.getString(LoginActivity.KEY_GOOGLE_GIVEN_NAME, null);
        mUserId = preferences.getLong(LoginActivity.KEY_USER_ID, 0);

        ((MainActivity) getActivity()).mLanguageDataLoaded = false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(LOG_TAG, "in onCreateView");
        View view = inflater.inflate(R.layout.fragment_onboarding, container, false);
        mUnbinder = ButterKnife.bind(this, view);

        // Adjust the wording when it's a returning user vs proper onboarding.
        if (mUserAlreadyOnboarded) {
            mNameTextView.setText(getResources().getString(R.string.user_greeting, mDisplayName));
            mCallToActionTextView.setText(getResources().getString(R.string.learn_next_pick_a_language));
        }
        else {
            mNameTextView.setText(getResources().getString(R.string.user_first_greeting, mDisplayName));
            mCallToActionTextView.setText(getResources().getString(R.string.onboarding_pick_a_language));
        }

        mWheelPicker.setOnItemSelectedListener(this);
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }


    @Override
    public void onResume() {
        Log.d(LOG_TAG, "in onResume");
        super.onResume();
        ((MainActivity) getActivity()).mLanguageDataLoaded = false;
        getLoaderManager().restartLoader(LANGUAGES_LOADER, null, this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onItemSelected(WheelPicker picker, Object data, int position) {
        Log.d(LOG_TAG, String.format("in onItemSelected, position: %d", position));

        // We could make a query to retrieve the flag icon and the id, I find this is less cumbersome.
        String iconName = mWheelDataIconNames.get(position);

        mFlagImageView.setImageResource(getResources()
                .getIdentifier(iconName, "drawable", getContext().getPackageName()));
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.d(LOG_TAG, "in onCreateLoader");
        Uri uri = DbContract.LanguageEntry.buildLanguagesNotLearnedByUserUri(mUserId);
        // We don't really care about the sort order but we want to be consistent.
        String sortOrder = DbContract.LanguageEntry.COLUMN_NAME + " ASC";
        return new CursorLoader(getActivity(), uri, LANGUAGES_COLUMNS, null, null, sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.d(LOG_TAG, "in onLoadFinished");
        // We parse data to put language names into a list and into a wheel.
        // We maintain a separate list for icon names that 'shares indices' with the wheel data.
        // We'll use it to retrieve the icon name for the selected language.
        List<String> wheelData = new ArrayList<>();
        mWheelDataIconNames = new ArrayList<>();
        mWheelDataIds = new ArrayList<>();
        if (data == null) {
            Log.d(LOG_TAG, "in onLoadFinished, cursor is null!");
            return;
        }
        while (data.moveToNext()) {
            wheelData.add(data.getString(COL_LANGUAGE_NAME));
            mWheelDataIconNames.add(data.getString(COL_LANGUAGE_ICON_NAME));
            mWheelDataIds.add(data.getLong(COL_LANGUAGE_ID));
        }

        mWheelPicker.setData(wheelData);

        // We want to be sure we have our own (=non-default) data in the wheel picker to avoid indexErrors.
        if (wheelData.size() > 0) {
            // In case there was a small misunderstanding previously, make sure we see the wheel.
            mWheelPicker.setVisibility(View.VISIBLE);
            mStartLearningButton.setVisibility(View.VISIBLE);
            mEmptyWheelTextview.setVisibility(View.GONE);

            // Set the flag icon to the current position.
            // The flag changes 'onItemSelected' but we need to initialize it.
            int position = mWheelPicker.getCurrentItemPosition();
            if (position == -1) {
                // This can happen when we arrive to this fragment using the back button.
                // The wheel does not have any current item so it returns -1.
                mWheelPicker.setSelectedItemPosition(0);
                position = 0;
            }
            String iconName = mWheelDataIconNames.get(position);
            mFlagImageView.setImageResource(getResources()
                    .getIdentifier(iconName, "drawable", getContext().getPackageName()));
        }
        else {
            Log.d(LOG_TAG, "No data in the wheel.");
            mWheelPicker.setVisibility(View.GONE);
            // That's important cuz we don't behave nice when you want to start learning NULL.
            mStartLearningButton.setVisibility(View.GONE);
            mEmptyWheelTextview.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.d(LOG_TAG, "in onLoaderReset");
        // onLoaderReset is invoked on fragment destruction, so we don't have a wheel picker no mo'!
        if (mWheelPicker != null) {
            mWheelPicker.setData(new ArrayList<String>());
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void onLanguageSelected(String languageSelectedName, long languageId);
    }

    @OnClick(R.id.start_learning_button)
    public void onStartLearning() {
        Log.d(LOG_TAG, "in onStartLearning");
        // 1. Add the language to the user's list of languages.
        // 2.download data for the language.
        // 3.launch the game with the right language... Only once the data is downloaded!

        if(!Utils.isNetworkAvailable(getActivity())) {
            Log.d(LOG_TAG, "Canceling startLearning because there's no network!");
            Toast.makeText(getContext(), getString(R.string.no_network_cant_add_language), Toast.LENGTH_LONG).show();
            return;
        }

        int position = mWheelPicker.getCurrentItemPosition();
        final long languageSelectedId = mWheelDataIds.get(position);
        final String languageSelectedName = (String) mWheelPicker.getData().get(position);
        final ContentValues contentValues = new ContentValues();
        contentValues.put(DbContract.UserLanguageEntry.COLUMN_LANGUAGE_ID, languageSelectedId);
        contentValues.put(DbContract.UserLanguageEntry.COLUMN_USER_ID, mUserId);
        contentValues.put(DbContract.UserLanguageEntry.COLUMN_CREATED_TIMESTAMP, System.currentTimeMillis() / 1000);

        FetchLanguageTask fetchLanguageTask = new FetchLanguageTask(
                getActivity(), languageSelectedName, mProgressBar, new FetchLanguageTask.OnPostExecuteCallback(){

            @Override
            public void onPostExecute() {
                // We associate the language with the user ONLY if we successfully downloaded the data.
                getContext().getContentResolver().insert(DbContract.UserLanguageEntry.CONTENT_URI, contentValues);

                mProgressBar.setProgress(100);
                mProgressBar.setVisibility(View.GONE);

                ((MainActivity) getActivity()).mLanguageDataLoaded = true;

                mListener.onLanguageSelected(languageSelectedName, languageSelectedId);
            }
        });

        fetchLanguageTask.execute();
    }
}
