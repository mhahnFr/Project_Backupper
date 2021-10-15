package hahn.rmi;

import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

/**
 * Überprüft mit der Annotation {@link RemoteExecution} annotierte Elemente auf seine Anforderungen.
 * 
 * @author Manuel Hahn
 * @since 08.04.2019
 */
@SupportedAnnotationTypes(value = { "hahn.rmi.RemoteExecution" })
public class RemoteExecutionVerifier extends AbstractProcessor {
	private Types typeUtils;
	private TypeMirror remoteExecution;
	
	@Override
	public synchronized void init(ProcessingEnvironment processingEnv) {
		super.init(processingEnv);
		typeUtils = processingEnv.getTypeUtils();
		remoteExecution = processingEnv.getElementUtils().getTypeElement(RemoteExecution.class.getName()).asType();
	}
	
	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		roundEnv.getElementsAnnotatedWith(RemoteExecution.class).forEach(this::verifyIsPublic);
		return false;
	}
	
	/**
	 * Sichert zu, dass alle annotierten Elemente öffentlich sind.
	 * 
	 * @param element das zu überprüfende Element
	 */
	private void verifyIsPublic(Element element) {
		if(!isPublic(element.getModifiers())) {
			compilerErrorMessage(element);
		}
	}
	
	/**
	 * Überprüft, ob der Modifier {@link Modifier#PUBLIC} vorhanden ist.
	 * 
	 * @param modifiers die zu überprüfenden Modifier
	 * @return ob der {@link Modifier#PUBLIC} gefunden wurde
	 */
	private boolean isPublic(Set<Modifier> modifiers) {
		boolean isPublic = false;
		for(Modifier m : modifiers) {
			isPublic = m == Modifier.PUBLIC;
			if(isPublic) {
				break;
			}
		}
		return isPublic;
	}
	
	/**
	 * Gibt die Fehlermeldung für das angegebene Element aus.
	 * 
	 * @param element das zu bemängelnde Element
	 */
	private void compilerErrorMessage(Element element) {
		AnnotationMirror annotation = element.getAnnotationMirrors().stream()
											 .filter(m -> typeUtils.isSameType(m.getAnnotationType(), remoteExecution))
											 .findFirst()
											 .orElseThrow(() -> new RuntimeException("internal compiler error"));
		processingEnv.getMessager().printMessage(
				Diagnostic.Kind.ERROR, "wrong use of annotation: type must be public", element, annotation);
	}
}