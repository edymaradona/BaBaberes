package com.rozdoum.socialcomponents.main.search.news;

import android.content.Context;

import com.rozdoum.socialcomponents.main.base.BasePresenter;
import com.rozdoum.socialcomponents.main.search.posts.SearchPostsView;
import com.rozdoum.socialcomponents.managers.NewsManager;
import com.rozdoum.socialcomponents.managers.PostManager;
import com.rozdoum.socialcomponents.model.News;
import com.rozdoum.socialcomponents.model.Post;
import com.rozdoum.socialcomponents.utils.LogUtil;

import java.util.List;

public class SearchNewsPresenter extends BasePresenter<SearchNewsView> {
    public static final int LIMIT_POSTS_FILTERED_BY_LIKES = 10;
    private Context context;
    private NewsManager newsManager;

    public SearchNewsPresenter(Context context) {
        super(context);
        this.context = context;
        newsManager = NewsManager.getInstance(context);
    }

    public void search() {
        search("");
    }

    public void search(String searchText) {
        if (checkInternetConnection()) {
            if (searchText.isEmpty()) {
                filterByLikes();
            } else {
                ifViewAttached(SearchNewsView::showLocalProgress);
                newsManager.searchByTitle(searchText, this::handleSearchResult);
            }

        } else {
            ifViewAttached(SearchNewsView::hideLocalProgress);
        }
    }

    private void filterByLikes() {
        if (checkInternetConnection()) {
            ifViewAttached(SearchNewsView::showLocalProgress);
            newsManager.filterByLikes(LIMIT_POSTS_FILTERED_BY_LIKES, this::handleSearchResult);
        } else {
            ifViewAttached(SearchNewsView::hideLocalProgress);
        }
    }

    private void handleSearchResult(List<News> list) {
        ifViewAttached(view -> {
            view.hideLocalProgress();
            view.onSearchResultsReady(list);

            if (list.isEmpty()) {
                view.showEmptyListLayout();
            }

            LogUtil.logDebug(TAG, "found items count: " + list.size());
        });
    }
}
