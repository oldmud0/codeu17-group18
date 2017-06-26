package codeu.chat.server;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
/**
 * Defines the name of a property as to be serialized/deserialized in the JSON file.
 */
public @interface JsonProperty {
  
  /**
   * Returns the name of the property.
   */
  String value();

}
