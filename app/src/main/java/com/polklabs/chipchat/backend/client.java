package com.polklabs.chipchat.backend;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import org.apache.commons.codec.binary.ApacheBase64;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class client extends AsyncTask<String, String, String> {
    public ArrayList<String> commands = new ArrayList<>();
    public ArrayList<String> messages = new ArrayList<>();
    public ArrayList<Bitmap> images = new ArrayList<>();

    public boolean stop;

    private DataOutputStream out;
    private ChatRoom chatRoom;

    public client(ChatRoom chatRoom){
        this.chatRoom = chatRoom;
    }

    @Override
    protected String doInBackground(String... params){
        try {
            // Wait so client can connect to the server.
            Thread.sleep(2000);
            this.out = chatRoom.getOut();
        }catch (Exception e){
            Log.e("ChatRoom", "Could not start messenger.", e);
        }

        while(true){
            if(stop){
                Command("close");
                stop = false;
                return "";
            }

            while(messages.size() > 0){
                Message(messages.get(0));
                messages.remove(0);
            }
            while(images.size() > 0){
                Bitmap bmp = images.get(0);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] b = stream.toByteArray();
                Image(Base64.encodeToString(b, Base64.DEFAULT));
                images.remove(0);
            }
            while(commands.size() > 0){
                Command(commands.get(0));
                commands.remove(0);
            }

            try {
                Thread.sleep(500);
            }catch (Exception e){}
        }
    }

    private void Message(String text){
        try {
            while (!chatRoom.closed) {
                try {
                    if (chatRoom.kicked)
                        break;

                    if (!text.equals("")) {
                        JSONObject obj = new JSONObject();
                        obj.put("type", "text");
                        obj.put("sender", chatRoom.username);
                        obj.put("body", chatRoom.DE.encryptOnce(text, null, true));

                        JSONObject messageFinal = new JSONObject();
                        messageFinal.put("type", "message");

                        JSONObject data = new JSONObject();
                        for(String s : chatRoom.users){
                            if(!s.equals("Server") && !s.equals(chatRoom.username)) {
                                data.put(s, chatRoom.DE.encryptOnce(obj.toString(), s, false));
                            }
                        }
                        messageFinal.put("messages", data);


                        //Send message if message is not empty
                        byte[] s = messageFinal.toString().getBytes();
                        out.writeInt(s.length);
                        out.write(s);
                        out.flush();
                    }
                } catch (IOException e) {
                    Log.d("ChatRoom","::Could not send message. " + e);
                    break;
                }
                return;
            }
            chatRoom.sock.close();
        }catch(Exception e){
            Log.e("ChatRoom", "Some exception", e);
        }
    }

    private void Image(String data){
        try {
            while (!chatRoom.closed) {
                try {
                    if (chatRoom.kicked)
                        break;

                    if (data.length() > 0) {
                        JSONObject messageFinal = new JSONObject();
                        messageFinal.put("type", "data");
                        messageFinal.put("messages", chatRoom.DE.encryptOnce(data, null, true));

                        //Send message if message is not empty
                        byte[] s = messageFinal.toString().getBytes();
                        out.writeInt(s.length);
                        out.write(s);
                        out.flush();
                    }
                } catch (IOException e) {
                    Log.d("ChatRoom","::Could not send message. " + e);
                    break;
                }
                return;
            }
            chatRoom.sock.close();
        }catch(Exception e){
            Log.e("ChatRoom", "Some exception", e);
        }
    }

    private void Command(String text){
        try {
            while (!chatRoom.closed) {
                try {
                    if (chatRoom.kicked)
                        break;

                    if (!text.equals("")) {
                        JSONObject commandMessage = new JSONObject();
                        commandMessage.put("type", "command");
                        commandMessage.put("data", text);

                        //Send message if message is not empty
                        byte[] s = commandMessage.toString().getBytes();
                        out.writeInt(s.length);
                        out.write(s);
                        out.flush();
                    }
                } catch (IOException e) {
                    Log.d("ChatRoom","::Could not send message. " + e);
                    break;
                }
                return;
            }
            chatRoom.sock.close();
        }catch(Exception e){
            Log.e("ChatRoom", "Some exception", e);
        }
    }
}
