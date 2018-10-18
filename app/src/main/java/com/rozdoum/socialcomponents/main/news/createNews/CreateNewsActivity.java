package com.rozdoum.socialcomponents.main.news.createNews;

import android.support.annotation.NonNull;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.rozdoum.socialcomponents.R;
import com.rozdoum.socialcomponents.main.news.BaseCreateNewsActivity;

public class CreateNewsActivity extends BaseCreateNewsActivity<CreateNewsView, CreateNewsPresenter> implements CreateNewsView {
    public static final int CREATE_NEW_NEWS_REQUEST = 11;

    @NonNull
    @Override
    public CreateNewsPresenter createPresenter() {
        if (presenter == null) {
            return new CreateNewsPresenter(this);
        }
        return presenter;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.create_post_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.post:
                presenter.doSaveNews(imageUri);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
