package jp.id;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.apache.commons.text.WordUtils;

import java.io.File;
import java.util.List;

import jp.id.core.Impartus;
import jp.id.core.Utils;
import jp.id.model.AppLogs;
import jp.id.model.LectureItem;
import jp.id.model.Lectures;
import jp.id.service.DownloadService;

public class LectureAdapter extends RecyclerView.Adapter<LectureAdapter.ViewHolder> {

    final int DOWNLOAD_VIDEO = 0;
    final int PLAY_VIDEO = 1;

    private final Impartus impartus;
    private final Context context;
    private int downloadCounter = 0;

    private final String tag = "LectureAdapter";

    public LectureAdapter (Context context, View recyclerView, Impartus impartus){
        this.impartus = impartus;
        this.context = context;
    }

    @NonNull
    @Override
    public LectureAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        try {
            View rowItem = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.activity_listitem, parent, false);
            return new ViewHolder(rowItem);
        } catch (Exception e) {
            AppLogs.error(tag, String.format("Error onCreateView: %s", e));
            throw e;
        }
    }

    private void setProgressBarState(final LectureItem lectureItem, ViewHolder holder) {
        File mkvFilePath = Utils.getMkvFilePath(lectureItem, impartus.getDownloadDir());
        int value;
        int visibility;

        if (mkvFilePath.exists()) {
            lectureItem.setDownloadPercent(100);
        }

        // set visibility
        if(lectureItem.getDownloadPercent() > 0 || lectureItem.isDownloading()) {
            visibility = View.VISIBLE;
        } else {
            visibility = View.INVISIBLE;
        }

        value = lectureItem.getDownloadPercent();

        holder.progressBar.setProgress(value);
        holder.progressBar.setVisibility(visibility);

        holder.progressBarText.setText(String.format("%s%%", value));
        holder.progressBarText.setVisibility(visibility);
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onBindViewHolder(@NonNull LectureAdapter.ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        List<LectureItem> lectureItems = Lectures.getLectures();
        if (lectureItems.size() == 0) {
            return;
        }

        LectureItem lectureItem = lectureItems.get(position);
        this.setProgressBarState(lectureItem, holder);

        holder.topic.setText(
                String.format("[%02d] %s", lectureItem.getSeqNo(), WordUtils.capitalizeFully(lectureItem.getTopic())));
        holder.subject.setText(lectureItem.getSubjectName());
        holder.faculty.setText(WordUtils.capitalizeFully(lectureItem.getProfessorName()));

        int durationSeconds = lectureItem.getDuration();
        Integer hours = durationSeconds / 3600;
        Integer mins =  (durationSeconds % 3600) / 60;
        holder.duration.setText(String.format("Duration: %sh %02dm", hours, mins));
        holder.tracks.setText(String.format("Tracks: %s", lectureItem.getNumTracks()));
        holder.date.setText(lectureItem.getDate());
        holder.flipped.setText(String.format("Flipped: %s", lectureItem.isFlipped() ? "Y" : "N"));

        // create options menu...
        holder.contextMenuButton.setOnClickListener(view -> {
            //creating a popup menu
            PopupMenu popup = new PopupMenu(view.getContext(), holder.contextMenuButton);

            holder.setPopupMenu();

            //inflating menu from xml resource
            popup.inflate(R.menu.context_menu);
            final File mkvFilePath = Utils.getMkvFilePath(lectureItem, impartus.getDownloadDir());

            // disable download menu, if file is already downloaded or download in progress.
            if (mkvFilePath.exists() || lectureItem.isDownloading()) {
                popup.getMenu().getItem(DOWNLOAD_VIDEO).setEnabled(false);
            } else {
                popup.getMenu().getItem(DOWNLOAD_VIDEO).setEnabled(true);
            }

            // enable play video if file is downloaded.
            if (mkvFilePath.exists()) {
                popup.getMenu().getItem(PLAY_VIDEO).setEnabled(true);
            } else {
                popup.getMenu().getItem(PLAY_VIDEO).setEnabled(false);
            }

            //adding click listener
            popup.setOnMenuItemClickListener(menuItem -> {
                if (menuItem.getItemId() == R.id.download_video ) {
                    menuItem.setEnabled(false);
                    downloadCounter++;
                    Utils.saveDataKey(view.getContext(), "numDownloads", String.valueOf(downloadCounter));

                    if (downloadCounter > 1) {
                        AppLogs.info(tag, String.format("Download queued for %s", mkvFilePath.getAbsolutePath()));
                        Toast.makeText(view.getContext(), "Download Queued!", Toast.LENGTH_SHORT).show();
                    } else {
                        AppLogs.info(tag, String.format("Starting download for %s", mkvFilePath.getAbsolutePath()));
                        Toast.makeText(view.getContext(), "Download Started...", Toast.LENGTH_SHORT).show();
                    }

                    lectureItem.setDownloading(true);
                    lectureItem.setDownloadPercent(0);

                    setProgressBarState(lectureItem, holder);

                    serviceInit(lectureItem);
                    return true;
                } else if(menuItem.getItemId() == R.id.play_video) {
                    if (mkvFilePath.exists()) {
                        final String msg = String.format("Playing video %s", mkvFilePath.getAbsolutePath());
                        AppLogs.info(tag, msg);
                        Toast.makeText(view.getContext(), msg, Toast.LENGTH_LONG).show();
                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mkvFilePath.getAbsolutePath()));
                            intent.setDataAndType(Uri.parse(mkvFilePath.getAbsolutePath()), "video/mp4");
                            view.getContext().startActivity(intent);
                        } catch (ActivityNotFoundException e) {
                            e.printStackTrace();
                        }
                        return true;
                    } else {
                        Toast.makeText(view.getContext(), "Video does not exist, download it first.", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                } else {
                    return false;
                }
            });
            //displaying the popup
            popup.show();
        });
    }

    @Override
    public int getItemCount() {
        List<LectureItem> lectureItems = Lectures.getLectures();
        return lectureItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView topic;
        private final TextView subject;
        private final TextView faculty;
        private final TextView duration;
        private final TextView date;
        private final TextView tracks;
        private final ProgressBar progressBar;
        private final TextView progressBarText;
        private final TextView contextMenuButton;
        private final TextView flipped;

        public ViewHolder(View view) {
            super(view);

            this.topic = view.findViewById(R.id.topic);
            this.subject = view.findViewById(R.id.subject);
            this.faculty = view.findViewById(R.id.faculty);
            this.date = view.findViewById(R.id.date);
            this.duration = view.findViewById(R.id.duration);
            this.tracks = view.findViewById(R.id.tracks);
            this.progressBar = view.findViewById(R.id.progressBar);
            this.progressBarText = view.findViewById(R.id.progressBarText);
            this.contextMenuButton = view.findViewById(R.id.contextMenuButton);
            this.flipped = view.findViewById(R.id.flipped);
        }

        public void setPopupMenu() {
        }
    }

    private void serviceInit(final LectureItem item) {
        Intent intent = new Intent(this.context, DownloadService.class);

        boolean debug = Utils.getPrefsKey(context, "debug", false);
        String flippedVideoQuality = Utils.getPrefsKey(context, "video_quality", "highest");

        intent.putExtra("impartus", impartus);
        intent.putExtra("lectureitem", item);
        intent.putExtra("flippedVideoQuality", flippedVideoQuality);
        intent.putExtra("debug", debug);
        intent.putExtra("receiver", new DownloadReceiver(new Handler()));
        this.context.startService(intent);
    }

    public class DownloadReceiver extends ResultReceiver {

        public DownloadReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            super.onReceiveResult(resultCode, resultData);
            if (resultCode == DownloadService.UPDATE_PROGRESS) {
                int value = resultData.getInt("value");
                int position = resultData.getInt("position");
                LectureItem item = Lectures.getLectures().get(position);
                item.setDownloadPercent(value);

                if (value == 100) {
                    // no longer downloading, ensures Download/Play button states are appropriately set.
                    item.setDownloading(false);

                    downloadCounter--;
                    downloadCounter = Math.max(0, downloadCounter);
                    Utils.saveDataKey(context, "numDownloads", String.valueOf(downloadCounter));
                }
                notifyItemChanged(position);
            }
        }
    }
}
