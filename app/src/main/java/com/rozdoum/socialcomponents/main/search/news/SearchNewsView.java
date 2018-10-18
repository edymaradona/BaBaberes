package com.rozdoum.socialcomponents.main.search.news;

import com.rozdoum.socialcomponents.main.base.BaseFragmentView;
import com.rozdoum.socialcomponents.model.News;
import com.rozdoum.socialcomponents.model.Post;

import java.util.List;

public interface SearchNewsView extends BaseFragmentView {
    void onSearchResultsReady(List<News> news);

    void showLocalProgress();

    void hideLocalProgress();

    void showEmptyListLayout();
}