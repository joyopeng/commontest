package pluginhost.ismar.com.pluginapplication;

import android.app.Application;
import android.content.Intent;

import com.morgoo.droidplugin.PluginApplication;

import cn.ismartv.injectdb.library.ActiveAndroid;
import pluginhost.ismar.com.pluginapplication.service.UpdateService;


public class DaisyPluginApplication extends PluginApplication {
    @Override
    public void onCreate() {
        super.onCreate();
        Intent intent = new Intent();
        intent.setClass(getApplicationContext(), UpdateService.class);
        intent.putExtra("install_type", 0);
        startService(intent);
        ActiveAndroid.initialize(this);
    }
}
