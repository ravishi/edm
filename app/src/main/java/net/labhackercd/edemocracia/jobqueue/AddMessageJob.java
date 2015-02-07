package net.labhackercd.edemocracia.jobqueue;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.liferay.mobile.android.service.JSONObjectWrapper;
import com.liferay.mobile.android.v62.mbmessage.MBMessageService;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;

import org.json.JSONArray;
import org.json.JSONObject;

import net.labhackercd.edemocracia.content.Message;
import net.labhackercd.edemocracia.liferay.session.EDMGetSessionWrapper;
import net.labhackercd.edemocracia.liferay.session.EDMSession;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class AddMessageJob extends Job {

    public static final int PRIORITY = 1;

    private static final String TAG = "AddMessageJob";
    private static final Handler MAIN_THREAD = new Handler(Looper.getMainLooper());

    // XXX Injected fields are declared transient in order to not be serialized
    @Inject transient EventBus eventBus;
    @Inject transient EDMSession session;

    private final Message message;

    public AddMessageJob(Message message) {
        super(new Params(PRIORITY).requireNetwork().persist());
        this.message = message;
    }

    @Override
    public void onAdded() {
        eventBus.post(new JobAdded(message));
    }

    @Override
    public void onRun() throws Throwable {
        Log.d(TAG, "Adding message...");

        JSONObject serviceContextJson = new JSONObject();
        serviceContextJson.put("addGuestPermissions", true);

        JSONObjectWrapper serviceContext = new JSONObjectWrapper(
                "com.liferay.portal.service.ServiceContext", serviceContextJson);

        MBMessageService service = new MBMessageService(new EDMGetSessionWrapper(session));

        JSONObject insert = service.addMessage(
                message.getGroupId(), message.getCategoryId(), message.getThreadId(),
                message.getParentMessageId(), message.getSubject(), message.getBody(),
                message.getFormat(), new JSONArray(), message.isAnonymous(),
                message.getPriority(), message.allowPingbacks(), serviceContext);

        final Message inserted = Message.JSON_READER.fromJSON(insert);

        MAIN_THREAD.post(new Runnable() {
            @Override
            public void run() {
                eventBus.post(new Success(inserted));
            }
        });
    }

    @Override
    protected void onCancel() {
    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        Log.w(TAG, "Failed to add message: " + throwable);
        return true;
    }

    public static class JobAdded {
        private final Message message;

        public JobAdded(Message message) {
            this.message = message;
        }

        public Message getMessage() {
            return message;
        }
    }

    public static class Success {
        private final Message message;

        public Success(Message message) {
            this.message = message;
        }

        public Message getMessage() {
            return message;
        }
    }

    public static class Failure {
        private final Message message;
        private final Exception exception;

        public Failure(Message message, Exception exception) {
            this.message = message;
            this.exception = exception;
        }

        public Message getMessage() {
            return message;
        }

        public Exception getException() {
            return exception;
        }
    }
}
