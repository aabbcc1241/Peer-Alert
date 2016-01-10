package net.aabbcc1241.Peer_Alert;

import android.app.Activity;
import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.example.Peer_Alert.R;
import net.aabbcc1241.Peer_Alert.utils.ThreadUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MainActivity extends Activity {
    public static final String SERVICE_NAME = "Peer Alert Service";
    public static final String SERVICE_TYPE = "_http._tcp";
    MainActivity mMainActivity = this;
    String mServiceName;
    boolean isServiceRegistered = false;
    NsdServiceInfo mServiceInfo;
    NsdHelper mNsdHelper;
    ConnectionHelper mConnectionHelper = new ConnectionHelper();
    UiHelper mUiHelper;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        /* init UI */
        mUiHelper = new UiHelper() {
            TextView tvMsg = (TextView) findViewById(R.id.tvMsg);
            Button btnSendAlert = (Button) findViewById(R.id.btnSendAlert);
            Button btnCancelAlert = (Button) findViewById(R.id.btnCancelAlert);
            Button btnConfirmAlert = (Button) findViewById(R.id.btnConfirmAlert);
            Button btnSearchPeer = (Button) findViewById(R.id.btnSearchPeer);

            {
                showText(R.string.offline);
                btnSearchPeer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showToast(R.string.searching_peer);
                    }
                });
            }

            public void showToast(int resId) {
                mMainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mMainActivity, resId, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            public void showToast(String msg) {
                mMainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mMainActivity, msg, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void showText(int resId) {
                mMainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvMsg.setText(resId);
                    }
                });
            }

            @Override
            public void showText(String msg) {
                mMainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvMsg.setText(msg);
                    }
                });
            }

            void setVisible(View view, boolean isVisible) {
                mMainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        view.setVisibility(isVisible ? View.VISIBLE : View.GONE);
                    }
                });
            }

            @Override
            public void setMode(Status status) {
                mMainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setVisible(btnSendAlert, Status.idle.equals(status));
                        setVisible(btnCancelAlert, Status.sent.equals(status));
                        setVisible(btnConfirmAlert, Status.received.equals(status));
                        setVisible(btnSearchPeer, Status.offline.equals(status));
                    }
                });
            }
        };

        /* init status */
        mUiHelper.setMode(Status.offline);
    }

    @Override
    protected void onPause() {
        if (mNsdHelper != null) {
            mNsdHelper.tearDown();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mNsdHelper != null) {
            //TODO
            try {
                if (!mConnectionHelper.hasServer)
                    mConnectionHelper.initServer();
                mNsdHelper.registerService(mConnectionHelper.mServiceServerSocket.getLocalPort());
                mNsdHelper.discoverServices();
            } catch (IOException e) {
                Log.e(SERVICE_NAME, "Failed to server socket!");
                e.printStackTrace();
                mUiHelper.showText(R.string.network_failure);
            }
        }
    }

    @Override
    protected void onDestroy() {
        mNsdHelper.tearDown();
        mConnectionHelper.tearDown();
        super.onDestroy();
    }

    enum Status {received, sent, idle, offline}

    interface UiHelper {
        void showText(int resId);

        void showText(String msg);

        void setMode(Status status);

        void showToast(int resId);

        /*avoid making string directly for better language support */
        @Deprecated
        void showToast(String msg);
    }

    class ConnectionHelper {
        ServiceClientSocket mServiceClientSocket;
        ServiceServerSocket mServiceServerSocket;
        boolean hasServer = false;
        boolean hasClient = false;

        void initServer() throws IOException {
            if (hasServer)
                return;
            mServiceServerSocket = new ServiceServerSocket();
            mServiceServerSocket.initSocket();
            hasServer = true;
            mUiHelper.showToast("Waiting peer");
        }

        void initClient(InetAddress host, int remotePort) {
            if (hasClient)
                return;
            mServiceClientSocket = new ServiceClientSocket();
            mServiceClientSocket.initSocket(host, remotePort);
            hasClient = true;
            mUiHelper.showToast("Connected peer " + host + " (" + remotePort + ")");
        }

        void tearDown() {
            if (hasServer)
                mServiceServerSocket.tearDown();
            if (hasClient)
                mServiceClientSocket.tearDown();
        }

        class ServiceServerSocket {
            protected InetAddress host;
            ServerSocket serverSocket;
            ThreadUtils.LoopWorker mLoopWorker = new ThreadUtils.LoopWorker(new ServiceRunnable());
            ConcurrentLinkedQueue<ThreadUtils.LoopWorker> clientWorkers = new ConcurrentLinkedQueue();
            private int localPort;

            public int getLocalPort() {
                return localPort;
            }

            void tearDown() {
            }

            public void initSocket() throws IOException {
                serverSocket = new ServerSocket(0);
                localPort = serverSocket.getLocalPort();
                mLoopWorker.start();
                hasServer = true;
            }

            class ServiceRunnable implements Runnable {
                @Override
                public void run() {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        ObjectInputStream is = new ObjectInputStream(clientSocket.getInputStream());
                        ObjectOutputStream os = new ObjectOutputStream(clientSocket.getOutputStream());
                        /* fork to handle input */
                        ThreadUtils.LoopWorker inputWorker = new ThreadUtils.LoopWorker(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    PeerAlertMessage message = (PeerAlertMessage) is.readObject();
                                    return;
                                } catch (ClassNotFoundException e) {
                                    Log.e(SERVICE_NAME, "Failed to parse message into PeerAlertMessage :\n" + e);
                                    e.printStackTrace();
                                } catch (IOException e) {
                                    Log.e(SERVICE_NAME, "Failed to read from peer :\n" + e);
                                    e.printStackTrace();
                                }
                                mUiHelper.showToast("Failed to handle message");
                            }
                        });
                        clientWorkers.add(inputWorker);
                        inputWorker.start();
                    } catch (IOException e) {
                        mUiHelper.showToast("Failed to handle incoming peer");
                        e.printStackTrace();
                    }
                }
            }
        }

        class ServiceClientSocket {
            void tearDown() {
            }

            public void initSocket(InetAddress host, int remotePort) {
                hasClient = true;
            }
        }
    }


    class NsdHelper {
        final NsdManager mNsdManager = (NsdManager) getApplicationContext().getSystemService(Context.NSD_SERVICE);
        final ServiceRegistrationListener registrationListener = new ServiceRegistrationListener();
        final ServiceDiscoveryListener discoveryListener = new ServiceDiscoveryListener();

        public void tearDown() {
            mNsdManager.unregisterService(registrationListener);
            mNsdManager.stopServiceDiscovery(discoveryListener);
        }

        public void registerService(int port) {
            NsdServiceInfo serviceInfo = new NsdServiceInfo();
            serviceInfo.setServiceName(SERVICE_NAME);
            serviceInfo.setServiceType(SERVICE_TYPE);
            serviceInfo.setPort(port);

            mNsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener);
        }

        public void discoverServices() {
            mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
        }

        public void resolveService(NsdServiceInfo serviceInfo, NsdManager.ResolveListener resolveListener) {
            mNsdManager.resolveService(serviceInfo, resolveListener);
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
                    mNsdHelper.resolveService(serviceInfo, resolveListener);
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
            mConnectionHelper.initClient(mServiceInfo.getHost(), mServiceInfo.getPort());
        }
    }
}
