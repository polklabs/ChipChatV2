package com.polklabs.roomy.backend;

//**
// * General Imports
// */
import java.net.*;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import android.os.AsyncTask;
import android.util.Log;

/**
 * Main class for chat room client. Sends user to chat room and sends messages.
 * @author Andrew Polk
 * @version 0.1
 */
public class ChatRoom extends AsyncTask<String, String, String> {

    public interface Listener{
        void setText(String text);
        void setText2(String text);
    }

    private Listener listener;
    private String[] paramArray;

    public boolean loadedRoom = false;

    private String options = "";

    @Override
    protected String doInBackground(String... params){
        switch (paramArray[0]) {
            case "init":
                String popular;
                try {
                    popular = init();
                    sock.close();
                } catch (Exception e) {
                    popular = "";
                }
                String[] split = popular.split("/");
                if(split.length < 2){
                    split = new String[2];
                    if(popular.length() > 1) {
                        split[0] = popular.substring(0, popular.length() - 1);
                    }else{
                        split[0] = "";
                    }
                    split[1]="";
                }

                try{
                    Thread.sleep(1000);
                }catch (Exception e){}

                publishProgress(split[0]);
                return split[1];

            case "join":
                boolean joined = false;

                if(paramArray.length > 6){
                    options = paramArray[6];
                }

                try {
                    init();
                    joined = join(paramArray[3], paramArray[4], paramArray[5]);
                } catch (Exception e) {
                    Log.d("Roomy", "Exception: " + e);
                }
                if (joined) {
                    publishProgress("DONE");
                    Log.d("Roomy","Room: " + paramArray[3] + ", Password: " + paramArray[4] + ", Username: " + paramArray[5]);
                } else {
                    try{
                        sock.close();
                    }catch (Exception e){
                        Log.d("Roomy", "Couldn't close room");
                    }
                    return(errorMessage);
                }
        }

        while(!loadedRoom){
            try {
                Thread.sleep(100);
            }catch(Exception e){
                Log.d("Roomy","Sleep error");
            }
        }

        startListener();
        try {
            sock.close();
        }catch(Exception e){
            Log.d("Roomy", "Couldn't close Socket");
        }
        return "";
    }

    @Override
    protected void onPostExecute(String result){
        listener.setText2(result);
    }

    @Override
    protected void onProgressUpdate(String... values){
        listener.setText(values[0]);
    }

    public void setListener(Listener listener){
        this.listener = listener;
    }

    //***************************************************************************
    // * Public variables
    // ***************************************************************************/

    /**Instance of the encryption class*/
    public dataEncrypt DE;
    /**List of the users the client knows are in the chat room*/
    public ArrayList<String> users = new ArrayList<>();
    /**Connection has been closed, start a new one*/
    public boolean closed = false;

    public boolean kicked = false;

    public String errorMessage = "";
    public int errorType = 0;

    //***************************************************************************
    //* Private variables
    //***************************************************************************/

    private static final    String IP      = "pi1.polklabs.com";   //pi1.polklabs.com
    //private static final    String IP      = "192.168.0.16";
    private static final    int    PORT    = 3301;          //Server port
    public          String username;                //Users username
    public          Socket sock;                    //Socket to server

    public int setupLevel = 0;//The current state of the connection, forces inorder setup

    //Streams
    private DataOutputStream    out;
    private DataInputStream     in;

    //***************************************************************************
    //* Constructor
    //***************************************************************************/

    public ChatRoom(final Listener listener, String... params){
        this.listener = listener;
        this.paramArray = params;
        //Generate Private Public key pair
        DE = new dataEncrypt(1024, this);
    }

    //***************************************************************************
    //* Public methods
    //***************************************************************************/

    /**
     *
     * @param roomString name of the room
     * @param passwordString room password
     * @param usernameString username
     * @return true means user successfully joined room, false means an error occurred
     */
    private boolean join(String roomString, String passwordString, String usernameString){
        //Setup
        username = usernameString;

        try{
            // ALL MESSAGE FROM HERE ON ARE ENCRYPTED --------------------------
            //------------------------------------------------------------------

            //Send room name****************************************************
            if(setupLevel != 1){
                errorMessage = "Current SetupLevel: "+setupLevel+", Level 1 required.";
                return false;
            }
            roomString = roomString.trim();
            if(!isValid(roomString)){
                errorMessage = "Invalid room name.";
                errorType = 1;
                return false;
            }
            if(options.charAt(1) == 'Y'){
                out.writeUTF(DE.encryptText(paramArray[1]+paramArray[2]+":"+roomString));
            }else {
                out.writeUTF(DE.encryptText(roomString));
            }

            //Create new room if needed
            boolean newRm = false;
            if(DE.decryptText(in.readUTF()).equals("NEW")){
                newRm = true;
                out.writeUTF(DE.encryptText("ACK"+options));
            }
            setupLevel++;
            //******************************************************************

            //Send password if needed*******************************************
            if(setupLevel != 2){
                errorMessage = "Current SetupLevel: "+setupLevel+", Level 2 required.";
                return false;
            }
            String passMessage;
            if(newRm || (passMessage=DE.decryptText(in.readUTF())).equals("PASSWORD")){
                //Send password
                if(passwordString.equals("")){
                    out.writeUTF(DE.encryptText(""));
                }else{
                    out.writeUTF(DE.encryptText(new String(dataEncrypt.getEncryptedPassword(passwordString, roomString))));
                }

                if(!newRm){
                    if(DE.decryptText(in.readUTF()).equals("NACK")){
                        errorMessage = "Wrong password.";
                        errorType = 2;
                        return false;
                    }
                }
            }else if(passMessage.equals("NO")){
                sock.close();
                errorMessage = "Trying to enter local chat room.";
                errorType = 3;
                return false;
            }
            setupLevel++;
            //******************************************************************

            //Setup username****************************************************
            if(setupLevel != 3){
                errorMessage = "Current SetupLevel: "+setupLevel+", Level 3 required.";
                return false;
            }
            Set<String> usernames = new HashSet<>(Arrays.asList(DE.decryptText(in.readUTF()).split(";")));

            //Send username
            username = username.trim();
            if(isValid(username)){
                if(usernames.contains(username)){
                    errorMessage = "Username taken.";
                    errorType = 4;
                    return false;
                }
            }else{
                errorMessage = "Username invalid.";
                errorType = 5;
                return false;
            }
            out.writeUTF(DE.encryptText(username));
            setupLevel++;
            //******************************************************************

            //Update public keys************************************************
            if(setupLevel != 4){
                errorMessage = "Current SetupLevel: "+setupLevel+", Level 4 required.";
                return false;
            }
            updateKeys(DE.getPublicKey());
            setupLevel++;
            //******************************************************************

        } catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException | IOException | NoSuchAlgorithmException | InvalidKeySpecException ex) {
            errorMessage = ("Error: "+ex);
            return false;
        }

        return true;
    }

    //public void reportUser(String user){
       // message("/kick "+user);
    //}

    //***************************************************************************
    //* Private methods
    //***************************************************************************/

    /**
     * Requires setupLevel 0
     * @return The popular chat rooms, list in string form
     * @throws IOException ioException
     * @throws NoSuchAlgorithmException no such algorithm
     * @throws InvalidKeySpecException invalid key spec
     * @throws java.security.InvalidKeyException invalid key
     * @throws javax.crypto.IllegalBlockSizeException illegal block size ?
     * @throws javax.crypto.BadPaddingException bad padding, data is the wrong number of bytes
     */
    private String init() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException{
        errorMessage = "";
        errorType = 0;
        setupLevel = 0;

        kicked = false;
        closed = false;

        connectToServer();

        Log.d("ChatRoom", "Connected to server.");

        out = new DataOutputStream(sock.getOutputStream());
        in = new DataInputStream(sock.getInputStream());

        //Get Public key from server, send Public to server
        String publicKey = DE.getPublicKey();
        String serverPublicKey = in.readUTF();
        out.writeUTF(publicKey);

        Log.d("ChatRoom", "Got and sent public key.");

        users.add("Server");
        DE.addPublicKey("Server", serverPublicKey);

        out.writeUTF(DE.encryptText(paramArray[1]+":"+paramArray[2]));

        String popularString = DE.decryptText(in.readUTF());

        Log.d("ChatRoom", "Got popular.");

        setupLevel++;

        return popularString;
    }

    private void updateKeys(String publicKey)
            throws IOException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, InvalidKeySpecException{
        //get usernames and public keys from the server
        String usersFromServer = DE.decryptText(in.readUTF());
        users.clear();
        users.addAll(Arrays.asList(usersFromServer.split(";")));

        String publicKeys = DE.decryptText(in.readUTF());
        DE.userKeys.clear();
        for(int i = 0; i < users.size(); i++){
            DE.addPublicKey(users.get(i), publicKeys.substring(i*publicKey.length(), (i+1)*publicKey.length()));
        }
    }

    /**
     * Attempts to connect to the server.
     */
    private void connectToServer(){
        //Connect to server
        try{
            sock = new Socket(IP, PORT);
            return;
        }catch(UnknownHostException e){
            //Should never happen
            Log.d("ChatRoom","::Unknown server host.");
        }catch(IOException e){
            //Happens if server is offline or under high load
            Log.d("ChatRoom", "::Could not connect to server.");
            Log.d("ChatRoom", "::Hit enter to retry connection.");
        }
        errorMessage = "Server offline.";
        errorType = 6;
    }

    /**
     * Starts a listener thread then waits for input from the user.
     */
    private void startListener() {
        if(setupLevel != 5){
            errorMessage = "Current SetupLevel: "+setupLevel+", Level 3 required.";
            return;
        }

        String message;
        boolean waitingForKeys = false;

        while(true){
            try{
                message = DE.decryptText(in.readUTF());
                Log.d("Roomy", message);
                //Server updating keys
                String[] split = message.split(":", 2);
                if(split[0].equals("Server") && split.length > 1){
                    if(waitingForKeys){
                        if(!split[0].equals("*")){
                            DE.userKeys.clear();
                            int length = DE.getPublicKey().length();
                            String publicKeys = split[1];
                            for(int i = 0; i < users.size(); i++){
                                DE.addPublicKey(users.get(i), publicKeys.substring(i*length, (i+1)*length));
                            }
                        }
                        waitingForKeys = false;
                    }
                    else if(split[1].charAt(0) == '/'){
                        char temp = split[1].charAt(1);
                        split[1] = split[1].substring(2);
                        switch(temp){
                            case '*':
                                users.clear();
                                users.addAll(Arrays.asList(split[1].split(";")));
                                waitingForKeys = true;
                                break;
                            case '-':
                                kicked = true;
                                return;
                        }
                    }else {
                        //onProgressUpdate
                        publishProgress("\r"+message);
                        //System.out.println("\r"+message);
                    }
                }else if(split[1].length() >= 8 && split[1].substring(1, 8).equals("*image/")) {
                    publishProgress("\r"+split[0]+":Image from "+split[0]);
                }else{
                    publishProgress("\r"+message);
                }
            }catch(IOException e){
                publishProgress("::The connection has been closed.");
                //System.out.println("::The connection has been closed.");
                break;
            }catch(NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e){
                publishProgress("::Could not receive massage.");
                //System.out.println("::Could not receive message.");
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
    private static boolean isValid(String string){
        if(string.equals("")){
            return false;
        }

        String[] invalidChars = {";"," ", "/", ":", "\n", "\t"};
        for(String s : invalidChars){
            if(string.contains(s)){
                //System.out.println("::Can not contain \';\', \' \', or \'/\'");
                return false;
            }
        }
        return true;
    }
}