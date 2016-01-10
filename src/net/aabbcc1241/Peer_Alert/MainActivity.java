package net.aabbcc1241.Peer_Alert;

import android.app.Activity;
import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.util.Log;
import com.example.Peer_Alert.R;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

public class MainActivity extends Activity {
    public static final String SERVICE_NAME = "Peer Alert Service";
    public static final int DEFAULT_PORT = 8123;
    public static final String SERVICE_TYPE = "_http._tcp";
    String mServiceName;
    boolean isServiceRegistered = false;
    NsdServiceInfo mServiceInfo;
    private NsdManager mNsdManager;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }

    public void registerService(int port) {
        NsdServiceInfo serviceInfo = new NsdServiceInfo();
        serviceInfo.setServiceName(SERVICE_NAME);
        serviceInfo.setServiceType(SERVICE_TYPE);
        serviceInfo.setPort(port);

        mNsdManager = (NsdManager) getApplicationContext().getSystemService(Context.NSD_SERVICE);

        ServiceRegistrationListener registrationListener = new ServiceRegistrationListener();
        mNsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener);

        ServiceDiscoveryListener discoveryListener = new ServiceDiscoveryListener();
        mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);

    }

    static class ServiceSocket {
        int localPort;
        ServerSocket serverSocket;

        public void initServiceSocket() throws IOException {
            serverSocket = new ServerSocket(0);
            localPort = serverSocket.getLocalPort();
        }
    }

    class ServiceRegistrationListener implements NsdManager.RegistrationListener {

        @Override
        public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Log.e(SERVICE_NAME, "failed to register service (" + errorCode + ") :\n" + serviceInfo);
        }

        @Override
        public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Log.e(SERVICE_NAME, "failed to unregister service (" + errorCode + ") :\n" + serviceInfo);
        }

        @Override
        public void onServiceRegistered(NsdServiceInfo serviceInfo) {
            mServiceName = serviceInfo.getServiceName();
            isServiceRegistered = true;
        }

        @Override
        public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
            isServiceRegistered = false;
        }
    }

    class ServiceDiscoveryListener implements NsdManager.DiscoveryListener {
        @Override
        public void onStartDiscoveryFailed(String serviceType, int errorCode) {
            Log.e(SERVICE_NAME, "failed to start discovery (" + errorCode + ") :\n" + serviceType);
        }

        @Override
        public void onStopDiscoveryFailed(String serviceType, int errorCode) {
            Log.e(SERVICE_NAME, "failed to stop discovery (" + errorCode + ") : \n" + serviceType);
        }

        @Override
        public void onDiscoveryStarted(String serviceType) {
            Log.d(SERVICE_NAME, "Service discovery started");
        }

        @Override
        public void onDiscoveryStopped(String serviceType) {
            Log.e(SERVICE_NAME, "stopped discovery : \n" + serviceType);
        }

        @Override
        public void onServiceFound(NsdServiceInfo serviceInfo) {
            Log.d(SERVICE_NAME, "Service Found :\n" + serviceInfo);
            String serviceType = serviceInfo.getServiceType();
            if (!serviceType.equals(SERVICE_TYPE)) {
                Log.d(SERVICE_NAME, "Unknown Service Type : " + serviceType);
            } else {
                String serviceName = serviceInfo.getServiceName();
                if (serviceName.equals(serviceName)) {
                    Log.d(SERVICE_NAME, "Same machine : " + serviceName);
                } else if (serviceName.contains(SERVICE_NAME)) {
                    ServiceResolveListener resolveListener = new ServiceResolveListener();
                    mNsdManager.resolveService(serviceInfo, resolveListener);
                }
            }
        }

        @Override
        public void onServiceLost(NsdServiceInfo serviceInfo) {
            Log.e(SERVICE_NAME, "service lost :\n" + serviceInfo);
        }
    }

    class ServiceResolveListener implements NsdManager.ResolveListener {
        @Override
        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Log.e(SERVICE_NAME, "Failed to resolve service (" + errorCode + ") :\n" + serviceInfo);
        }

        @Override
        public void onServiceResolved(NsdServiceInfo serviceInfo) {
            Log.d(SERVICE_NAME, "resolved service :\n" + serviceInfo);
            if (serviceInfo.getServiceName().equals(mServiceName)) {
                Log.d(SERVICE_NAME, "Same IP.");
                return;
            }
            mServiceInfo = serviceInfo;
            int port = mServiceInfo.getPort();
            InetAddress host = mServiceInfo.getHost();
            //TODO
        }
    }

}
