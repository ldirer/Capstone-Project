package com.belenos.udacitycapstone;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.belenos.udacitycapstone.data.DbContract;
import com.belenos.udacitycapstone.utils.TrackedFragment;

import butterknife.BindView;
import butterknife.ButterKnife;


/**
 * A fragment to show the languages the user has started playing.
 * The user can start a new language or resume playing another one.
 * The user can also login with a different Google account should he wish to.
 */
public class HomeFragment extends TrackedFragment implements LoaderManager.LoaderCallbacks<Cursor>{

    private static final String LOG_TAG = HomeFragment.class.getSimpleName();
    public static final String FRAGMENT_ARG_ALREADY_ONBOARDED = "ALREADY_ONBOARDED";


    // Not actually using this listener. I'm leaving it because it's a nice pattern to be aware of.
    private OnFragmentInteractionListener mListener;
    private long mUserId;


    @BindView(R.id.languages_recyclerview) RecyclerView mRecyclerView;
    private LanguagesAdapter mUserLanguagesAdapter;

    // Loader stuff
    private static final int LANGUAGES_FOR_USER_LOADER = 0;
    private static final String[] LANGUAGES_FOR_USER_COLUMNS = {
            // In this case the id and name need to be fully qualified with a table name, since
            // the content provider joins several tables.
            DbContract.LanguageEntry.TABLE_NAME + "." + DbContract.LanguageEntry._ID,
            DbContract.LanguageEntry.TABLE_NAME + "." + DbContract.LanguageEntry.COLUMN_NAME,
            DbContract.LanguageEntry.COLUMN_ICON_NAME
    };

    public static final int COL_LANGUAGES_FOR_USER_ID = 0;
    public static final int COL_LANGUAGES_FOR_USER_NAME = 1;
    public static final int COL_LANGUAGES_FOR_USER_ICON_NAME = 2;




    public HomeFragment() {
        // Required empty public constructor
    }

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        getLoaderManager().initLoader(LANGUAGES_FOR_USER_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        mUserId = preferences.getLong(LoginActivity.KEY_USER_ID, 0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        ButterKnife.bind(this, view);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(layoutManager);
        mUserLanguagesAdapter = new LanguagesAdapter(this.getContext(), this);
        mRecyclerView.setAdapter(mUserLanguagesAdapter);

        return view;
    }


    @Override
    public void onResume() {
        super.onResume();
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
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.d(LOG_TAG, "in onCreateLoader");
        Uri uri = DbContract.UserEntry.buildLanguagesForUserUri(mUserId);
        // We don't really care about the sort order but we want to be consistent.
        String sortOrder = DbContract.LanguageEntry.TABLE_NAME + "." + DbContract.LanguageEntry.COLUMN_NAME + " ASC";
        return new CursorLoader(getActivity(), uri, LANGUAGES_FOR_USER_COLUMNS, null, null, sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.d(LOG_TAG, "in onLoadFinished");
        mUserLanguagesAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.d(LOG_TAG, "in onLoaderReset");
        mUserLanguagesAdapter.swapCursor(null);

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
        void onAddLanguage();
    }
}
