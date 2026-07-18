package com.tireshop.service;

import com.tireshop.util.SettingsService;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.function.DoubleConsumer;
import java.util.logging.Logger;

/**
 * In-app updater.
 *
 * Checks a GitHub repository's latest Release for a newer version, downloads the
 * attached JAR, backs up the current one, and swaps them via a helper batch script
 * (a running JAR cannot overwrite itself on Windows). If the new version fails to
 * start, the script automatically restores the backup - the app always comes back.
 *
 * Release workflow (developer machine):
 *   1. Bump <version> in pom.xml and run: mvn clean package
 *   2. Create a GitHub release tagged v<version> (e.g. v1.2.0)
 *   3. Attach target/tire-shop-pos-<version>.jar renamed to tire-shop-pos.jar
 * Clients pick it up from Admin -> General Settings -> Updates.
 */
public class UpdateService {

    private static final Logger LOGGER = Logger.getLogger(UpdateService.class.getName());

    // Settings keys (stored in config.properties)
    public static final String KEY_OWNER = "update.github.owner";
    public static final String KEY_REPO = "update.github.repo";
    public static final String KEY_AUTO_CHECK = "update.auto.check";
    public static final String KEY_RESTART_CMD = "update.restart.command";

    private static final String FLAG_FILE = "update-in-progress.flag";
    private static final String DOWNLOAD_DIR = "update-download";
    private static final String BACKUP_DIR = "backups";
    private static final String APPLY_SCRIPT = "apply-update.bat";
    private static final String DEFAULT_RESTART_CMD = "start \"\" javaw -jar \"{JAR}\"";

    private final SettingsService settingsService;
    private final HttpClient httpClient;

    public UpdateService(SettingsService settingsService) {
        this.settingsService = settingsService;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /** Details about an available update */
    public static class UpdateInfo {
        public String version;
        public String downloadUrl;
        public String assetName;
        public String releaseNotes;
        public String sha256; // may be null
        public long size;

        public String getVersion() { return version; }
        public String getReleaseNotes() { return releaseNotes; }
        public long getSize() { return size; }
    }

    // ------------------------------------------------------------------
    // Version helpers
    // ------------------------------------------------------------------

    /** The version of the running application (from version.properties, set by Maven) */
    public String getCurrentVersion() {
        try (InputStream in = getClass().getResourceAsStream("/version.properties")) {
            if (in != null) {
                Properties props = new Properties();
                props.load(in);
                String version = props.getProperty("app.version");
                if (version != null && !version.contains("${")) {
                    return version.trim();
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Could not read version.properties: " + e.getMessage());
        }
        return "0.0.0";
    }

    /**
     * Compare two version strings (e.g. "1.2.0" vs "v1.10").
     * @return negative if a < b, zero if equal, positive if a > b
     */
    public static int compareVersions(String a, String b) {
        String[] partsA = normalizeVersion(a).split("\\.");
        String[] partsB = normalizeVersion(b).split("\\.");
        int len = Math.max(partsA.length, partsB.length);
        for (int i = 0; i < len; i++) {
            int numA = i < partsA.length ? parseVersionPart(partsA[i]) : 0;
            int numB = i < partsB.length ? parseVersionPart(partsB[i]) : 0;
            if (numA != numB) {
                return numA - numB;
            }
        }
        return 0;
    }

    private static String normalizeVersion(String version) {
        if (version == null) {
            return "0";
        }
        String v = version.trim();
        if (v.startsWith("v") || v.startsWith("V")) {
            v = v.substring(1);
        }
        int dash = v.indexOf('-'); // strip -SNAPSHOT etc.
        if (dash >= 0) {
            v = v.substring(0, dash);
        }
        return v;
    }

    private static int parseVersionPart(String part) {
        try {
            return Integer.parseInt(part.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // ------------------------------------------------------------------
    // Update check
    // ------------------------------------------------------------------

    public boolean isConfigured() {
        String owner = settingsService.getSetting(KEY_OWNER, "");
        String repo = settingsService.getSetting(KEY_REPO, "");
        return !owner.isEmpty() && !repo.isEmpty();
    }

    /**
     * Check GitHub for a newer release.
     * @return UpdateInfo if a newer version exists, null if already up to date
     * @throws Exception on network/API errors or if not configured
     */
    public UpdateInfo checkForUpdate() throws Exception {
        if (!isConfigured()) {
            throw new IllegalStateException("Update source not configured. Set the GitHub owner and repository in Admin settings.");
        }

        String owner = settingsService.getSetting(KEY_OWNER, "");
        String repo = settingsService.getSetting(KEY_REPO, "");
        String url = "https://api.github.com/repos/" + owner + "/" + repo + "/releases/latest";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "tireshop-pos-updater")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 404) {
            throw new IOException("No releases found for " + owner + "/" + repo);
        }
        if (response.statusCode() != 200) {
            throw new IOException("GitHub API error (HTTP " + response.statusCode() + ")");
        }

        JSONObject release = new JSONObject(response.body());
        String tag = release.optString("tag_name", "");
        if (tag.isEmpty()) {
            throw new IOException("Latest release has no tag");
        }

        String latestVersion = normalizeVersion(tag);
        String currentVersion = getCurrentVersion();
        LOGGER.info("Update check: current=" + currentVersion + " latest=" + latestVersion);

        if (compareVersions(latestVersion, currentVersion) <= 0) {
            return null; // Up to date
        }

        // Find the JAR asset
        JSONArray assets = release.optJSONArray("assets");
        if (assets == null || assets.isEmpty()) {
            throw new IOException("Release " + tag + " has no attached files. Attach the built JAR to the release.");
        }

        JSONObject jarAsset = null;
        for (int i = 0; i < assets.length(); i++) {
            JSONObject asset = assets.getJSONObject(i);
            String name = asset.optString("name", "");
            if ("tire-shop-pos.jar".equals(name)) {
                jarAsset = asset;
                break;
            }
            if (name.endsWith(".jar") && jarAsset == null) {
                jarAsset = asset;
            }
        }
        if (jarAsset == null) {
            throw new IOException("Release " + tag + " has no JAR file attached.");
        }

        UpdateInfo info = new UpdateInfo();
        info.version = latestVersion;
        info.assetName = jarAsset.getString("name");
        info.downloadUrl = jarAsset.getString("browser_download_url");
        info.size = jarAsset.optLong("size", -1);
        info.releaseNotes = release.optString("body", "");
        // GitHub provides a digest for newer releases, e.g. "sha256:abc123..."
        String digest = jarAsset.optString("digest", "");
        if (digest.startsWith("sha256:")) {
            info.sha256 = digest.substring("sha256:".length());
        }
        return info;
    }

    // ------------------------------------------------------------------
    // Download
    // ------------------------------------------------------------------

    /**
     * Download the update JAR to the update-download directory.
     * @param progress receives values 0.0 - 1.0 (may be called from a background thread)
     * @return path to the downloaded file
     */
    public Path download(UpdateInfo info, DoubleConsumer progress) throws Exception {
        Path dir = Paths.get(DOWNLOAD_DIR);
        Files.createDirectories(dir);
        Path dest = dir.resolve("tire-shop-pos-" + info.version + ".jar");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(info.downloadUrl))
                .timeout(Duration.ofMinutes(10))
                .header("User-Agent", "tireshop-pos-updater")
                .GET()
                .build();

        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            throw new IOException("Download failed (HTTP " + response.statusCode() + ")");
        }

        long total = response.headers().firstValueAsLong("Content-Length").orElse(info.size);
        try (InputStream in = response.body(); OutputStream out = Files.newOutputStream(dest)) {
            byte[] buffer = new byte[8192];
            long downloaded = 0;
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                downloaded += read;
                if (progress != null && total > 0) {
                    progress.accept((double) downloaded / total);
                }
            }
        }

        // Integrity check when GitHub provided a digest
        if (info.sha256 != null) {
            String actual = sha256Hex(dest);
            if (!actual.equalsIgnoreCase(info.sha256)) {
                Files.deleteIfExists(dest);
                throw new IOException("Downloaded file failed integrity check. Please try again.");
            }
        }

        LOGGER.info("Update downloaded to: " + dest.toAbsolutePath());
        return dest;
    }

    private static String sha256Hex(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : digest.digest()) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // Apply / rollback
    // ------------------------------------------------------------------

    /** True when running from an installed JAR (updates can't be applied from an IDE/classes dir) */
    public boolean isRunningFromJar() {
        try {
            Path location = getRunningLocation();
            return location != null && location.toString().endsWith(".jar");
        } catch (Exception e) {
            return false;
        }
    }

    private Path getRunningLocation() throws Exception {
        return Paths.get(UpdateService.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI()).toAbsolutePath();
    }

    /**
     * Back up the current JAR and launch the helper script that swaps in the new
     * version and restarts the app. The caller should exit the application
     * immediately after this returns (the script waits for the app to close).
     *
     * @return the backup path (for display to the user)
     */
    public Path applyUpdateAndRestart(Path downloadedJar, String newVersion) throws Exception {
        if (!isRunningFromJar()) {
            throw new IllegalStateException("Updates can only be installed when running from the installed application.");
        }

        Path jar = getRunningLocation();
        Path appDir = jar.getParent();

        // 1. Back up the current JAR (this is the fallback)
        Path backupDir = appDir.resolve(BACKUP_DIR);
        Files.createDirectories(backupDir);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path backup = backupDir.resolve("app-backup-" + getCurrentVersion() + "-" + timestamp + ".jar");
        Files.copy(jar, backup, StandardCopyOption.REPLACE_EXISTING);
        LOGGER.info("Current version backed up to: " + backup);

        // 2. Write the helper script
        String restartCmd = settingsService.getSetting(KEY_RESTART_CMD, "").trim();
        if (restartCmd.isEmpty()) {
            restartCmd = detectRestartCommand(jar);
        }
        restartCmd = restartCmd.replace("{JAR}", jar.toString());

        Path script = appDir.resolve(APPLY_SCRIPT);
        String scriptContent = buildApplyScript(jar, downloadedJar.toAbsolutePath(), backup,
                appDir.resolve(FLAG_FILE), restartCmd);
        Files.write(script, scriptContent.getBytes("UTF-8"));

        // 3. Launch the script detached, then the caller exits the app
        new ProcessBuilder("cmd", "/c", "start", "\"TireShopPOS-Update\"",
                "/min", "cmd", "/c", "\"" + script + "\"")
                .directory(appDir.toFile())
                .start();

        LOGGER.info("Update script launched. Application should now close to complete the update.");
        return backup;
    }

    /**
     * Work out how to relaunch the app after an update. When running from a
     * jpackage app-image the jar lives at <install>\app\*.jar with the launcher
     * exe one level up - prefer that exe (it uses the bundled runtime, so no
     * system Java is needed). Fall back to "javaw -jar" otherwise.
     */
    private String detectRestartCommand(Path jar) {
        try {
            Path appDir = jar.getParent();
            Path installRoot = appDir != null ? appDir.getParent() : null;
            if (installRoot != null && Files.isDirectory(installRoot)) {
                try (java.util.stream.Stream<Path> stream = Files.list(installRoot)) {
                    java.util.Optional<Path> exe = stream
                            .filter(p -> p.getFileName() != null
                                    && p.getFileName().toString().toLowerCase().endsWith(".exe"))
                            .findFirst();
                    if (exe.isPresent()) {
                        LOGGER.info("Restarting via launcher exe: " + exe.get());
                        return "start \"\" \"" + exe.get().toString() + "\"";
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Could not auto-detect launcher exe, using javaw fallback: " + e.getMessage());
        }
        return DEFAULT_RESTART_CMD;
    }

    /**
     * Batch script: wait for app exit -> swap JAR -> restart -> if the app does not
     * confirm a successful start within 90 seconds, restore the backup and restart it.
     */
    private String buildApplyScript(Path jar, Path newJar, Path backup, Path flagFile, String restartCmd) {
        StringBuilder sb = new StringBuilder();
        sb.append("@echo off\r\n");
        sb.append("setlocal\r\n");
        sb.append("set \"JAR=").append(jar).append("\"\r\n");
        sb.append("set \"NEW=").append(newJar).append("\"\r\n");
        sb.append("set \"BACKUP=").append(backup).append("\"\r\n");
        sb.append("set \"FLAG=").append(flagFile).append("\"\r\n");
        sb.append("echo Applying Tire Shop POS update...\r\n");
        // Wait for the application to close
        sb.append("timeout /t 4 /nobreak >nul\r\n");
        // Swap in the new JAR (retry while the old process releases the file)
        sb.append("set /a tries=0\r\n");
        sb.append(":copyloop\r\n");
        sb.append("set /a tries+=1\r\n");
        sb.append("copy /y \"%NEW%\" \"%JAR%\" >nul 2>&1\r\n");
        sb.append("if errorlevel 1 (\r\n");
        sb.append("    if %tries% geq 30 goto rollback\r\n");
        sb.append("    timeout /t 1 /nobreak >nul\r\n");
        sb.append("    goto copyloop\r\n");
        sb.append(")\r\n");
        // Mark update in progress and start the new version
        sb.append("echo updating > \"%FLAG%\"\r\n");
        sb.append(restartCmd).append("\r\n");
        // Wait for the app to confirm startup by deleting the flag (done in Main.start)
        sb.append("set /a waits=0\r\n");
        sb.append(":waitloop\r\n");
        sb.append("if not exist \"%FLAG%\" goto success\r\n");
        sb.append("set /a waits+=1\r\n");
        sb.append("if %waits% geq 90 goto rollback\r\n");
        sb.append("timeout /t 1 /nobreak >nul\r\n");
        sb.append("goto waitloop\r\n");
        // Success path
        sb.append(":success\r\n");
        sb.append("del \"%NEW%\" >nul 2>&1\r\n");
        sb.append("echo Update completed successfully.\r\n");
        sb.append("timeout /t 3 >nul\r\n");
        sb.append("exit /b 0\r\n");
        // Rollback path
        sb.append(":rollback\r\n");
        sb.append("echo Update failed - restoring previous version...\r\n");
        sb.append("copy /y \"%BACKUP%\" \"%JAR%\" >nul\r\n");
        sb.append("del \"%FLAG%\" >nul 2>&1\r\n");
        sb.append(restartCmd).append("\r\n");
        sb.append("echo Previous version restored.\r\n");
        sb.append("timeout /t 5 >nul\r\n");
        sb.append("exit /b 1\r\n");
        return sb.toString();
    }

    /**
     * Called on application startup. If an update flag exists, the app has just been
     * updated - deleting the flag confirms to the update script that the new version
     * started successfully (otherwise it rolls back automatically).
     *
     * @return true if this launch follows an update
     */
    public static boolean confirmSuccessfulBootAfterUpdate() {
        try {
            Path location = Paths.get(UpdateService.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()).toAbsolutePath();
            Path appDir = location.getParent();
            if (appDir == null) {
                return false;
            }
            Path flag = appDir.resolve(FLAG_FILE);
            if (Files.exists(flag)) {
                Files.delete(flag); // signals success to the waiting update script
                LOGGER.info("Update confirmed - new version started successfully.");
                return true;
            }
        } catch (Exception e) {
            LOGGER.warning("Could not confirm update boot: " + e.getMessage());
        }
        return false;
    }
}
