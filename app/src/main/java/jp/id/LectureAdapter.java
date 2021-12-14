package jp.id;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;

import java.io.File;
import java.util.List;

import jp.id.core.Impartus;
import jp.id.core.Utils;
import jp.id.model.LectureItem;
import jp.id.model.Lectures;
import jp.id.service.DownloadService;

public class LectureAdapter extends RecyclerView.Adapter<LectureAdapter.ViewHolder> {

    final int DOWNLOAD_VIDEO = 0;
    final int PLAY_VIDEO = 1;
    final int SHOW_LOGS = 2;

    private final Impartus impartus;
    private final Context context;
    private int downloadCounter = 0;
    private Handler handler = null;

    public LectureAdapter (Context context, View recyclerView, Impartus impartus){
        this.impartus = impartus;
        this.context = context;
    }

    @NonNull
    @Override
    public LectureAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        try {
            View rowItem = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.activity_listitem, parent, false);
            return new ViewHolder(rowItem);
        } catch (Exception e) {
            Log.e(this.getClass().getName(), "onCreateView", e);
            throw e;
        }
    }


    private void setProgressBarVisibility(ViewHolder holder, final int visibility) {
        holder.progressBar.setVisibility(visibility);
        holder.progressBarText.setVisibility(visibility);
    }

    private void updateProgressBar(ViewHolder holder, int value) {
        holder.progressBar.setProgress(value);
        holder.progressBarText.setText(String.format("%s%%", value));
    }

    private void setProgressBarState(final LectureItem lectureItem, ViewHolder holder) {
        File mkvFilePath = Utils.getMkvFilePath(lectureItem, impartus.getDownloadDir());
        if (mkvFilePath.exists()) {
            lectureItem.setDownloadPercent(100);
            lectureItem.setOfflinePath(mkvFilePath);
            this.setProgressBarVisibility(holder, View.VISIBLE);
        } else if (lectureItem.isDownloading()) {
            this.setProgressBarVisibility(holder, View.VISIBLE);
        } else {
            this.setProgressBarVisibility(holder, View.INVISIBLE);
        }
        this.updateProgressBar(holder, lectureItem.getDownloadPercent());
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onBindViewHolder(@NonNull LectureAdapter.ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        LectureItem lectureItem = Lectures.lectureItems.get(position);
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
        holder.contextMenuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //creating a popup menu
                PopupMenu popup = new PopupMenu(view.getContext(), holder.contextMenuButton);

                holder.setPopupMenu(popup);

                //inflating menu from xml resource
                popup.inflate(R.menu.context_menu);

                // disable download menu, if file is already downloaded or download in progress.
                if ((lectureItem.getOfflinePath() != null && lectureItem.getOfflinePath().exists()) ||
                        lectureItem.isDownloading() ) {
                    popup.getMenu().getItem(DOWNLOAD_VIDEO).setEnabled(false);
                } else {
                    popup.getMenu().getItem(DOWNLOAD_VIDEO).setEnabled(true);
                }

                // enable play video if file is downloaded.
                if (lectureItem.getOfflinePath() != null && lectureItem.getOfflinePath().exists()) {
                    popup.getMenu().getItem(PLAY_VIDEO).setEnabled(true);
                } else {
                    popup.getMenu().getItem(PLAY_VIDEO).setEnabled(false);
                }

                // enable show logs if file is being downloaded or has been downloaded.
                if (lectureItem.isDownloading() || lectureItem.getLogs().size() > 0) {
                    popup.getMenu().getItem(SHOW_LOGS).setEnabled(true);
                } else {
                    popup.getMenu().getItem(SHOW_LOGS).setEnabled(false);
                }

                //adding click listener
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        if (menuItem.getItemId() == R.id.download_video ) {
                            menuItem.setEnabled(false);
                            downloadCounter++;
                            Utils.saveDataKey(view.getContext(), "numDownloads", String.valueOf(downloadCounter));

                            if (downloadCounter > 1) {
                                Toast.makeText(view.getContext(), "Download Queued!", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(view.getContext(), "Download Started...", Toast.LENGTH_SHORT).show();
                            }

                            lectureItem.setDownloading(true);
                            lectureItem.setDownloadPercent(0);

                            setProgressBarState(lectureItem, holder);

                            serviceInit(lectureItem);
                            return true;
                        } else if(menuItem.getItemId() == R.id.play_video) {
                            File mkvFilePath = Utils.getMkvFilePath(lectureItem, impartus.getDownloadDir());
                            if (mkvFilePath.exists()) {
                                Toast.makeText(view.getContext(), String.format("Playing video %s", mkvFilePath.getAbsolutePath()), Toast.LENGTH_LONG).show();
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
                        } else if(menuItem.getItemId() == R.id.show_logs) {
                            AlertDialog.Builder alert = new AlertDialog.Builder(view.getContext());
                            alert.setTitle("Logs");
                            // Create TextView
                            final TextView logsView = new TextView (view.getContext());
                            logsView.setVerticalScrollBarEnabled(true);
                            logsView.setTextIsSelectable(true);
                            logsView.setText(StringUtils.join(lectureItem.getLogs(), "\n"));
                            alert.setView(logsView);

                            alert.setNegativeButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    // Canceled.
                                }
                            });
                            alert.show();
                            return true;
                        } else {
                            return false;
                        }
                    }
                });
                //displaying the popup
                popup.show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return Lectures.lectureItems != null ? Lectures.lectureItems.size() : 0;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void clear() {
        Lectures.lectureItems.clear();
        notifyDataSetChanged();
    }

    // Add a list of items -- change to type used
    @SuppressLint("NotifyDataSetChanged")
    public void addAll(List<LectureItem> list) {
        Lectures.lectureItems.addAll(list);
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView topic;
        private final TextView subject;
        private final TextView faculty;
        private final TextView duration;
        private final TextView date;
        private final TextView tracks;
        private final ProgressBar progressBar;
        private final TextView progressBarText;
        private final TextView contextMenuButton;
        private PopupMenu popupMenu;
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

        public void setPopupMenu(PopupMenu menu) {
            this.popupMenu = menu;
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
                Log.i("me", String.format("notifyItemChanged at pos: %s with value: %s", position, value));
                if (value == 100) {
                    downloadCounter--;
                    downloadCounter = Math.max(0, downloadCounter);
                    Utils.saveDataKey(context, "numDownalods", String.valueOf(downloadCounter));
                }
                Lectures.lectureItems.get(position).setDownloadPercent(value);
                notifyItemChanged(position);
            }
        }
    }
}
