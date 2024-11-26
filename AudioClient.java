import javax.sound.sampled.*;
import java.net.*;
import java.util.Enumeration;

public class AudioClient {
    public static void main(String[] args) {
        try {
            // Get LAN IP address
            String ipAddress = getLanIpAddress();
            if (ipAddress == null) {
                System.out.println("Could not determine LAN IP address. Exiting...");
                return;
            }

            // Encode IP to unique code
            String ipCode = encodeIp(ipAddress);
            System.out.println("Your LAN IP: " + ipAddress);
            System.out.println("Your IP Code: " + ipCode);
            System.out.println("Share this code with the server to start streaming.");

            // Audio format configuration
            AudioFormat format = new AudioFormat(44100.0f, 16, 2, true, false);
            int bufferSize = 512;

            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            if (!AudioSystem.isLineSupported(info)) {
                System.out.println("The specified audio format is not supported for playback!");
                return;
            }

            SourceDataLine speakers = (SourceDataLine) AudioSystem.getLine(info);
            speakers.open(format);
            speakers.start();

            DatagramSocket socket = new DatagramSocket(5005); // Listen on port 5005
            byte[] buffer = new byte[bufferSize];

            System.out.println("Client is receiving audio...");
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                // Play received audio
                speakers.write(packet.getData(), 0, packet.getLength());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Function to get the LAN IP address
    private static String getLanIpAddress() {
        String lastIp = null;
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress inetAddress = inetAddresses.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        lastIp =  inetAddress.getHostAddress(); // Return the LAN IP
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lastIp;
    }

    // Function to encode an IP address into a unique code
    private static String encodeIp(String ipAddress) {
        String[] parts = ipAddress.split("\\.");
        StringBuilder code = new StringBuilder();
        for (String part : parts) {
            int num = Integer.parseInt(part);
            code.append((char) ('A' + (num / 16))); // First letter
            code.append((char) ('A' + (num % 16))); // Second letter
        }
        return code.toString();
    }
}


