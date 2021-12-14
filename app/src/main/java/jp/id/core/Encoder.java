package jp.id.core;

import android.util.Log;

import com.arthenica.ffmpegkit.ExecuteCallback;
import com.arthenica.ffmpegkit.ReturnCode;
import com.arthenica.ffmpegkit.Session;
import com.arthenica.ffmpegkit.SessionState;

import org.apache.commons.io.FileUtils;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jp.id.model.LectureItem;

public class Encoder {

    public static ExecuteCallback defaultCallback = new ExecuteCallback() {
        @Override
        public void apply(Session session) {
            SessionState state = session.getState();
            ReturnCode returnCode = session.getReturnCode();
            Log.d(Encoder.class.getName(), String.format("FFmpeg process exited with state %s and rc %s.%s", state, returnCode, session.getFailStackTrace()));
        }
    };

    public static File join(List<File> files, String dir, int trackNumber, final LectureItem item) {
        File outFilePath = new File(String.format("%s/track-%s.ts", dir, trackNumber));
        for(File file: files) {
            try {
                byte[] bytes = FileUtils.readFileToByteArray(file);
                FileUtils.writeByteArrayToFile(outFilePath, bytes, true);
            } catch (IOException e) {
                item.appendLog(String.format("Exception while joining streams: %s", e.getMessage()));
                e.printStackTrace();
                return null;
            }
        }
        return outFilePath;
    }

    public static boolean splitTrack(final List<File> trackFiles, final int duration, final boolean debug, final LectureItem item) {
        String logLevel = "quiet";
        if (debug) {
            logLevel = "verbose";
        }

        // take out splices from track 0 ts_file and create ts_file1, ts_file2 ..
        for (int i=1; i < trackFiles.size(); i++ ) {
            int startSs = i * duration;
            List<String> commandArgs = new ArrayList<>();
            commandArgs.add("-y");
            commandArgs.add("-loglevel");
            commandArgs.add(logLevel);
            commandArgs.add("-i");
            commandArgs.add(trackFiles.get(0).getAbsolutePath());
            commandArgs.add("-c");
            commandArgs.add("copy");
            commandArgs.add("-ss");
            commandArgs.add(String.valueOf(startSs));
            commandArgs.add("-t");
            commandArgs.add(String.valueOf(duration));
            commandArgs.add(trackFiles.get(i).getAbsolutePath());

            Utils.runFfmpeg(commandArgs);
        }

        // trim ts_file 0, so that it contains only track 0 content
        String tmpFilePath = String.format("%s/%s", trackFiles.get(0).getParent(), "tmp.ts");
        List<String> commandArgs = new ArrayList<>();
        commandArgs.add("-y");
        commandArgs.add("-loglevel");
        commandArgs.add(logLevel);
        commandArgs.add("-i");
        commandArgs.add(trackFiles.get(0).getAbsolutePath());
        commandArgs.add("-c");
        commandArgs.add("copy");
        commandArgs.add("-ss");
        commandArgs.add("0");
        commandArgs.add("-t");
        commandArgs.add(String.valueOf(duration));
        commandArgs.add(tmpFilePath);
        Utils.runFfmpeg(commandArgs);

        return new File(tmpFilePath).renameTo(trackFiles.get(0).getAbsoluteFile());
    }

    public static boolean encodeMkv(final LectureItem item, final List<File> trackFiles, final String mkvFilePath, final boolean debug) {
        // probe size is needed to lookup timestamp info in files where multiple tracks are
        // joined in a single channel and possibly with incorrect timestamps.
        String probeSize = "2147483647";

        // ffmpeg log_level.
        String ffmpegLogLevel = "quiet";
        if (debug) {
            ffmpegLogLevel = "verbose";
        }
        try {
            // ffmpeg command syntax we expect to run
            // ffmpeg [global_flags] [in1_flags] -i in1.ts [in2_flags] -i in2.ts .. -c copy -map 0 -map 1 .. $outfile
            List<String> inputArgs = new ArrayList<>();
            List<String> mapArgs = new ArrayList<>();

            boolean splitFlag = false;

            for (int i = 0; i < trackFiles.size(); i++) {
                File tsFile = trackFiles.get(i);
                inputArgs.add("-analyzeduration");
                inputArgs.add(probeSize);
                inputArgs.add("-probesize");
                inputArgs.add(probeSize);
                inputArgs.add("-i");
                inputArgs.add(tsFile.getAbsolutePath());

                mapArgs.add("-map");
                mapArgs.add(String.valueOf(i));

                // if any of the ts_file is 0 sized, it's content exists in track 0
                // split track 0, if that is the case.
                if (tsFile.length() == 0) {
                    splitFlag = true;
                }
            }

            if (splitFlag) {
                Log.d(Encoder.class.getName(), String.format("[%s]: Splitting track 0 .. ", item.getId()));
                boolean splitSuccess = Encoder.splitTrack(trackFiles, item.getDuration(), debug, item);
                if (! splitSuccess) {
                    Log.e(Encoder.class.getName(), "Error splitting track 0");
                    item.appendLog("ERROR splitting track 0.");
                    return false;
                }
            }

            Log.i(Encoder.class.getName(), String.format("[%s]: Encoding output file ..", item.getId()));
            List<String> commandArgs = new ArrayList<>();
            commandArgs.add("-y");
            commandArgs.add("-loglevel");
            commandArgs.add(ffmpegLogLevel);
            commandArgs.addAll(inputArgs);

            // adding id to metadata.
            if (item.isFlipped()) {
                commandArgs.add("-metadata");
                commandArgs.add(String.format("fcid=%s", item.getId()));
            } else {
                commandArgs.add("-metadata");
                commandArgs.add(String.format("ttid=%s", item.getId()));
            }

            commandArgs.add("-c");
            commandArgs.add("copy");

            commandArgs.addAll(mapArgs);
            commandArgs.add(mkvFilePath);

            Utils.runFfmpeg(commandArgs);
        } catch (Exception ex) {
            Log.e(Encoder.class.getName(), String.format("[%s]: ffmpeg exception: %s", item.getId(), ex));
            item.appendLog(String.format("[%s]: ffmpeg exception: %s", item.getId(), ex));

            String filename = trackFiles.get(0) != null ? trackFiles.get(0).getName() : "null";
            StringBuilder fileNames = new StringBuilder(filename);
            for(int i=1; i<trackFiles.size(); i++) {
                filename = trackFiles.get(0) != null ? trackFiles.get(i).getName() : "null";
                fileNames.append(", ").append(filename);
            }
            Log.e(Encoder.class.getName(), String.format("[%s]: Check the ts file(s) generated at location: %s", item.getId(), fileNames));
            return false;
        }
        return true;
    }
}
