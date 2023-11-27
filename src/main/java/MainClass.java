import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

public class MainClass {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String channelId = scanner.nextLine();
        parse(channelId);
        parseTextFromDocument("text.txt");
    }

    public static void parseTextFromDocument(String path) {
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            StringBuilder paragraph = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    if (paragraph.length() > 0) {
                        System.out.println(paragraph.toString());
                        downloadYouTubeVideo(paragraph.toString());
                        paragraph.setLength(0);
                    }
                } else {
                    paragraph.append(line).append("\n");
                }
            }

            if (paragraph.length() > 0) {
                System.out.println(paragraph.toString());
                downloadYouTubeVideo(paragraph.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void parse(String channelId) {
    ProcessBuilder processBuilder = new ProcessBuilder(
        "yt-dlp",
        "--get-id",
        "--ignore-config",
        "--flat-playlist",
        "https://www.youtube.com/" + channelId + "/videos"
    );
    processBuilder.redirectOutput(new File("text.txt"));
    try {
        Process process = processBuilder.start();
        int processCode = process.waitFor();
        if (processCode == 0){
            System.out.println("Success");

            List<String> videoIds = Files.readAllLines(Paths.get("text.txt"));
            for (String videoId : videoIds) {
                System.out.println(videoId);
            }
        } else {
            System.out.println("Error");
        }
    } catch (IOException e) {
        throw new RuntimeException(e);
    } catch (InterruptedException e) {
        throw new RuntimeException(e);
    }
}

    public static void downloadYouTubeVideo(String youtubeUrl) {

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String timestamp = dateFormat.format(new Date());
        System.out.println("Enter YouTube video ID");
//        String youtubeVideoId = new Scanner(System.in).nextLine();
        String youtube = "https://www.youtube.com/watch?v=";
        String command = "yt-dlp";

        System.out.println("Enter the path to the directory where to save the video:");
//        String outputDirectory = new Scanner(System.in).nextLine();
        String outputDirectory = "/home/siriusm/Music";
        String outputFilePath = outputDirectory + "/%s.mp4";

        ProcessBuilder processBuilder = new ProcessBuilder(
                command,
                "-o",
                String.format(outputFilePath, timestamp),
                youtube + youtubeUrl
        );
        try {
            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                System.out.println("The video was successfully saved to the selected directory.");
                String videoFilePath = String.format(outputFilePath, timestamp);
                runFFMpegCommand(videoFilePath);
                System.out.println(videoFilePath);
            } else {
                System.err.println("downloadYouTubeVideo Command execution error.");
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static int printVideoDuration(String path) {
        int minutes = 0;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "ffprobe",
                    "-v",
                    "error",
                    "-show_entries",
                    "format=duration",
                    "-of",
                    "default=noprint_wrappers=1:nokey=1",
                    path
            );

            Process process = processBuilder.start();

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String output = reader.readLine();

                if (output != null) {
                    double duration = Double.parseDouble(output);
                    minutes = (int) (duration / 60);
                } else {
                    System.err.println("Could not retrieve video length information.");
                }
            } else {
                System.err.println("printVideoDuration: Command execution error.");
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return minutes;
    }

    public static void runFFMpegCommand(String path) {
        int duration = printVideoDuration(path + ".webm");
        String pathToVideo = "/home/siriusm/Downloads/videos/package/test.mp4";
        System.out.println("Video duration: " + duration + " minutes");
        try {
            for (int i = 0; i < duration; i++) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
                String timestamp = dateFormat.format(new Date());
                String audioFileName = "audio_" + timestamp + ".mp3";
                System.out.println(audioFileName);
                ProcessBuilder processBuilder = new ProcessBuilder(
                        "ffmpeg",
                        "-i",
                        path + ".webm",
                        "-ss",
                        "00:" + i + ":00",
                        "-t",
                        "00:01:00",
                        "-vn",
                        "-c:a",
                        "libmp3lame",
                        "/home/siriusm/Downloads/videos/package/" + audioFileName
                );

                Process process = processBuilder.start();

                int exitCode = process.waitFor();

                if (exitCode == 0) {
                    System.out.println("Command succeeded");
                    mergeAudioAndVideoWithFFMpeg("/home/siriusm/Downloads/videos/package/" + audioFileName, pathToVideo);
                } else {
                    System.err.println("runFFMpegCommand: Command execution error");
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void mergeAudioAndVideoWithFFMpeg(String audioPath, String videoPath) throws IOException, InterruptedException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String timestamp = dateFormat.format(new Date());
        String outputFileName = "/home/siriusm/Downloads/videos/package/tiktokvideos/output_" + timestamp + ".mp4";

        ProcessBuilder processBuilder = new ProcessBuilder(
                "ffmpeg",
                "-i",
                audioPath,
                "-i",
                videoPath,
                "-c:v",
                "copy",
                "-c:a",
                "aac",
                outputFileName
        );

        Process process = processBuilder.start();

        int exitCode = process.waitFor();

        if (exitCode == 0) {
            System.out.println("mergeAudioAndVideoWithFFMpeg Command succeeded");
        } else {
            System.err.println("mergeAudioAndVideoWithFFMpeg Command execution error");
        }
    }
}
