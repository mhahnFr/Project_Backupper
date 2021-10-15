package hahn.rmi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Diese Annotation erlaubt die Markierung einer Methode, die über die API-Schnittstelle 
 * aufrufbar sein soll. Eine solche Methode muss public sein.
 * 
 * @author Manuel Hahn
 * @since 22.05.2018
 */
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.TYPE})
@Inherited
@Retention(value = RetentionPolicy.RUNTIME)
public @interface RemoteExecution {

}