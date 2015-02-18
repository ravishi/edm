package net.labhackercd.edemocracia.ui.thread;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.liferay.mobile.android.v62.mbcategory.MBCategoryService;
import com.liferay.mobile.android.v62.mbmessage.MBMessageService;
import com.liferay.mobile.android.v62.mbthread.MBThreadService;

import org.json.JSONArray;

import java.util.List;

import javax.inject.Inject;

import net.labhackercd.edemocracia.data.model.Category;
import net.labhackercd.edemocracia.data.model.Forum;
import net.labhackercd.edemocracia.data.model.Group;
import net.labhackercd.edemocracia.data.model.Message;
import net.labhackercd.edemocracia.data.model.Thread;
import net.labhackercd.edemocracia.ui.SimpleRecyclerViewFragment;
import net.labhackercd.edemocracia.data.api.EDMBatchSession;
import net.labhackercd.edemocracia.data.api.EDMSession;
import net.labhackercd.edemocracia.data.model.util.JSONReader;

import de.greenrobot.event.EventBus;

public class ThreadListFragment extends SimpleRecyclerViewFragment<ThreadItem> {

    public static String ARG_PARENT = "parent";

    @Inject EventBus eventBus;
    @Inject EDMSession session;

    private Forum forum;

    public static ThreadListFragment newInstance(Forum forum) {
        ThreadListFragment fragment = new ThreadListFragment();

        Bundle args = new Bundle();
        args.putParcelable(ARG_PARENT, forum);

        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        Bundle args = getArguments();

        if (args != null) {
            forum = args.getParcelable(ARG_PARENT);
        }
    }

    @Override
    protected RecyclerView.Adapter createAdapter(List<ThreadItem> items) {
        return new ThreadListAdapter(getActivity(), eventBus, items);
    }

    @Override
    protected List<ThreadItem> blockingFetchItems() throws Exception {
        EDMBatchSession batchSession = new EDMBatchSession(session);

        MBThreadService threadService = new MBThreadService(batchSession);
        MBCategoryService categoryService = new MBCategoryService(batchSession);

        boolean forumIsAGroup = forum instanceof Group;

        if (forumIsAGroup) {
            threadService.getGroupThreads(forum.getGroupId(), -1, 0, -1, -1);
            categoryService.getCategories(forum.getGroupId());
        } else {
            threadService.getThreads(forum.getGroupId(), forum.getCategoryId(), 0, -1, -1);
            categoryService.getCategories(forum.getGroupId(), forum.getCategoryId(), -1, -1);
        }

        JSONArray jsonResult = batchSession.invoke();
        JSONArray jsonThreads = jsonResult.getJSONArray(0);
        JSONArray jsonCategories = jsonResult.getJSONArray(1);

        if (jsonThreads == null) {
            jsonThreads = new JSONArray();
        }

        if (jsonCategories == null) {
            jsonCategories = new JSONArray();
        }

        List<Thread> threads = JSONReader.fromJSON(jsonThreads, Thread.JSON_READER);
        List<Category> categories = JSONReader.fromJSON(jsonCategories, Category.JSON_READER);

        if (forumIsAGroup) {
            // Ignore threads and categories that don't belong to the given group. This is
            // required because the webservice returns all the threads and categories inside a
            // group, even the ones nested inside subcategories.
            threads = Lists.newArrayList(Collections2.filter(threads, new Predicate<Thread>() {
                @Override
                public boolean apply(@Nullable Thread thread) {
                    return thread != null && thread.getCategoryId() == 0;
                }
            }));

            categories = Lists.newArrayList(Collections2.filter(categories, new Predicate<Category>() {
                @Override
                public boolean apply(@Nullable Category category) {
                    return category != null && category.getParentCategoryId() == 0;
                }
            }));
        }

        MBMessageService messageService = new MBMessageService(batchSession);

        for (Thread thread : threads) {
            messageService.getMessage(thread.getRootMessageId());
        }

        JSONArray jsonMessages = batchSession.invoke();

        if (jsonMessages == null) {
            jsonMessages = new JSONArray();
        }

        List<Message> messages = JSONReader.fromJSON(jsonMessages, Message.JSON_READER);

        for (Message message : messages) {
            threads.get(messages.indexOf(message)).setRootMessage(message);
        }

        Iterable<ThreadItem> ithreads = Collections2.transform(threads, new Function<Thread, ThreadItem>() {
            @Override
            public ThreadItem apply(Thread thread) {
                return new ThreadItem(thread);
            }
        });

        Iterable<ThreadItem> icategories = Collections2.transform(categories, new Function<Category, ThreadItem>() {
            @Override
            public ThreadItem apply(Category category) {
                return new ThreadItem(category);
            }
        });

        return Lists.newArrayList(Iterables.concat(icategories, ithreads));
    }
}