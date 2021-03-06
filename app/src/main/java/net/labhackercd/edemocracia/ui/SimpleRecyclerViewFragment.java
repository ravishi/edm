package net.labhackercd.edemocracia.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import net.labhackercd.edemocracia.EDMApplication;
import net.labhackercd.edemocracia.R;
import net.labhackercd.edemocracia.data.api.error.ClientErrorEvent;

import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;
import de.greenrobot.event.util.ThrowableFailureEvent;
import timber.log.Timber;

public abstract class SimpleRecyclerViewFragment<T> extends Fragment {
    private RefreshListTask<List<T>> refreshListTask;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Inject EventBus eventBus;

    @InjectView(R.id.progress_container) View progressView;
    @InjectView(R.id.load_error_container) View errorContainerView;
    @InjectView(R.id.errorMessage) TextView errorMessageView;
    @InjectView(android.R.id.list) RecyclerView recyclerView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EDMApplication.get(getActivity()).inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.simple_recycler_view, container, false);

        ButterKnife.inject(this, view);

        // Progress view starts hidden
        progressView.setVisibility(View.GONE);

        // Error view also starts hidden
        errorContainerView.setVisibility(View.GONE);

        Activity activity = getActivity();

        recyclerView.addItemDecoration(new SimpleRecyclerViewDivider(activity));
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));

        // XXX Is this the right way to deal with null container?
        Context context = container != null ? container.getContext() : inflater.getContext();

        swipeRefreshLayout = new SwipeRefreshLayout(context) {
            @Override
            public boolean canChildScrollUp() {
                // XXX Only works for vertical LinearLayoutManager. If you need something else,
                // implement it.
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                int position = layoutManager.findFirstCompletelyVisibleItemPosition();
                return position != 0;
            }
        };

        int matchParent = ViewGroup.LayoutParams.MATCH_PARENT;
        swipeRefreshLayout.addView(view, matchParent, matchParent);
        swipeRefreshLayout.setLayoutParams(new ViewGroup.LayoutParams(matchParent, matchParent));

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshList();
            }
        });

        // The swipe-to-refresh gesture should be disabled on the first load
        swipeRefreshLayout.setEnabled(false);

        return swipeRefreshLayout;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Register for sticky events because we don't want to miss anything
        // that could have arrived while we were paused.
        eventBus.registerSticky(this);

        // Reload the list only if there is no adapter set, and only after registering
        // in the event bus. This is because it can happen that current sticky event in
        // the event bus is still our list, so we don't need to load it again.
        if (recyclerView.getAdapter() == null) {
            refreshList();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        eventBus.unregister(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.reset(this);
    }

    @Override
    public void onDestroy() {
        destroyRefreshListTask();
        super.onDestroy();
    }

    @OnClick(R.id.retryButton)
    @SuppressWarnings("UnusedDeclaration")
    public void onRetryButtonClick(View view) {
        refreshList();
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEventMainThread(RefreshListTask.SuccessEvent<List<T>> event) {
        if (!getClass().equals(event.getExecutionScope())) {
            // Not our business...
            return;
        }

        // Destroy the task.
        destroyRefreshListTask();

        // Capture the results.
        List<T> result = event.getResult();

        // Ensure that the error message is not visible
        errorContainerView.setVisibility(View.GONE);

        // Set the items
        RecyclerView.Adapter adapter = null;

        if (recyclerView != null && result != null) {
            adapter = createAdapter(result);
            recyclerView.setAdapter(adapter);
        }

        // TODO Show something when the list is empty

        // Ensure that the progress is also not visible
        setProgressVisibility(false);

        // Stop that the SwipeRefreshLayout refreshing animation
        swipeRefreshLayout.setRefreshing(false);

        // Keep the swipe-to-refresh gesture enabled only if there are list items to display
        if (adapter != null && adapter.getItemCount() > 0) {
            swipeRefreshLayout.setEnabled(true);
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEventMainThread(ThrowableFailureEvent event) {
        if (!getClass().equals(event.getExecutionScope())) {
            // Not our business...
            return;
        }

        final Activity activity = getActivity();
        final Context context = activity != null ? activity : recyclerView.getContext();

        if (ClientErrorEvent.isAuthorizationError(event)) {
            new AlertDialog.Builder(context)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // TODO Redirect the user back to this same screen after sign in
                            startActivity(new Intent(context, SignInActivity.class));

                            dialog.dismiss();

                            if (activity != null) {
                                activity.finish();
                            }
                        }
                    })
                    .setMessage(R.string.authorization_error_message)
                    .create()
                    .show();
            return;
        }

        // Try to explain the most common problems in a non-scary way.
        int errorMessage;
        if (ClientErrorEvent.isNetworkError(event)) {
            errorMessage = R.string.network_error_message;
        } else {
            // Log the failure.
            Timber.e(event.getThrowable(), "Failed to load list data.");

            // Generic error message
            errorMessage = R.string.load_error_message;
        }

        if (swipeRefreshLayout.isEnabled()) {
            // If it was loaded through the swipe-to-refresh gesture, we don't
            // need to do much. Just show a toast.
            swipeRefreshLayout.setRefreshing(false);

            // Show a toast with the error message.
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show();
        } else {
            // Hide everything...
            progressView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.GONE);

            // Disable the swipe-to-refresh gesture...
            swipeRefreshLayout.setRefreshing(false);
            swipeRefreshLayout.setEnabled(false);

            errorMessageView.setText(errorMessage);

            // Show the error message
            errorContainerView.setVisibility(View.VISIBLE);
        }
    }

    private void destroyRefreshListTask() {
        if (refreshListTask != null && refreshListTask.getStatus() != AsyncTask.Status.FINISHED) {
            refreshListTask.cancel(true);
            refreshListTask = null;
        }
    }

    protected void refreshList() {
        /**
         * If the swipe-to-refresh gesture is not enabled, or this specific refresh
         * was triggered by something else, we disable it and use the progress-based
         * refresh indicator.
         */
        if (!swipeRefreshLayout.isEnabled() || !swipeRefreshLayout.isRefreshing()) {
            setProgressVisibility(true);
            swipeRefreshLayout.setEnabled(false);
            swipeRefreshLayout.setRefreshing(false);
        }

        // Kick the background task to refresh the list
        refreshListTask = new RefreshListTask<>(getClass(), eventBus, new RefreshListTask.Task<List<T>>() {
            @Override
            public List<T> execute() throws Exception {
                return blockingFetchItems();
            }
        });
        refreshListTask.execute();
    }

    protected void setProgressVisibility(boolean visible) {
        progressView.setVisibility(visible ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(visible ? View.GONE : View.VISIBLE);
    }

    /**
     * Synchronously fetch list items. This method is called inside an AsyncTask to refresh the list.
     *
     * If an exception is thrown, it'll be logged and silenced.
     *
     * @return List<T>
     * @throws Exception
     */
    protected abstract List<T> blockingFetchItems() throws Exception;

    /**
     * Create a RecyclerView adapter for the list.
     *
     * @param items
     * @return adapter
     */
    protected abstract RecyclerView.Adapter createAdapter(List<T> items);
}