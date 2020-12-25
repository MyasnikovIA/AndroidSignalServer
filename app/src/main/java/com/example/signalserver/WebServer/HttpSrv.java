package com.example.signalserver.WebServer;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.PowerManager;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Set;

/**
 * Created by myasnikov on 14.01.16.
 */
public class HttpSrv {

    public static int port = 9090;
    public static int TimeOut = 300000;
    private boolean process = false;
    private static Context context;
    private PackageManager packageManager = null;

    public HttpSrv(Context context) {
        this.context = context;
    }

    /**
     * Запуск вэб сервера<br>
     * Пример: new HTTP().start("C:\\AppServ\\www",
     * "9090","C:\\AppServ\\www\\lib", true);
     *
     * @param port порт на котором будет работать сервер
     *             компилируются, false классы java компилируются 1 раз )
     */
    public void Start(String port) {
        process = true;
        this.port = Integer.valueOf(port);
        Thread myThready;
        myThready = new Thread(new Runnable() {
            public void run() {
                try {
                    ServerSocket ss = new ServerSocket(HttpSrv.port);
                    while (process == true) {
                        Socket socket = ss.accept();
                        new Thread(new SocketProcessor(socket)).start();
                    }
                } catch (Exception ex) {
                    Logger.getLogger(HttpSrv.class.getName()).log(Level.SEVERE, null, ex);
                } catch (Throwable ex) {
                    Logger.getLogger(HttpSrv.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
        });
        myThready.start();    //Запуск потока
        Toast.makeText(context, "Start SignalServer", Toast.LENGTH_LONG).show();
    }


    /**
     * Остановить сервер
     */
    public void Stop() {
        process = false;
        Toast.makeText(context, "Stop SignalServer", Toast.LENGTH_LONG).show();
    }

    private static class SocketProcessor implements Runnable {
        File sdcard;
        private Hashtable<String, Object> Json = new Hashtable<String, Object>(10, (float) 0.5);
        private Hashtable<String, Object> JsonParam = new Hashtable<String, Object>(10, (float) 0.5);
        public static Hashtable<String, OutputStream> DeviceIO = new Hashtable<String, OutputStream>(10, (float) 0.5);
        public static Hashtable<String, String> DevicePass = new Hashtable<String, String>(10, (float) 0.5);
        public static Hashtable<String, Socket> DeviceSocket = new Hashtable<String, Socket>(10, (float) 0.5);

        private Socket socket;
        private InputStream is;
        private OutputStream os;

        private SocketProcessor(Socket socket) throws Throwable {
            this.socket = socket;
            // this.socket.setSoTimeout(TimeOut);
            this.is = socket.getInputStream();
            this.os = socket.getOutputStream();
            Json.clear();
            JsonParam.clear();
            String Adress = socket.getRemoteSocketAddress().toString();
            Json.put("RemoteIPAdress", Adress);
            Adress = Adress.split(":")[0];
            Adress = Adress.replace("/", "");
            InetAddress address = InetAddress.getByName(Adress);
        }

        public void run() {
            try {
                // PrintStream out = new PrintStream(os);
                // System.setOut(out);
                // System.setErr(out);
                readInputHeaders();
            } catch (Throwable t) {
                stopSocket();
            } finally {
                stopSocket();
            }
        }

        private void stopSocket() {
            try {
                socket.shutdownInput();
                socket.shutdownOutput();
                socket.close();
            } catch (Throwable t) {
                /*do nothing*/
            }
        }

        private List<String> param = new ArrayList<String>();
        private byte[] POST = new byte[0];
        private String Koderovka = "";
        //  private String getCommand = "";
        private String getCmd = "";
        private PackageManager packageManager = null;

        /**
         * Чтение входных данных от клиента
         *
         * @throws java.io.IOException
         */
        private void readInputHeaders() throws IOException {
            //  FileWriter outLog = new FileWriter(rootPath + "\\log.txt", true); //the true will append the new data
            //  outLog.write("add a line\n");//appends the string to the file
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuffer sbInData = new StringBuffer();
            InputStreamReader isr = new InputStreamReader(is);
            int charInt;
            char[] charArray = new char[1024];
            byte[] POST = new byte[0];
            int numLine = 0;
            String DevName = "";
            String DevNameTmp = "";
            String deviceName = "";
            String PassText = "";
            String PassTextTmp = "";
            boolean isStream = false;
            // Читаем заголовок
            while (socket.isConnected()) {
                Json.clear();
                JsonParam.clear();
                StringBuffer sb = new StringBuffer();
                StringBuffer sbTmp = new StringBuffer();
                ByteArrayOutputStream bufferRaw = new ByteArrayOutputStream();
                while ((charInt = isr.read()) != -1) {
                    if (socket.isConnected() == false) {
                        return;
                    }
                    if ((isStream == true) && (charInt == 0)) {
                        break;
                    }
                    sbTmp.append((char) charInt);
                    if (sbTmp.toString().indexOf("\r") != -1) {
                        if ((numLine == 0) && (DevName.length() == 0)) {
                            DevName = sbTmp.toString().replace("\n", "").replace("\r", "");
                            DevNameTmp = DevName;
                            numLine++;
                            sbTmp.setLength(0);
                            continue;
                        }
                        if ((numLine == 1) && (PassTextTmp.length() == 0)) {
                            PassTextTmp = sbTmp.toString().replace("\n", "").replace("\r", "");
                            numLine++;
                            sbTmp.setLength(0);
                            continue;
                        }

                        // если в первой строке невстречается слово GET или POST, тогда отключаем соединение
                        if (sbTmp.toString().length() == 2) {
                            break; // чтение заголовка окончено
                        }
                        sbTmp.setLength(0);
                    }
                    sb.append((char) charInt);
                    bufferRaw.write((char) charInt);
                }
                if (sb.toString().indexOf("Content-Length: ") != -1) {
                    String sbTmp2 = sb.toString().substring(sb.toString().indexOf("Content-Length: ") + "Content-Length: ".length(), sb.toString().length());
                    String lengPostStr = sbTmp2.substring(0, sbTmp2.indexOf("\n")).replace("\r", "");
                    int LengPOstBody = Integer.valueOf(lengPostStr);
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    while ((charInt = isr.read()) > 0) {
                        if (socket.isConnected() == false) {
                            return;
                        }
                        buffer.write((char) charInt);
                        LengPOstBody--;
                        if (LengPOstBody == 0) {
                            break;
                        }
                    }
                    buffer.flush();
                    POST = buffer.toByteArray();
                }
                boolean isPost = false;
                if (sb.toString().indexOf("len:") != -1) {
                    String sbTmp2 = sb.toString().substring(sb.toString().indexOf("len:") + "len:".length(), sb.toString().length());
                    String lengPostStr = sbTmp2.substring(0, sbTmp2.indexOf("\n")).replace("\r", "");
                    int LengPOstBody = Integer.valueOf(lengPostStr) + 1;
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    int countByte = 0;
                    while ((charInt = isr.read()) > 0) {
                        if (socket.isConnected() == false) {
                            return;
                        }
                        if (countByte > 0) {
                            buffer.write((char) charInt);
                        }
                        countByte++;
                        LengPOstBody--;
                        if (LengPOstBody == 0) {
                            break;
                        }
                    }
                    buffer.flush();
                    POST = buffer.toByteArray();
                    isPost = true;
                }
                // парсим входящий заголовок
                for (String TitleLine : sb.toString().split("\r")) {
                    if (TitleLine.split(":").length > 0) {
                        TitleLine = TitleLine.replace("\n", "");
                        String val = TitleLine.split(":")[0];
                        val = val.replace(" ", "_");
                        if (val.length() > 0) {
                            Json.put(val, TitleLine.replace(TitleLine.split(":")[0] + ":", ""));
                        }
                    }
                }
                // Если запрос из ВЭБ браузера, тогда выводим HTML страницу
                if ((DevNameTmp.indexOf("GET ") != -1) && (DevNameTmp.indexOf("HTTP/1.1") != -1)) {
                    drawHTML(DevNameTmp);
                    break;
                }
                // запоменаем имя устройства/парольбьтбюьт
                if (DevNameTmp.length() > 0) {
                    DevNameTmp = DevNameTmp.replace("\n", "");
                    DevNameTmp = DevNameTmp.replace("\r", "");
                    DevNameTmp = DevNameTmp.replace(" ", "");
                    PassTextTmp = PassTextTmp.replace("\n", "");
                    PassTextTmp = PassTextTmp.replace("\r", "");
                    PassTextTmp = PassTextTmp.replace(" ", "");
                    DeviceIO.put(DevNameTmp, os);
                    DevicePass.put(DevNameTmp, PassTextTmp);
                    DeviceSocket.put(DevNameTmp, socket);
                    DevNameTmp = "";
                    os.write(("DevName=" + DevName + "\r\n").getBytes());
                    continue;
                }

                // получить список подключенных устройств
                if (Json.toString().indexOf("{list=list}") != -1) {
                    Set<String> keys = DeviceIO.keySet();
                    for (String key : keys) {
                        os.write((key + "  (" + key.length() + " ").getBytes());
                        if (DeviceSocket.get(key).isConnected()) {
                            os.write((" connect ").getBytes());
                        } else {
                            os.write((" disconnect ").getBytes());
                            //DeviceIO.remove(key);
                            //DeviceSocket.remove(key);
                        }
                        os.write((")\r\n").getBytes());
                    }
                    continue;
                }

                if (Json.toString().indexOf("{reboot=reboot}") != -1) {
                    os.write(("Kill connect SignalServer \r\n").getBytes());
                    rebootDevice();
                    continue;
                }

                // получить список подключенных устройств
                if (Json.toString().indexOf("{exit=exit}") != -1) {
                    break;
                }

                if (Json.containsKey("device") == true) {
                    deviceName = Json.get("device").toString();
                }
                if ((Json.containsKey("pass") == true) && (deviceName.length() > 0)) {
                    PassText = Json.get("pass").toString();
                    PassText = PassText.replace("\n", "");
                    PassText = PassText.replace("\r", "");
                    PassText = PassText.replace(" ", "");
                }

                if (Json.containsKey("stream") == true) {
                    isStream = true;
                }

                // отправляем сообщение другому устройству текст
                if ((deviceName.length() > 0) && (Json.containsKey("msg") == true)) {
                    if (DeviceIO.containsKey(deviceName) == true) {
                        OutputStream osDst = DeviceIO.get(deviceName);
                        String PassTextTMP = DevicePass.get(deviceName);
                        if (PassText.equals(PassTextTMP) == false) {
                            os.write(("\r\nerror pass:" + deviceName + "  " + PassTextTMP.length() + "\r\n").getBytes());
                            continue;
                        }
                        osDst.write(Json.get("msg").toString().getBytes());
                        os.write(("\r\nsend:" + deviceName + "\r\n").getBytes());
                    } else {
                        //DeviceIO.remove(DevName);
                        os.write(("\r\nno device\r\n").getBytes());
                    }
                    continue;
                }

                // отправляем сообщение другому устройству POST
                if ((deviceName.length() > 0) && (isPost == true)) {
                    if (DeviceIO.containsKey(deviceName) == true) {
                        OutputStream osDst = DeviceIO.get(deviceName);
                        String PassTextTMP = DevicePass.get(deviceName);
                        if (PassText.equals(PassTextTMP) == false) {
                            os.write(("\r\nerror pass:" + deviceName + "  " + PassTextTMP.length() + "\r\n").getBytes());
                            continue;
                        }
                        osDst.write(POST);
                        os.write(("\r\nsend:" + deviceName).getBytes());
                    } else {
                        //DeviceIO.remove(DevName);
                        os.write(("\r\nno device\r\n").getBytes());
                    }
                    continue;
                }

                // [CDS]shutdownInput in read
                if (deviceName.length() > 0) {
                    if (DeviceIO.containsKey(deviceName) == true) {
                        OutputStream osDst = DeviceIO.get(deviceName);
                        String PassTextTMP = DevicePass.get(deviceName);
                        if (PassText.equals(PassTextTMP) == false) {
                            os.write(("\r\nerror pass:" + deviceName + "  " + PassTextTMP.length() + "\r\n").getBytes());
                            continue;
                        }
                        osDst.write(bufferRaw.toByteArray());
                        os.write(("\r\nsend:" + deviceName).getBytes());
                    } else {
                        //DeviceIO.remove(DevName);
                        os.write(("\r\nno device\r\n").getBytes());
                    }
                    continue;
                }
                // os.write(("ok\r\n").getBytes());
                // os.write((Json.toString() + "\r\n").getBytes());
                // os.write((Json.toString().length() + "\r\n").getBytes());
                Json.clear();
            }
            is.close();
            os.close();

            stopSocket();
            DeviceIO.remove(DevName);
            return;
        }
        public static void rebootDevice() {
            Set<String> keys = DeviceIO.keySet();
            for (String key : keys) {
                OutputStream osDst = DeviceIO.get(key);
                Socket soc = DeviceSocket.get(key);
                if (soc.isConnected()){
                    try {
                        osDst.write(" Kill connect \r\n".getBytes());
                        soc.shutdownInput();
                        soc.shutdownOutput();
                        soc.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                DeviceIO.remove(key);
                DevicePass.remove(key);
                DeviceSocket.remove(key);
            }
        }


        private void drawHTML(String DevNameTmp) throws IOException {
            if (DevNameTmp.indexOf("GET /run:") != -1) {
                String packName = DevNameTmp.substring(DevNameTmp.indexOf("GET /run:") + "GET /run:".length(), DevNameTmp.length() - " HTTP/1.1".length());
                os.write("HTTP/1.1 200 OK\r\n".getBytes());
                os.write("Content-Type: text/plain; charset=utf-8\r\n".getBytes());
                os.write("Connection: close\r\n".getBytes());
                os.write("Server: HTMLserver\r\n".getBytes());
                os.write("\r\n\r\n".getBytes());
                os.write(("run:" + packName).getBytes());
                try {
                    Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packName);
                    if (launchIntent != null) {
                        context.startActivity(launchIntent);//null pointer check in case package name was not found
                    }
                } catch (Exception e) {
                    os.write(("\r\nError:" + e.toString()).getBytes());
                }
                return;
            }
            os.write("HTTP/1.1 200 OK\r\n".getBytes());
            os.write("Content-Type: text/plain; charset=utf-8\r\n".getBytes());
            os.write("Connection: close\r\n".getBytes());
            os.write("Server: HTMLserver\r\n".getBytes());
            os.write("\r\n\r\n".getBytes());
            os.flush();
            os.write(("Signal server work" + "\r\n").getBytes());
            os.write(("------- Send byt[] -----" + "\r\n").getBytes());
            os.write(("device:dev1" + "\r\n").getBytes());
            os.write(("len:5" + "\r\n").getBytes());
            os.write(("" + "\r\n").getBytes());
            os.write(("12345" + "\r\n").getBytes());
            os.write(("------ Send text -------" + "\r\n").getBytes());
            os.write(("device:dev1" + "\r\n").getBytes());
            os.write(("msg:12345" + "\r\n").getBytes());
            os.write(("------ List device -----" + "\r\n").getBytes());
            os.write(("list" + "\r\n").getBytes());
            os.write(("------------------------" + "\r\n").getBytes());
            Set<String> keys = DeviceIO.keySet();
            for (String key : keys) {
                os.write((key + "  (" + key.length() + " ").getBytes());
                if (DeviceSocket.get(key).isConnected()) {
                    os.write((" connect ").getBytes());
                } else {
                    os.write((" disconnect ").getBytes());
                    // DeviceIO.remove(key);
                    // DeviceSocket.remove(key);
                }
                os.write((")\r\n").getBytes());
            }
            os.write(("=======Query=======" + "\r\n").getBytes());
            os.write((DevNameTmp + "\r\n").getBytes());
            os.write(("=======Query=======" + "\r\n").getBytes());
            os.write(POST);
            os.write(("\r\n").getBytes());
            os.write(("=======APP=======" + "\r\n").getBytes());
            packageManager = context.getPackageManager();
            List<ApplicationInfo> list = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
            for (ApplicationInfo app : list) {
                try {
                    PackageInfo info = packageManager.getPackageInfo(app.packageName, 0);
                    os.write(("\r\n\n===========").getBytes());
                    os.write(("\r\n packageName:" + app.packageName.toString()).getBytes());
                    os.write(("\r\n packageName:" + app.packageName.toString()).getBytes());
                    os.write(("\r\n loadLabel: " + app.loadLabel(packageManager)).getBytes());
                    os.write(("\r\n Install time: " + info.firstInstallTime).getBytes());
                    os.write(("\r\n Last update time: " + info.lastUpdateTime).getBytes());
                    os.write(("\r\n===========").getBytes());
                    Drawable appIcon = context.getPackageManager().getApplicationIcon(app.packageName);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
            }
            os.write(("=======APP=======" + "\r\n").getBytes());
        }

    }
}

