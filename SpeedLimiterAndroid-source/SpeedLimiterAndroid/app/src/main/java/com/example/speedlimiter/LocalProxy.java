package com.example.speedlimiter;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

// Local HTTP CONNECT proxy. Intended for testing on the user's own Wi-Fi network.
public final class LocalProxy {
    private final int port;
    private final long bytesPerSecond;
    private volatile boolean running;
    private ServerSocket server;
    private final ExecutorService workers = Executors.newCachedThreadPool();
    LocalProxy(int port, int kbps) { this.port = port; this.bytesPerSecond = kbps * 1000L / 8L; }
    void start() throws IOException {
        server = new ServerSocket();
        server.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), port));
        running = true;
        workers.submit(() -> {
            while (running) {
                try { Socket client = server.accept(); workers.submit(() -> handle(client)); }
                catch (IOException e) { if (running) e.printStackTrace(); }
            }
        });
    }
    private void handle(Socket client) {
        try (Socket c = client) {
            c.setSoTimeout(30000);
            InputStream in = c.getInputStream(); OutputStream out = c.getOutputStream();
            String request = readLine(in);
            if (request == null) return;
            String[] parts = request.split(" ");
            if (parts.length < 2 || !parts[0].equalsIgnoreCase("CONNECT")) {
                out.write("HTTP/1.1 405 Method Not Allowed\r\nContent-Length: 0\r\n\r\n".getBytes(StandardCharsets.US_ASCII)); return;
            }
            String hostPort = parts[1]; int split = hostPort.lastIndexOf(':');
            if (split < 1) return;
            String host = hostPort.substring(0, split);
            if (host.startsWith("[") && host.endsWith("]")) host = host.substring(1, host.length()-1);
            int remotePort = Integer.parseInt(hostPort.substring(split+1));
            if (remotePort < 1 || remotePort > 65535) return;
            String line;
            while ((line = readLine(in)) != null && !line.isEmpty()) { /* consume headers */ }
            try (Socket remote = new Socket()) {
                remote.connect(new InetSocketAddress(host, remotePort), 10000);
                remote.setSoTimeout(30000);
                out.write("HTTP/1.1 200 Connection Established\r\n\r\n".getBytes(StandardCharsets.US_ASCII)); out.flush();
                Future<?> upstream = workers.submit(() -> copy(in, remote, remote.getOutputStream(), false));
                copy(remote.getInputStream(), c, out, true);
                upstream.cancel(true);
            }
        } catch (Exception ignored) { }
    }
    private static String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream(); int x;
        while ((x=in.read())!=-1) {
            if (x=='\n') break;
            if (b.size()>8192) throw new IOException("Header too long");
            if (x!='\r') b.write(x);
        }
        if (x==-1 && b.size()==0) return null;
        return b.toString("US-ASCII");
    }
    private void copy(InputStream in, Socket other, OutputStream out, boolean limit) {
        byte[] buf = new byte[4096];
        long start = System.nanoTime(); long sent = 0;
        try {
            int n;
            while (!Thread.currentThread().isInterrupted() && (n=in.read(buf))!=-1) {
                if (limit) {
                    sent += n;
                    long due = start + sent * 1_000_000_000L / bytesPerSecond;
                    long wait = due - System.nanoTime();
                    if (wait > 0) TimeUnit.NANOSECONDS.sleep(wait);
                }
                out.write(buf, 0, n); out.flush();
            }
        } catch (Exception ignored) { }
        try { other.shutdownOutput(); } catch (IOException ignored) { }
    }
    void close() {
        running = false;
        try { if (server != null) server.close(); } catch (IOException ignored) { }
        workers.shutdownNow();
    }
}
