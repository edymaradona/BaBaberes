package com.rozdoum.socialcomponents.main.news;

import android.net.Uri;

import com.rozdoum.socialcomponents.main.pickImageBase.PickImageView;

public interface BaseCreateNewsPostView extends PickImageView {

    void setDescriptionError(String error);

    void setTitleError(String error);

    String getTitleText();

    String getDescriptionText();

    void requestImageViewFocus();

    void onNewsSavedSuccess();

    Uri getImageUri();
}