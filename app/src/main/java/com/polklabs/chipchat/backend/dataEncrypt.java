package com.polklabs.chipchat.backend;

//**
// * For public private key gen and encryption
// */
import android.util.Log;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.security.KeyFactory;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import org.apache.commons.codec.binary.ApacheBase64;
import java.security.spec.InvalidKeySpecException;

//**
// * For password authentication
// */
import java.security.spec.KeySpec;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

//**
// * General Imports
// */
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.io.UnsupportedEncodingException;

/**
 * Can encrypt text, decrypt text, generate key pairs, hash passwords
 * @author Andrew Polk
 * @version 0.1
 */
public final class dataEncrypt {

    //***************************************************************************
    // * Public variables
    // ***************************************************************************/

    /**Public key for this client*/
    private PublicKey                publicKey;
    /**Dictionary of the public keys of the other users*/
    public Map<String, PublicKey>   userKeys = new HashMap<>();

    private static final String ALGORITHM = "RSA";
    private static final String CIPHER_ALGORITHM = "RSA/ECB/PKCS1Padding";

    //***************************************************************************
    // * Private variables
    // ***************************************************************************/

    private KeyPairGenerator    keyGen;
    private PrivateKey          privateKey; //Users private key, NEVER SHARE
    private Cipher              cipher;
    private ChatRoom            parent;

    //***************************************************************************
    // * Constructor
    // ***************************************************************************/

    /**Constructor
     * Initializes the encryption and creates keys
     * @param keyLength How long the encryption is, STD=1024
     * @param parent the instance that called this class
     */
    public dataEncrypt(int keyLength, ChatRoom parent) {
        this.parent = parent;
        this.keyGen = null;
        this.cipher = null;
        try{
            //Key gen
            this.keyGen = KeyPairGenerator.getInstance(ALGORITHM);
            this.keyGen.initialize(keyLength);

            //Encryption/Decryption
            this.cipher = Cipher.getInstance(CIPHER_ALGORITHM);

            //Generate keys
            createKeys();
        }catch(NoSuchAlgorithmException | NoSuchPaddingException e){
            System.err.println(e.getMessage());
        }
    }

    //***************************************************************************
    // * Public methods
    // ***************************************************************************/

    /**Converts the current users public key to a string
     * @return the string representation of the users public key
     * @throws NoSuchAlgorithmException no such algorithm
     * @throws InvalidKeySpecException invalid key spec
     */
    public String getPublicKey()
            throws NoSuchAlgorithmException, InvalidKeySpecException {

        return PublicKeyToString(this.publicKey);
    }

    /**Puts a public key into the user Dictionary based on a username string
     * @param username name of the user to be inserted, NOT SELF
     * @param key The string representation of another users public key
     * @throws NoSuchAlgorithmException no such algorithm
     * @throws InvalidKeySpecException .
     */
    public void addPublicKey(String username, String key)
            throws NoSuchAlgorithmException, InvalidKeySpecException{

        userKeys.put(username, stringToPublicKey(key));
    }

    /**Removes a user from the public key dictionary
     * May not be called
     * @param username User to remove
     */
    public void removePublicKey(String username){
        userKeys.remove(username);
    }

    /**
     * @param msg string message
     * @param key public key to use
     * @param usePrivateKey is it for the server
     * @return .
     * @throws java.security.InvalidKeyException .
     * @throws javax.crypto.IllegalBlockSizeException .
     * @throws javax.crypto.BadPaddingException .
     */
    public String encryptOnce(String msg, String key, boolean usePrivateKey)
            throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

        return encrypt(msg.getBytes(), userKeys.get(key), usePrivateKey);
    }

    public String encryptOnce(byte[] msg, String key, boolean usePrivateKey)
            throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

        return encrypt(msg, userKeys.get(key), usePrivateKey);
    }

    /**Doubly encrypts a string of text to be sent to the server
     * @param msg the string to be encrypted
     * @return the encrypted string
     * @throws InvalidKeyException .
     * @throws IllegalBlockSizeException .
     * @throws BadPaddingException .
     */
    public String encryptText(String msg, String username)
            throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

        return encrypt(encrypt(msg.getBytes(), null, true).getBytes(), userKeys.get(username),false);
    }

    public String decryptOnce(String msg, String key, boolean usePrivateKey)
            throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException {
        return new String(decrypt(msg, userKeys.get(key), usePrivateKey), "UTF-8");
    }

    public byte[] decryptOnceByte(String msg, String key, boolean usePrivateKey)
            throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

        return decrypt(msg, userKeys.get(key), usePrivateKey);
    }

    /**Decrypts a string twice, Once with the users private key. Then the first
     * char in the partially decrypted string is the int value of the public key
     * that should be used to decrypt the message fully.
     * This will return an error if the correct public key does not exist in the
     * public key dictionary.
     * @param msg the doubly encrypted string that the user received.
     * @param key the public key to use
     * @return the decrypted data in string format.
     * @throws InvalidKeyException .
     * @throws IllegalBlockSizeException .
     * @throws BadPaddingException .
     * @throws UnsupportedEncodingException .
     */
    public String decryptText(String msg, String key)
            throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException {

        String one = new String(decrypt(msg, null, true), "UTF-8");
        return new String(decrypt(one, userKeys.get(key),false), "UTF-8");
    }

    //***************************************************************************
    // * Private methods
    // ***************************************************************************/

    //Creates a private public key pair
    private void createKeys() {
        KeyPair pair = this.keyGen.generateKeyPair();
        this.privateKey = pair.getPrivate();
        this.publicKey = pair.getPublic();
    }

    //Basic encrypt function that can take in an arbritrary length
    private String encrypt(byte[] bytes, PublicKey key, boolean usePrivateKey) throws
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

        if(usePrivateKey){
            this.cipher.init(Cipher.ENCRYPT_MODE, this.privateKey);
        }else{
            this.cipher.init(Cipher.ENCRYPT_MODE, key);
        }

        String result = "";

        int SIZE = 53;
        int start = 0, index = SIZE;
        while(true){
            //See if you only need to encrypt a smaller portion
            if(bytes.length-start < SIZE)
                index = bytes.length;

            //Get sub array of total message
            byte[] msg1 = Arrays.copyOfRange(bytes, start, index);

            //Encrypt the bytes
            result += (ApacheBase64.encodeBase64String(cipher.doFinal(msg1)));

            //increment and break when done
            start += SIZE;
            index += SIZE;
            if(start > bytes.length-1)
                break;
        }

        return result;
    }

    //Basic decrypt function that can take in an arbitrary length and return bytes
    private byte[] decrypt(String string, PublicKey key, boolean usePrivateKey)
            throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

        if(usePrivateKey){
            this.cipher.init(Cipher.DECRYPT_MODE, this.privateKey);
        }else{
            this.cipher.init(Cipher.DECRYPT_MODE, key);
        }

        byte[] result = new byte[0];

        int SIZE = 88;
        int start = 0, index = SIZE;
        while(true){
            if(string.length()-start <= SIZE)
                index = string.length();

            String msg1 = string.substring(start, index);

            //Encrypt the bytes
            byte[] temp2 = cipher.doFinal(ApacheBase64.decodeBase64(msg1));
            byte[] newResult = new byte[result.length + temp2.length];

            //Join result with new data together
            System.arraycopy(result, 0, newResult, 0, result.length);
            System.arraycopy(temp2, 0, newResult, result.length, temp2.length);

            result = newResult;

            start += SIZE;
            index += SIZE;
            if(start > string.length()-1)
                break;
        }
        return result;
    }

    //***************************************************************************
    // * Static methods
    // ***************************************************************************/

    /**Converts a public key to a string, used when sending public key to server.
     * @param pubKey the key to be converted
     * @return the public key as a string
     * @throws NoSuchAlgorithmException .
     * @throws InvalidKeySpecException .
     */
    public static String PublicKeyToString(PublicKey pubKey)
            throws NoSuchAlgorithmException, InvalidKeySpecException {

        KeyFactory kf = KeyFactory.getInstance(ALGORITHM);
        X509EncodedKeySpec spec = kf.getKeySpec(pubKey, X509EncodedKeySpec.class);
        return ApacheBase64.encodeBase64String(spec.getEncoded());
    }

    /**Convert a string to a public key, used when getting a public key from the
     * server
     * @param key string from the server
     * @return public key after conversion
     * @throws NoSuchAlgorithmException if missing library
     * @throws InvalidKeySpecException  if key was not created using the same type, version error
     */
    public static PublicKey stringToPublicKey(String key)
            throws NoSuchAlgorithmException, InvalidKeySpecException {

        byte[] data = ApacheBase64.decodeBase64(key);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(data);
        KeyFactory kf = KeyFactory.getInstance(ALGORITHM);
        return kf.generatePublic(spec);
    }

    /**Hashes a password from string, not sure if really useful...
     * @param password raw password
     * @param salt string to add to password, room name
     * @return byte array of the hashed password
     * @throws NoSuchAlgorithmException if missing a library
     * @throws InvalidKeySpecException shouldn't be a problem
     */
    public static byte[] getEncryptedPassword(String password, String salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {

        //Static parameters for the hash generation
        String algorithm = "PBKDF2WithHmacSHA1";
        int derivedKeyLength = 256;
        int iterations = 20000;

        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), iterations, derivedKeyLength);

        SecretKeyFactory f = SecretKeyFactory.getInstance(algorithm);

        return f.generateSecret(spec).getEncoded();
    }
}