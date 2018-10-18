package com.rozdoum.socialcomponents.adapters;

import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.rozdoum.socialcomponents.R;
import com.rozdoum.socialcomponents.adapters.holders.LoadViewHolder;
import com.rozdoum.socialcomponents.adapters.holders.NewsViewHolder;
import com.rozdoum.socialcomponents.controllers.LikeNewsController;
import com.rozdoum.socialcomponents.enums.ItemType;
import com.rozdoum.socialcomponents.main.main.MainActivity;
import com.rozdoum.socialcomponents.managers.NewsManager;
import com.rozdoum.socialcomponents.managers.listeners.OnNewsListChangedListener;
import com.rozdoum.socialcomponents.model.News;
import com.rozdoum.socialcomponents.model.NewsListResult;
import com.rozdoum.socialcomponents.utils.PreferencesUtil;

import java.util.List;

public class NewsAdapter extends BaseNewsAdapter {
    public static final String TAG = NewsAdapter.class.getSimpleName();

    private NewsAdapter.Callback callback;
    private boolean isLoading = false;
    private boolean isMoreDataAvailable = true;
    private long lastLoadedItemCreatedDate;
    private SwipeRefreshLayout swipeContainer;
    private MainActivity mainActivity;

    public NewsAdapter(final MainActivity activity, SwipeRefreshLayout swipeContainer) {
        super(activity);
        this.mainActivity = activity;
        this.swipeContainer = swipeContainer;
        initRefreshLayout();
        setHasStableIds(true);
    }

    private void initRefreshLayout() {
        if (swipeContainer != null) {
            this.swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    onRefreshAction();
                }
            });
        }
    }

    private void onRefreshAction() {
        if (activity.hasInternetConnection()) {
            loadFirstPage();
            cleanSelectedNewsInformation();
        } else {
            swipeContainer.setRefreshing(false);
            mainActivity.showFloatButtonRelatedSnackBar(R.string.internet_connection_failed);
        }
    }

    public void setCallback(NewsAdapter.Callback callback) {
        this.callback = callback;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == ItemType.ITEM.getTypeCode()) {
            return new NewsViewHolder(inflater.inflate(R.layout.news_item_list_view, parent, false),
                    createOnClickListener(), activity);
        } else {
            return new LoadViewHolder(inflater.inflate(R.layout.loading_view, parent, false));
        }
    }

    private NewsViewHolder.OnClickListener createOnClickListener() {
        return new NewsViewHolder.OnClickListener() {
            @Override
            public void onItemClick(int position, View view) {
                if (callback != null) {
                    selectedNewsPosition = position;
                    callback.onItemClick(getItemByPosition(position), view);
                }
            }

            @Override
            public void onLikeClick(LikeNewsController likeController, int position) {
                News news = getItemByPosition(position);
                likeController.handleLikeClickAction(activity, news);
            }

            @Override
            public void onAuthorClick(int position, View view) {
                if (callback != null) {
                    callback.onAuthorClick(getItemByPosition(position).getAuthorId(), view);
                }
            }
        };
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (position >= getItemCount() - 1 && isMoreDataAvailable && !isLoading) {
            Handler mHandler = activity.getWindow().getDecorView().getHandler();
            mHandler.post(new Runnable() {
                public void run() {
                    //change adapter contents
                    if (activity.hasInternetConnection()) {
                        isLoading = true;
                        newsList.add(new News(ItemType.LOAD));
                        notifyItemInserted(newsList.size());
                        loadNext(lastLoadedItemCreatedDate - 1);
                    } else {
                        mainActivity.showFloatButtonRelatedSnackBar(R.string.internet_connection_failed);
                    }
                }
            });


        }

        if (getItemViewType(position) != ItemType.LOAD.getTypeCode()) {
            ((NewsViewHolder) holder).bindData(newsList.get(position));
        }
    }

    private void addList(List<News> list) {
        this.newsList.addAll(list);
        notifyDataSetChanged();
        isLoading = false;
    }

    public void loadFirstPage() {
        loadNext(0);
        NewsManager.getInstance(mainActivity.getApplicationContext()).clearNewNewsCounter();
    }

    private void loadNext(final long nextItemCreatedDate) {

        if (!PreferencesUtil.isNewsWasLoadedAtLeastOnce(mainActivity) && !activity.hasInternetConnection()) {
            mainActivity.showFloatButtonRelatedSnackBar(R.string.internet_connection_failed);
            hideProgress();
            callback.onListLoadingFinished();
            return;
        }

        OnNewsListChangedListener<News> onNewsDataChangedListener = new OnNewsListChangedListener<News>() {
            @Override
            public void onListChanged(NewsListResult result) {
                lastLoadedItemCreatedDate = result.getLastItemCreatedDate();
                isMoreDataAvailable = result.isMoreDataAvailable();
                List<News> list = result.getNews();

                if (nextItemCreatedDate == 0) {
                    newsList.clear();
                    notifyDataSetChanged();
                    swipeContainer.setRefreshing(false);
                }

                hideProgress();

                if (!list.isEmpty()) {
                    addList(list);

                    if (!PreferencesUtil.isNewsWasLoadedAtLeastOnce(mainActivity)) {
                        PreferencesUtil.setNewsWasLoadedAtLeastOnce(mainActivity, true);
                    }
                } else {
                    isLoading = false;
                }

                callback.onListLoadingFinished();
            }

            @Override
            public void onCanceled(String message) {
                callback.onCanceled(message);
            }
        };

        NewsManager.getInstance(activity).getNewsList(onNewsDataChangedListener, nextItemCreatedDate);
    }

    private void hideProgress() {
        if (!newsList.isEmpty() && getItemViewType(newsList.size() - 1) == ItemType.LOAD.getTypeCode()) {
            newsList.remove(newsList.size() - 1);
            notifyItemRemoved(newsList.size() - 1);
        }
    }

    public void removeSelectedNews() {
        newsList.remove(selectedNewsPosition);
        notifyItemRemoved(selectedNewsPosition);
    }

    @Override
    public long getItemId(int position) {
        return getItemByPosition(position).getId().hashCode();
    }

    public interface Callback {
        void onItemClick(News news, View view);
        void onListLoadingFinished();
        void onAuthorClick(String authorId, View view);
        void onCanceled(String message);
    }
}