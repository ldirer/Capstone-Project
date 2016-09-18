package com.belenos.udacitycapstone;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Optional;

public class LanguagesAdapter extends RecyclerView.Adapter<LanguagesAdapter.LanguagesAdapterViewHolder> {

    private static final String LOG_TAG = LanguagesAdapter.class.getSimpleName();
    private static final int ADD_A_LANGUAGE_VIEW_TYPE = 100;
    private static final int REGULAR_ITEM_VIEW_TYPE = 200;
    private final Context mContext;
    private Cursor mCursor;
    private HomeFragment mFragment;


    public LanguagesAdapter(Context context, HomeFragment fragment) {
        super();
        mContext = context;
        mFragment = fragment;
    }

    /**
     * Inflate a *single item* and return a ViewHolder for it.
     */
    @Override
    public LanguagesAdapter.LanguagesAdapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        int layoutId = -1;
        switch (viewType) {
            case REGULAR_ITEM_VIEW_TYPE:
                layoutId = R.layout.language_item;
                break;
            case ADD_A_LANGUAGE_VIEW_TYPE:
                // We might want to change this in the future.
                layoutId = R.layout.add_new_language_item;
                break;
        }

        // Inflate layout and create (& return!) ViewHolder.
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);

        return new LanguagesAdapterViewHolder(view);
    }


    @Override
    public void onBindViewHolder(LanguagesAdapter.LanguagesAdapterViewHolder holder, int position) {
        // Set values to the ViewHolder attributes (that are TextView, ImageView...).
        switch (getItemViewType(position)) {
            case ADD_A_LANGUAGE_VIEW_TYPE:
                // Nothing to do.
                return;
            default:
        }

        mCursor.moveToPosition(position);
        String iconName = mCursor.getString(HomeFragment.COL_LANGUAGES_FOR_USER_ICON_NAME);
        String language = mCursor.getString(HomeFragment.COL_LANGUAGES_FOR_USER_NAME);

        holder.mLanguageTextView.setText(language);
        holder.mFlagImageView.setImageResource(
                mContext.getResources()
                        .getIdentifier(iconName, "drawable", mContext.getPackageName()));
    }

    @Override
    public int getItemCount() {
        Log.d(LOG_TAG, "in getItemCount");
        // We always return one more because we want a 'start learning a new language' item.
        if (null == mCursor) return 1;
        return mCursor.getCount() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        if (position >= getItemCount() - 1) {
            return ADD_A_LANGUAGE_VIEW_TYPE;
        } else {
            return REGULAR_ITEM_VIEW_TYPE;
        }
    }

    public void swapCursor(Cursor newCursor) {
        mCursor = newCursor;
        notifyDataSetChanged();
        // TODO: add an empty view
        //  mEmptyView.setVisibility(getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    public Cursor getCursor() {
        return mCursor;
    }

    public class LanguagesAdapterViewHolder extends RecyclerView.ViewHolder {

        @Nullable @BindView(R.id.language_item_flag_imageview) ImageView mFlagImageView;
        @Nullable @BindView(R.id.language_item_language_textview) TextView mLanguageTextView;
        @Nullable @BindView(R.id.language_item_resume_button) Button mResumeButton;


        public LanguagesAdapterViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        @Optional
        @OnClick(R.id.add_a_language)
        public void addLanguage() {
            Log.d(LOG_TAG, "in addLanguage");
            // Use the fragment interaction interface to communicate with the activity.
            ((MainActivity) mFragment.getActivity()).onAddLanguage();
        }

    }
}
