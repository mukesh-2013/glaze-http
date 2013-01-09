package glaze.examples.mashape;

import java.math.BigInteger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class AuthUtil
{

   private static final String base64code = "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "abcdefghijklmnopqrstuvwxyz" + "0123456789"
         + "+/";

   private static final int splitLinesAt = 76;

   public static String getAuthToken(String publicKey, String privateKey)
   {
      String hash = getHMAC_SHA1(publicKey, privateKey);
      String headerValue = publicKey + ":" + hash;
      return encode(new String(headerValue.getBytes())).replace("\r\n", "");
   }

   private static String encode(String string)
   {

      String encoded = "";
      byte[] stringArray;
      try {
         stringArray = string.getBytes("UTF-8"); // use appropriate encoding
         // string!
      } catch (Exception ignored) {
         stringArray = string.getBytes(); // use locale default rather than
         // croak
      }
      // determine how many padding bytes to add to the output
      int paddingCount = (3 - (stringArray.length % 3)) % 3;
      // add any necessary padding to the input
      stringArray = zeroPad(stringArray.length + paddingCount, stringArray);
      // process 3 bytes at a time, churning out 4 output bytes
      // worry about CRLF insertions later
      for (int i = 0; i < stringArray.length; i += 3) {
         int j = ((stringArray[i] & 0xff) << 16) + ((stringArray[i + 1] & 0xff) << 8) + (stringArray[i + 2] & 0xff);
         encoded = encoded + base64code.charAt((j >> 18) & 0x3f) + base64code.charAt((j >> 12) & 0x3f)
               + base64code.charAt((j >> 6) & 0x3f) + base64code.charAt(j & 0x3f);
      }
      // replace encoded padding nulls with "="
      return splitLines(encoded.substring(0, encoded.length() - paddingCount) + "==".substring(0, paddingCount));

   }

   private static String getHMAC_SHA1(String value, String key)
   {
      if (value == null || key == null || value.trim() == "" || key.trim() == "") {
         throw new RuntimeException("Please enter your Mashape keys in the configuration file.");
      }
      try {
         // Get an hmac_sha1 key from the raw key bytes
         byte[] keyBytes = key.getBytes();
         SecretKeySpec signingKey = new SecretKeySpec(keyBytes, "HmacSHA1");

         // Get an hmac_sha1 Mac instance and initialize with the signing key
         Mac mac = Mac.getInstance("HmacSHA1");
         mac.init(signingKey);

         // Compute the hmac on input data bytes
         byte[] rawHmac = mac.doFinal(value.getBytes());

         String hmac = "";
         BigInteger hash = new BigInteger(1, rawHmac);
         hmac = hash.toString(16);
         if (hmac.length() % 2 != 0) {
            hmac = "0" + hmac;
         }
         return hmac;
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   private static String splitLines(String string)
   {

      String lines = "";
      for (int i = 0; i < string.length(); i += splitLinesAt) {

         lines += string.substring(i, Math.min(string.length(), i + splitLinesAt));
         lines += "\r\n";

      }
      return lines;

   }

   private static byte[] zeroPad(int length, byte[] bytes)
   {
      byte[] padded = new byte[length]; // initialized to zero by JVM
      System.arraycopy(bytes, 0, padded, 0, bytes.length);
      return padded;
   }

}
