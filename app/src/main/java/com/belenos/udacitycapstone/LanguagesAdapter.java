package com.belenos.udacitycapstone;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class LanguagesAdapter extends RecyclerView.Adapter<LanguagesAdapter.LanguagesAdapterViewHolder>{

    private static final String LOG_TAG = LanguagesAdapter.class.getSimpleName();
    List<String> placeholderData = new ArrayList<>();

    public LanguagesAdapter() {
        super();
        placeholderData.add("French");
        placeholderData.add("German");
        placeholderData.add("Romanian");
        placeholderData.add("Spanish");
    }

    @Override
    public LanguagesAdapter.LanguagesAdapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        // Inflate layout and create (& return!) ViewHolder.
        //TODO: here we want to have a special case for the 'Start learning a new language' item.
        int layoutId = R.layout.language_item;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);

        return new LanguagesAdapterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(LanguagesAdapter.LanguagesAdapterViewHolder holder, int position) {
        // Set values to the ViewHolder attributes (that are TextView, ImageView...).
        String language = placeholderData.get(position);
        holder.mLanguageTextView.setText(language);
        holder.mFlagImageView.setImageResource(R.drawable.ro);
    }

    @Override
    public int getItemCount() {
        Log.d(LOG_TAG, String.format("in getItemCount: count=%d", placeholderData.size()));
        return placeholderData.size();
    }

    public class LanguagesAdapterViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.language_item_flag_imageview) ImageView mFlagImageView;
        @BindView(R.id.language_item_language_textview) TextView mLanguageTextView;
        @BindView(R.id.language_item_resume_button) Button mResumeButton;

        public LanguagesAdapterViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
