package net.labhackercd.edemocracia.jobqueue;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.youtube.YouTube;
import com.google.common.collect.Lists;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.JobManager;
import com.path.android.jobqueue.Params;

import java.io.IOException;
import java.io.InputStream;

import net.labhackercd.edemocracia.R;
import net.labhackercd.edemocracia.content.Message;
import net.labhackercd.edemocracia.ytdl.Auth;

import javax.inject.Inject;

public class VideoUploadJob extends Job {

    public static final int PRIORITY = -1;

    private static final String TAG = "VideoUploadTask";

    // XXX Injected fields are declared transient in order to not be serialized
    @Inject transient Context context;
    @Inject transient JobManager jobManager;

    private String video;
    private String account;
    private Message message;

    public VideoUploadJob(Uri video, String account, Message message) {
        super(new Params(PRIORITY).requireNetwork().persist());
        this.video = video.toString();
        this.account = account;
        this.message = message;
    }

    @Override
    public void onAdded() {
    }

    @Override
    public void onRun() throws Throwable {
        GoogleAccountCredential credential = GoogleAccountCredential
                .usingOAuth2(context.getApplicationContext(), Lists.newArrayList(Auth.SCOPES));

        credential.setSelectedAccountName(account);
        credential.setBackOff(new ExponentialBackOff());

        String appName = context.getResources().getString(R.string.app_name);

        final HttpTransport transport = AndroidHttp.newCompatibleTransport();
        final JsonFactory jsonFactory = new GsonFactory();

        YouTube youtube = new YouTube.Builder(transport, jsonFactory, credential)
                .setApplicationName(appName).build();

        ContentResolver contentResolver = context.getContentResolver();

        Uri video = Uri.parse(VideoUploadJob.this.video);
        InputStream fileInputStream = null;

        try {
            long fileSize = contentResolver.openFileDescriptor(video, "r").getStatSize();
            fileInputStream = contentResolver.openInputStream(video);
            String[] projection = {MediaStore.Images.Media.DATA};
            Cursor cursor = contentResolver.query(video, projection, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();

            final String videoId = ResumableUpload.upload(
                    youtube, fileInputStream, fileSize, video,
                    cursor.getString(column_index), context);

            assert videoId != null;

            // Once the video is submitted, we inject it in the message as a tag...
            String newBody = "[center][youtube]" + videoId + "[/youtube][/center]\n" + message.getBody();

            Message videoMessage = Message.create(
                    message.getGroupId(), message.getCategoryId(), message.getThreadId(),
                    message.getParentMessageId(), message.getSubject(), newBody,
                    message.getFormat(), message.isAnonymous(), message.getPriority(),
                    message.allowPingbacks());

            // and register a job to add the message.
            jobManager.addJob(new AddMessageJob(videoMessage));
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    @Override
    protected void onCancel() {
    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        return getCurrentRunCount() < 3;
    }
}