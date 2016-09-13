package com.belenos.udacitycapstone;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.aigestudio.wheelpicker.WheelPicker;

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
public class OnboardingFragment extends Fragment implements WheelPicker.OnItemSelectedListener {

    private static final String LOG_TAG = OnboardingFragment.class.getSimpleName();
    private OnFragmentInteractionListener mListener;

    @BindView(R.id.user_name_text_view) TextView mNameTextView;
    @BindView(R.id.wheel_picker) WheelPicker mWheelPicker;
    @BindView(R.id.flag_image_view) ImageView mFlagImageView;

    private String mDisplayName = "";


    // Butterknife stuff
    private Unbinder mUnbinder;
    private String mLanguageSelected;

    public OnboardingFragment() {
        // Required empty public constructor
    }

    public static OnboardingFragment newInstance() {
        return new OnboardingFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDisplayName = getArguments().getString(LoginActivity.KEY_GOOGLE_GIVEN_NAME);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(LOG_TAG, "in onCreateView");
        View view = inflater.inflate(R.layout.fragment_onboarding, container, false);
        mUnbinder = ButterKnife.bind(this, view);

        mNameTextView.setText(getResources().getString(R.string.user_greeting, mDisplayName));

        List<String> data = new ArrayList<>();
        data.add("French");
        data.add("German");
        data.add("Romanian");
        data.add("Romanian");
        data.add("Romanian");
        data.add("Romanian");
        data.add("Romanian");

        mWheelPicker.setOnItemSelectedListener(this);
        mWheelPicker.setData(data);
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
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onItemSelected(WheelPicker picker, Object data, int position) {
        Log.d(LOG_TAG, "in onItemSelected");
        Log.d(LOG_TAG, String.format("position: %d", position));
        mLanguageSelected = (String) data;
        mFlagImageView.setImageDrawable(getResources().getDrawable(R.drawable.fr));
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
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
        void onLanguageSelected(Integer languageId);
    }


    @OnClick(R.id.start_learning_button)
    public void onStartLearning() {
        // TODO: add language to user.
        // TODO: download data for the language.
        // TODO: launch the game with the right language.

        // TODO: get the id instead of the language string.
        Integer mLanguageSelectedId = 2;
        mListener.onLanguageSelected(mLanguageSelectedId);
    }
}
