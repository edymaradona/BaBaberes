package com.rozdoum.socialcomponents.main.news;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.StringRes;
import android.text.TextUtils;

import com.rozdoum.socialcomponents.R;
import com.rozdoum.socialcomponents.main.pickImageBase.PickImagePresenter;
import com.rozdoum.socialcomponents.managers.NewsManager;
import com.rozdoum.socialcomponents.managers.listeners.OnNewsCreatedListener;
import com.rozdoum.socialcomponents.utils.LogUtil;
import com.rozdoum.socialcomponents.utils.ValidationUtil;

public abstract class BaseCreateNewsPresenter<V extends BaseCreateNewsPostView> extends PickImagePresenter<V> implements OnNewsCreatedListener {

    protected boolean creatingNews = false;
    protected NewsManager newsManager;

    public BaseCreateNewsPresenter(Context context) {
        super(context);
        newsManager = NewsManager.getInstance(context);
    }

    @StringRes
    protected abstract int getSaveFailMessage();

    protected abstract void saveNews(final String title, final String description);

    protected abstract boolean isImageRequired();

    protected void attemptCreateNews(Uri imageUri) {
        // Reset errors.
        ifViewAttached(view -> {
            view.setTitleError(null);
            view.setDescriptionError(null);
            //view.setAlamatError(null);

            String title = view.getTitleText().trim();
            String description = view.getDescriptionText().trim();

            boolean cancel = false;

            if (TextUtils.isEmpty(description)) {
                view.setDescriptionError(context.getString(R.string.warning_empty_description));
                cancel = true;
            }

            if (TextUtils.isEmpty(title)) {
                view.setTitleError(context.getString(R.string.warning_empty_title));
                cancel = true;
            } else if (!ValidationUtil.isPostTitleValid(title)) {
                view.setTitleError(context.getString(R.string.error_post_title_length));
                cancel = true;
            }

            if (isImageRequired() && view.getImageUri() == null) {
                view.showWarningDialog(R.string.warning_empty_image);
                view.requestImageViewFocus();
                cancel = true;
            }

            if (!cancel) {
                creatingNews = true;
                view.hideKeyboard();
                saveNews(title, description);
            }
        });
    }

    public void doSaveNews(Uri imageUri) {
        if (!creatingNews) {
            if (hasInternetConnection()) {
                attemptCreateNews(imageUri);
            } else {
                ifViewAttached(view -> view.showSnackBar(R.string.internet_connection_failed));
            }
        }
    }

    @Override
    public void onNewsSaved(boolean success) {
        creatingNews = false;

        ifViewAttached(view -> {
            view.hideProgress();
            if (success) {
                view.onNewsSavedSuccess();
                LogUtil.logDebug(TAG, "Post was saved");
            } else {
                view.showSnackBar(getSaveFailMessage());
                LogUtil.logDebug(TAG, "Failed to save a post");
            }
        });
    }

}
