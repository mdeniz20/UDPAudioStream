import javax.sound.sampled.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class AudioServer {
    private static volatile boolean isPaused = false;
    private static volatile boolean isStopped = false;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Collect client codes
        List<InetAddress> clientAddresses = new ArrayList<>();
        System.out.println("Enter client codes (type '-end' to finish):");
        while (true) {
            String code = scanner.nextLine();
            if (code.equalsIgnoreCase("-end")) {
                break;
            }
            String ip = decodeIp(code);
            if (ip != null) {
                try {
                    clientAddresses.add(InetAddress.getByName(ip));
                    System.out.println("Added client with IP: " + ip);
                } catch (UnknownHostException e) {
                    System.out.println("Error decoding IP for code: " + code);
                }
            } else {
                System.out.println("Invalid code: " + code);
            }
        }

        if (clientAddresses.isEmpty()) {
            System.out.println("No clients to stream to. Exiting...");
            return;
        }

        // Ask for the audio file path
        System.out.println("Enter the path to the WAV file to stream:");
        String filePath = scanner.nextLine();
        File audioFile = new File(filePath);

        if (!audioFile.exists()) {
            System.out.println("File not found! Exiting...");
            return;
        }

        System.out.println("Streaming audio. Use the following commands:");
        System.out.println("p: Pause");
        System.out.println("r: Resume");
        System.out.println("s: Stop");

        // Control thread
        Thread controlThread = new Thread(() -> {
            while (true) {
                String command = scanner.nextLine();
                switch (command.toLowerCase()) {
                    case "p":
                        isPaused = true;
                        System.out.println("Playback paused.");
                        break;
                    case "r":
                        isPaused = false;
                        isStopped = false;
                        System.out.println("Playback resumed.");
                        break;
                    case "s":
                        isStopped = true;
                        System.out.println("Playback stopped.");
                        break;
                    default:
                        System.out.println("Unknown command. Use p (pause), r (resume), s (stop).");
                }
            }
        });

        controlThread.start();

        try (DatagramSocket socket = new DatagramSocket()) {
            List<Integer> clientPorts = new ArrayList<>();
            for (int i = 0; i < clientAddresses.size(); i++) {
                clientPorts.add(5005); // Default port for all clients
            }

            // Open audio file and stream
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile);
            AudioFormat baseFormat = audioInputStream.getFormat();
            AudioFormat targetFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    44100, // Sample rate
                    16,    // Sample size in bits
                    baseFormat.getChannels(),
                    baseFormat.getChannels() * 2, // Frame size
                    44100, // Frame rate
                    false  // Little endian
            );

            AudioInputStream pcmStream = AudioSystem.getAudioInputStream(targetFormat, audioInputStream);

            byte[] buffer = new byte[512]; // Smaller buffer size
            int frameSize = targetFormat.getFrameSize();
            float frameRate = targetFormat.getFrameRate();
            long packetDuration = (long) ((buffer.length / (frameRate * frameSize)) * 1_000_000_000); // Duration in nanoseconds
            int bytesRead;

            System.out.println("Streaming WAV audio to clients...");
            long nextSendTime = System.nanoTime();
            while ((bytesRead = pcmStream.read(buffer)) != -1) {
                if (isStopped) {
                    break;
                }

                while (isPaused) {
                    Thread.sleep(100); // Wait while paused
                }

                // Send packet to all clients
                for (int i = 0; i < clientAddresses.size(); i++) {
                    DatagramPacket packet = new DatagramPacket(buffer, bytesRead, clientAddresses.get(i), clientPorts.get(i));
                    socket.send(packet);
                }
                                // Synchronize to maintain timing
                nextSendTime += packetDuration;
                long sleepTime = nextSendTime - System.nanoTime();
                if (sleepTime > 0) {
                    Thread.sleep(sleepTime / 1_000_000, (int) (sleepTime % 1_000_000));
                }

            }

            System.out.println("Streaming finished or stopped.");
            pcmStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Function to decode a unique code back into an IP address
    private static String decodeIp(String code) {
        if (code.length() != 8) {
            return null; // Invalid code length
        }
        StringBuilder ip = new StringBuilder();
        for (int i = 0; i < code.length(); i += 2) {
            int num = (code.charAt(i) - 'A') * 16 + (code.charAt(i + 1) - 'A');
            ip.append(num);
            if (i < 6) ip.append(".");
        }
        return ip.toString();
    }
}

