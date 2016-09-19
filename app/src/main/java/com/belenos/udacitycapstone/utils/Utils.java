package com.belenos.udacitycapstone.utils;

public class Utils {

    private static long mFakeItIndex = 0;
    public static long getNextWordId(Long mWordToTranslateId, long mLanguageId, long mUserId) {
        // TODO: smart stuff to get the id of the next word we want to show.
        mFakeItIndex += 1;
        return mFakeItIndex;
    }
}
