package jp.id.core;

import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import jp.id.command.Callable;
import jp.id.model.LectureItem;
import jp.id.model.SubjectItem;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Timeout;

public class Impartus implements Parcelable {
    private String sessionToken = null;
    private final String baseUrl;
    private final File cacheDir;
    private final File downloadDir;
    private OkHttpClient client;

    private String flippedVideoQuality = "highest";

    public Impartus(final String baseUrl, final File cacheDir) {
        this.baseUrl = baseUrl;
        this.cacheDir = cacheDir;
        this.downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
        client = new OkHttpClient.Builder()
                .callTimeout(10, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    public Impartus(final String baseUrl, final File cacheDir, final String sessionToken) {
        this(baseUrl, cacheDir);
        this.sessionToken = sessionToken;
    }

    public File getDownloadDir() {
        return downloadDir;
    }

    public void setFlippedVideoQuality(String videoQuality) {
        this.flippedVideoQuality = videoQuality;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(sessionToken);
        dest.writeString(baseUrl);
        dest.writeString(cacheDir.getAbsolutePath());
        dest.writeString(downloadDir.getAbsolutePath());
    }

    public static final Parcelable.Creator<Impartus> CREATOR = new Parcelable.Creator<Impartus>() {
        public Impartus createFromParcel(Parcel in) {
            return new Impartus(in);
        }

        public Impartus[] newArray(int size) {
            return new Impartus[size];
        }
    };

    private Impartus(Parcel in) {
        sessionToken = in.readString();
        baseUrl = in.readString();
        cacheDir = new File(in.readString());
        downloadDir = new File(in.readString());
    }


    public Response sendPostRequest(final String url, final JSONObject json, boolean withToken) {
        String jsonData = json.toString();

        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody requestBody = RequestBody.create(jsonData, JSON);

        Request.Builder builder = new Request.Builder()
                .url(url)
                .post(requestBody);

        if (withToken) {
            if (sessionToken != null) {
                builder.addHeader("Authorization", String.format("Bearer %s", sessionToken));
                builder.addHeader("Cookie", String.format("Bearer %s", sessionToken));
            }
        }
        Request request = builder.build();

        try {
            Log.d(this.getClass().getName(), String.format("Sending POST request to %s", url));
            return client.newCall(request).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Response sendGetRequest(final String url, boolean withToken) {
        Request.Builder builder = new Request.Builder()
                .url(url)
                .get();

        if (withToken) {
            if (sessionToken != null) {
                builder.addHeader("Authorization", String.format("Bearer %s", sessionToken));
                builder.addHeader("Cookie", String.format("Bearer %s", sessionToken));
            }
        }
        Request request = builder.build();

        int maxRetries = 3;
        int retryCount = 0;
        while(true) {
            try {
                Log.d(this.getClass().getName(), String.format("Sending GET request to %s", url));
                return client.newCall(request).execute();
            } catch (UnknownHostException e) {
                Log.w(this.getClass().getName(), "No internet ?");
                return null;
            } catch (IOException e) {
                retryCount++;
                if (retryCount <= maxRetries) {
                    Log.d(this.getClass().getName(), String.format("Request to %s timed out, retrying.", url));
                } else {
                    Log.w(this.getClass().getName(), String.format("Error when sending request to %s, giving up.", url));
                    e.printStackTrace();
                    break;
                }
            }
        }
        return null;
    }

    public boolean login(final String username, final String password) {

        String url = String.format("%s/api/auth/signin", baseUrl);
        JSONObject json = new JSONObject();
        try {
            json.put("username", username);
            json.put("password", password);
        } catch (JSONException e) {
            Log.w(this.getClass().getName(), "Invalid json format for username/password");
        }

        Log.d(this.getClass().getName(), String.format("Authenticating with username %s to %s", username, url));
        Response response = sendPostRequest(url, json, false);

        try {
            if (response.code() == 200) {
                Log.d(this.getClass().getName(), "Successfully authenticated with impartus.");
                String body = Objects.requireNonNull(response.body()).string();
                JSONObject jsonResponse = new JSONObject(body);
                sessionToken = jsonResponse.get("token").toString();
                return true;
            } else {
                Log.e(this.getClass().getName(), "Error authenticating with impartus.");
                Log.e(this.getClass().getName(), String.format("response body: %s", response.body()));
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean isAuthenticated() {
        return this.sessionToken != null;
    }
    public String getSessionToken() {
        return this.sessionToken;
    }

    public void invalidateSession() {
        sessionToken = null;
    }

    public static boolean isValidSession(String sessionToken) {
        String[] split = sessionToken.split("\\.");
        try {
            JSONObject header = getJson(split[0]);
            JSONObject body = getJson(split[1]);
            long currentEpoch = System.currentTimeMillis()/1000;
            int delta = 3600;   // Return false if the session is going to expire in less than an hour.
                                // better invalidate the session now, instead of probably breaking in the mid of a download.
            return body.getLong("exp") - currentEpoch > delta;
        } catch (JSONException e) {
            Log.w(Impartus.class.getName(), String.format("Error decoding session token, will re-authenticate. Error: %s", e));
            return false;
        }
    }

    private static JSONObject getHeaderFromSessionToken(String sessionToken) {
        String[] split = sessionToken.split("\\.");
        try {
            return Impartus.getJson(split[0]);
        } catch (JSONException e) {
            Log.w(Impartus.class.getName(), String.format("Error decoding header from session token. Error: %s", e));
            return null;
        }
    }
    private static JSONObject getBodyFromSessionToken(String sessionToken) {
        String[] split = sessionToken.split("\\.");
        try {
            return Impartus.getJson(split[1]);
        } catch (JSONException e) {
            Log.w(Impartus.class.getName(), String.format("Error decoding body from session token, will reauthenticate. Error: %s", e));
            return null;
        }
    }

    private static JSONObject getJson(String strEncoded) throws JSONException {
        byte[] decodedBytes = Base64.decode(strEncoded, Base64.URL_SAFE);
        return new JSONObject(new String(decodedBytes, StandardCharsets.UTF_8));
    }


    public String getUserInfo() throws UnsupportedEncodingException {
        if (!isAuthenticated()) {
            Log.w(this.getClass().getName(), "getUserInfo called but user not authenticated.");
            return null;
        }
        int start = sessionToken.indexOf('.') + 1;
        int end = sessionToken.indexOf('.', start);
        String base64TokenField = sessionToken.substring(start, end);
        byte[] data = Base64.decode(base64TokenField, Base64.DEFAULT);
        return new String(data, StandardCharsets.UTF_8);
    }

    public List<LectureItem> getLectures() {
        final List<LectureItem> lectures = new ArrayList<>();


        final List<SubjectItem> subjects = getSubjects();
        int viewIndex = 0;
        for (int i = 0; i < subjects.size(); i++) {
            SubjectItem subjectItem = subjects.get(i);

            String url = String.format("%s/api/subjects/%s/lectures/%s", baseUrl, subjectItem.getId(), subjectItem.getSessionId());
            Response response = sendGetRequest(url, true);

            try {
                if (response.code() == 200) {
                    Log.d(this.getClass().getName(), String.format("Fetched lectures list for %s.", subjectItem.getName()));
                    String body = Objects.requireNonNull(response.body()).string();
                    JSONArray jsonArray = new JSONArray(body);

                    for (int j = 0; j < jsonArray.length(); j++) {
                        JSONObject lecture = jsonArray.getJSONObject(j);

                        int lectureId = Integer.parseInt(lecture.get("ttid").toString());
                        int seqNo = Integer.parseInt(lecture.get("seqNo").toString());
                        String topic = lecture.get("topic").toString();
                        String professorName = lecture.get("professorName").toString();

                        String date = StringUtils.substring(lecture.get("startTime").toString(), 0, 10);

                        int tracks = Integer.parseInt(lecture.get("tapNToggle").toString());
                        int duration = Integer.parseInt(lecture.get("actualDuration").toString());

                        LectureItem lectureItem = new LectureItem(lectureId, seqNo, topic, professorName, date, tracks, duration, subjectItem.getName(), false, viewIndex);
                        lectures.add(lectureItem);
                        viewIndex++;
                        Log.d(this.getClass().getName(), String.format("Added Lecture: %s", topic));
                    }
                } else {
                    Log.e(this.getClass().getName(), "Error authenticating with impartus.");
                    Log.e(this.getClass().getName(), String.format("response body: %s", response.body()));
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }

        }
        return lectures;
    }

    public List<SubjectItem> getSubjects() {
        String url = String.format("%s/api/subjects", baseUrl);
        Response response = sendGetRequest(url, true);
        List<SubjectItem> subjects = new ArrayList<>();

        try {
            if (response.code() == 200) {
                String body = Objects.requireNonNull(response.body()).string();
                JSONArray jsonArray = new JSONArray(body);

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject subject = jsonArray.getJSONObject(i);

                    Long subjectId = Long.parseLong(subject.get("subjectId").toString());
                    String subjectName = subject.get("subjectName").toString();
                    Long sessionId = Long.parseLong(subject.get("sessionId").toString());
                    SubjectItem subjectItem = new SubjectItem(subjectId, subjectName, sessionId);
                    subjects.add(subjectItem);
                }
            } else {
                Log.e(this.getClass().getName(), "Error authenticating with impartus.");
                Log.e(this.getClass().getName(), String.format("response body: %s", response.body()));
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return subjects;
    }

    public boolean downloadLecture(LectureItem item, Callable task) {
        // Encode all ts files into a single output mkv.

        File mkvFilePath = Utils.getMkvFilePath(item, downloadDir);
        if (mkvFilePath.exists()) {
            return true;
        }

        String[] m3u8Content = getM3u8Content(item);

        if (m3u8Content.length > 0) {
            // parse m3u8 file and extract urls for key files, media files for all the tracks.
            M3u8Parser parser = new M3u8Parser();
            List<Track> tracks = parser.parse(m3u8Content, item.getNumTracks());
            Map<Integer, byte[]> encryptionKeys = new HashMap<>();

            // also get a summarized view.
            Map<String, Integer> summary = parser.getSummary();

            List<File> filesToDelete = new ArrayList<>();
            List<File> trackFiles = new ArrayList<>();

            int itemsProcessed = 0;
            File tempDir = Utils.getTempCacheDir(cacheDir, item.getId());
            if (tempDir == null) {
                return false;
            }
            for (Track track : tracks) {

                List<File> tsFiles = new ArrayList<>();
                for (VideoStream stream : track.getStreams()) {

                    // download encrypted stream..
                    File encStreamFilepath = writeStreamToFile(stream, tempDir);
                    filesToDelete.add(encStreamFilepath);

                    // decrypt stream file if encrypted.
                    if (stream.getEncryptionMethod().equals("NONE")) {
                        tsFiles.add(encStreamFilepath);
                    } else {
                        File decryptedStreamFilepath = decryptStreamFile(stream, encStreamFilepath, encryptionKeys, tempDir);

                        tsFiles.add(decryptedStreamFilepath);
                        filesToDelete.add(decryptedStreamFilepath);
                    }

                    // get percentage of items processed.
                    itemsProcessed += 1;
                    Integer mediaFiles = summary.get("mediaFiles");
                    int mediaFilesCount = mediaFiles != null ? Math.max(1, mediaFiles) : 1;

                    // update progress bar here...
                    int itemsProcessedPercent = itemsProcessed * 100 / mediaFilesCount;
                    item.setDownloadPercent(itemsProcessedPercent);
                    task.call(itemsProcessedPercent);
                }   // for each videoStream

                // All stream files for this track are decrypted, join them.
                Log.i(this.getClass().getName(), String.format("[%s]: Joining streams for track %s ..", item.getId(), track.getNumber()));
                File trackFile = Encoder.join(tsFiles, tempDir.getAbsolutePath(), track.getNumber());
                trackFiles.add(trackFile);
                filesToDelete.add(trackFile);
            }   // for track in tracks

            // Get output file path, create any directories required.
            File mkvDirPath = mkvFilePath.getParentFile();
            if (mkvDirPath != null && ! mkvDirPath.exists()) {
                boolean flag = mkvDirPath.mkdir();
                if (!flag) {
                    Log.e(this.getClass().getName(), String.format("Error creating directory %s", mkvDirPath));
                    return false;
                }
            }

            boolean debug = false;

            // encode mkv.
            boolean encodeSuccess = Encoder.encodeMkv(item.getId(), trackFiles, mkvFilePath.getAbsolutePath(), item.getDuration(), item.isFlipped(), debug);

            if (encodeSuccess) {
                item.setOfflinePath(mkvFilePath);
                Log.i(this.getClass().getName(), String.format("[%s]: Processed %s\n---", item.getId(), mkvFilePath));
            }

            // delete temp files.
            if (!debug) {
                for (File file : filesToDelete) {
                    boolean deleteSuccess = file.delete();
                    if (!deleteSuccess) {
                        Log.e(this.getClass().getName(), String.format("Error deleting file %s", file.getAbsolutePath()));
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private File decryptStreamFile(
            final VideoStream stream,
            final File encStreamFilepath,
            final Map<Integer, byte[]> encryptionKeys,
            final File tempDir) {

        byte[] key;
        if (encryptionKeys.get(stream.getKeyId()) == null) {
            Response keyResponse = sendGetRequest(stream.getEncryptionKeyUrl(), true);
            try {
                key = Objects.requireNonNull(keyResponse.body()).bytes();
                key = Arrays.copyOfRange(key, 2, key.length);   // drop 2 bytes.
                for (int i = 0; i < key.length / 2; i++) { // reverse
                    byte tmp = key[i];
                    key[i] = key[key.length - 1 - i];
                    key[key.length - 1 - i] = tmp;
                }
                encryptionKeys.put(stream.getKeyId(), key);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        key = encryptionKeys.get(stream.getKeyId());

        File decryptedStreamFilepath;
        if (key != null) {
            decryptedStreamFilepath = Decrypter.decrypt(key, encStreamFilepath, tempDir.getAbsolutePath());
        } else {
            decryptedStreamFilepath = encStreamFilepath;
        }
        return decryptedStreamFilepath;
    }

    private File writeStreamToFile(final VideoStream stream, final File tempDir) {
        File encStreamFilepath = new File(String.format("%s/%s", tempDir, stream.getFileNumber()));

        boolean downloadFlag = false;

        while (!downloadFlag) {
            try {
                Response streamResponse = sendGetRequest(stream.getUrl(), false);
                FileUtils.copyInputStreamToFile(Objects.requireNonNull(streamResponse.body()).byteStream(), encStreamFilepath);

                downloadFlag = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return encStreamFilepath;
    }

    private List<String> getM3u8Urls(final String masterUrl) {
        Response response = sendGetRequest(masterUrl, true);

        List<String> m3u8Urls = new ArrayList<>();
        if (response.code() == 200) {

            String[] lines = new String[0];
            try {
                lines = Objects.requireNonNull(response.body()).string().split("\\r?\\n");
            } catch (IOException e) {
                e.printStackTrace();
            }

            for (String line : lines) {
                if (line.startsWith("http")) {
                    m3u8Urls.add(line.trim());
                }
            }
        }
        return m3u8Urls;
    }

    private String[] getM3u8Content(LectureItem item) {
        String masterUrl;
        if (item.isFlipped()) {
            masterUrl = String.format("%s/api/fetchvideo?fcid=%s&token=%s&type=index.m3u8", baseUrl, item.getId(), sessionToken);
        } else {
            masterUrl = String.format("%s/api/fetchvideo?ttid=%s&token=%s&type=index.m3u8", baseUrl, item.getId(), sessionToken);
        }
        List<String> m3u8Urls = getM3u8Urls(masterUrl);

        if (m3u8Urls.size() > 0) {
            Response response;
            if (item.isFlipped()) {
                String url;
                if (flippedVideoQuality.equals("highest")) {
                    url = Utils.getUrlForHighestQualityVideo(m3u8Urls);
                } else if (flippedVideoQuality.equals("lowest")) {
                    url = Utils.getUrlForLowestQualityVideo(m3u8Urls);
                } else {
                    url = Utils.getUrlForLectureQuality(m3u8Urls, flippedVideoQuality);
                }
                response = sendGetRequest(url, true);
            } else {
                response = sendGetRequest(m3u8Urls.get(0), true);
            }

            if (response.code() == 200) {
                try {
                    return Objects.requireNonNull(response.body()).string().split("\\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return new String[0];
    }
}
