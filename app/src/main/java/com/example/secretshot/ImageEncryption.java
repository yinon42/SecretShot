package com.example.secretshot;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.util.Arrays;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;
import java.util.Base64;

public class ImageEncryption {

    private static final String TAG = "ImageEncryption"; // Tag for logs

    /**
     * Encrypts the given text with the provided password, then embeds it into the Bitmap image.
     * @param image    Original Bitmap image
     * @param text     Text to encrypt
     * @param password Password for AES encryption
     * @return A new Bitmap with the encrypted data embedded, or null if an error occurred
     */


    public static Bitmap encryptText(Bitmap image, String text, String password) {
        Log.d(TAG, "encryptText: Starting encryption process");
        try {
            // Step 1: Encrypt text using AES
            Log.d(TAG, "encryptText: Encrypting text with AES");
            String encryptedText = encryptAES(text, password);

            // Step 2: Convert the encrypted text to binary
            Log.d(TAG, "encryptText: Converting encrypted text to binary");
            String binaryText = stringToBinary(encryptedText);

            // Step 3: Validate space in image
            int imageCapacity = image.getWidth() * image.getHeight() * 3;
            int textLength = binaryText.length();
            Log.d(TAG, "encryptText: Image capacity (bits) = " + imageCapacity + ", Required = " + textLength);

            if (textLength > imageCapacity - 32) {
                Log.e(TAG, "encryptText: Not enough space in image to store text");
                throw new IllegalArgumentException("Text is too long to be encrypted within the image.");
            }

            // Step 4: Create a mutable copy of the image
            Bitmap encryptedImage = image.copy(Bitmap.Config.ARGB_8888, true);

            // Step 5: Store the text length (32 bits)
            String binaryLength = String.format("%32s", Integer.toBinaryString(textLength)).replace(' ', '0');
            int lengthIndex = 0;
            for (int i = 0; i < 32; i++) {
                int pixel = encryptedImage.getPixel(i, 0);
                int red = Color.red(pixel);
                red = modifyLSB(red, binaryLength.charAt(lengthIndex));
                pixel = Color.rgb(red, Color.green(pixel), Color.blue(pixel));
                encryptedImage.setPixel(i, 0, pixel);
                lengthIndex++;
            }
            Log.d(TAG, "encryptText: Embedded text length into the first row of pixels");

            // Step 6: Embed the encrypted text in the rest of the image
            int textIndex = 0;
            for (int y = 1; y < encryptedImage.getHeight(); y++) {
                for (int x = 0; x < encryptedImage.getWidth(); x++) {
                    if (textIndex < textLength) {
                        int pixel = encryptedImage.getPixel(x, y);
                        int red = modifyLSB(Color.red(pixel), binaryText.charAt(textIndex));
                        pixel = Color.rgb(red, Color.green(pixel), Color.blue(pixel));
                        encryptedImage.setPixel(x, y, pixel);
                        textIndex++;
                    }
                }
            }
            Log.d(TAG, "encryptText: Successfully embedded encrypted text in image");
            return encryptedImage;
        } catch (Exception e) {
            Log.e(TAG, "encryptText: Encryption or embedding failed", e);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Decrypts the embedded text from the given image using the provided password.
     * @param image    Bitmap image containing the encrypted data
     * @param password Password for AES decryption
     * @return The decrypted text, or an error message if it fails
     */
    public static String decryptText(Bitmap image, String password) {
        Log.d(TAG, "decryptText: Starting decryption process");
        try {
            // Step 1: Read the text length (32 bits)
            StringBuilder binaryLength = new StringBuilder();
            for (int i = 0; i < 32; i++) {
                int pixel = image.getPixel(i, 0);
                binaryLength.append(getLSB(Color.red(pixel)));
            }

            int textLength = Integer.parseInt(binaryLength.toString(), 2);
            Log.d(TAG, "decryptText: Retrieved text length = " + textLength);

            // Step 2: Extract the binary text from the image
            StringBuilder binaryText = new StringBuilder();
            int textIndex = 0;
            for (int y = 1; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    if (textIndex < textLength) {
                        int pixel = image.getPixel(x, y);
                        binaryText.append(getLSB(Color.red(pixel)));
                        textIndex++;
                    }
                }
            }
            Log.d(TAG, "decryptText: Extracted binary text of length " + textIndex);


            // Step 3: Convert the binary data back into the encrypted text
            String encryptedText = binaryToString(binaryText.toString());
            Log.d(TAG, "decryptText: Converted binary to encrypted string");

            // Step 4: Decrypt with AES
            Log.d(TAG, "decryptText: Decrypting with AES");
            return decryptAES(encryptedText, password);

        } catch (Exception e) {
            Log.e(TAG, "decryptText: Failed to decrypt - possibly incorrect password or data", e);
            return "Incorrect password or no encrypted data found!";
        }
    }

    // Encrypt text with AES
    private static String encryptAES(String text, String password) throws Exception {
        Log.d(TAG, "encryptAES: Generating key and encrypting text");
        SecretKeySpec key = generateKey(password);
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encrypted = cipher.doFinal(text.getBytes());
        return Base64.getEncoder().encodeToString(encrypted);
    }

    // Decrypt text with AES
    private static String decryptAES(String encryptedText, String password) throws Exception {
        Log.d(TAG, "decryptAES: Generating key and decrypting text");
        SecretKeySpec key = generateKey(password);
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
        return new String(decrypted);
    }

    // Generate a secret key from the given password
    private static SecretKeySpec generateKey(String password) throws NoSuchAlgorithmException {
        Log.d(TAG, "generateKey: Creating key using SHA-256");
        byte[] key = password.getBytes();
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        key = sha.digest(key);
        // Use the first 16 bytes (128 bits) for AES
        return new SecretKeySpec(Arrays.copyOf(key, 16), "AES");
    }

    // Convert a string to binary
    private static String stringToBinary(String text) {
        StringBuilder binaryText = new StringBuilder();
        for (char c : text.toCharArray()) {
            String binaryChar = Integer.toBinaryString(c);
            binaryText.append(String.format("%8s", binaryChar).replace(' ', '0'));
        }
        return binaryText.toString();
    }

    // Convert binary to a string
    private static String binaryToString(String binaryText) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < binaryText.length(); i += 8) {
            String binaryChar = binaryText.substring(i, Math.min(i + 8, binaryText.length()));
            text.append((char) Integer.parseInt(binaryChar, 2));
        }
        return text.toString();
    }

    // Modify the least significant bit (LSB) of the given value
    private static int modifyLSB(int value, char bit) {
        // Clear the LSB
        value = value & 0xFE;
        // Set LSB based on the bit char
        return value | (bit - '0');
    }

    // Get the LSB from the given value
    private static int getLSB(int value) {
        return value & 0x01;
    }
}
