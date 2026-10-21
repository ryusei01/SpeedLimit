# Speed Limiter Android (Wi-Fi proxy prototype)

Android 8+ / Java 17 / Android Gradle Plugin 8.7.3.

## What works

- Starts a local HTTP CONNECT proxy on `127.0.0.1:8888`.
- Limits **download throughput** of proxied TCP tunnels to a configured kbps value.
- Works only when the Android Wi-Fi connection supports manual HTTP proxy configuration and the browser honors it.
- No root required. Does not inspect HTTPS contents.

## Important limitations

**This is NOT a mobile-data throttling VPN.** It cannot throttle povo/LINEMO cellular data, all Android apps, QUIC/UDP, or apps that bypass the Wi-Fi HTTP proxy. Android's Wi-Fi proxy UI may not allow loopback host addresses on all devices; if so this prototype cannot be used on that device. It also does not guarantee Edge Canary will honor the proxy. This project is an experimental Wi-Fi proxy prototype, not a finished cellular speed limiter.

The proxy accepts CONNECT only; plain HTTP requests return 405. Actual throughput can be lower or bursty. It limits each TCP tunnel independently, not a global aggregate. Keeping the app in foreground is necessary because there is no foreground service. For testing only; do not use for time-critical purchases.

## Build on GitHub from Android phone

1. Create a new GitHub repository and upload the contents of this folder to its root (including `.github/workflows/build.yml`).
2. Open **Actions** > **Build APK** > **Run workflow** (or push to `main`).
3. After the run succeeds, open the workflow run and download `SpeedLimiter-debug-apk` under Artifacts.
4. Extract `app-debug.apk` from the artifact ZIP and install it on your Android phone.
5. Open the app, set e.g. `1000` kbps, press Start. For a Wi-Fi connection, set manual HTTP proxy host `127.0.0.1` and port `8888`, if supported. Restore proxy to None after testing.

## Security

The server binds only to loopback; it is not accessible from other devices. It forwards HTTPS CONNECT bytes without TLS interception. The debug APK is signed with an auto-generated debug key, not a production release key.
