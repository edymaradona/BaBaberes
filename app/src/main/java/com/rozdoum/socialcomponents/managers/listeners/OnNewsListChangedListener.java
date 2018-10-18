package com.rozdoum.socialcomponents.managers.listeners;

import com.rozdoum.socialcomponents.model.NewsListResult;
import com.rozdoum.socialcomponents.model.PostListResult;

public interface OnNewsListChangedListener<News> {

    public void onListChanged(NewsListResult result);

    void onCanceled(String message);
}
