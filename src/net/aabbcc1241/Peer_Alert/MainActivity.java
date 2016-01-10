package net.aabbcc1241.Peer_Alert;

import android.app.Activity;
import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import net.aabbcc1241.Peer_Alert.utils.Network;
import net.aabbcc1241.Peer_Alert.utils.ThreadUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MainActivity extends Activity {
    public static final String SERVICE_NAME = "Peer Alert Service";
    public static final String SERVICE_TYPE = "_http._tcp.";
    static final boolean AUTO_DISCOVER = false;
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
            TextView tvStatus = (TextView) findViewById(R.id.tvStatus);
            TextView tvSubStatus = (TextView) findViewById(R.id.tvSubStatus);
            TextView tvMessage = (TextView) findViewById(R.id.tvMessage);
            Button btnSendAlert = (Button) findViewById(R.id.btnSendAlert);
            Button btnCancelAlert = (Button) findViewById(R.id.btnCancelAlert);
            Button btnConfirmAlert = (Button) findViewById(R.id.btnConfirmAlert);
            Button btnSearchPeer = (Button) findViewById(R.id.btnSearchPeer);
            ListView lvPeer = (ListView) findViewById(R.id.lvPeer);

            {
                /* set content */
                tvStatus.setText("");
                tvSubStatus.setText("");
                tvMessage.setText("");
                /* set listener */
                btnSearchPeer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showToast(R.string.searching_peer);
                        startDiscover();
                    }
                });
                //TODO
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
            public void showStatus(int resId) {
                mMainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvStatus.setText(resId);
                    }
                });
            }

            @Override
            public void showStatus(String msg) {
                mMainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvStatus.setText(msg);
                    }
                });
            }

            @Override
            public void showSubStatus(String msg) {
                mMainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvSubStatus.setText(msg);
                    }
                });
            }

            @Override
            public void showMessage(String msg) {
                mMainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvMessage.setText(msg);
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
            public void setMode(LocalStatus status) {
                mMainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setVisible(btnSendAlert, LocalStatus.idle.equals(status));
                        setVisible(btnCancelAlert, LocalStatus.sent.equals(status));
                        setVisible(btnConfirmAlert, LocalStatus.received.equals(status));
                        setVisible(btnSearchPeer, LocalStatus.offline.equals(status));
                        switch (status) {
                            case offline:
                                tvMessage.setText(R.string.offline);
                                break;
                            case idle:
                                tvMessage.setText(R.string.online);
                                break;
                            case sent:
                                tvMessage.setText(R.string.sent_alert);
                                break;
                            case received:
                                tvMessage.setText(R.string.received_alert);
                                break;
                            default:
                                Log.e(SERVICE_NAME, "Unsupported status");
                        }
                    }
                });
            }
        };

        /* init status */
        mUiHelper.setMode(LocalStatus.offline);

        /* init services */
        mNsdHelper = new NsdHelper();
    }

    @Override
    protected void onPause() {
        if (mNsdHelper != null) {
            mNsdHelper.tearDown();
        }
        super.onPause();
    }

    /**
     * this method should not cause issue when called more than once continuously
     */
    void startDiscover() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (!mConnectionHelper.hasServer)
                    try {
                        mConnectionHelper.initServer();
                    } catch (IOException e) {
                        Log.e(SERVICE_NAME, "Failed to start server socket");
                        e.printStackTrace();
                        mUiHelper.showStatus(R.string.network_failure);
                    }
                mNsdHelper.init();
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (AUTO_DISCOVER) {
            startDiscover();
        }
    }

    @Override
    protected void onDestroy() {
        mNsdHelper.tearDown();
        mConnectionHelper.tearDown();
        super.onDestroy();
    }


    interface UiHelper {
        void showStatus(int resId);

        void showStatus(String msg);

        void showSubStatus(String msg);

        void showMessage(String msg);

        void setMode(LocalStatus status);

        void showToast(int resId);

        /*avoid making string directly for better language support */
        @Deprecated
        void showToast(String msg);
    }

    class ConnectionHelper {
        ServiceClientSocket mServiceClientSocket;
        ServiceServerSocket mServiceServerSocket;
        @Deprecated
        boolean hasServer = false;
        @Deprecated
        boolean hasClient = false;
        private ConcurrentLinkedQueue<ClientConnection> clientConnections = new ConcurrentLinkedQueue<>();

        void onAlertMessageReceived(ClientConnection sender, PeerAlertMessage message) {
            //TODO
        }

        void sendAlertMessage(PeerAlertMessage message) {
            for (ClientConnection clientConnection : clientConnections) {
                clientConnection.send(message);
            }
        }

        void initServer() throws IOException {
            if (hasServer)
                return;
            mServiceServerSocket = new ServiceServerSocket();
            hasServer = true;
            mUiHelper.showToast("Waiting peer");
        }

        void initClient(InetAddress host, int remotePort) throws IOException {
            if (hasClient)
                return;
            mServiceClientSocket = new ServiceClientSocket(host, remotePort);
            hasClient = true;
            mUiHelper.showToast("Connected peer " + host + " (" + remotePort + ")");
            mUiHelper.setMode(LocalStatus.idle);
        }

        synchronized void tearDown() {
            if (hasServer)
                mServiceServerSocket.tearDown();
            if (hasClient)
                mServiceClientSocket.tearDown();
            Vector<ClientConnection> xs = new Vector<ClientConnection>();
            for (ClientConnection clientConnection : clientConnections) {
                clientConnection.tearDown();
                xs.add(clientConnection);
            }
            for (ClientConnection x : xs) {
                clientConnections.remove(x);
            }
        }

        class ClientConnection {
            private final Socket clientSocket;
            private final ThreadUtils.LoopWorker inputWorker;
            private final ObjectInputStream objectInputStream;
            private final ObjectOutputStream objectOutputStream;

            ClientConnection(Socket clientSocket) throws IOException {
                this.clientSocket = clientSocket;
                objectInputStream = new ObjectInputStream(clientSocket.getInputStream());
                objectOutputStream = new ObjectOutputStream(clientSocket.getOutputStream());
                /* fork to handle input */
                inputWorker = new ThreadUtils.LoopWorker(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            PeerAlertMessage message = (PeerAlertMessage) objectInputStream.readObject();
                            onReceived(message);
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
                inputWorker.start();
            }

            public void tearDown() {
                try {
                    objectInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    objectOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            public void send(PeerAlertMessage message) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            objectOutputStream.writeObject(message);
                        } catch (IOException e) {
                            Log.e(SERVICE_NAME, "Failed to send message!\nMessage : " + message + "\nIOException : " + e);
                            e.printStackTrace();
                        }
                    }
                }).start();
            }

            private void onReceived(PeerAlertMessage message) {
                onAlertMessageReceived(this, message);
            }
        }

        class ServiceServerSocket {
            ServerSocket serverSocket;
            ThreadUtils.LoopWorker mLoopWorker = new ThreadUtils.LoopWorker(new ServiceRunnable());
            ConcurrentLinkedQueue<ThreadUtils.LoopWorker> clientWorkers = new ConcurrentLinkedQueue();
            private int localPort;

            public int getLocalPort() {
                return localPort;
            }

            void tearDown() {
            }

            public ServiceServerSocket() throws IOException {
                serverSocket = new ServerSocket(0);
                localPort = serverSocket.getLocalPort();
                mLoopWorker.start();
//                mUiHelper.showSubStatus("self : " + Network.getLocalIpWifi(mMainActivity) + " : " + localPort);
//                mUiHelper.showSubStatus("self : " + serverSocket.getInetAddress().getHostAddress() + " : " + localPort);
                mUiHelper.showSubStatus("self : " + Network.getLocalIpAddresses(true) + " : " + localPort);
                hasServer = true;
            }

            /* this runnable will be called repeatably */
            class ServiceRunnable implements Runnable {
                @Override
                public void run() {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        ClientConnection clientConnection = new ClientConnection(clientSocket);
                        clientConnections.add(clientConnection);
                        mUiHelper.setMode(LocalStatus.idle);
                    } catch (IOException e) {
                        mUiHelper.showToast("Failed to handle incoming peer");
                        e.printStackTrace();
                    }
                }
            }
        }

        class ServiceClientSocket {
            public ServiceClientSocket(InetAddress host, int remotePort) throws IOException {
                Socket clientSocket = new Socket(host, remotePort);
                ClientConnection clientConnection = new ClientConnection(clientSocket);
                clientConnections.add(clientConnection);
                hasClient = true;
            }

            void tearDown() {
            }
        }
    }


    class NsdHelper {
        final NsdManager mNsdManager = (NsdManager) mMainActivity.getSystemService(Context.NSD_SERVICE);
        final ServiceRegistrationListener registrationListener = new ServiceRegistrationListener();
        final ServiceDiscoveryListener discoveryListener = new ServiceDiscoveryListener();
        boolean hasInit = false;

        public synchronized void init() {
            if (hasInit)
                return;
            hasInit = true;
            mNsdHelper.registerService(mConnectionHelper.mServiceServerSocket.getLocalPort());
            mNsdHelper.discoverServices();
        }

        public synchronized void tearDown() {
            if (!hasInit)
                return;
            hasInit = false;
            mNsdManager.unregisterService(registrationListener);
            mNsdManager.stopServiceDiscovery(discoveryListener);
        }

        private void registerService(int port) {
            Log.d(SERVICE_NAME, "Registering service on port : " + port);
            NsdServiceInfo serviceInfo = new NsdServiceInfo();
            serviceInfo.setServiceName(SERVICE_NAME);
            serviceInfo.setServiceType(SERVICE_TYPE);
            serviceInfo.setPort(port);

            mNsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener);
        }

        private void discoverServices() {
            mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
        }

        private void resolveService(NsdServiceInfo serviceInfo, NsdManager.ResolveListener resolveListener) {
            Log.d(SERVICE_NAME, "Trying to resolve service :\n" + serviceInfo);
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
//        private final ServiceResolveListener resolveListener = new ServiceResolveListener();

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
                if (serviceName.equals(mServiceName)) {
                    Log.d(SERVICE_NAME, "Same machine : " + serviceName);
                } else if (serviceName.contains(SERVICE_NAME)) {
                    Log.d(SERVICE_NAME, "Not same machine : " + serviceName);
                    mNsdHelper.resolveService(serviceInfo, new ServiceResolveListener());
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
            try {
                mConnectionHelper.initClient(mServiceInfo.getHost(), mServiceInfo.getPort());
                mUiHelper.setMode(LocalStatus.idle);
            } catch (IOException e) {
                Log.e(SERVICE_NAME, "Failed to connect to service host\nserviceInfo : " + serviceInfo);
                e.printStackTrace();
                mUiHelper.showToast(R.string.network_failure);
            }
        }
    }
}

