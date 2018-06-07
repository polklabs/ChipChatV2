package com.polklabs.chipchat.backend;

//**
// * General Imports
// */
import java.net.*;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Iterator;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.commons.codec.binary.ApacheBase64;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Main class for chat room client. Sends user to chat room and sends messages.
 * @author Andrew Polk
 * @version 0.1
 */
public class ChatRoom extends AsyncTask<String, String, String> {

    //***************************************************************************
    // * Public variables
    // ***************************************************************************/

    /**Instance of the encryption class*/
    public dataEncrypt DE;
    /**List of the users the client knows are in the chat room*/
    public ArrayList<String> users = new ArrayList<>();
    private JSONArray userList;
    /**Connection has been closed, start a new one*/
    public boolean closed = false;
    public boolean kicked = false;
    public int errorType = 0;
    public boolean loadedRoom = false;
    public interface Listener{
        void publishText(String text);
        void returnText(String text);
        void setList(boolean popular, JSONArray list);
        void publishMessage(String sender, String body);
        void publishImage(String sender, String  data);
    }
    public          String username = "";                //Users username
    public          String password = "";
    public          String name = "";
    public          Socket sock;                    //Socket to server

    //***************************************************************************
    //* Private variables
    //***************************************************************************/

    private static final    String IP      = "pi1.polklabs.com";   //pi1.polklabs.com
    //private static final    String IP      = "169.231.10.204";
    private static final    int    PORT    = 3301;          //Server port
    private DataOutputStream    out;
    private DataInputStream     in;
    private Listener listener;
    private String[] paramArray;
    private JSONArray popularList;
    private JSONArray localList;
    private String[] errorMessage = {"", "Invalid Room Name", "Wrong Password", "Cannot Enter Local Chat Room", "Username Taken", "Username Invalid", "Room Is Full", "Could not connect to the server."};

    //***************************************************************************
    //* Constructor
    //***************************************************************************/

    public ChatRoom(final Listener listener, String... params){
        this.listener = listener;
        this.paramArray = params;
        if(paramArray.length > 5) {
            this.username = paramArray[5];
        }
        //Generate Private Public key pair
        DE = new dataEncrypt(512, this);
    }

    @Override
    protected String doInBackground(String... params){
        switch (paramArray[0]) {
            case "init":
                long startTime = System.currentTimeMillis();
                try {
                    init();
                    sock.close();
                } catch (Exception e) {
                    Log.e("ChatRoom", "init", e);
                }

                long duration = (System.currentTimeMillis() - startTime);
                if(duration < 1000) {
                    try {
                        Thread.sleep(1000 - duration);
                    } catch (Exception e) {
                        Log.d("ChatRoom", e.toString());
                    }
                }
                return errorMessage[errorType];

            case "join":
                this.password = paramArray[4];
                this.name = paramArray[3];
                boolean joined = false;
                try {
                    joined = join();
                } catch (Exception e) {
                    Log.e("ChipChat", "Exception", e);
                }
                if (joined) {
                    publishProgress("join", "DONE");
                } else {
                    try{
                        sock.close();
                    }catch (Exception e){
                        Log.d("ChipChat", "Couldn't close room");
                    }
                    return(errorMessage[errorType]);
                }
        }

        while(!loadedRoom){
            try {
                Thread.sleep(100);
            }catch(Exception e){
                Log.d("ChipChat","Sleep error");
            }
        }

        startListener();
        try {
            sock.close();
        }catch(Exception e){
            Log.d("ChipChat", "Couldn't close Socket");
        }
        return "";
    }

    @Override
    protected void onPostExecute(String result){
        listener.returnText(result);
    }

    @Override
    protected void onProgressUpdate(String... values){

        switch(values[0]) {
            case "init":
                listener.setList(true, popularList);
                listener.setList(false, localList);
                break;
            case "join":
                listener.publishText(values[1]);
                break;
            case "text":
                listener.publishMessage(values[1], values[2]);
                break;
            case "image":
                listener.publishImage(values[1], values[2]);
                break;
            case "users":
                listener.setList(false, userList);
                break;
        }
    }

    public void setListener(Listener listener){
        this.listener = listener;
    }

    //***************************************************************************
    //* Public methods
    //***************************************************************************/

    /**
     * @return true means user successfully joined room, false means an error occurred
     */
    private boolean join() throws JSONException, BadPaddingException, InvalidKeyException, IllegalBlockSizeException, IOException, InvalidKeySpecException, NoSuchAlgorithmException {
        if(isInvalid(paramArray[3])){
            errorType = 1;
            return false;
        }
        if(isInvalid(paramArray[5])){
            errorType = 5;
            return false;
        }

        String name = paramArray[3];
        if(paramArray[6].equals("true")){
            if(!paramArray[1].equals("") && !paramArray[2].equals("")){
                name = paramArray[1]+paramArray[2]+":"+name;
            }
        }

        JSONObject obj = new JSONObject();
        obj.put("type", paramArray[0]);
        obj.put("locationState", paramArray[1]);
        obj.put("locationCity", paramArray[2]);
        obj.put("name", name);
        obj.put("password", paramArray[4]);
        obj.put("username", paramArray[5]);
        obj.put("local", paramArray[6].equals("true"));
        obj.put("unlisted", paramArray[7].equals("true"));

        if(!connectToServer()) return false;

        out = new DataOutputStream(sock.getOutputStream());
        in = new DataInputStream(sock.getInputStream());

        //Get Public key from server, send Public to server
        out.writeUTF(DE.getPublicKey());
        String serverPublicKey = in.readUTF();
        Log.d("ChipChat", "Got and sent public key.");
        users.add("Server");
        DE.addPublicKey("Server", serverPublicKey);

        byte[] b = DE.encryptText(obj.toString(), "Server").getBytes();
        out.writeInt(b.length);
        out.write(b);
        out.flush();

        int length = in.readInt();
        b = new byte[length];
        in.readFully(b);

        obj = new JSONObject(DE.decryptText(new String(b, "UTF-8"), "Server"));
        if(obj.getBoolean("success")) {
            return true;
        }else{
            errorType = obj.getInt("error");
            return false;
        }
    }

    //***************************************************************************
    //* Private methods
    //***************************************************************************/

    /**
     * @throws IOException ioException
     * @throws NoSuchAlgorithmException no such algorithm
     * @throws InvalidKeySpecException invalid key spec
     * @throws java.security.InvalidKeyException invalid key
     * @throws javax.crypto.IllegalBlockSizeException illegal block size ?
     * @throws javax.crypto.BadPaddingException bad padding, data is the wrong number of bytes
     */
    private void init() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, JSONException {
        if(!connectToServer()) return;

        Log.d("ChipChat", "Connected to server.");

        out = new DataOutputStream(sock.getOutputStream());
        in = new DataInputStream(sock.getInputStream());

        //Get Public key from server, send Public to server
        out.writeUTF(DE.getPublicKey());
        String serverPublicKey = in.readUTF();
        Log.d("ChipChat", "Got and sent public key.");
        users.add("Server");
        DE.addPublicKey("Server", serverPublicKey);

        JSONObject obj = new JSONObject();
        obj.put("type", paramArray[0]);
        obj.put("locationState", paramArray[1]);
        obj.put("locationCity", paramArray[2]);

        byte[] b = DE.encryptText(obj.toString(), "Server").getBytes();
        out.writeInt(b.length);
        out.write(b);
        out.flush();


        int length = in.readInt();
        b = new byte[length];
        in.readFully(b);

        obj = new JSONObject(DE.decryptText(new String(b, "UTF-8"), "Server"));
        popularList = (JSONArray) obj.get("popularRooms");
        localList = (JSONArray) obj.get("localRooms");
        publishProgress("init");

        Log.d("ChipChat", "Got LISTS.");
    }

    /**
     * Attempts to connect to the server.
     */
    private boolean connectToServer(){
        //Connect to server
        try{
            sock = new Socket();
            sock.connect(new InetSocketAddress(IP, PORT), 750);
            return true;
        }catch(UnknownHostException e){
            //Should never happen
            Log.d("ChipChat","::Unknown server host.");
        }catch(IOException e){
            //Happens if server is offline or under high load
            Log.d("ChipChat", "::Could not connect to server.");
        }
        errorType = 7;
        return false;
    }

    /**
     * Starts a listener thread then waits for input from the user.
     */
    private void startListener() {
        JSONObject message;

        while(true){
            try{
                int lengthData = in.readInt();
                byte[] b = new byte[lengthData];
                in.readFully(b);

                message = new JSONObject(new String(b, "UTF-8"));

                switch (message.getString("type")){
                    case "message":
                        JSONObject inner = new JSONObject(DE.decryptOnce(message.getString("message"), null, true));
                        if(inner.getString("type").equals("text")){
                            publishProgress( "text", inner.getString("sender"), DE.decryptOnce(inner.getString("body"), inner.getString("sender"), false));
                        }else if(inner.getString("type").equals("image")){
                            publishProgress("image", inner.getString("sender"), DE.decryptOnce(inner.getString("body"), inner.getString("sender"), false));
                        }
                        break;
                    case "command":
                        switch (message.getString("data")){
                            case "kick":
                                return;
                            default:
                                Log.d("ChipChat", "Command: "+message.getString("data"));
                        }
                        break;
                    case "keys":
                        users.clear();
                        userList = message.getJSONArray("users");
                        for(int i = 0; i < userList.length(); i++){
                            users.add(userList.getString(i));
                        }
                        DE.userKeys.clear();
                        JSONObject tmp2 = message.getJSONObject("keys");
                        Iterator<String> iterator = tmp2.keys();
                        while(iterator.hasNext()){
                            String s = iterator.next();
                            String pubKey = tmp2.getString(s);
                            DE.userKeys.put(s, dataEncrypt.stringToPublicKey(pubKey));
                        }
                        publishProgress("users");
                        break;
                    default:
                        Log.d("ChatRoom", "Message: "+message.getString("type"));
                        break;
                }

            }catch(IOException e){
                publishProgress("Server", "::The connection has been closed.");
                break;
            }catch(JSONException | IllegalBlockSizeException | BadPaddingException | InvalidKeyException | NoSuchAlgorithmException | InvalidKeySpecException e){
                Log.d("ChipChat", "Could not receive a message.");
            }
        }
    }

    public DataOutputStream getOut(){
        return this.out;
    }

    //***************************************************************************
    //* Static methods
    //***************************************************************************/

    /**
     * Check if a string contains any of the illegal characters
     * @param string String to check if valid
     * @return true if the username/room name is valid
     */
    private static boolean isInvalid(String string){
        if(string.equals("")){
            return true;
        }

        String[] invalidChars = {";", "/", ":", "\n", "\t"};
        for(String s : invalidChars){
            if(string.contains(s)){
                return true;
            }
        }
        return false;
    }
}