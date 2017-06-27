package pluginhost.ismar.com.pluginapplication.entity;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by huaijie on 10/22/15.
 */
public class VersionInfoV2Entity {
    private List<ApplicationEntity> upgrades;
    private PluginEntity plugins;
    private String version;
    private boolean force_upgrade_all;

    public boolean isForce_upgrade_all() {
        return force_upgrade_all;
    }

    public void setForce_upgrade_all(boolean force_upgrade_all) {
        this.force_upgrade_all = force_upgrade_all;
    }

    public void setPlugins(PluginEntity plugins) {
        this.plugins = plugins;
    }

    public PluginEntity getPlugins() {
        return plugins;
    }

    private String homepage;

    public List<ApplicationEntity> getUpgrades() {
        return upgrades;
    }

    public void setUpgrades(List<ApplicationEntity> upgrades) {
        this.upgrades = upgrades;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getHomepage() {
        return homepage;
    }

    public void setHomepage(String homepage) {
        this.homepage = homepage;
    }

    public static class ApplicationEntity {
        private boolean checkUpgrade = true;
        private String product;
        private String name;
        private String screenshot;
        private String url;
        private ArrayList<String> update;
        private String summary;
        private String version;
        private String md5;
        private boolean force_upgrade;

        public Boolean getForce_upgrade() {
            return force_upgrade;
        }

        public void setForce_upgrade(Boolean force_upgrade) {
            this.force_upgrade = force_upgrade;
        }

        public boolean isCheckUpgrade() {
            return checkUpgrade;
        }

        public void setCheckUpgrade(boolean checkUpgrade) {
            this.checkUpgrade = checkUpgrade;
        }

        public String getProduct() {
            return product;
        }

        public void setProduct(String product) {
            this.product = product;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getScreenshot() {
            return screenshot;
        }

        public void setScreenshot(String screenshot) {
            this.screenshot = screenshot;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public ArrayList<String> getUpdate() {
            return update;
        }

        public void setUpdate(ArrayList<String> update) {
            this.update = update;
        }

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getMd5() {
            return md5;
        }

        public void setMd5(String md5) {
            this.md5 = md5;
        }
    }

    public static class PluginItem {
        private boolean checkUpgrade = true;
        private String plugin_version;
        private String plugin_name;
        private String plugin_url;
        private String type;
        private String plugin_md5;
        private int order;
        private JsonObject plugin_params;

        public void setPlugin_params(JsonObject plugin_params) {
            this.plugin_params = plugin_params;
        }

        public JsonObject getPlugin_params() {
            return plugin_params;
        }

        public boolean isCheckUpgrade() {
            return checkUpgrade;
        }

        public String getPlugin_version() {
            return plugin_version;
        }

        public String getPlugin_name() {
            return plugin_name;
        }

        public String getPlugin_url() {
            return plugin_url;
        }

        public String getType() {
            return type;
        }

        public String getPlugin_md5() {
            return plugin_md5;
        }

        public int getOrder() {
            return order;
        }

        public void setCheckUpgrade(boolean checkUpgrade) {
            this.checkUpgrade = checkUpgrade;
        }

        public void setPlugin_version(String plugin_version) {
            this.plugin_version = plugin_version;
        }

        public void setPlugin_name(String plugin_name) {
            this.plugin_name = plugin_name;
        }

        public void setPlugin_url(String plugin_url) {
            this.plugin_url = plugin_url;
        }

        public void setType(String type) {
            this.type = type;
        }

        public void setPlugin_md5(String plugin_md5) {
            this.plugin_md5 = plugin_md5;
        }

        public void setOrder(int order) {
            this.order = order;
        }
    }


    public static class PluginEntity {
        private String plugins_log;
        private List<PluginItem> plugins_list;

        public String getPlugins_log() {
            return plugins_log;
        }

        public List<PluginItem> getPlugins_list() {
            return plugins_list;
        }

        public void setPlugins_log(String plugins_log) {
            this.plugins_log = plugins_log;
        }

        public void setPlugins_list(List<PluginItem> plugins_list) {
            this.plugins_list = plugins_list;
        }
    }
}
