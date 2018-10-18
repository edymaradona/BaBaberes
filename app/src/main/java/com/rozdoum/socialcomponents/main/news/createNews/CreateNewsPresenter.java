package com.rozdoum.socialcomponents.main.news.createNews;

import android.content.Context;

import com.google.firebase.auth.FirebaseAuth;
import com.rozdoum.socialcomponents.R;
import com.rozdoum.socialcomponents.main.news.BaseCreateNewsPresenter;
import com.rozdoum.socialcomponents.model.News;

public class CreateNewsPresenter extends BaseCreateNewsPresenter<CreateNewsView> {

    public CreateNewsPresenter(Context context) {
        super(context);
    }

    @Override
    protected int getSaveFailMessage() {
        return R.string.error_fail_create_post;
    }

    @Override
    protected void saveNews(String title, String description) {
        ifViewAttached(view -> {
            view.showProgress(R.string.message_creating_news);
            News news = new News();
            news.setTitle(title);
            news.setDescription(description);
            news.setAuthorId(FirebaseAuth.getInstance().getCurrentUser().getUid());
            newsManager.createOrUpdateNewsWithImage(view.getImageUri(), this, news);
        });
    }

    @Override
    protected boolean isImageRequired() {
        return true;
    }
}
