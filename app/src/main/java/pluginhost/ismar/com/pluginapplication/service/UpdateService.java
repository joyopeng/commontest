package pluginhost.ismar.com.pluginapplication.service;

import android.app.Service;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.blankj.utilcode.utils.AppUtils;
import com.blankj.utilcode.utils.FileUtils;
import com.blankj.utilcode.utils.StringUtils;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import cn.ismartv.downloader.DownloadEntity;
import cn.ismartv.downloader.DownloadManager;
import cn.ismartv.downloader.DownloadStatus;
import cn.ismartv.injectdb.library.content.ContentProvider;
import cn.ismartv.injectdb.library.query.Select;
import pluginhost.ismar.com.pluginapplication.core.Md5;
import pluginhost.ismar.com.pluginapplication.entity.UpgradeRequestEntity;
import pluginhost.ismar.com.pluginapplication.entity.VersionInfoV2Entity;
import rx.Observer;
import rx.schedulers.Schedulers;

/**
 * Created by huibin on 10/20/16.
 */

public class UpdateService extends Service implements Loader.OnLoadCompleteListener<Cursor> {
    public static final String APP_UPDATE_ACTION = "cn.ismartv.vod.action.app_update";
    private static final String TAG = "UpdateService";
    private SkyService mSkyService;

    private File upgradeFile;
    private CursorLoader mCursorLoader;

    public static final int LOADER_ID_APP_UPDATE = 0xca;

    private CopyOnWriteArrayList<String> md5Jsons;
    private boolean isPluginUpdate;

//    private VersionInfoV2Entity mVersionInfoV2Entity;

    public static final int INSTALL_SILENT = 0x7c;

    private volatile boolean isInstallSilent = false;

    public static boolean installAppLoading = false;

    public volatile boolean isMD5Error = false;
    private Boolean force_upgrade_all;
    private String plugins_log;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        upgradeFile = Environment.getExternalStorageDirectory();
        mSkyService = SkyService.ServiceManager.getUpgradeService();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        fetchAppUpgrade();
        return super.onStartCommand(intent, flags, startId);
    }

    private void fetchAppUpgrade() {
        isMD5Error = false;
        SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
//        String sn = mSharedPreferences.getString("sn_token", "");
//        if (TextUtils.isEmpty(sn)) {
//            return;
//        }
        String manu = "sharp";
        String app = "sky";
//        String modelName = Build.PRODUCT.replace(" ", "_");
//        String location = IsmartvActivator.getInstance().getProvince().get("province_py");
//        int versionCode = 222;

        int versionCode = fetchInstallVersionCode();

        List<UpgradeRequestEntity> upgradeRequestEntities = new ArrayList<>();
        UpgradeRequestEntity requestEntity = new UpgradeRequestEntity();
        requestEntity.setApp(app);
        requestEntity.setLoc("BJ");
        requestEntity.setManu("sharp");
        requestEntity.setModelname("lcd_s3a01");
        requestEntity.setSn("lcd_uf30a");
        requestEntity.setVer(String.valueOf(290));

        upgradeRequestEntities.add(requestEntity);

        mSkyService.appUpgrade(upgradeRequestEntities)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())
                .subscribe(new Observer<VersionInfoV2Entity>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        checkRemaindUpdateFile();
                        checkRemaindUpdateFile();
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(VersionInfoV2Entity versionInfoV2Entity) {
                        if (mCursorLoader != null) {
                            mCursorLoader.unregisterListener(UpdateService.this);
                        }
                        force_upgrade_all = versionInfoV2Entity.isForce_upgrade_all();
                        plugins_log = versionInfoV2Entity.getPlugins().getPlugins_log();
                        Log.v(TAG, versionInfoV2Entity.getPlugins().getPlugins_list().get(0).getPlugin_params().toString());
                        if (versionInfoV2Entity.getPlugins() != null) {
                            isPluginUpdate = true;
                        }
                        md5Jsons = new CopyOnWriteArrayList<String>();
                        String title;
                        String selection = "title in (";
                        if (versionInfoV2Entity.getPlugins() != null) {
                            for (VersionInfoV2Entity.PluginItem applicationEntity : versionInfoV2Entity.getPlugins().getPlugins_list()) {
                                title = Md5.md5(new Gson().toJson(applicationEntity));
                                md5Jsons.add(title);
                                checkPluginUpgrade(applicationEntity);
                                selection += "?,";
                            }
                        } else {
                            for (VersionInfoV2Entity.ApplicationEntity applicationEntity : versionInfoV2Entity.getUpgrades()) {
                                title = Md5.md5(new Gson().toJson(applicationEntity));
                                md5Jsons.add(title);
                                checkUpgrade(applicationEntity);
                                selection += "?,";
                            }
                        }
                        selection = selection.substring(0, selection.length() - 1);
                        selection += ")";


                        final String finalSelection = selection;
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                if (mCursorLoader != null) {
                                    mCursorLoader.reset();
                                }

                                mCursorLoader = new CursorLoader(getApplicationContext(), ContentProvider.createUri(DownloadEntity.class, null),
                                        null, finalSelection, md5Jsons.toArray(new String[]{}), null);
                                mCursorLoader.registerListener(LOADER_ID_APP_UPDATE, UpdateService.this);
                                mCursorLoader.startLoading();
                            }
                        });
                    }
                });
    }


    private void checkUpgrade(final VersionInfoV2Entity.ApplicationEntity applicationEntity) {
        Log.i(TAG, "server version code ---> " + applicationEntity.getVersion());
        int installVersionCode;
        PackageInfo packageInfo = null;
        try {
            packageInfo = getPackageManager().getPackageInfo(applicationEntity.getProduct(), 0);
            installVersionCode = packageInfo.versionCode;


        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "can't find this application!!!");
            installVersionCode = 0;
        }

        if (installVersionCode >= Integer.parseInt(applicationEntity.getVersion())) {
            Log.w(TAG, "installVersionCode >= applicationEntity.getVersion()");
            return;
        }

        Log.i(TAG, "local version code ---> " + installVersionCode);
        String title = Md5.md5(new Gson().toJson(applicationEntity));


        DownloadEntity download = new Select().from(DownloadEntity.class).where("title = ?", title).executeSingle();
        if (download == null || download.status != DownloadStatus.COMPLETED) {
            postDownload(applicationEntity);

        } else {
            final File apkFile = new File(download.savePath);
            if (installVersionCode < Integer.parseInt(applicationEntity.getVersion())) {
                if (apkFile.exists()) {
                    String serverMd5Code = applicationEntity.getMd5();
                    String localMd5Code = Md5.md5File(apkFile);
                    Log.d(TAG, "local md5 ---> " + localMd5Code);
                    Log.d(TAG, "server md5 ---> " + serverMd5Code);
//                String currentActivityName = getCurrentActivityName(mContext);

                    int apkVersionCode = getLocalApkVersionCode(apkFile.getAbsolutePath());
                    int serverVersionCode = Integer.parseInt(applicationEntity.getVersion());

                    Log.i(TAG, "download apk version code: " + apkVersionCode);
                    Log.i(TAG, "server apk version code: " + serverVersionCode);

                    if (serverMd5Code.equalsIgnoreCase(localMd5Code) && apkVersionCode == serverVersionCode) {
                        Log.i(TAG, "send install broadcast ...");
                        new Thread() {
                            @Override
                            public void run() {
                                String path = apkFile.getAbsolutePath();
                                Log.d(TAG, "install apk path: " + path);
                                Log.d(TAG, "isInstallSilent: " + isInstallSilent);
                                if (isInstallSilent) {
                                    try {
                                        String[] args2 = {"chmod", "604", path};
                                        Runtime.getRuntime().exec(args2);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    boolean installSilentSuccess = installAppSilent(path, getApplicationContext());
                                    if (!installSilentSuccess) {
                                    }
                                    Log.d(TAG, "installSilentSuccess: " + installSilentSuccess);
                                } else {
                                    Bundle bundle = new Bundle();
                                    bundle.putStringArrayList("msgs", applicationEntity.getUpdate());
                                    bundle.putString("path", apkFile.getAbsolutePath());
                                    bundle.putBoolean("force_upgrade", force_upgrade_all);
                                    sendUpdateBroadcast(bundle);
                                }
                            }
                        }.start();
                    } else {
                        isMD5Error = true;
                        if (apkFile.exists()) {
                            apkFile.delete();
                        }
                    }
                } else {
                    if (apkFile.exists()) {
                        apkFile.delete();
                    }
                    postDownload(applicationEntity);
                }
            } else {
                if (apkFile.exists()) {
                    apkFile.delete();
                }
            }
        }
    }

    private void checkPluginUpgrade(final VersionInfoV2Entity.PluginItem applicationEntity) {
        Log.i(TAG, "server version code ---> " + applicationEntity.getPlugin_version());
        int installVersionCode;
        PackageInfo packageInfo = null;
        try {
            packageInfo = getPackageManager().getPackageInfo(applicationEntity.getPlugin_name(), 0);
            installVersionCode = packageInfo.versionCode;


        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "can't find this application!!!");
            installVersionCode = 0;
        }

        if (installVersionCode >= Integer.parseInt(applicationEntity.getPlugin_version())) {
            Log.w(TAG, "installVersionCode >= applicationEntity.getVersion()");
            return;
        }

        Log.i(TAG, "local version code ---> " + installVersionCode);
        String title = Md5.md5(new Gson().toJson(applicationEntity));


        DownloadEntity download = new Select().from(DownloadEntity.class).where("title = ?", title).executeSingle();
        if (download == null || download.status != DownloadStatus.COMPLETED) {
            postDownloadPlugin(applicationEntity);

        } else {
            final File apkFile = new File(download.savePath);
            if (installVersionCode < Integer.parseInt(applicationEntity.getPlugin_version())) {
                if (apkFile.exists()) {
                    String serverMd5Code = applicationEntity.getPlugin_md5();
                    String localMd5Code = Md5.md5File(apkFile);
                    Log.d(TAG, "local plugin md5 ---> " + localMd5Code);
                    Log.d(TAG, "server plugin md5 ---> " + serverMd5Code);
//                String currentActivityName = getCurrentActivityName(mContext);

                    final int apkVersionCode = getLocalApkVersionCode(apkFile.getAbsolutePath());
                    final int serverVersionCode = Integer.parseInt(applicationEntity.getPlugin_version());

                    Log.i(TAG, "download apk version code: " + apkVersionCode);
                    Log.i(TAG, "server apk version code: " + serverVersionCode);

                    if (serverMd5Code.equalsIgnoreCase(localMd5Code) && apkVersionCode == serverVersionCode) {
                        Log.i(TAG, "send install broadcast ...");
                        new Thread() {
                            @Override
                            public void run() {
                                String path = apkFile.getAbsolutePath();
                                Log.d(TAG, "install apk path: " + path);
                                Log.d(TAG, "isInstallSilent: " + isInstallSilent);
//                                if (isInstallSilent) {
//                                    try {
//                                        if (PluginManager.getInstance().isConnected()) {
//                                            PackageInfo packageInfo1 = PluginManager.getInstance().getPackageInfo("com.lenovo.dll.nebula.vod", 0);
//                                            if(packageInfo1 != null){
//                                                if(packageInfo1.versionCode < apkVersionCode){
//                                                    PluginManager.getInstance().installPackage(path, PackageManagerCompat.INSTALL_REPLACE_EXISTING);
//                                                }
//                                            }else{
//                                                PluginManager.getInstance().installPackage(path, 0);
//                                                if (apkFile.exists()) {
//                                                    apkFile.delete();
//                                                }
//                                            }
//                                        }
//                                    } catch (RemoteException e1) {
//                                        e1.printStackTrace();
//                                    }
//                                } else {
                                Bundle bundle = new Bundle();
                                ArrayList<String> msgs = new ArrayList<String>();
                                if(StringUtils.isEmpty(plugins_log)){
                                    plugins_log = "plugin test update";
                                }
                                msgs.add(plugins_log);
                                bundle.putStringArrayList("msgs", msgs);
                                bundle.putString("path", apkFile.getAbsolutePath());
                                bundle.putBoolean("force_upgrade", force_upgrade_all);
                                bundle.putInt("apkVersionCode", serverVersionCode);
                                bundle.putBoolean("ispluginupdate", true);
                                sendUpdateBroadcast(bundle);
//                                }
                            }
                        }.start();
                    } else {
                        isMD5Error = true;
                        if (apkFile.exists()) {
                            apkFile.delete();
                        }
//                        postDownload(applicationEntity);
                    }
                } else {
                    if (apkFile.exists()) {
                        apkFile.delete();
                    }
                    postDownloadPlugin(applicationEntity);
                }
            } else {
                if (apkFile.exists()) {
                    apkFile.delete();
                }
            }
        }
    }

    private void sendUpdateBroadcast(Bundle bundle) {
        Intent intent = new Intent();
        intent.setAction(APP_UPDATE_ACTION);
        intent.putExtra("data", bundle);
        sendBroadcast(intent);
    }

    private int getLocalApkVersionCode(String path) {
        PackageManager pm = getPackageManager();
        PackageInfo info = pm.getPackageArchiveInfo(path, 0);
        int versionCode;
        try {
            versionCode = info.versionCode;
        } catch (Exception e) {
            versionCode = 0;
        }
        return versionCode;
    }

    private int fetchInstallVersionCode() {
        int versionCode = 0;
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionCode = packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "can't find this application!!!");
        }
        return versionCode;
    }


    private void downloadApp(VersionInfoV2Entity.ApplicationEntity entity) {
        String url = entity.getUrl();
        String json = new Gson().toJson(entity);
        String title = Md5.md5(json);
        String filePath = getFilesDir().getAbsolutePath();
        DownloadManager.getInstance().start(url, title, json, filePath);
    }

    private void downloadPlugin(VersionInfoV2Entity.PluginItem entity) {
        String url = entity.getPlugin_url();
        String json = new Gson().toJson(entity);
        String title = Md5.md5(json);
        String filePath = getFilesDir().getAbsolutePath();
        DownloadManager.getInstance().start(url, title, json, filePath);
    }

    public static boolean installAppSilent(String filePath, Context context) {
        File file = FileUtils.getFileByPath(filePath);
        if (!FileUtils.isFileExists(file)) return false;
        boolean isSuccess = AppUtils.installAppSilent(filePath);
        return isSuccess;
    }

    @Override
    public void onLoadComplete(Loader<Cursor> loader, Cursor data) {
        if(data == null)
            return;
        for (String json : md5Jsons) {

            for (data.moveToFirst(); !data.isAfterLast(); data.moveToNext()) {
                String title = data.getString(data.getColumnIndex("title"));
//            Log.d(TAG, "onLoadComplete title: " + title);
                String status = data.getString(data.getColumnIndex("status"));
                if (status.equalsIgnoreCase("COMPLETED")) {
                    DownloadEntity downloadEntity = new Select().from(DownloadEntity.class).where("title = ?", title).executeSingle();
                    if (downloadEntity != null && downloadEntity.status == DownloadStatus.COMPLETED) {
                        if (isPluginUpdate) {
                            VersionInfoV2Entity.PluginItem applicationEntity = new Gson().fromJson(downloadEntity.json, VersionInfoV2Entity.PluginItem.class);
                            Log.d(TAG, "onLoadComplete pkg: " + applicationEntity.getPlugin_name());
                            Log.d(TAG, "onLoadComplete version: " + applicationEntity.getPlugin_version());
                            checkPluginUpgrade(applicationEntity);
                        } else {
                            VersionInfoV2Entity.ApplicationEntity applicationEntity = new Gson().fromJson(downloadEntity.json, VersionInfoV2Entity.ApplicationEntity.class);
                            Log.d(TAG, "onLoadComplete pkg: " + applicationEntity.getProduct());
                            Log.d(TAG, "onLoadComplete version: " + applicationEntity.getVersion());
                            checkUpgrade(applicationEntity);
                        }
                    }
                }
            }
        }
    }

    private void postDownload(VersionInfoV2Entity.ApplicationEntity applicationEntity) {
        if (isMD5Error) {

        } else {
            downloadApp(applicationEntity);
        }
    }

    private void postDownloadPlugin(VersionInfoV2Entity.PluginItem applicationEntity) {
        if (isMD5Error) {

        } else {
            downloadPlugin(applicationEntity);
        }
    }

    private void checkRemaindUpdateFile() {
        List<DownloadEntity> downloadEntities = new Select().from(DownloadEntity.class).execute();
        for (DownloadEntity entity : downloadEntities) {
            if (isPluginUpdate) {
                VersionInfoV2Entity.PluginItem applicationEntity = new Gson().fromJson(entity.json, VersionInfoV2Entity.PluginItem.class);
                int versionCode = AppUtils.getAppVersionCode(this, applicationEntity.getPlugin_name());
                int saveFileVersionCode = getLocalApkVersionCode(entity.savePath);
                Log.d(TAG, "checkRemaindUpdateFile: versionCode " + versionCode);
                Log.d(TAG, "checkRemaindUpdateFile: saveFileVersionCode " + saveFileVersionCode);
                if (versionCode >= saveFileVersionCode) {
                    File file = new File(entity.savePath);
                    if (file.exists()) {
                        file.delete();
                    }
                    entity.delete();
                }
            } else {
                VersionInfoV2Entity.ApplicationEntity applicationEntity = new Gson().fromJson(entity.json, VersionInfoV2Entity.ApplicationEntity.class);
                int versionCode = AppUtils.getAppVersionCode(this, applicationEntity.getProduct());
                int saveFileVersionCode = getLocalApkVersionCode(entity.savePath);
                Log.d(TAG, "checkRemaindUpdateFile: versionCode " + versionCode);
                Log.d(TAG, "checkRemaindUpdateFile: saveFileVersionCode " + saveFileVersionCode);
                if (versionCode >= saveFileVersionCode) {
                    File file = new File(entity.savePath);
                    if (file.exists()) {
                        file.delete();
                    }
                    entity.delete();
                }
            }
        }
    }
}
