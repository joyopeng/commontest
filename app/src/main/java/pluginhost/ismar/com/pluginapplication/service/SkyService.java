package pluginhost.ismar.com.pluginapplication.service;

import android.util.Log;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import pluginhost.ismar.com.pluginapplication.entity.UpgradeRequestEntity;
import pluginhost.ismar.com.pluginapplication.entity.VersionInfoV2Entity;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;
import rx.Observable;

public interface SkyService {

    @POST("api/v2/upgrade/")
    Observable<VersionInfoV2Entity> appUpgrade(
            @Body List<UpgradeRequestEntity> upgradeRequestEntities
    );

    class ServiceManager {
        private volatile static ServiceManager serviceManager;
        static final int DEFAULT_CONNECT_TIMEOUT = 6;
        static final int DEFAULT_READ_TIMEOUT = 15;
        public static final String API_HOST = "http://wx.api.tvxio.com/";
        static final String IRIS_TVXIO_HOST = "http://iris.tvxio.com/";
        static final String SPEED_CALLA_TVXIO_HOST = "http://speed.calla.tvxio.com/";
        static final String LILY_TVXIO_HOST = "http://lily.tvxio.com/";
        private SkyService upgradeService;
        String defaultUpdateDomain = "http://updatetest.tvxio.com/api/v2/upgrade/";
        SSLContext sc = null;
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {

            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[]{};
            }

            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                Log.i("TrustManager", "checkClientTrusted");
            }

            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                Log.i("TrustManager", "checkServerTrusted");
            }
        }};

        private ServiceManager() {
            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            SSLContext sc = null;
            try {
                sc = SSLContext.getInstance("TLS");
                sc.init(null, trustAllCerts, new java.security.SecureRandom());
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (KeyManagementException e) {
                e.printStackTrace();
            }

            final OkHttpClient mClient = new OkHttpClient.Builder()
                    .connectTimeout(DEFAULT_CONNECT_TIMEOUT, TimeUnit.SECONDS)
                    .readTimeout(DEFAULT_READ_TIMEOUT, TimeUnit.SECONDS)
//                    .addInterceptor(VodApplication.getHttpParamsInterceptor())
//                    .addNetworkInterceptor(VodApplication.getHttpTrafficInterceptor())
//                    .retryOnConnectionFailure(true)
                    .addInterceptor(interceptor)
                    .sslSocketFactory(sc.getSocketFactory())
                    .build();
            Retrofit upgradeRetrofit = new Retrofit.Builder()
                    .baseUrl(defaultUpdateDomain)
                    .addConverterFactory(GsonConverterFactory.create())
                    .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                    .client(mClient)
                    .build();

            upgradeService = upgradeRetrofit.create(SkyService.class);
        }

        private static ServiceManager getInstance() {    //对获取实例的方法进行同步
            synchronized (ServiceManager.class) {
                if (serviceManager == null) {
                    serviceManager = new ServiceManager();
                }
            }
            return serviceManager;
        }

        public static SkyService getUpgradeService() {

            return getInstance().upgradeService;
        }
    }
}
