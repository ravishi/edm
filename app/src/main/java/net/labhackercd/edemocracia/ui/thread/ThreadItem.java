package net.labhackercd.edemocracia.ui.thread;

import android.net.Uri;

import net.labhackercd.edemocracia.data.model.*;
import net.labhackercd.edemocracia.data.model.Thread;

import java.util.Date;

import javax.annotation.Nullable;

/**
 * An item in a ThreadListFragment. It warps a Thread or a Category.
 */
class ThreadItem {
    private final Thread thread;
    private final Category category;

    public ThreadItem(Thread thread) {
        this.thread = thread;
        this.category = null;
    }

    public ThreadItem(Category category) {
        this.thread = null;
        this.category = category;
    }

    @Nullable
    public Forum getForum() {
        if (category != null) {
            return category;
        }
        return null;
    }

    @Nullable
    public Thread getThread() {
        return thread;
    }

    @Override
    public String toString() {
        Thread thread = getThread();
        if (thread != null) {
            return thread.toString();
        } else {
            return category.toString();
        }
    }

    public Uri getUserPortrait() {
        if (thread != null) {
            return thread.getUserPortrait();
        } else {
            return category.getUserPortrait();
        }
    }

    public Date getLastPostDate() {
        if (thread != null) {
            return thread.getLastPostDate();
        } else {
            return category.getLastPostDate();
        }
    }

    public int getItemCount() {
        if (thread != null) {
            return thread.getMessageCount();
        } else {
            return category.getThreadCount();
        }
    }

    public Date getCreateDate() {
        if (thread != null) {
            Message root = thread.getRootMessage();
            if (root != null) {
                return root.getCreateDate();
            } else {
                return null;
            }
        } else {
            return category.getCreateDate();
        }
    }

    public String getBody() {
        Thread thread = getThread();
        if (thread != null) {
            Message root = thread.getRootMessage();
            if (root != null) {
                return root.getBody();
            }
        }
        return null;
    }

    public String getUserName() {
        if (thread != null) {
            Message root = thread.getRootMessage();
            if (root != null) {
                return root.getUserName();
            } else {
                return "[User " + thread.getRootMessageUserId() + "]";
            }
        } else {
            return category.getUserName();
        }
    }
}
