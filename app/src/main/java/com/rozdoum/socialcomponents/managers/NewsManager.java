package com.rozdoum.socialcomponents.managers;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.firebase.database.ValueEventListener;
import com.rozdoum.socialcomponents.main.interactors.FollowInteractor;
import com.rozdoum.socialcomponents.main.interactors.NewsInteractor;
import com.rozdoum.socialcomponents.managers.listeners.OnDataChangedListener;
import com.rozdoum.socialcomponents.managers.listeners.OnNewsChangedListener;
import com.rozdoum.socialcomponents.managers.listeners.OnNewsCreatedListener;
import com.rozdoum.socialcomponents.managers.listeners.OnNewsListChangedListener;
import com.rozdoum.socialcomponents.managers.listeners.OnObjectExistListener;
import com.rozdoum.socialcomponents.managers.listeners.OnPostChangedListener;
import com.rozdoum.socialcomponents.managers.listeners.OnPostCreatedListener;
import com.rozdoum.socialcomponents.managers.listeners.OnTaskCompleteListener;
import com.rozdoum.socialcomponents.model.FollowingPost;
import com.rozdoum.socialcomponents.model.Like;
import com.rozdoum.socialcomponents.model.News;
import com.rozdoum.socialcomponents.model.Post;

public class NewsManager extends FirebaseListenersManager {

    private static final String TAG = NewsManager.class.getSimpleName();
    private static NewsManager instance;
    private int newNewsCounter = 0;
    private NewsCounterWatcher newsCounterWatcher;
    private NewsInteractor newsInteractor;

    private Context context;

    public static NewsManager getInstance(Context context) {
        if (instance == null) {
            instance = new NewsManager(context);
        }

        return instance;
    }

    private NewsManager(Context context) {
        this.context = context;
        newsInteractor = NewsInteractor.getInstance(context);
    }

    public void createOrUpdateNews(News news) {
        try {
            newsInteractor.createOrUpdateNews(news);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public void getNewsList(OnNewsListChangedListener<News> onDataChangedListener, long date) {
        newsInteractor.getNewsList(onDataChangedListener, date);
    }

    public void getNewsListByUser(OnDataChangedListener<News> onDataChangedListener, String userId) {
        newsInteractor.getNewsListByUser(onDataChangedListener, userId);
    }

    public void getNews(Context context, String newsId, OnNewsChangedListener onNewsChangedListener) {
        ValueEventListener valueEventListener = newsInteractor.getNews(newsId, onNewsChangedListener);
        addListenerToMap(context, valueEventListener);
    }

    public void getSingleNewsValue(String newsId, OnNewsChangedListener onNewsChangedListener) {
        newsInteractor.getSingleNews(newsId, onNewsChangedListener);
    }

    public void createOrUpdateNewsWithImage(Uri imageUri, final OnNewsCreatedListener onNewsCreatedListener, final News news) {
        newsInteractor.createOrUpdateNewsWithImage(imageUri, onNewsCreatedListener, news);
    }

    public void removeNews(final News news, final OnTaskCompleteListener onTaskCompleteListener) {
        newsInteractor.removeNews(news, onTaskCompleteListener);
    }

    public void addComplain(News news) {
        newsInteractor.addComplainToNews(news);
    }

    public void hasCurrentUserLike(Context activityContext, String newsId, String userId, final OnObjectExistListener<Like> onObjectExistListener) {
        ValueEventListener valueEventListener = newsInteractor.hasCurrentUserLike(newsId, userId, onObjectExistListener);
        addListenerToMap(activityContext, valueEventListener);
    }

    public void hasCurrentUserLikeSingleValue(String newsId, String userId, final OnObjectExistListener<Like> onObjectExistListener) {
        newsInteractor.hasCurrentUserLikeSingleValue(newsId, userId, onObjectExistListener);
    }

    public void isNewsExistSingleValue(String newsId, final OnObjectExistListener<News> onObjectExistListener) {
        newsInteractor.isNewsExistSingleValue(newsId, onObjectExistListener);
    }

    public void incrementWatchersCount(String newsId) {
        newsInteractor.incrementWatchersCount(newsId);
    }

    public void incrementNewNewsCounter() {
        newNewsCounter++;
        notifyNewsCounterWatcher();
    }

    public void clearNewNewsCounter() {
        newNewsCounter = 0;
        notifyNewsCounterWatcher();
    }

    public int getNewNewsCounter() {
        return newNewsCounter;
    }

    public void setNewsCounterWatcher(NewsManager.NewsCounterWatcher newsCounterWatcher) {
        this.newsCounterWatcher = newsCounterWatcher;
    }

    private void notifyNewsCounterWatcher() {
        if (newsCounterWatcher != null) {
            newsCounterWatcher.onNewsCounterChanged(newNewsCounter);
        }
    }

    public void searchByTitle(String searchText, OnDataChangedListener<News> onDataChangedListener) {
        closeListeners(context);
        ValueEventListener valueEventListener = newsInteractor.searchNewsByTitle(searchText, onDataChangedListener);
        addListenerToMap(context, valueEventListener);
    }

    public void filterByLikes(int limit, OnDataChangedListener<News> onDataChangedListener) {
        closeListeners(context);
        ValueEventListener valueEventListener = newsInteractor.filterNewsByLikes(limit, onDataChangedListener);
        addListenerToMap(context, valueEventListener);
    }

    public interface NewsCounterWatcher {
        void onNewsCounterChanged(int newValue);
    }
}
