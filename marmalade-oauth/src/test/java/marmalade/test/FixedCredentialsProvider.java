package marmalade.test;

import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import marmalade.oauth.OAuthCredentials;
import marmalade.oauth.spi.DefaultCredentialsProvider;

public class FixedCredentialsProvider extends DefaultCredentialsProvider
{

   @Override
   protected OAuthCredentials getOAuthCredentials()
   {
      CommonsHttpOAuthConsumer consumer = new CommonsHttpOAuthConsumer("keyone", "secretone");
      consumer.setTokenWithSecret("abcdefg", "secret");

      return new OAuthCredentials(consumer);
   }

}
