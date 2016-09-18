package com.belenos.udacitycapstone;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.text.Editable;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link GameFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link GameFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class GameFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String LOG_TAG = GameFragment.class.getSimpleName();
    private static Button mSwitchAccountButton;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mLanguageName;


    @BindView(R.id.what_to_do) TextView mWhatToDoTextview;
    @BindView(R.id.game_cardview) CardView mGameCardview;
    @BindView(R.id.to_translate_textview) TextView mToTranslateTextview;
    @BindView(R.id.translated_textview) TextView mTranslatedTextview;
    @BindView(R.id.answer_edittext) EditText mAnswerEdittext;

    private OnFragmentInteractionListener mListener;

    private int mCardStateIndex;
    private int mCardState;
    // The possible values for the card state. Note order *does* matter.
    private static final int FRONT_SHOWN_STATE = 0;
    private static final int FLIPPING_FRONT_SHOWN_STATE = 1;
    private static final int FLIPPING_BACK_SHOWN_STATE = 2;
    private static final int BACK_SHOWN_STATE = 3;
    private static final Integer[] STATE_SEQUENCE = {FRONT_SHOWN_STATE, FLIPPING_FRONT_SHOWN_STATE, FLIPPING_BACK_SHOWN_STATE, BACK_SHOWN_STATE, FLIPPING_BACK_SHOWN_STATE, FLIPPING_FRONT_SHOWN_STATE};


    private String mWordToTranslate = "I eat";
    private String mWordTranslated = "Je mange";


    public GameFragment() {
        // Required empty public constructor
    }

    public static GameFragment newInstance(Button switchAccountButton) {
        GameFragment fragment = new GameFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        mSwitchAccountButton = switchAccountButton;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOG_TAG, "in onCreate");
        if (getArguments() != null) {
            Log.d(LOG_TAG, "in onCreate: we have args!");
            mParam1 = getArguments().getString(ARG_PARAM1);
            mLanguageName = getArguments().getString(MainActivity.FRAGMENT_ARG_LANGUAGE_NAME);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_game, container, false);

        ButterKnife.bind(this, view);

        mWhatToDoTextview.setText(getResources().getString(R.string.translate_into_language, mLanguageName));
        mSwitchAccountButton.setVisibility(View.GONE);
        // TODO: the visibility will depend on whether the user has seen the word before.
        mToTranslateTextview.setText(mWordToTranslate);
        mTranslatedTextview.setText(mWordTranslated);
        mTranslatedTextview.setVisibility(View.INVISIBLE);
        mCardState = FRONT_SHOWN_STATE;
        return view;
    }

    /**
     * The flip card method launches the animations and sets up listeners that maintain the card state and contain some game logic.
     */
    @OnClick(R.id.game_cardview)
    public void flipCard() {
        Log.d(LOG_TAG, "in flipCard");
        if (cardIsFlipping()) {
            // We dont want to do anything. The user will only be able to click once the animation is finished.
            return;
        }
        // We want to increment the state *right now*
        // if we wait for onAnimationStart a second click event (double tap from the user) could already be handled.
        incrementState();
        Animator animatorOut = AnimatorInflater.loadAnimator(getContext(), R.animator.card_flip_out);
        animatorOut.setTarget(mGameCardview);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorOut.addListener(new Animator.AnimatorListener() {
                                    @Override
                                    public void onAnimationStart(Animator animator) {
                                        Log.d(LOG_TAG, String.format("in onAnimationStart, state=%d", mCardState));
                                        if (!cardIsFlipping())
                                            throw new AssertionError();
                                    }

                                    @Override
                                    public void onAnimationEnd(Animator animator) {
                                        incrementState();
                                        Log.d(LOG_TAG, String.format("in onAnimationEnd 1, state after=%d", mCardState));
                                        mTranslatedTextview.setVisibility(mCardState == FLIPPING_BACK_SHOWN_STATE ? View.VISIBLE : View.INVISIBLE);
                                    }

                                    @Override
                                    public void onAnimationCancel(Animator animator) {
                                    }

                                    @Override
                                    public void onAnimationRepeat(Animator animator) {
                                    }
                                }
        );
        Animator animatorIn = AnimatorInflater.loadAnimator(getContext(), R.animator.card_flip_in);
        animatorIn.setTarget(mGameCardview);

        animatorIn.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                incrementState();
                Log.d(LOG_TAG, String.format("in onAnimationEnd 2, state after=%d", mCardState));
                // Disable text entry when the user sees the solution.
                if (mCardState == BACK_SHOWN_STATE) {
                    mAnswerEdittext.setEnabled(false);
                    mAnswerEdittext.setInputType(InputType.TYPE_NULL);
                }
                else if (mCardState == FRONT_SHOWN_STATE) {
                    mAnswerEdittext.setEnabled(true);
                    mAnswerEdittext.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                }
                else {
                    // We screwed up the state! The user should still be able to enter text because the experience of NOT being able to when you should is really terrible.
                    Log.e(LOG_TAG, "in onAnimationEnd 2, but in a bad bad state.");
                    mAnswerEdittext.setEnabled(true);
                    mAnswerEdittext.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                }

            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        Animator[] animatorList = {animatorOut, animatorIn};
        animatorSet.playSequentially(animatorList);
        animatorSet.start();
    }

    private boolean cardIsFlipping() {
        return (mCardState == FLIPPING_FRONT_SHOWN_STATE || mCardState == FLIPPING_BACK_SHOWN_STATE);
    }

    /**
     * Change the card state to the next one.
     */
    private void incrementState() {
        mCardStateIndex = (mCardStateIndex + 1) % STATE_SEQUENCE.length;
        mCardState = STATE_SEQUENCE[mCardStateIndex];
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @OnClick(R.id.check_answer_button)
    public void checkUserAnswer() {
        String userAnswer = mAnswerEdittext.getText().toString();

        // Later on we could imagine having a more tolerant system to check the result, for instance correct if levenstein distance < 1.
        if (userAnswer.equals(mWordTranslated)) {
            Toast.makeText(getContext(), getString(R.string.answer_correct_toast), Toast.LENGTH_LONG).show();

        } else {
            Toast.makeText(getContext(), R.string.answer_incorrect_toast, Toast.LENGTH_LONG).show();
        }
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
    }
}
