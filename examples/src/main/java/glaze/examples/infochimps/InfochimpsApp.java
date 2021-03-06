package glaze.examples.infochimps;

import static glaze.examples.infochimps.InfochimpsApi.*;
import static glaze.Glaze.Get;

import glaze.examples.infochimps.model.InfluenceMetrics;
import glaze.examples.infochimps.model.UfoSights;

import java.net.URI;
import java.util.Collections;
import java.util.Map;

import com.google.common.base.Optional;


/**
 * Infochimps Social API example.
 * 
 */
public class InfochimpsApp
{
   private static final String API_KEY = "api_test-W1cipwpcdu9Cbd9pmm8D4Cjc469";

   private static final Map<String, Object> EMPTY_MAP = Collections.emptyMap();

   public static void main(String... args)
   {
      String screenName = "ladygaga"; // "infochimps";
      String query = "location:spain"; // "description:ufo"

      InfochimpsApp app = new InfochimpsApp();

      app.tsrank(screenName);

      app.influence(screenName);

      app.ufoSight(query);
   }

   // Here for example purposes.
   // Logically it must be encapsulated in the Api class.

   private void influence(String screenName)
   {
      URI uri = uriInfluenceMetrics(API_KEY, screenName);
      InfluenceMetrics metrics = Get(uri).withErrorHandler(infochimpsErrors()).map(InfluenceMetrics.class);

      System.out.format("%s\n", metrics);
   }

   private void tsrank(String screenName)
   {
      URI uri = uriTsrank(API_KEY, screenName);
      Map<String, Object> result = Get(uri).withErrorHandler(infochimpsErrors()).map();

      System.out.format("Rank: %s\n", Optional.fromNullable(result).or(EMPTY_MAP).get("trstrank"));
   }

   private void ufoSight(String query)
   {
      URI uri = uriUfoSight(API_KEY, query, 0, 2);
      UfoSights sights = Get(uri).withErrorHandler(infochimpsErrors()).map(UfoSights.class);

      System.out.format("%s\n", sights);
   }
}
