package jp.id.core;

import org.apache.commons.lang3.RegExUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class M3u8Parser {

    private Map<String, Integer> summary;

    public Map<String, Integer> getSummary() {
        return summary;
    }

    public List<Track> parse(String[] content, int numTracks) {
        List<Track> tracks = new ArrayList<>();
        for(int i=0; i<numTracks; i++) {
            Track t = new Track(i);
            tracks.add(t);
        }

        int currentTrackNumber = 0;
        int currentFileNumber = -1;
        double currentFileDuration = 0.0;
        String currentEncryptionMethod = "NONE";
        String currentEncryptionKeyUrl = null;

        int keyId = 0;
        int keyFiles = 0;
        int mediaFiles = 0;
        double totalDuration = 0.0;

        Track currentTrack = tracks.get(currentTrackNumber);
        for(String line: content) {
            if (line.startsWith("#EXT-X-KEY:METHOD")) {  // encryption algorithm
                currentEncryptionMethod = RegExUtils.replaceFirst(line, "^#EXT-X-KEY:METHOD=([A-Z0-9-]+).*$", "$1");

                if (currentEncryptionMethod.equals("NONE")) {
                    currentEncryptionKeyUrl = null;
                } else {
                    currentEncryptionKeyUrl = RegExUtils.replaceFirst(line, "^#EXT-X-KEY:METHOD=([A-Z0-9-]+).*(http.*)\"$", "$2");
                    keyId = Integer.parseInt(RegExUtils.replaceFirst(currentEncryptionKeyUrl, "^.*keyid=([0-9]+).*$", "$1"));
                    keyFiles += 1;
                }
            } else if (line.startsWith("#EXTINF:")) {  // duration
                currentFileDuration = Double.parseDouble(RegExUtils.replaceFirst(line, "^#EXTINF:([0-9]+\\.[0-9]+),.*", "$1"));
                totalDuration += currentFileDuration;
            } else if (line.startsWith("http")) {  // media file
                currentFileNumber += 1;
                mediaFiles += 1;
                VideoStream stream = new VideoStream(currentFileNumber, currentFileDuration, currentEncryptionKeyUrl, keyId, currentEncryptionMethod, line.trim());
                currentTrack.addStream(stream);
            } else if (line.startsWith("#EXT-X-DISCONTINUITY")) {
                continue; // do we need anything here ?
            } else if (line.startsWith("#EXT-X-MEDIA-SEQUENCE")) {   // switch view
                currentTrackNumber = Integer.parseInt(RegExUtils.replaceFirst(line, "#EXT-X-MEDIA-SEQUENCE:", ""));
                while(currentTrackNumber >= tracks.size()) {   // just in case...
                    Track t = new Track(currentTrackNumber);
                    tracks.add(t);
                }
                currentTrack = tracks.get(currentTrackNumber);
            } else if (line.startsWith("#EXT-X-ENDLIST")) {    // end of streams
                break;
            }
        }

        summary = new HashMap<>();
        summary.put("keyFiles", keyFiles);
        summary.put("mediaFiles", mediaFiles);
        summary.put("totalFiles", keyFiles + mediaFiles);
        summary.put("totalDuration", (int)totalDuration);

        return tracks;
    }
}
