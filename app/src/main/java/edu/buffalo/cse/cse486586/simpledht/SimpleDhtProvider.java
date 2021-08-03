package edu.buffalo.cse.cse486586.simpledht;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Formatter;

import android.bluetooth.BluetoothClass;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.Telephony;
import android.telephony.TelephonyManager;
import android.util.DebugUtils;
import android.util.Log;

public class SimpleDhtProvider extends ContentProvider {

    public static final int SERVER_PORT = 10000;
    public static int connected=0;
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    public static String[] columns = new String[]{"key","value"};
    public static  MatrixCursor result = new MatrixCursor(columns,1);
    public static final String TAG = "SimpleDhtProvider";
    public static String portStr;
    public static boolean done=false;
    public static class predecessor{
        int port;
        String hash;

    }
    public static class successor{
        int port;
        String hash;

    }
    public static StringBuilder key_val =new StringBuilder();
    predecessor p= new predecessor();
    public static String first;

    successor s = new successor();
    public static int successor;

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        try {
            if (selection.compareTo("@") == 0) {
                String[] files = getContext().fileList();
                for (String k : files) {
                    getContext().deleteFile(k);
                }

            } else if (selection.compareTo("*") == 0) {
                Socket socket0 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), (Integer.parseInt(first)) * 2);
                socket0.setSoTimeout(500);
                DataOutputStream insertsend = new DataOutputStream(socket0.getOutputStream());
                insertsend.writeUTF("DeleteAll");


                DataInputStream getPortStream = new DataInputStream(new BufferedInputStream(socket0.getInputStream()));
                String ack = getPortStream.readUTF();

                Log.e(TAG, ack);


                if (ack.compareTo("ACK") == 0) {
                    getPortStream.close();
                    insertsend.close();
                    socket0.close();
                }
            }else{

                Socket socket0 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), (Integer.parseInt(first)) * 2);
                socket0.setSoTimeout(500);
                DataOutputStream insertsend = new DataOutputStream(socket0.getOutputStream());
                insertsend.writeUTF("Delete:"+ selection);
                //Log.d("Hey", "File Written123");

                DataInputStream getPortStream = new DataInputStream(new BufferedInputStream(socket0.getInputStream()));
                String ack = getPortStream.readUTF();

                Log.e(TAG, ack);


                if (ack.compareTo("ACK") == 0) {
                    getPortStream.close();
                    insertsend.close();
                    socket0.close();
                }

            }
        }catch (Exception e){
            Log.e(TAG,e.toString());
        }
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    public void dummy(ContentValues values){
        try{
            FileOutputStream outStream;
            outStream = getContext().openFileOutput(values.getAsString("key"), Context.MODE_PRIVATE);
            Log.d("OutputFILe", values.getAsString("key"));
            String msg = values.getAsString("value");
            outStream.write(msg.getBytes());
            outStream.close();
            Log.d("Hey", "File Written");}catch (Exception e){}
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {

        try {

            if(connected==0 && portStr.compareTo("5554")!=0){
                dummy(values);
            }
                Socket socket0 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), (Integer.parseInt(first))*2);
                socket0.setSoTimeout(500);
                DataOutputStream insertsend= new DataOutputStream(socket0.getOutputStream());
                insertsend.writeUTF("Insert:"+values.getAsString("key")+","+values.getAsString("value"));
                //Log.d("Hey", "File Written123");

                DataInputStream getPortStream = new DataInputStream(new BufferedInputStream(socket0.getInputStream()));
                String ack= getPortStream.readUTF();

                Log.e(TAG,ack);


                if (ack.compareTo("ACK")==0){
                    getPortStream.close();
                    insertsend.close();
                    socket0.close();
                }

        }
        catch (Exception e)
        {
            Log.d("exception",e.getMessage());
        }


        Log.d("insert", values.toString());
        return uri;
    }

    @Override
    public boolean onCreate() {
        // TODO Auto-generated method stub
        TelephonyManager tel =
                (TelephonyManager)getContext().getSystemService (Context.TELEPHONY_SERVICE);
        portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        if(portStr.equalsIgnoreCase("5554")){
            p.port=Integer.parseInt(portStr);
            s.port=Integer.parseInt(portStr);
            first="5554";
        }else{
            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, portStr);
        }
            try {
                ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
                new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
            }catch (Exception e) {
                Log.e(TAG, "Server Cannot Start" + e);
            }


        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        int i=0;
        result=new MatrixCursor(columns,1);
        char msg;
        StringBuilder sb = new StringBuilder();
        Object[] res = new Object[2];
        try {
            if(selection.compareTo("*")==0){
                if(connected==0){
                    String[] files =getContext().fileList();
                    for( String k: files) {
                        FileInputStream inputStream=getContext().openFileInput(k);
                        while((i= inputStream.read()) != -1){
                            msg = ((char) i);
                            sb.append(msg);
                        }
                        res[0]= k;
                        res[1]= sb.toString();
                        sb.setLength(0);
                        result.addRow(res);
                    }
                }else {

                    Socket socket0 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), (Integer.parseInt(first)) * 2);
                    socket0.setSoTimeout(500);
                    DataOutputStream insertsend = new DataOutputStream(socket0.getOutputStream());
                    insertsend.writeUTF("QueryALL:" + portStr);

                    //ObjectOutputStream sendres = new ObjectOutputStream(socket0.getOutputStream());
                    insertsend.writeUTF("");

                    //Log.d("Hey", "File Written123");

                    DataInputStream getPortStream = new DataInputStream(new BufferedInputStream(socket0.getInputStream()));
                    String ack = getPortStream.readUTF();

                    Log.e(TAG, ack);


                    if (ack.compareTo("ACK") == 0) {
                        getPortStream.close();
                        insertsend.close();
                        socket0.close();
                    }
                    while (!done) {
                        continue;
                    }
                    done = false;
                }

            }
            else if(selection.compareTo("@")==0){
                String[] files =getContext().fileList();
                for( String k: files) {
                    FileInputStream inputStream=getContext().openFileInput(k);
                    while((i= inputStream.read()) != -1){
                        msg = ((char) i);
                        sb.append(msg);
                    }
                    res[0]= k;
                    res[1]= sb.toString();
                    sb.setLength(0);
                    result.addRow(res);
                }

            }
            else {

                if(connected==0){
                    FileInputStream inputStream=getContext().openFileInput(selection);
                    while((i= inputStream.read()) != -1){
                        msg = ((char) i);
                        sb.append(msg);
                    }
                    res[0]= selection;
                    res[1]= sb.toString();
                    sb.setLength(0);
                    result.addRow(res);
                }else {

                    Socket socket0 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), (Integer.parseInt(first)) * 2);
                    socket0.setSoTimeout(500);
                    DataOutputStream insertsend = new DataOutputStream(socket0.getOutputStream());
                    insertsend.writeUTF("Query:" + portStr + "," + selection);
                    //Log.d("Hey", "File Written123");

                    DataInputStream getPortStream = new DataInputStream(new BufferedInputStream(socket0.getInputStream()));
                    String ack = getPortStream.readUTF();

                    Log.e(TAG, ack);


                    if (ack.compareTo("ACK") == 0) {
                        getPortStream.close();
                        insertsend.close();
                        socket0.close();
                    }
                    while (!done) {
                        continue;
                    }
                    done = false;

                }
            }
        }
        catch (Exception e)
        {
            Log.d("TAG", "exception occured");
        }
        return result;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            String myPort= msgs[0];

            try {
                    Socket socket0 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(REMOTE_PORT0));
                    socket0.setSoTimeout(500);
                    DataOutputStream outputStream= new DataOutputStream(socket0.getOutputStream());
                    outputStream.writeUTF("1:"+myPort);

                    DataInputStream getPortStream = new DataInputStream(new BufferedInputStream(socket0.getInputStream()));
                    first= getPortStream.readUTF();

                    //s.port=Integer.parseInt(receivedmsg.split(",")[0]);
                    //p.port=Integer.parseInt(receivedmsg.split(",")[1]);

                    //Log.e(TAG, first);


                    if (!first.isEmpty()){
                        getPortStream.close();
                        outputStream.close();
                        socket0.close();
                        connected=1;
                    }

                }
                catch (Exception e)
                {
                    Log.e(TAG,"Error in client Task"+e);
                }

            return null;
        }

    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void>{


        public void sendpredtofirst(String port){

            try {
                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), (Integer.parseInt(first))*2);

                DataOutputStream sendlast= new DataOutputStream(socket.getOutputStream());
                sendlast.writeUTF("last:"+port);

                DataInputStream readack= new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                String ack=readack.readUTF();

                if(ack.compareTo("ACK")==0){
                    socket.close();
                    sendlast.close();
                    readack.close();

                }
            }catch (Exception e){
                Log.e(TAG,"last cannot be gotten");
            }

        }

        public void decideinsert(String msg){

            FileOutputStream outStream;


            try {
                String key= msg.split(",")[0];
                String value= msg.split(",")[1];
                String keyhash = genHash(key);

                if ((keyhash.compareTo(genHash(Integer.toString(p.port))) > 0 && keyhash.compareTo(genHash(portStr)) < 0) ||
                        ((portStr.compareTo(first))==0 && (keyhash.compareTo(genHash(Integer.toString(p.port)))>0 || keyhash.compareTo(genHash(portStr))<0))) {
                    outStream = getContext().openFileOutput(key, Context.MODE_PRIVATE);
                    Log.d("OutputFILe", key);
                    Log.e(TAG, "Inserted******"+ keyhash);
                    outStream.write(value.getBytes());
                    outStream.close();
                    Log.d("Hey", "File Written");
                }
                else{
                    Socket socket0 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), s.port*2);
                    socket0.setSoTimeout(500);
                    DataOutputStream insertsend= new DataOutputStream(socket0.getOutputStream());
                    insertsend.writeUTF("Insert:"+key+","+value);
                    //Log.d("Hey", "File Written123");

                    DataInputStream getPortStream = new DataInputStream(new BufferedInputStream(socket0.getInputStream()));
                    String ack= getPortStream.readUTF();

                    Log.e(TAG,ack);


                    if (ack.compareTo("ACK")==0){
                        getPortStream.close();
                        insertsend.close();
                        socket0.close();
                    }

                }

            }
            catch (Exception e)
            {
                Log.d("exception",e.toString());
            }
        }


        public void queryall(String msg, StringBuilder temp){
            int i=0;
            int port= Integer.parseInt(msg);
            StringBuilder sb = new StringBuilder();
            char c;
            try {
                String[] files = getContext().fileList();
                for (String k : files) {
                    FileInputStream inputStream = getContext().openFileInput(k);
                    while ((i = inputStream.read()) != -1) {
                        c = ((char) i);
                        sb.append(c);
                    }
                    temp.append(k+"_"+sb.toString()+",");
                    sb.setLength(0);
                }

                if(s.port==Integer.parseInt(first) ){
                    Socket socket0 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), (Integer.parseInt(msg))*2);
                    socket0.setSoTimeout(500);
                    DataOutputStream allquery = new DataOutputStream(socket0.getOutputStream());

                    if(temp.length()>0) {
                        temp = temp.deleteCharAt(temp.length() - 1);
                    }
                    allquery.writeUTF("AllResult:"+temp.toString());

                    DataInputStream getPortStream = new DataInputStream(new BufferedInputStream(socket0.getInputStream()));
                    String ack = getPortStream.readUTF();

                    if (ack.compareTo("ACK") == 0) {
                        getPortStream.close();
                        allquery.close();
                        socket0.close();
                    }
                }
                else{
                    Socket socket0 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), s.port * 2);
                    socket0.setSoTimeout(500);
                    DataOutputStream insertsend = new DataOutputStream(socket0.getOutputStream());
                    insertsend.writeUTF("QueryALL:" + msg);

                    //ObjectOutputStream sendres = new ObjectOutputStream(socket0.getOutputStream());
                    insertsend.writeUTF(temp.toString());
                    temp.setLength(0);

                    //Log.d("Hey", "File Written123");

                    DataInputStream getPortStream = new DataInputStream(new BufferedInputStream(socket0.getInputStream()));
                    String ack = getPortStream.readUTF();

                    Log.e(TAG, ack);


                    if (ack.compareTo("ACK") == 0) {
                        getPortStream.close();
                        insertsend.close();
                        socket0.close();
                    }
                }
            }catch (Exception e ){

                Log.e(TAG, e.toString());

            }

        }

        public void delquery(String msg){


            String key=msg;


            try {
                String keyhash = genHash(key);

                if ((keyhash.compareTo(genHash(Integer.toString(p.port))) > 0 && keyhash.compareTo(genHash(portStr)) < 0) ||
                        ((portStr.compareTo(first))==0 && (keyhash.compareTo(genHash(Integer.toString(p.port)))>0 || keyhash.compareTo(genHash(portStr))<0))) {
                    getContext().deleteFile(key);


                }
                else{

                    Socket socket0 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), s.port*2);
                    socket0.setSoTimeout(500);
                    DataOutputStream insertsend= new DataOutputStream(socket0.getOutputStream());
                    insertsend.writeUTF("Delete:"+key);
                    //Log.d("Hey", "File Written123");

                    DataInputStream getPortStream = new DataInputStream(new BufferedInputStream(socket0.getInputStream()));
                    String ack= getPortStream.readUTF();

                    Log.d("HEY", "HERE2");
                    Log.e(TAG,ack);


                    if (ack.compareTo("ACK")==0){
                        getPortStream.close();
                        insertsend.close();
                        socket0.close();
                    }

                }

            }
            catch (Exception e)
            {
                Log.d("exception",e.toString());
            }
        }

        public void findquery(String msg){

            int i=0;
            char dum;
            int port=Integer.parseInt(msg.split(",")[0]);
            String key=msg.split(",")[1];
            StringBuilder sb = new StringBuilder();


            try {
                String keyhash = genHash(key);

                if ((keyhash.compareTo(genHash(Integer.toString(p.port))) > 0 && keyhash.compareTo(genHash(portStr)) < 0) ||
                        ((portStr.compareTo(first))==0 && (keyhash.compareTo(genHash(Integer.toString(p.port)))>0 || keyhash.compareTo(genHash(portStr))<0))) {
                    FileInputStream inputStream = getContext().openFileInput(key);
                    while ((i = inputStream.read()) != -1) {
                        dum = ((char) i);
                        sb.append(dum);
                    }


                    if(portStr.compareTo(Integer.toString(port))==0){

                        Object[] res = new Object[2];
                        res[0] = key;
                        res[1] = sb.toString();
                        result.addRow(res);
                        done=true;

                    }
                    else {
                        Socket socket0 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), port * 2);
                        socket0.setSoTimeout(500);
                        DataOutputStream insertsend = new DataOutputStream(socket0.getOutputStream());
                        insertsend.writeUTF("Result:" + key + "," + sb.toString());

                        DataInputStream getPortStream = new DataInputStream(new BufferedInputStream(socket0.getInputStream()));
                        String ack = getPortStream.readUTF();

                        Log.d("HEY", "HERE1");
                        Log.e(TAG, ack);


                        if (ack.compareTo("ACK") == 0) {
                            getPortStream.close();
                            insertsend.close();
                            socket0.close();
                        }
                        Log.d("MSG", sb.toString());
                    }

                }
                else{

                    Socket socket0 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), s.port*2);
                    socket0.setSoTimeout(500);
                    DataOutputStream insertsend= new DataOutputStream(socket0.getOutputStream());
                    insertsend.writeUTF("Query:"+port+","+key);
                    //Log.d("Hey", "File Written123");

                    DataInputStream getPortStream = new DataInputStream(new BufferedInputStream(socket0.getInputStream()));
                    String ack= getPortStream.readUTF();

                    Log.d("HEY", "HERE2");
                    Log.e(TAG,ack);


                    if (ack.compareTo("ACK")==0){
                        getPortStream.close();
                        insertsend.close();
                        socket0.close();
                    }

                }

            }
            catch (Exception e)
            {
                Log.d("exception",e.toString());
            }
        }



        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            Socket socket;

            synchronized (this) {
                while (true) {
                    try
                    {
                        socket = serverSocket.accept();// Connection Accepted
                        socket.setSoTimeout(500);

                        DataInputStream inputStream = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

                        String clientMsg=inputStream.readUTF();
                        Log.e(TAG, clientMsg);

                        //Insert Msg
                        if(clientMsg.matches("Insert:.*")){
                            DataOutputStream sendack = new DataOutputStream(socket.getOutputStream());
                            sendack.writeUTF("ACK");
                            sendack.close();
                            socket.close();
                            decideinsert(clientMsg.split(":")[1]);
                        }


                        //QueryALL
                        if(clientMsg.matches("QueryALL:.*")){

                            key_val.append(inputStream.readUTF());
                            DataOutputStream sendack = new DataOutputStream(socket.getOutputStream());
                            sendack.writeUTF("ACK");
                            sendack.close();
                            socket.close();
                            queryall(clientMsg.split(":")[1],key_val);

                        }

                        //Query Request
                        if(clientMsg.matches("Query:.*")){
                            DataOutputStream sendack = new DataOutputStream(socket.getOutputStream());
                            sendack.writeUTF("ACK");
                            sendack.close();
                            socket.close();
                            findquery(clientMsg.split(":")[1]);
                        }

                        //Delete Request
                        if(clientMsg.matches("Delete:.*")){
                            DataOutputStream sendack = new DataOutputStream(socket.getOutputStream());
                            sendack.writeUTF("ACK");
                            sendack.close();
                            socket.close();
                            delquery(clientMsg.split(":")[1]);
                        }

                        //Delete ALL
                        if(clientMsg.matches("DeleteAll"))
                        {
                            DataOutputStream sendack = new DataOutputStream(socket.getOutputStream());
                            sendack.writeUTF("ACK");
                            sendack.close();
                            socket.close();
                            delete( buildUri("content", "edu.buffalo.cse.cse486586.simpledht.provider"),"@",null);
                            if(s.port!=Integer.parseInt(first)){

                                Socket socket0 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), s.port * 2);
                                socket0.setSoTimeout(500);
                                DataOutputStream insertsend = new DataOutputStream(socket0.getOutputStream());
                                insertsend.writeUTF("DeleteAll");


                                //Log.d("Hey", "File Written123");

                                DataInputStream getPortStream = new DataInputStream(new BufferedInputStream(socket0.getInputStream()));
                                String ack = getPortStream.readUTF();

                                Log.e(TAG, ack);


                                if (ack.compareTo("ACK") == 0) {
                                    getPortStream.close();
                                    insertsend.close();
                                    socket0.close();
                                }

                            }

                        }

                        //Query Response
                        if(clientMsg.matches("Result:.*")){
                            DataOutputStream sendack = new DataOutputStream(socket.getOutputStream());
                            sendack.writeUTF("ACK");
                            sendack.close();
                            socket.close();
                            //String[] columns = new String[]{"key","value"};
                            //MatrixCursor result = new MatrixCursor(columns,1);
                            Object[] res = new Object[2];
                            res[0] = clientMsg.split(":")[1].split(",")[0];
                            res[1] = clientMsg.split(":")[1].split(",")[1];
                            result.addRow(res);
                            done=true;

                        }


                        // * Query Result
                        if(clientMsg.matches("AllResult:.*")){
                            DataOutputStream sendack = new DataOutputStream(socket.getOutputStream());
                            sendack.writeUTF("ACK");
                            sendack.close();
                            socket.close();
                            if(clientMsg.compareTo("AllResult:")!=0) {
                                String[] key_val = clientMsg.split(":")[1].split(",");
                                for (String dum : key_val) {
                                    Object[] res = new Object[2];
                                    res[0] = dum.split("_")[0];
                                    res[1] = dum.split("_")[1];
                                    result.addRow(res);
                                }
                            }
                            done =true;

                        }



                        //Transfer From AVD0 to First Node

                        if(clientMsg.matches("1:.*"))
                        {
                            String port = clientMsg.split(":")[1];
                            try{


                                DataOutputStream ackstream = new DataOutputStream(socket.getOutputStream());
                                ackstream.writeUTF(first);
                                socket.close();
                                connected=1;

                                Socket initialSock= new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), (Integer.parseInt(first))*2);
                                DataOutputStream forwardRequestStream1= new DataOutputStream(initialSock.getOutputStream());
                                forwardRequestStream1.writeUTF(port+":"+genHash(port));

                                if(portStr.compareTo("5554")!=0) {

                                    DataInputStream recvack = new DataInputStream(new BufferedInputStream(initialSock.getInputStream()));
                                    String ack = recvack.readUTF();

                                    if (recvack.readUTF().compareTo("ACK") == 0) {
                                        initialSock.close();
                                        forwardRequestStream1.close();
                                    }
                                }

                            }catch (Exception e ){
                                Log.e(TAG,e.toString());
                            }
                        }


                        //First node reception

                        if(clientMsg.matches("first:.*")){

                            DataOutputStream sendack = new DataOutputStream(socket.getOutputStream());
                            sendack.writeUTF("ACK");
                            socket.close();
                            first=clientMsg.split(":")[1];
                            //Log.e(TAG,first);

                        }


                        //last node reception

                        if(clientMsg.matches("last:.*")){
                            DataOutputStream sendack= new DataOutputStream(socket.getOutputStream());
                            sendack.writeUTF("ACK");
                            sendack.close();
                            socket.close();
                            p.port=Integer.parseInt(clientMsg.split(":")[1]);
                            Log.e(TAG, "Predecessor"+p.port);
                        }



                        //Logic for Node Join

                        if(clientMsg.matches("[0-9]{4}:.*"))
                        {
                            Log.e(TAG,clientMsg);
                            //if(portStr.compareTo("5554")!=0) {
                                DataOutputStream sendack12 = new DataOutputStream(socket.getOutputStream());
                                sendack12.writeUTF("ACK");
                                sendack12.close();
                            //}
                            socket.close();
                            String port=clientMsg.split(":")[0];
                            String hash=clientMsg.split(":")[1];
                            if(hash.compareTo(genHash(portStr))>0){
                                if(s.port==Integer.parseInt(first)){
                                    //Log.e(TAG,"inga1");
                                    s.port=Integer.parseInt(port);
                                    Log.e(TAG,clientMsg);
                                    Socket portSock= new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), (Integer.parseInt(port))*2);
                                    DataOutputStream sendport= new DataOutputStream(portSock.getOutputStream());
                                    sendport.writeUTF(portStr+","+first);

                                    if(portStr.compareTo(first)!=0) {
                                        sendpredtofirst(port);
                                    }else{
                                        p.port=Integer.parseInt(port);
                                        //Log.e(TAG,"last set");
                                    }

                                    DataInputStream ackinp = new DataInputStream(new BufferedInputStream(portSock.getInputStream()));
                                    String ack= ackinp.readUTF();

                                    if (ack.compareTo("ACK")==0){
                                        sendport.flush();
                                        sendport.close();
                                        portSock.close();
                                    }

                                }else{
                                    Log.e(TAG,"here");
                                    Socket reqSock= new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), (s.port*2));
                                    DataOutputStream sendreqt= new DataOutputStream(reqSock.getOutputStream());
                                    sendreqt.writeUTF(port+":"+genHash(port));

                                    DataInputStream ackinp = new DataInputStream(new BufferedInputStream(reqSock.getInputStream()));
                                    String ack= ackinp.readUTF();

                                    if (ack.compareTo("ACK")==0){
                                        sendreqt.flush();
                                        sendreqt.close();
                                        reqSock.close();
                                    }


                                    sendreqt.close();
                                    reqSock.close();
                                }
                            }else {

                                if(portStr.compareTo(first)==0) {

                                    Log.e(TAG, "inga123");
                                    /*if(portStr.compareTo("5554")==0){
                                        first=port;
                                    }*/

                                    first = port;
                                    int send_port = Integer.parseInt(REMOTE_PORT0);
                                    for (int i = 0; i < 5; i++) {
                                        try {
                                            Socket firstsocket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), send_port);
                                            firstsocket.setSoTimeout(500);
                                            DataOutputStream sendport = new DataOutputStream(firstsocket.getOutputStream());
                                            sendport.writeUTF("first:" + port);

                                            DataInputStream ackrecv = new DataInputStream(new BufferedInputStream(firstsocket.getInputStream()));
                                            String ack = ackrecv.readUTF();
                                            send_port = send_port + 4;

                                            Log.e(TAG, ack);
                                            if (ack.compareTo("ACK") == 0) {
                                                sendport.flush();
                                                sendport.close();
                                                firstsocket.close();
                                            }
                                        } catch (Exception e) {
                                            send_port = send_port + 4;
                                            continue;
                                        }

                                    }
                                }
                                    Socket portsendsocket= new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), (Integer.parseInt(port))*2);
                                    DataOutputStream portsend= new DataOutputStream(portsendsocket.getOutputStream());
                                    portsend.writeUTF(p.port+","+portStr);
                                    Log.e(TAG,"first"+first);
                                    p.port=Integer.parseInt(port);

                                    DataInputStream ackinp = new DataInputStream(new BufferedInputStream(portsendsocket.getInputStream()));
                                    String ack= ackinp.readUTF();

                                    if (ack.compareTo("ACK")==0) {
                                        portsend.flush();
                                        portsend.close();
                                        portsendsocket.close();
                                    }

                            }
                        }


                        //Recv Successor and Predecessor Ports
                        if(clientMsg.matches("[0-9]{4},[0-9]{4}"))
                        {
                            DataOutputStream sendack = new DataOutputStream(socket.getOutputStream());
                            sendack.writeUTF("ACK");
                            socket.close();
                            p.port= Integer.parseInt(clientMsg.split(",")[0]);
                            s.port = Integer.parseInt(clientMsg.split(",")[1]);
                            try {
                                Socket predSock = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), (p.port * 2));
                                while(!predSock.isConnected()){

                                }
                                DataOutputStream sendsuccport = new DataOutputStream(predSock.getOutputStream());
                                sendsuccport.writeUTF(portStr);
                            }catch (Exception e1){
                                Log.e(TAG, e1.toString());
                            }
                        }


                        //Recv Predecessor from joined node
                        if(clientMsg.matches("[0-9]{4}")){
                            socket.close();
                            s.port=Integer.parseInt(clientMsg);
                        }

                        Log.e(TAG,"Pred and succe"+ p.port+" "+s.port);

                    }catch (Exception e){
                        Log.e(TAG,"Exception in server task"+e );
                        continue;
                    }
                }
            }
        }


        protected void onProgressUpdate(String...strings) {

            return;
        }
    }

    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

    private String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }
}
