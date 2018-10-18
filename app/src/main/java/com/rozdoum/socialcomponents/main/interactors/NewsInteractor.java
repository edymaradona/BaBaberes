package com.rozdoum.socialcomponents.main.interactors;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Query;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.UploadTask;
import com.rozdoum.socialcomponents.ApplicationHelper;
import com.rozdoum.socialcomponents.Constants;
import com.rozdoum.socialcomponents.R;
import com.rozdoum.socialcomponents.enums.UploadImagePrefix;
import com.rozdoum.socialcomponents.managers.DatabaseHelper;
import com.rozdoum.socialcomponents.managers.listeners.OnDataChangedListener;
import com.rozdoum.socialcomponents.managers.listeners.OnNewsChangedListener;
import com.rozdoum.socialcomponents.managers.listeners.OnNewsCreatedListener;
import com.rozdoum.socialcomponents.managers.listeners.OnNewsListChangedListener;
import com.rozdoum.socialcomponents.managers.listeners.OnObjectExistListener;
import com.rozdoum.socialcomponents.managers.listeners.OnPostChangedListener;
import com.rozdoum.socialcomponents.managers.listeners.OnPostCreatedListener;
import com.rozdoum.socialcomponents.managers.listeners.OnPostListChangedListener;
import com.rozdoum.socialcomponents.managers.listeners.OnTaskCompleteListener;
import com.rozdoum.socialcomponents.model.Like;
import com.rozdoum.socialcomponents.model.News;
import com.rozdoum.socialcomponents.model.NewsListResult;
import com.rozdoum.socialcomponents.model.Post;
import com.rozdoum.socialcomponents.model.PostListResult;
import com.rozdoum.socialcomponents.utils.ImageUtil;
import com.rozdoum.socialcomponents.utils.LogUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewsInteractor {
    private static final String TAG = NewsInteractor.class.getSimpleName();
    private static NewsInteractor instance;

    private DatabaseHelper databaseHelper;
    private Context context;

    public static NewsInteractor getInstance(Context context) {
        if (instance == null) {
            instance = new NewsInteractor(context);
        }

        return instance;
    }

    private NewsInteractor(Context context) {
        this.context = context;
        databaseHelper = ApplicationHelper.getDatabaseHelper();
    }

    public String generateNewsId() {
        return databaseHelper
                .getDatabaseReference()
                .child(DatabaseHelper.NEWS_DB_KEY)
                .push()
                .getKey();
    }

    public void createOrUpdateNews(News news) {
        try {
            Map<String, Object> newsValues = news.toMap();
            Map<String, Object> childUpdates = new HashMap<>();
            childUpdates.put("/" + DatabaseHelper.NEWS_DB_KEY + "/" + news.getId(), newsValues);

            databaseHelper.getDatabaseReference().updateChildren(childUpdates);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public Task<Void> removeNews(News news) {
        DatabaseReference newsRef = databaseHelper.getDatabaseReference().child(DatabaseHelper.NEWS_DB_KEY).child(news.getId());
        return newsRef.removeValue();
    }

    public void incrementWatchersCount(String newsId) {
        DatabaseReference newsRef = databaseHelper.getDatabaseReference().child(DatabaseHelper.NEWS_DB_KEY + "/" + newsId + "/watchersCount");
        newsRef.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                Integer currentValue = mutableData.getValue(Integer.class);
                if (currentValue == null) {
                    mutableData.setValue(1);
                } else {
                    mutableData.setValue(currentValue + 1);
                }

                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                LogUtil.logInfo(TAG, "Updating Watchers count transaction is completed.");
            }
        });
    }

    //////////////////////////////////////////

    public void getNewsList(final OnNewsListChangedListener<News> onDataChangedListener, long date) {
        DatabaseReference databaseReference = databaseHelper.getDatabaseReference().child(DatabaseHelper.NEWS_DB_KEY);
        Query newsQuery;
        if (date == 0) {
            newsQuery = databaseReference.limitToLast(Constants.News.NEWS_AMOUNT_ON_PAGE).orderByChild("createdDate");
        } else {
            newsQuery = databaseReference.limitToLast(Constants.News.NEWS_AMOUNT_ON_PAGE).endAt(date).orderByChild("createdDate");
        }

        newsQuery.keepSynced(true);
        newsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Map<String, Object> objectMap = (Map<String, Object>) dataSnapshot.getValue();
                NewsListResult result = parseNewsList(objectMap);

                if (result.getNews().isEmpty() && result.isMoreDataAvailable()) {
                    getNewsList(onDataChangedListener, result.getLastItemCreatedDate() - 1);
                } else {
                    onDataChangedListener.onListChanged(parseNewsList(objectMap));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                LogUtil.logError(TAG, "getNewsList(), onCancelled", new Exception(databaseError.getMessage()));
                onDataChangedListener.onCanceled(context.getString(R.string.permission_denied_error));
            }
        });
    }

    public void getNewsListByUser(final OnDataChangedListener<News> onDataChangedListener, String userId) {
        DatabaseReference databaseReference = databaseHelper.getDatabaseReference().child(DatabaseHelper.NEWS_DB_KEY);
        Query newsQuery;
        newsQuery = databaseReference.orderByChild("authorId").equalTo(userId);

        newsQuery.keepSynced(true);
        newsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                NewsListResult result = parseNewsList((Map<String, Object>) dataSnapshot.getValue());
                onDataChangedListener.onListChanged(result.getNews());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                LogUtil.logError(TAG, "getNewsListByUser(), onCancelled", new Exception(databaseError.getMessage()));
            }
        });
    }

    public ValueEventListener getNews(final String id, final OnNewsChangedListener listener) {
        DatabaseReference databaseReference = databaseHelper.getDatabaseReference().child(DatabaseHelper.NEWS_DB_KEY).child(id);
        ValueEventListener valueEventListener = databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() != null) {
                    if (isNewsValid((Map<String, Object>) dataSnapshot.getValue())) {
                        News news = dataSnapshot.getValue(News.class);
                        if (news != null) {
                            news.setId(id);
                        }
                        listener.onObjectChanged(news);
                    } else {
                        listener.onError(String.format(context.getString(R.string.error_general_news), id));
                    }
                } else {
                    listener.onObjectChanged(null);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                LogUtil.logError(TAG, "getPost(), onCancelled", new Exception(databaseError.getMessage()));
            }
        });

        databaseHelper.addActiveListener(valueEventListener, databaseReference);
        return valueEventListener;
    }

    public void getSingleNews(final String id, final OnNewsChangedListener listener) {
        DatabaseReference databaseReference = databaseHelper.getDatabaseReference().child(DatabaseHelper.NEWS_DB_KEY).child(id);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() != null && dataSnapshot.exists()) {
                    if (isNewsValid((Map<String, Object>) dataSnapshot.getValue())) {
                        News news = dataSnapshot.getValue(News.class);
                        news.setId(id);
                        listener.onObjectChanged(news);
                    } else {
                        listener.onError(String.format(context.getString(R.string.error_general_news), id));
                    }
                } else {
                    listener.onError(context.getString(R.string.message_post_was_removed));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                LogUtil.logError(TAG, "getSinglePost(), onCancelled", new Exception(databaseError.getMessage()));
            }
        });
    }

    private NewsListResult parseNewsList(Map<String, Object> objectMap) {
        NewsListResult result = new NewsListResult();
        List<News> list = new ArrayList<News>();
        boolean isMoreDataAvailable = true;
        long lastItemCreatedDate = 0;

        if (objectMap != null) {
            isMoreDataAvailable = Constants.News.NEWS_AMOUNT_ON_PAGE == objectMap.size();

            for (String key : objectMap.keySet()) {
                Object obj = objectMap.get(key);
                if (obj instanceof Map) {
                    Map<String, Object> mapObj = (Map<String, Object>) obj;

                    if (!isNewsValid(mapObj)) {
                        LogUtil.logDebug(TAG, "Invalid news, id: " + key);
                        continue;
                    }

                    boolean hasComplain = mapObj.containsKey("hasComplain") && (boolean) mapObj.get("hasComplain");
                    long createdDate = (long) mapObj.get("createdDate");

                    if (lastItemCreatedDate == 0 || lastItemCreatedDate > createdDate) {
                        lastItemCreatedDate = createdDate;
                    }

                    if (!hasComplain) {
                        News news = new News();
                        news.setId(key);
                        news.setTitle((String) mapObj.get("title"));
                        news.setDescription((String) mapObj.get("description"));
                        news.setImagePath((String) mapObj.get("imagePath"));
                        news.setImageTitle((String) mapObj.get("imageTitle"));
                        news.setAuthorId((String) mapObj.get("authorId"));
                        news.setCreatedDate(createdDate);
                        if (mapObj.containsKey("commentsCount")) {
                            news.setCommentsCount((long) mapObj.get("commentsCount"));
                        }
                        if (mapObj.containsKey("likesCount")) {
                            news.setLikesCount((long) mapObj.get("likesCount"));
                        }
                        if (mapObj.containsKey("watchersCount")) {
                            news.setWatchersCount((long) mapObj.get("watchersCount"));
                        }
                        list.add(news);
                    }
                }
            }

            Collections.sort(list, (lhs, rhs) -> ((Long) rhs.getCreatedDate()).compareTo(lhs.getCreatedDate()));

            result.setNews(list);
            result.setLastItemCreatedDate(lastItemCreatedDate);
            result.setMoreDataAvailable(isMoreDataAvailable);
        }

        return result;
    }

    private boolean isNewsValid(Map<String, Object> news) {
        return news.containsKey("title")
                && news.containsKey("description")
                && news.containsKey("imagePath")
                && news.containsKey("imageTitle")
                && news.containsKey("authorId")
                && news.containsKey("description");
    }

    ///////////////////////////////// 2 ///////////////////

    public void addComplainToNews(News news) {
        databaseHelper.getDatabaseReference().child(DatabaseHelper.NEWS_DB_KEY).child(news.getId()).child("hasComplain").setValue(true);
    }

    public void isNewsExistSingleValue(String newsId, final OnObjectExistListener<News> onObjectExistListener) {
        DatabaseReference databaseReference = databaseHelper.getDatabaseReference().child(DatabaseHelper.NEWS_DB_KEY).child(newsId);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                onObjectExistListener.onDataChanged(dataSnapshot.exists());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                LogUtil.logError(TAG, "isNewsExistSingleValue(), onC-=ancelled", new Exception(databaseError.getMessage()));
            }
        });
    }

    public void subscribeToNewNews() {
        FirebaseMessaging.getInstance().subscribeToTopic("NewsTopic");
    }

    ////////////////////////////////////////////////////////////////////////////////// buat newsProfile Interactor

    public void removeNews(final News news, final OnTaskCompleteListener onTaskCompleteListener) {
        final DatabaseHelper databaseHelper = ApplicationHelper.getDatabaseHelper();
        Task<Void> removeImageTask = databaseHelper.removeImage(news.getImageTitle());

        removeImageTask.addOnSuccessListener(aVoid -> {
            removeNews(news).addOnCompleteListener(task -> {
                onTaskCompleteListener.onTaskComplete(task.isSuccessful());
                //////////////////////////////////////////////////////////////////////////////////////////////////////////////
                ProfileInteractor.getInstance(context).updateProfileLikeCountAfterRemovingNews(news);
                removeObjectsRelatedToNews(news.getId());
                LogUtil.logDebug(TAG, "removeNews(), is success: " + task.isSuccessful());
            });
            LogUtil.logDebug(TAG, "removeImage(): success");
        }).addOnFailureListener(exception -> {
            LogUtil.logError(TAG, "removeImage()", exception);
            onTaskCompleteListener.onTaskComplete(false);
        });
    }

    private void removeObjectsRelatedToNews(final String newsId) {
        CommentInteractor.getInstance(context).removeCommentsByNews(newsId).addOnSuccessListener(aVoid -> LogUtil.logDebug(TAG, "Comments related to news with id: " + newsId + " was removed")).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                LogUtil.logError(TAG, "Failed to remove comments related to post with id: " + newsId, e);
            }
        });

        removeLikesByNews(newsId).addOnSuccessListener(aVoid -> LogUtil.logDebug(TAG, "Likes related to post with id: " + newsId + " was removed")).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                LogUtil.logError(TAG, "Failed to remove likes related to post with id: " + newsId, e);
            }
        });
    }

    ///////////////////////////////////

    public void createOrUpdateNewsWithImage(Uri imageUri, final OnNewsCreatedListener onNewsCreatedListener, final News news) {
        // Register observers to listen for when the download is done or if it fails
        DatabaseHelper databaseHelper = ApplicationHelper.getDatabaseHelper();
        if (news.getId() == null) {
            news.setId(generateNewsId());
        }

        final String imageTitle = ImageUtil.generateImageTitle(UploadImagePrefix.NEWS, news.getId());
        UploadTask uploadTask = databaseHelper.uploadImage(imageUri, imageTitle);

        if (uploadTask != null) {
            uploadTask.addOnFailureListener(exception -> {
                // Handle unsuccessful uploads
                onNewsCreatedListener.onNewsSaved(false);

            }).addOnSuccessListener(taskSnapshot -> {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                Uri downloadUrl = taskSnapshot.getDownloadUrl();
                LogUtil.logDebug(TAG, "successful upload image, image url: " + String.valueOf(downloadUrl));

                news.setImagePath(String.valueOf(downloadUrl));
                news.setImageTitle(imageTitle);
                createOrUpdateNews(news);

                onNewsCreatedListener.onNewsSaved(true);
            });
        }
    }

    public void createOrUpdateLike(final String newsId, final String newsAuthorId) {
        try {
            String authorId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference mLikesReference = databaseHelper
                    .getDatabaseReference()
                    .child(DatabaseHelper.NEWS_LIKES_DB_KEY)
                    .child(newsId)
                    .child(authorId);
            mLikesReference.push();
            String id = mLikesReference.push().getKey();
            Like like = new Like(authorId);
            like.setId(id);

            mLikesReference.child(id).setValue(like, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                    if (databaseError == null) {
                        DatabaseReference newsRef = databaseHelper
                                .getDatabaseReference()
                                .child(DatabaseHelper.NEWS_DB_KEY + "/" + newsId + "/likesCount");

                        incrementLikesCount(newsRef);
                        DatabaseReference profileRef = databaseHelper
                                .getDatabaseReference()
                                .child(DatabaseHelper.PROFILES_DB_KEY + "/" + newsAuthorId + "/likesCount");

                        incrementLikesCount(profileRef);
                    } else {
                        LogUtil.logError(TAG, databaseError.getMessage(), databaseError.toException());
                    }
                }

                private void incrementLikesCount(DatabaseReference newsRef) {
                    newsRef.runTransaction(new Transaction.Handler() {
                        @Override
                        public Transaction.Result doTransaction(MutableData mutableData) {
                            Integer currentValue = mutableData.getValue(Integer.class);
                            if (currentValue == null) {
                                mutableData.setValue(1);
                            } else {
                                mutableData.setValue(currentValue + 1);
                            }

                            return Transaction.success(mutableData);
                        }

                        @Override
                        public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                            LogUtil.logInfo(TAG, "Updating likes count transaction is completed.");
                        }
                    });
                }

            });
        } catch (Exception e) {
            LogUtil.logError(TAG, "createOrUpdateLike()", e);
        }

    }

    public void removeLike(final String newsId, final String newsAuthorId) {
        String authorId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference mLikesReference = databaseHelper
                .getDatabaseReference()
                .child(DatabaseHelper.NEWS_LIKES_DB_KEY)
                .child(newsId)
                .child(authorId);
        mLikesReference.removeValue(new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                if (databaseError == null) {
                    DatabaseReference newsRef = databaseHelper
                            .getDatabaseReference()
                            .child(DatabaseHelper.NEWS_DB_KEY + "/" + newsId + "/likesCount");
                    decrementLikesCount(newsRef);

                    DatabaseReference profileRef = databaseHelper
                            .getDatabaseReference()
                            .child(DatabaseHelper.PROFILES_DB_KEY + "/" + newsAuthorId + "/likesCount");
                    decrementLikesCount(profileRef);
                } else {
                    LogUtil.logError(TAG, databaseError.getMessage(), databaseError.toException());
                }
            }

            private void decrementLikesCount(DatabaseReference newsRef) {
                newsRef.runTransaction(new Transaction.Handler() {
                    @Override
                    public Transaction.Result doTransaction(MutableData mutableData) {
                        Long currentValue = mutableData.getValue(Long.class);
                        if (currentValue == null) {
                            mutableData.setValue(0);
                        } else {
                            mutableData.setValue(currentValue - 1);
                        }

                        return Transaction.success(mutableData);
                    }

                    @Override
                    public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                        LogUtil.logInfo(TAG, "Updating likes count transaction is completed.");
                    }
                });
            }
        });
    }

    public ValueEventListener hasCurrentUserLike(String newsId, String userId, final OnObjectExistListener<Like> onObjectExistListener) {
        DatabaseReference databaseReference = databaseHelper
                .getDatabaseReference()
                .child(DatabaseHelper.NEWS_LIKES_DB_KEY)
                .child(newsId)
                .child(userId);
        ValueEventListener valueEventListener = databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                onObjectExistListener.onDataChanged(dataSnapshot.exists());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                LogUtil.logError(TAG, "hasCurrentUserLike(), onCancelled", new Exception(databaseError.getMessage()));
            }
        });

        databaseHelper.addActiveListener(valueEventListener, databaseReference);
        return valueEventListener;
    }

    public void hasCurrentUserLikeSingleValue(String newsId, String userId, final OnObjectExistListener<Like> onObjectExistListener) {
        DatabaseReference databaseReference = databaseHelper
                .getDatabaseReference()
                .child(DatabaseHelper.NEWS_LIKES_DB_KEY)
                .child(newsId)
                .child(userId);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                onObjectExistListener.onDataChanged(dataSnapshot.exists());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                LogUtil.logError(TAG, "hasCurrentUserLikeSingleValue(), onCancelled", new Exception(databaseError.getMessage()));
            }
        });
    }

    public Task<Void> removeLikesByNews(String newsId) {
        return databaseHelper
                .getDatabaseReference()
                .child(DatabaseHelper.NEWS_LIKES_DB_KEY)
                .child(newsId)
                .removeValue();
    }

    public ValueEventListener searchNewsByTitle(String searchText, OnDataChangedListener<News> onDataChangedListener) {
        DatabaseReference reference = databaseHelper.getDatabaseReference().child(DatabaseHelper.NEWS_DB_KEY);
        ValueEventListener valueEventListener = getSearchQuery(reference,"title", searchText).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                NewsListResult result = parseNewsList((Map<String, Object>) dataSnapshot.getValue());
                onDataChangedListener.onListChanged(result.getNews());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                LogUtil.logError(TAG, "searchNewsByTitle(), onCancelled", new Exception(databaseError.getMessage()));
            }
        });

        databaseHelper.addActiveListener(valueEventListener, reference);

        return valueEventListener;
    }

    public ValueEventListener filterNewsByLikes(int  limit, OnDataChangedListener<News> onDataChangedListener) {
        DatabaseReference reference = databaseHelper.getDatabaseReference().child(DatabaseHelper.NEWS_DB_KEY);
        ValueEventListener valueEventListener = getFilteredQuery(reference,"likesCount", limit).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                NewsListResult result = parseNewsList((Map<String, Object>) dataSnapshot.getValue());
                onDataChangedListener.onListChanged(result.getNews());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                LogUtil.logError(TAG, "filterNewsByLikes(), onCancelled", new Exception(databaseError.getMessage()));
            }
        });

        databaseHelper.addActiveListener(valueEventListener, reference);

        return valueEventListener;
    }

    private Query getSearchQuery(DatabaseReference databaseReference, String childOrderBy, String searchText) {
        return databaseReference
                .orderByChild(childOrderBy)
                .startAt(searchText)
                .endAt(searchText + "\uf8ff");
    }

    private Query getFilteredQuery(DatabaseReference databaseReference, String childOrderBy, int limit) {
        return databaseReference
                .orderByChild(childOrderBy)
                .limitToLast(limit);
    }

}
