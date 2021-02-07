package com.example.signalserver.WebServer;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.PowerManager;
import android.telephony.TelephonyManager;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Set;

import static java.net.URLEncoder.encode;

/**
 * Created by myasnikov on 14.01.16.
 */
public class HttpSrv {

    public static int port = 9090;
    public static int TimeOut = 300000;
    private boolean process = false;
    private static Context context;
    private PackageManager packageManager = null;
    public static Hashtable<String, Object> sessionGlobal = new Hashtable<String, Object>(10, (float) 0.5);

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
        public static Hashtable<String, String> DeviceRouterIp = new Hashtable<String, String>(10, (float) 0.5);
        public static Hashtable<String, String> DevicePass = new Hashtable<String, String>(10, (float) 0.5);
        public static Hashtable<String, Socket> DeviceSocket = new Hashtable<String, Socket>(10, (float) 0.5);

        private Socket socket;
        private InputStream is;
        private OutputStream os;
        private String Adress = "";

        private SocketProcessor(Socket socket) throws Throwable {
            this.socket = socket;
            // this.socket.setSoTimeout(TimeOut);
            this.is = socket.getInputStream();
            this.os = socket.getOutputStream();
            Json.clear();
            JsonParam.clear();
            Adress = socket.getRemoteSocketAddress().toString();
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
            String DevNameSocket = "";
            String DevPasseSocket = "";
            String deviceName = "";
            String PassText = "";
            String PassTextTmp = "";
            String RouterIpTmp = "";
            boolean isStream = false;
            rebootOffLineDevice();
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
                        if ((numLine == 2) && (RouterIpTmp.length() == 0)) {
                            RouterIpTmp = sbTmp.toString().replace("\n", "").replace("\r", "");
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
                    drawHTML(DevNameTmp, Json);
                    break;
                }
                if ((DevNameTmp.indexOf("POST ") != -1) && (DevNameTmp.indexOf("HTTP/1.1") != -1)) {
                    drawPostHTML(DevNameTmp, Json, POST);
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
                    RouterIpTmp = RouterIpTmp.replace("\n", "");
                    RouterIpTmp = RouterIpTmp.replace("\r", "");
                    RouterIpTmp = RouterIpTmp.replace(" ", "");
                    rebootOneDevice(DevNameTmp);
                    DeviceIO.put(DevNameTmp, os);
                    DevicePass.put(DevNameTmp, PassTextTmp);
                    DeviceSocket.put(DevNameTmp, socket);
                    DeviceRouterIp.put(DevNameTmp, RouterIpTmp);
                    DevNameSocket = DevNameTmp;
                    DevPasseSocket = PassTextTmp;
                    DevNameTmp = "";
                    os.write(("DevName=" + DevName + "\r\n").getBytes());
                    os.write(("RouterIpTmp=" + RouterIpTmp + "\r\n").getBytes());
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
                    rebootDevice(DevName);
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
                if ((deviceName.length() > 0) && (Json.containsKey("routerip") == true)) {
                    DeviceRouterIp.put(deviceName, Json.get("routerip").toString());
                }

                // отправляем сообщение другому устройству текст
                if ((deviceName.length() > 0) && (Json.containsKey("msg") == true)) {
                    if (DeviceIO.containsKey(deviceName) == true) {
                        try {
                            OutputStream osDst = DeviceIO.get(deviceName);
                            String PassTextTMP = DevicePass.get(deviceName);
                            if (PassText.equals(PassTextTMP) == false) {
                                os.write(("\r\nerror pass:" + deviceName + "  " + PassTextTMP.length() + "\r\n").getBytes());
                                continue;
                            }
                            osDst.write(Json.get("msg").toString().getBytes());
                            // osDst.write(Json.get("msg").toString().getBytes());
                            os.write(("\r\nsend:" + deviceName + "\r\n").getBytes());
                        } catch (Exception e) {
                            rebootOneDevice(deviceName);
                        }
                    } else {
                        //DeviceIO.remove(DevName);
                        os.write(("\r\nno device\r\n").getBytes());
                    }
                    continue;
                }

                if (Json.toString().indexOf("{ping=ping}") != -1) {
                    os.write(("ping\r\n").getBytes());
                    continue;
                }

                if (Json.toString().indexOf("{from=from}") != -1) {
                    if (DeviceIO.containsKey(deviceName) == true) {
                        try {
                            OutputStream osDst = DeviceIO.get(deviceName);
                            if (DevPasseSocket.length()>0){
                                osDst.write(("from:" + DevNameSocket + ":" + DevPasseSocket + "\r\n").getBytes());
                            } else {
                                osDst.write(("from:" + DevNameSocket + "\r\n").getBytes());
                            }
                            os.write(("\r\nsend:" + deviceName + "\r\n").getBytes());
                        } catch (Exception e) {
                            rebootOneDevice(deviceName);
                        }
                    } else {
                        //DeviceIO.remove(DevName);
                        os.write(("\r\nno device\r\n").getBytes());
                    }
                    continue;
                }

                // отправляем сообщение другому устройству POST
                if ((deviceName.length() > 0) && (isPost == true)) {
                    if (DeviceIO.containsKey(deviceName) == true) {
                        try {
                            OutputStream osDst = DeviceIO.get(deviceName);
                            String PassTextTMP = DevicePass.get(deviceName);
                            if (PassText.equals(PassTextTMP) == false) {
                                os.write(("\r\nerror pass:" + deviceName + "  " + PassTextTMP.length() + "\r\n").getBytes());
                                continue;
                            }
                            osDst.write(POST);
                            os.write(("\r\nsend:" + deviceName + "\r\n").getBytes());
                        } catch (Exception e) {
                            rebootOneDevice(deviceName);
                        }
                    } else {
                        //DeviceIO.remove(DevName);
                        os.write(("\r\nno device\r\n").getBytes());
                    }
                    continue;
                }

                // [CDS]shutdownInput in read
                if (deviceName.length() > 0) {
                    if (DeviceIO.containsKey(deviceName) == true) {
                        try {
                            OutputStream osDst = DeviceIO.get(deviceName);
                            String PassTextTMP = DevicePass.get(deviceName);
                            if (PassText.equals(PassTextTMP) == false) {
                                os.write(("\r\nerror pass:" + deviceName + "  " + PassTextTMP.length() + "\r\n").getBytes());
                                continue;
                            }
                            osDst.write(bufferRaw.toByteArray());
                            os.write(("\r\nsend:" + deviceName + "\r\n").getBytes());
                        } catch (Exception e) {
                            rebootOneDevice(deviceName);
                        }
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
            DevicePass.remove(DevName);
            DeviceSocket.remove(DevName);

            return;
        }

        public void rebootDevice(String deviceName) {
            Set<String> keys = DeviceIO.keySet();
            for (String key : keys) {
                if (deviceName == key) {
                    continue;
                }
                OutputStream osDst = DeviceIO.get(key);
                Socket soc = DeviceSocket.get(key);
                if (soc.isConnected()) {
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

            if (deviceName.length() > 0) {
                OutputStream osDst = DeviceIO.get(deviceName);
                Socket soc = DeviceSocket.get(deviceName);
                if (soc.isConnected()) {
                    try {
                        osDst.write(" Kill connect \r\n".getBytes());
                        soc.shutdownInput();
                        soc.shutdownOutput();
                        soc.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                DeviceIO.remove(deviceName);
                DevicePass.remove(deviceName);
                DeviceSocket.remove(deviceName);
            }
        }

        public void rebootOffLineDevice() {
            Set<String> keys = DeviceSocket.keySet();
            for (String key : keys) {
                Socket soc = DeviceSocket.get(key);
                if (soc.isConnected() == false) {
                    DeviceIO.remove(key);
                    DevicePass.remove(key);
                    DeviceSocket.remove(key);
                }
            }
        }

        public static void rebootOneDevice(String DevName) {
            Set<String> keys = DeviceIO.keySet();
            for (String key : keys) {
                if (key.equals(DevName) == true) {
                    OutputStream osDst = DeviceIO.get(key);
                    Socket soc = DeviceSocket.get(key);
                    if (soc.isConnected()) {
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
        }

        public static String getMimeType(String url) {
            String type = null;
            String extension = MimeTypeMap.getFileExtensionFromUrl(url);
            if (extension != null) {
                type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            }
            return type;
        }

        /**
         * Закодировать строку кодировкой MD5
         *
         * @param input
         * @return
         */
        private static String getMD5(String input) {
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] messageDigest = md.digest(input.getBytes());
                BigInteger number = new BigInteger(1, messageDigest);
                String hashtext = number.toString(16);
                while (hashtext.length() < 32) {
                    hashtext = "0" + hashtext;
                }
                return hashtext;
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Создание иденификатора подключаемого компьютера  и сохранение его в куках клиенской машины
         */
        private Hashtable<String, String> CreateCompId(Hashtable<String, Object> jsonParam) {
            String IDcomp = "";
            Hashtable<String, String> session = null;
            IDcomp = getMD5(Adress);
            if (sessionGlobal.containsKey(IDcomp) == true) {
                session = (Hashtable<String, String>) sessionGlobal.get(IDcomp);
            } else {
                session = new Hashtable<String, String>(10, (float) 0.5);
                session.put("ID", IDcomp);
                sessionGlobal.put(IDcomp, session);
            }
            return session;
        }


        /**
         * Обработка POST запроса
         *
         * @param DevNameTmp
         * @param jsonParam
         * @param POST
         * @throws IOException
         */
        private void drawPostHTML(String DevNameTmp, Hashtable<String, Object> jsonParam, byte[] POST) throws IOException {
            String path = DevNameTmp.substring(DevNameTmp.indexOf("POST /") + "POST /".length(), DevNameTmp.length() - " HTTP/1.1".length());
            if (path.length() == 0) {
                path = "index.html";
            }
            String ext = getMimeType(path);
            Hashtable<String, String> session = CreateCompId(jsonParam);
            try {
                InputStream stream = context.getAssets().open(path);
                int size = stream.available();
                byte[] buffer = new byte[size];
                stream.read(buffer);
                stream.close();
                os.write("HTTP/1.1 200 OK\r\n".getBytes());
                os.write(("Content-Type: " + ext + "; charset=utf-8\r\n").getBytes());
                os.write("Connection: close\r\n".getBytes());
                os.write("Server: HTMLserver\r\n".getBytes());
                os.write("\r\n".getBytes());
                os.write(buffer);
                os.flush();
                return;
            } catch (IOException e) {
                // файл не найден FileNonFound
            }

            if (DevNameTmp.indexOf("POST /cmd:") != -1) {
                String msg = DevNameTmp.substring("POST /cmd:".length(), DevNameTmp.length() - " HTTP/1.1".length());
                if (msg.indexOf(":") != -1) {
                    String nam = msg.split(":")[0];
                    String val = msg.split(":")[1];
                    session.put(nam, val);
                } else {
                    session.put("msg", msg);
                }
                String deviceName = "";
                String PassText = "";
                if (session.containsKey("device") == true) {
                    deviceName = session.get("device").toString();
                }
                if (session.containsKey("pass") == true) {
                    PassText = session.get("pass").toString();
                }
                os.write("HTTP/1.1 200 OK\r\n".getBytes());
                os.write(("Content-Type: " + ext + "; charset=utf-8\r\n").getBytes());
                os.write("Connection: close\r\n".getBytes());
                os.write("Server: HTMLserver\r\n".getBytes());
                os.write("\r\n".getBytes());
                if (deviceName.length() > 0) {
                    if (DeviceIO.containsKey(deviceName) == true) {
                        OutputStream osDst = DeviceIO.get(deviceName);
                        String PassTextTMP = DevicePass.get(deviceName);
                        if (PassText.equals(PassTextTMP) == false) {
                            os.write(("\r\nerror pass:" + deviceName + "  " + PassTextTMP.length() + "\r\n").getBytes());
                            os.flush();
                            return;
                        } else {
                            osDst.write(POST);
                            os.write(("\r\nsend:" + deviceName + "\r\n").getBytes());
                            os.flush();
                            return;
                        }
                    } else {
                        //DeviceIO.remove(DevName);
                        os.write(("\r\nno device\r\n").getBytes());
                        os.flush();
                        return;
                    }
                }
                os.write("\r\ncmd:\r\n".getBytes());
                os.write(POST);
                os.flush();
                return;
            }

            // дописать обработку POST запроса
            os.write("HTTP/1.1 200 OK\r\n".getBytes());
            os.write(("Content-Type: " + ext + "; charset=utf-8\r\n").getBytes());
            os.write("Connection: close\r\n".getBytes());
            os.write("Server: HTMLserver\r\n".getBytes());
            os.write("\r\n".getBytes());
            os.write("\r\nPOST\r\n".getBytes());
            os.write(POST);
            os.flush();
            // ---------------------------------
        }

        /**
         * Обработка GET запроса
         *
         * @param DevNameTmp
         * @param jsonParam
         * @throws IOException
         */
        private void drawHTML(String DevNameTmp, Hashtable<String, Object> jsonParam) throws IOException {
            String path = DevNameTmp.substring(DevNameTmp.indexOf("GET /") + "GET /".length(), DevNameTmp.length() - " HTTP/1.1".length());
            if (path.length() == 0) {
                path = "index.html";
            }
            String ext = getMimeType(path);
            Hashtable<String, String> session = CreateCompId(jsonParam);

            try {
                InputStream stream = context.getAssets().open(path);
                int size = stream.available();
                byte[] buffer = new byte[size];
                stream.read(buffer);
                stream.close();
                os.write("HTTP/1.1 200 OK\r\n".getBytes());
                os.write(("Content-Type: " + ext + "; charset=utf-8\r\n").getBytes());
                os.write("Connection: close\r\n".getBytes());
                os.write("Server: HTMLserver\r\n".getBytes());
                os.write("\r\n".getBytes());
                os.write(buffer);
                os.flush();
                return;
            } catch (IOException e) {
                // файл не найден FileNonFound
            }
            /// показать список установленых программ
            if (DevNameTmp.indexOf("GET /cmd:") != -1) {
                String msg = DevNameTmp.substring("GET /cmd:".length(), DevNameTmp.length() - " HTTP/1.1".length());
                if (msg.indexOf(":") != -1) {
                    String nam = msg.split(":")[0];
                    String val = msg.split(":")[1];
                    session.put(nam, val);
                } else {
                    session.put("msg", msg);
                }
                String deviceName = "";
                String PassText = "";

                if (session.containsKey("device") == true) {
                    deviceName = session.get("device").toString();
                }
                if (session.containsKey("pass") == true) {
                    PassText = session.get("pass").toString();
                }

                os.write("HTTP/1.1 200 OK\r\n".getBytes());
                os.write("Content-Type: application/json; charset=utf-8\r\n".getBytes());
                os.write("Connection: close\r\n".getBytes());
                os.write("Server: HTMLserver\r\n".getBytes());
                os.write("\r\n".getBytes());

                if (deviceName.length() > 0) {
                    if (DeviceIO.containsKey(deviceName) == true) {
                        OutputStream osDst = DeviceIO.get(deviceName);
                        String PassTextTMP = DevicePass.get(deviceName);
                        if (PassText.equals(PassTextTMP) == false) {
                            os.write(("\r\nerror pass:" + deviceName + "  " + PassTextTMP.length() + "\r\n").getBytes());
                            os.flush();
                            return;
                        } else {
                            osDst.write(msg.getBytes());
                            os.write(("\r\nsend:" + deviceName + "\r\n").getBytes());
                            os.flush();
                            return;
                        }
                    } else {
                        //DeviceIO.remove(DevName);
                        os.write(("\r\nno device\r\n").getBytes());
                        os.flush();
                        return;
                    }
                }
                os.write(("\r\n" + msg).getBytes());
                os.write(("\r\n session " + session).getBytes());
                os.write(("\r\n jsonParam " + jsonParam).getBytes());
                os.write(("\r\n Adress " + Adress).getBytes());
                os.flush();
                return;
            }


            // список подключенных устройств
            if (DevNameTmp.indexOf("GET /device.json") != -1) {
                // показать список подключенных устройств
                os.write("HTTP/1.1 200 OK\r\n".getBytes());
                // os.write("Content-Type: text/plain; charset=utf-8\r\n".getBytes());
                os.write("Content-Type: application/json; charset=utf-8\r\n".getBytes());
                os.write("Connection: close\r\n".getBytes());
                os.write("Server: HTMLserver\r\n".getBytes());
                os.write("\r\n".getBytes());
                os.flush();
                try {
                    JSONArray jsonAr = new JSONArray();
                    for (Object key : DeviceIO.keySet()) {
                        JSONObject jsonDev = new JSONObject();
                        jsonDev.put("devname", key.toString());
                        jsonDev.put("routerip", DeviceRouterIp.get(key));
                        Socket soc = DeviceSocket.get(key);
                        jsonDev.put("isConnected", soc.isClosed());
                        jsonAr.put(jsonDev);
                    }
                    jsonParam.put("device", jsonAr);
                    //-----------------------------------------------------
                    TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(context.TELEPHONY_SERVICE);
                    String devicIMEI = telephonyManager.getDeviceId();
                    jsonParam.put("devicIMEI", devicIMEI);
                } catch (JSONException e) {
                    os.write("-ERROR-\r\n".getBytes());
                }
                os.write((jsonParam + "").getBytes());
                return;
            }

            /*
            os.write("HTTP/1.1 200 OK\r\n".getBytes());
            os.write("Content-Type: text/html; charset=utf-8\r\n".getBytes());
            os.write("Connection: close\r\n".getBytes());
            os.write("Server: HTMLserver\r\n".getBytes());
            os.write("\r\n".getBytes());
            os.flush();
            os.write(("<br/><a  target=\"_blank\"  href=\"device.json\">Device</a> ").getBytes());
            os.write(("<br/><a  target=\"_blank\"  href=\"app.json\">AppList</a> ").getBytes());
            os.write(("<br/><hr/>").getBytes());
            packageManager = context.getPackageManager();
            List<ApplicationInfo> listApp = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
            for (ApplicationInfo appInf : listApp) {
                String packTxt = appInf.loadLabel(packageManager).toString();
                String packName = appInf.packageName.toString();
                os.write(("<br/><a  target=\"_blank\"  href=\"/run:" + packName + "\">" + packTxt + "</a> ").getBytes());
            }
            */
        }

        private void drawHTMLOld(String DevNameTmp) throws IOException {
            JSONObject json = new JSONObject();

            if (DevNameTmp.indexOf("GET /run:") != -1) {
                String packName = DevNameTmp.substring(DevNameTmp.indexOf("GET /run:") + "GET /run:".length(), DevNameTmp.length() - " HTTP/1.1".length());
                os.write("HTTP/1.1 200 OK\r\n".getBytes());
                os.write("Content-Type: application/json; charset=utf-8\r\n".getBytes());
                os.write("Connection: close\r\n".getBytes());
                os.write("Server: HTMLserver\r\n".getBytes());
                os.write("\r\n".getBytes());
                packName = URLDecoder.decode(packName, "UTF-8");
                packageManager = context.getPackageManager();
                List<ApplicationInfo> listApp = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
                for (ApplicationInfo appInf : listApp) {
                    String packTxt = appInf.loadLabel(packageManager).toString();
                    if (packTxt.equals(packName)) {
                        packName = appInf.packageName.toString();
                        break;
                    }
                }
                os.write(("{\"run\":\"" + packName + "\"}").getBytes());
                Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packName);
                if (launchIntent != null) {
                    context.startActivity(launchIntent);//null pointer check in case package name was not found
                }
                return;
            }

            /// показать список установленых программ
            if (DevNameTmp.indexOf("GET /app.json") != -1) {
                try {
                    os.write("HTTP/1.1 200 OK\r\n".getBytes());
                    // os.write("Content-Type: text/plain; charset=utf-8\r\n".getBytes());
                    os.write("Content-Type: application/json; charset=utf-8\r\n".getBytes());
                    os.write("Connection: close\r\n".getBytes());
                    os.write("Server: HTMLserver\r\n".getBytes());
                    os.write("\r\n".getBytes());
                    os.flush();
                    JSONArray appInfoArr = new JSONArray();
                    packageManager = context.getPackageManager();
                    List<ApplicationInfo> listApp = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
                    for (ApplicationInfo appInf : listApp) {
                        try {
                            JSONObject jsonApp = new JSONObject();
                            PackageInfo info = packageManager.getPackageInfo(appInf.packageName, 0);
                            jsonApp.put("packageName", appInf.packageName.toString());
                            jsonApp.put("loadLabel", appInf.loadLabel(packageManager));
                            jsonApp.put("Last_update_time", info.lastUpdateTime);
                            // Drawable appIcon = context.getPackageManager().getApplicationIcon(app.packageName);
                            appInfoArr.put(jsonApp);
                        } catch (PackageManager.NameNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                    json.put("app", appInfoArr);
                } catch (JSONException e) {
                    os.write("-ERROR-\r\n".getBytes());
                }
                os.write((json + "").getBytes());
                return;
            }

            // список подключенных устройств
            if (DevNameTmp.indexOf("GET /device.json") != -1) {
                // показать список подключенных устройств
                os.write("HTTP/1.1 200 OK\r\n".getBytes());
                // os.write("Content-Type: text/plain; charset=utf-8\r\n".getBytes());
                os.write("Content-Type: application/json; charset=utf-8\r\n".getBytes());
                os.write("Connection: close\r\n".getBytes());
                os.write("Server: HTMLserver\r\n".getBytes());
                os.write("\r\n".getBytes());
                os.flush();
                try {
                    JSONArray jsonAr = new JSONArray();
                    for (Object key : DeviceIO.keySet()) {
                        JSONObject jsonDev = new JSONObject();
                        jsonDev.put("devname", key.toString());
                        jsonDev.put("routerip", DeviceRouterIp.get(key));
                        Socket soc = DeviceSocket.get(key);
                        jsonDev.put("isConnected", soc.isClosed());
                        jsonAr.put(jsonDev);
                    }
                    json.put("device", jsonAr);
                    //-----------------------------------------------------
                    TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(context.TELEPHONY_SERVICE);
                    String devicIMEI = telephonyManager.getDeviceId();
                    json.put("devicIMEI", devicIMEI);
                } catch (JSONException e) {
                    os.write("-ERROR-\r\n".getBytes());
                }
                os.write((json + "").getBytes());
                return;
            }

            os.write("HTTP/1.1 200 OK\r\n".getBytes());
            os.write("Content-Type: text/html; charset=utf-8\r\n".getBytes());
            os.write("Connection: close\r\n".getBytes());
            os.write("Server: HTMLserver\r\n".getBytes());
            os.write("\r\n".getBytes());
            os.flush();
            os.write(("<br/><a  target=\"_blank\"  href=\"device.json\">Device</a> ").getBytes());
            os.write(("<br/><a  target=\"_blank\"  href=\"app.json\">AppList</a> ").getBytes());
            os.write(("<br/><hr/>").getBytes());
            packageManager = context.getPackageManager();
            List<ApplicationInfo> listApp = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
            for (ApplicationInfo appInf : listApp) {
                String packTxt = appInf.loadLabel(packageManager).toString();
                String packName = appInf.packageName.toString();
                os.write(("<br/><a  target=\"_blank\"  href=\"/run:" + packName + "\">" + packTxt + "</a> ").getBytes());
            }
        }
    }
}

