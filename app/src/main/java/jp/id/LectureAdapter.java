package jp.id;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
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
import androidx.recyclerview.widget.RecyclerView;
import org.apache.commons.text.WordUtils;

import java.io.File;
import java.util.List;

import jp.id.activities.VideoActivity;
import jp.id.command.Callable;
import jp.id.core.Impartus;
import jp.id.core.Utils;
import jp.id.model.LectureItem;

public class LectureAdapter extends RecyclerView.Adapter<LectureAdapter.ViewHolder> {

    final int DOWNLOAD_VIDEO = 0;
    final int PLAY_VIDEO = 1;

    private final List<LectureItem> lectureItems;
    private final Impartus impartus;
    private int downloadCounter = 0;

    public LectureAdapter (List<LectureItem> lectureItems, Impartus impartus){
        this.lectureItems = lectureItems;
        this.impartus = impartus;
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

    @SuppressLint("DefaultLocale")
    @Override
    public void onBindViewHolder(@NonNull LectureAdapter.ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        LectureItem lectureItem = lectureItems.get(position);
        File mkvFilePath = Utils.getMkvFilePath(lectureItem, impartus.getDownloadDir());
        if (mkvFilePath.exists()) {
            lectureItem.setDownloadPercent(100);
            lectureItem.setOfflinePath(mkvFilePath);
            holder.progressBar.setVisibility(View.VISIBLE);
            holder.progressBarText.setVisibility(View.VISIBLE);
        } else if (lectureItem.isDownloading()) {
            holder.progressBar.setVisibility(View.VISIBLE);
            holder.progressBarText.setVisibility(View.VISIBLE);
        } else {
            holder.progressBar.setVisibility(View.INVISIBLE);
            holder.progressBarText.setVisibility(View.INVISIBLE);
        }
        holder.progressBar.setProgress(lectureItem.getDownloadPercent());
        holder.progressBarText.setText(String.format("%s%%", lectureItem.getDownloadPercent()));

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

                //adding click listener
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        if (menuItem.getItemId() == R.id.download_video ) {
                            menuItem.setEnabled(false);
                            downloadCounter++;
                            if (downloadCounter > 1) {
                                Toast.makeText(view.getContext(), "Download Queued!", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(view.getContext(), "Download Started...", Toast.LENGTH_SHORT).show();
                            }

                            lectureItem.setDownloading(true);
                            holder.progressBar.setVisibility(View.VISIBLE);
                            holder.progressBarText.setVisibility(View.VISIBLE);
                            holder.progressBar.setProgress(0);
                            holder.progressBarText.setText(String.format("%s%%", lectureItem.getDownloadPercent()));

                            LectureAdapter.DownloadLecture asyncTask = new LectureAdapter.DownloadLecture(view.getContext());
                            asyncTask.execute(lectureItem);
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
        return this.lectureItems.size();
    }

    private class DownloadLecture extends AsyncTask<LectureItem, Integer, Boolean> {
        LectureItem lectureItem;
        Context context;

        public DownloadLecture(Context context) {
            this.context = context;
        }

        @Override
        protected Boolean doInBackground(LectureItem... items) {
            lectureItem = items[0];
            boolean debug = Utils.getKeyFromPrefs(context, "debug", false);
            String videoQuality = Utils.getKeyFromPrefs(context, "video_quality", "highest");
            return impartus.downloadLecture(lectureItem, new Callable() {
                @Override
                public void call(int value) {
                    publishProgress(value);
                }
            }, videoQuality, debug);
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            notifyItemChanged(lectureItem.getViewPosition());
        }

        @Override
        protected void onPostExecute(Boolean status) {
            downloadCounter--;
            if(status) {
                lectureItem.setDownloading(false);
                lectureItem.setDownloadPercent(100);
                Toast.makeText(context, "Download complete!", Toast.LENGTH_LONG).show();
            } else {
                lectureItem.setDownloading(false);
                lectureItem.setDownloadPercent(0);
                Toast.makeText(context, "Error downloading file!", Toast.LENGTH_LONG).show();
            }
        }
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
}
