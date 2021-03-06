package glaze.mime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface TextMultipart
{

   String charset() default "UTF-8";

   String mime() default "text/plain";

   String name() default "";

}
