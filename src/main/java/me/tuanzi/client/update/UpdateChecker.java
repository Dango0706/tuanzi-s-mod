package me.tuanzi.client.update;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.tuanzi.util.ModLog;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Environment(EnvType.CLIENT)
public class UpdateChecker {
    public enum Status {
        IDLE, CHECKING, HAS_UPDATE, NO_UPDATE, FAILED, DOWNLOADING, DOWNLOADED, DOWNLOAD_FAILED
    }

    private static Status status = Status.IDLE;
    private static String latestVersion = "";
    private static String changelog = "";
    private static String downloadUrl = "";
    private static String downloadFilename = "";
    private static String errorMessage = "";
    
    // 用于记录是否需要弹出提醒
    private static boolean alreadyNotified = false;
    private static long ignoredTime = 0L;

    private static boolean hasShownFailedNotification = false;
    private static long failedNotificationStartTime = -1L;

    private static final String MODRINTH_PROJECT_ID = "zJPSl9H6";
    private static final String GAME_VERSION = "26.1";

    public static boolean isHasShownFailedNotification() {
        return hasShownFailedNotification;
    }

    public static void setHasShownFailedNotification(boolean shown) {
        hasShownFailedNotification = shown;
    }

    public static long getFailedNotificationStartTime() {
        return failedNotificationStartTime;
    }

    public static void setFailedNotificationStartTime(long startTime) {
        failedNotificationStartTime = startTime;
    }

    public static Status getStatus() {
        return status;
    }

    public static String getLatestVersion() {
        return latestVersion;
    }

    public static String getChangelog() {
        return changelog;
    }

    public static String getDownloadUrl() {
        return downloadUrl;
    }

    public static String getErrorMessage() {
        return errorMessage;
    }

    public static boolean isAlreadyNotified() {
        return alreadyNotified;
    }

    public static void setAlreadyNotified(boolean notified) {
        alreadyNotified = notified;
    }

    public static void loadConfig() {
        if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) {
            return;
        }
        ModLog.info("[自动更新检测] 正在加载本地更新历史配置...");
        try {
            Path configPath = FabricLoader.getInstance().getConfigDir().resolve("tuanzis_mod_update.json");
            if (Files.exists(configPath)) {
                String content = Files.readString(configPath);
                JsonObject json = JsonParser.parseString(content).getAsJsonObject();
                if (json.has("ignoredTime")) {
                    ignoredTime = json.get("ignoredTime").getAsLong();
                    ModLog.info("[自动更新检测] 成功读取到本地忽略时间戳: " + ignoredTime);
                }
            } else {
                ModLog.info("[自动更新检测] 未检测到本地忽略配置文件，将采用默认首选提示规则。");
            }
        } catch (Exception e) {
            ModLog.error("[自动更新检测] 载入本地更新配置文件失败: " + e.getMessage());
        }
    }

    public static void saveIgnoreTime() {
        if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) {
            return;
        }
        ModLog.info("[自动更新检测] 正在写入忽略更新一天的配置...");
        try {
            ignoredTime = System.currentTimeMillis();
            Path configPath = FabricLoader.getInstance().getConfigDir().resolve("tuanzis_mod_update.json");
            Files.createDirectories(configPath.getParent());
            JsonObject json = new JsonObject();
            json.addProperty("ignoredTime", ignoredTime);
            Files.writeString(configPath, json.toString());
            ModLog.info("[自动更新检测] 成功忽略更新一天，忽略时间记为: " + ignoredTime);
        } catch (Exception e) {
            ModLog.error("[自动更新检测] 保存本地忽略时间配置失败: " + e.getMessage());
        }
    }

    public static boolean shouldNotify() {
        if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) {
            return false;
        }
        // 如果本次游戏已经提示并且玩家进行了操作（点击了忽略、暂不），在当前运行周期内绝对不再弹窗，避免死循环挂钩
        if (alreadyNotified) {
            return false;
        }
        
        // 开发沙箱环境：每次重启游戏启动时都会强制显示弹窗提醒，方便调试
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            return status == Status.HAS_UPDATE;
        }
        
        // 检查“忽略更新一天”是否在 24 小时 (86400000 毫秒) 保护期内
        if (ignoredTime > 0L) {
            long elapsed = System.currentTimeMillis() - ignoredTime;
            if (elapsed >= 0 && elapsed < 86400000L) {
                ModLog.info("[自动更新检测] 处于“忽略更新一天”保护期内，本次将不再弹出更新提醒。");
                return false;
            }
        }
        
        return status == Status.HAS_UPDATE;
    }

    public static void checkUpdate() {
        if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) {
            return;
        }
        if (status == Status.CHECKING) {
            ModLog.warn("[自动更新检测] 检测服务已在运行中，请勿重复启动检查任务。");
            return;
        }
        status = Status.CHECKING;
        loadConfig();

        ModLog.info("[自动更新检测] 正在发起 Modrinth 异步更新请求...");
        ModLog.info("[自动更新检测] 目标 Project ID: " + MODRINTH_PROJECT_ID + " | 兼容游戏版本: " + GAME_VERSION + " | 兼容加载器: fabric");

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        String apiRequestUrl = "https://api.modrinth.com/v2/project/" + MODRINTH_PROJECT_ID + "/version";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiRequestUrl))
                .header("User-Agent", "tuanzis_mod/update-checker")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(UpdateChecker::parseAndCompare)
                .exceptionally(ex -> {
                    status = Status.FAILED;
                    errorMessage = ex.getMessage();
                    if (errorMessage == null || errorMessage.isEmpty()) {
                        errorMessage = ex.getClass().getSimpleName();
                    }
                    ModLog.error("[自动更新检测] 网络请求通信发生异常！故障详情: " + errorMessage);
                    return null;
                });
    }

    private static void parseAndCompare(String jsonBody) {
        ModLog.info("[自动更新检测] 成功收到 Modrinth API 数据响应，正在验证正文有效性...");
        try {
            JsonElement parsed = JsonParser.parseString(jsonBody);
            if (!parsed.isJsonArray()) {
                status = Status.FAILED;
                errorMessage = "Invalid API response format";
                ModLog.error("[自动更新检测] 数据正文校验失败：返回的数据结构不为 JSON 数组！格式异常！");
                return;
            }

            JsonArray versions = parsed.getAsJsonArray();
            String currentVersion = FabricLoader.getInstance().getModContainer("tuanzis_mod")
                    .map(container -> container.getMetadata().getVersion().getFriendlyString())
                    .orElse("1.0.5");

            ModLog.info("[自动更新检测] 开始遍历分析 Modrinth 历史发布版本，总共包含 " + versions.size() + " 个候选版本...");
            JsonObject targetVersion = null;
            for (JsonElement element : versions) {
                if (!element.isJsonObject()) continue;
                JsonObject ver = element.getAsJsonObject();
                
                String verNum = ver.has("version_number") ? ver.get("version_number").getAsString() : "未知版本";
                ModLog.info("[自动更新检测] 正在解析版本候选: v" + verNum);

                // 校验 loader 是否包含 fabric
                boolean supportsFabric = false;
                if (ver.has("loaders")) {
                    for (JsonElement loader : ver.getAsJsonArray("loaders")) {
                        if ("fabric".equalsIgnoreCase(loader.getAsString())) {
                            supportsFabric = true;
                            break;
                        }
                    }
                }
                if (!supportsFabric) {
                    ModLog.warn("[自动更新检测] 版本 v" + verNum + " 不支持 fabric 加载器，已跳过。");
                    continue;
                }

                // 校验 game_version 是否包含 26.1
                boolean supportsMC = false;
                if (ver.has("game_versions")) {
                    for (JsonElement gv : ver.getAsJsonArray("game_versions")) {
                        if (GAME_VERSION.equalsIgnoreCase(gv.getAsString())) {
                            supportsMC = true;
                            break;
                        }
                    }
                }
                if (!supportsMC) {
                    ModLog.warn("[自动更新检测] 版本 v" + verNum + " 不支持 Minecraft " + GAME_VERSION + "，已跳过。");
                    continue;
                }

                // 找到第一个满足条件的（即为最新版本，Modrinth 版本默认按发布时间降序）
                targetVersion = ver;
                ModLog.info("[自动更新检测] 成功锁定匹配架构的最新候选版本: v" + verNum);
                break;
            }

            // 如果在开发环境下没找到完全匹配 26.1/fabric 的版本，我们尝试取列表里的第一个作为真实数据的兜底
            if (targetVersion == null) {
                if (FabricLoader.getInstance().isDevelopmentEnvironment() && versions.size() > 0) {
                    targetVersion = versions.get(0).getAsJsonObject();
                    ModLog.info("[自动更新检测][开发环境沙箱] 虽未找到完全匹配 " + GAME_VERSION + "/fabric 的版本，但使用列表第一项作为真实数据兜底。");
                } else {
                    status = Status.NO_UPDATE;
                    ModLog.info("[自动更新检测] 未在 Modrinth 上搜索到任何匹配当前运行环境的可用版本。");
                    return;
                }
            }

            String remoteVer = targetVersion.get("version_number").getAsString();
            String realChangelog = targetVersion.has("changelog") && !targetVersion.get("changelog").isJsonNull() 
                    ? targetVersion.get("changelog").getAsString() 
                    : "无更新日志";
            
            String realDownloadUrl = "";
            String realDownloadFilename = "";
            if (targetVersion.has("files")) {
                JsonArray files = targetVersion.getAsJsonArray("files");
                JsonObject primaryFile = null;
                for (JsonElement f : files) {
                    JsonObject fileObj = f.getAsJsonObject();
                    if (fileObj.has("primary") && fileObj.get("primary").getAsBoolean()) {
                        primaryFile = fileObj;
                        break;
                    }
                }
                if (primaryFile == null && files.size() > 0) {
                    primaryFile = files.get(0).getAsJsonObject();
                }
                if (primaryFile != null) {
                    realDownloadUrl = primaryFile.get("url").getAsString();
                    realDownloadFilename = primaryFile.get("filename").getAsString();
                }
            }

            ModLog.info("[自动更新检测] 正在对比版本：本地版本: v" + currentVersion + " | Modrinth 最新兼容版本: v" + remoteVer);

            boolean isDev = FabricLoader.getInstance().isDevelopmentEnvironment();
            boolean hasNewer = compareVersion(remoteVer, currentVersion) > 0;

            if (hasNewer || isDev) {
                latestVersion = remoteVer;
                changelog = realChangelog;
                downloadUrl = realDownloadUrl;
                downloadFilename = realDownloadFilename;

                if (!downloadUrl.isEmpty()) {
                    status = Status.HAS_UPDATE;
                    if (isDev && !hasNewer) {
                        ModLog.info("[自动更新检测][开发环境沙箱] 虽然本地版本 (v" + currentVersion + ") >= 远程最新版本 (v" + remoteVer + ")，但在开发环境下强行开启弹窗提醒测试！数据完全采用远程真实数据。");
                    } else {
                        ModLog.info("[自动更新检测] 对比结论：Modrinth 存在可用的新版本！");
                    }
                } else {
                    status = Status.NO_UPDATE;
                    ModLog.warn("[自动更新检测] 对比结论：存在新版本，但未能获取到合法下载文件，跳过更新。");
                }
            } else {
                status = Status.NO_UPDATE;
                ModLog.info("[自动更新检测] 对比结论：本地已是最新版本，无需执行任何更新操作。");
            }
        } catch (Exception e) {
            status = Status.FAILED;
            errorMessage = e.getMessage();
            ModLog.error("[自动更新检测] 数据报文解析时发生严重错误: " + e.getMessage());
        }
    }

    public static int compareVersion(String v1, String v2) {
        String[] levels1 = v1.split("\\.");
        String[] levels2 = v2.split("\\.");
        int length = Math.max(levels1.length, levels2.length);
        for (int i = 0; i < length; i++) {
            int val1 = i < levels1.length ? parseVerNum(levels1[i]) : 0;
            int val2 = i < levels2.length ? parseVerNum(levels2[i]) : 0;
            if (val1 < val2) return -1;
            if (val1 > val2) return 1;
        }
        return 0;
    }

    private static int parseVerNum(String str) {
        try {
            return Integer.parseInt(str.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    public static CompletableFuture<Boolean> downloadAndInstall() {
        if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) {
            return CompletableFuture.completedFuture(false);
        }
        if (downloadUrl.isEmpty()) {
            ModLog.error("[自动更新下载] 下载 URL 为空，无法执行更新任务！");
            return CompletableFuture.completedFuture(false);
        }
        status = Status.DOWNLOADING;
        ModLog.info("[自动更新下载] 正在异步启动模组升级下载流程...");
        ModLog.info("[自动更新下载] 源文件 URL: " + downloadUrl);

        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(downloadUrl))
                        .GET()
                        .build();

                ModLog.info("[自动更新下载] 正在建立与下载服务器的 HTTP 连接...");
                HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
                ModLog.info("[自动更新下载] 响应状态码: " + response.statusCode());
                if (response.statusCode() != 200) {
                    status = Status.DOWNLOAD_FAILED;
                    ModLog.error("[自动更新下载] 服务器下载连接建立失败，状态码异常！");
                    return false;
                }

                Path modsFolder = FabricLoader.getInstance().getGameDir().resolve("mods");
                if (!Files.exists(modsFolder)) {
                    ModLog.info("[自动更新下载] 未在游戏根目录中检测到 mods 文件夹，正在自动创建...");
                    Files.createDirectories(modsFolder);
                }

                // 物理扫描 mods 目录，精确定位 Default File System 下的物理旧 .jar，避开 ZipFileSystem Path 引起的格式或不支持异常
                Path originPath = null;
                Path bakPath = null;
                if (Files.exists(modsFolder)) {
                    try (java.util.stream.Stream<Path> list = Files.list(modsFolder)) {
                        originPath = list.filter(p -> Files.isRegularFile(p)
                                && p.getFileName().toString().startsWith("tuanzis_mod-")
                                && p.getFileName().toString().endsWith(".jar"))
                                .findFirst()
                                .orElse(null);
                    } catch (Exception ex) {
                        ModLog.warn("[自动更新下载] 物理扫描 mods 文件夹寻找旧包失败: " + ex.getMessage());
                    }
                }
                
                if (originPath != null) {
                    bakPath = originPath.resolveSibling(originPath.getFileName().toString() + ".bak");
                }

                Path targetPath = modsFolder.resolve(downloadFilename);
                boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

                if (isWindows && originPath != null && bakPath != null) {
                    // Windows 平台：使用预先异步调起的锁检测批处理脚本，在游戏进程以任意方式关闭释放文件锁时瞬间完成替换
                    Path tmpTargetPath = targetPath.resolveSibling(downloadFilename + ".tmp");
                    ModLog.info("[自动更新下载] 运行系统检测为 Windows，为防运行中文件锁定，将新包暂存至: " + tmpTargetPath.getFileName());
                    
                    try (InputStream is = response.body()) {
                        Files.copy(is, tmpTargetPath, StandardCopyOption.REPLACE_EXISTING);
                    }

                    // 创建延迟替换批处理脚本 (update.bat)
                    // 使用 loop 循环检测 ren 操作是否成功，一旦成功说明游戏已经退出释放了文件锁，随后执行覆盖并自我销毁
                    Path batPath = modsFolder.resolve("update.bat");
                    String originFileStr = originPath.toAbsolutePath().toString();
                    String bakFileName = bakPath.getFileName().toString();
                    String tmpFileStr = tmpTargetPath.toAbsolutePath().toString();
                    String targetFileStr = targetPath.toAbsolutePath().toString();

                    String batContent = "@echo off\r\n" +
                            ":loop\r\n" +
                            "if exist \"" + originFileStr + "\" (\r\n" +
                            "    ren \"" + originFileStr + "\" \"" + bakFileName + "\" >nul 2>nul\r\n" +
                            "    if exist \"" + originFileStr + "\" (\r\n" +
                            "        timeout /t 1 /nobreak > nul\r\n" +
                            "        goto loop\r\n" +
                            "    )\r\n" +
                            ")\r\n" +
                            "if exist \"" + tmpFileStr + "\" (\r\n" +
                            "    move /y \"" + tmpFileStr + "\" \"" + targetFileStr + "\" >nul 2>nul\r\n" +
                            ")\r\n" +
                            "del \"%~f0\"\r\n";
                    
                    Files.writeString(batPath, batContent);
                    ModLog.info("[自动更新下载] Windows 锁检测延迟替换脚本生成成功: " + batPath.toAbsolutePath());

                    // 下载完成的瞬间立刻异步拉起该脚本，完全脱离游戏生命周期。不管玩家是重启、关闭游戏、点红叉还是强退，均能成功捕获锁释放并替换！
                    try {
                        new ProcessBuilder("cmd.exe", "/c", "start", "/b", "update.bat")
                                .directory(modsFolder.toFile())
                                .start();
                        ModLog.info("[自动更新下载] 已在后台异步拉起锁检测更新进程，等待客户端释放文件锁。");
                    } catch (Exception ex) {
                        ModLog.error("[自动更新下载] 启动 Windows 后台锁检测批处理进程失败: " + ex.getMessage());
                    }
                } else {
                    // 非 Windows 平台 (Linux/macOS) 或未定位到源 jar: 直接执行文件移动与覆盖
                    if (originPath != null && bakPath != null) {
                        ModLog.info("[自动更新下载] 正在执行安全重命名，将旧的加载包: " + originPath.getFileName() + " 重命名为 " + bakPath.getFileName());
                        Files.move(originPath, bakPath, StandardCopyOption.REPLACE_EXISTING);
                        ModLog.info("[自动更新下载] 旧包安全重命名完成。");
                    } else {
                        ModLog.warn("[自动更新下载] 未定位到物理 .jar 加载源文件，跳过备份重命名流程。");
                    }

                    ModLog.info("[自动更新下载] 正在将下载输入流写入至目标路径: " + targetPath.toAbsolutePath());
                    try (InputStream is = response.body()) {
                        Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                }

                status = Status.DOWNLOADED;
                ModLog.info("[自动更新下载] 模组包自动升级准备就绪！重启游戏即可应用新版本。");
                return true;
            } catch (Exception e) {
                status = Status.DOWNLOAD_FAILED;
                errorMessage = e.getMessage();
                ModLog.error("[自动更新下载] 升级包在下载或覆盖落盘时发生不可逆异常！异常详情: " + e.getMessage());
                return false;
            }
        });
    }
}
