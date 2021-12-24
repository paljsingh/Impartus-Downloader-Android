package jp.id.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;

import jp.id.command.Callable;
import jp.id.core.Impartus;
import jp.id.model.LectureItem;

public class DownloadService extends IntentService {
    public static final int UPDATE_PROGRESS = 8344;

    public DownloadService() {
        super("DownloadService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        Impartus impartus = intent.getParcelableExtra("impartus");
        LectureItem item = intent.getParcelableExtra("lectureitem");
        String flippedVideoQuality = intent.getStringExtra("flippedVideoQuality");
        boolean debug = intent.getBooleanExtra("debug", false);
        ResultReceiver receiver = intent.getParcelableExtra("receiver");

        //create url and connect
        Callable callback = new Callable() {
            @Override
            public void call(int value, int status) {
                Bundle resultData = new Bundle();
                resultData.putInt("position", item.getViewPosition());
                resultData.putInt("value", value);
                resultData.putInt("downloadStatus", status);
                receiver.send(UPDATE_PROGRESS, resultData);
            }
        };
        impartus.downloadLecture(item, callback, flippedVideoQuality, debug);
    }


}