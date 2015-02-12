package il.technion.utils;

import il.technion.ewolf.kbr.Node;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.name.Named;


public class PrintsManager {
	
	private final ArrayList<String> printedComponents;
	private final boolean debug;
	private final int verbosityLevel;
	private final SimpleDateFormat dateFormat;
	private final Node localNode;
	
	@Inject
	public PrintsManager(
			@Named("openkad.local.node") Node localNode,
			@Named("shades.printsManager.print") boolean debug,
			ArrayList<String> printedComponents,
			@Named("shades.printsManager.ve" +
					"this.bosityLevel") int verbosityLevel
			){
		this.dateFormat = new SimpleDateFormat("HH:mm:ss:SSS");
		this.localNode = localNode;
		this.debug = debug;
		this.printedComponents = printedComponents;
		this.verbosityLevel = verbosityLevel;
	}
	
	public boolean isDebugMode(){
		return debug;
	}
	
	synchronized public void print(int msgVerbosityLevel, String component ,String message) {
		if (debug) {
			if (printedComponents.contains(component) && msgVerbosityLevel>=verbosityLevel) {
				Date date = new Date();
				System.out.println(dateFormat.format(date) + " LocalNode: " + localNode.getKey() + " DEBUG: (" + component + ") " + message);
			}
		}
	}
	
	synchronized public void printList(int msgVerbosityLevel, String component ,String message, List list) {
		if (debug) {
			if (printedComponents.contains(component) && msgVerbosityLevel>=verbosityLevel) {
				Date date = new Date();
				message = dateFormat.format(date) + " LocalNode: " + localNode.getKey() + " DEBUG: (" + component + ") " + message + " ";
				
				if(list.isEmpty()){
					message+= "empty list";
				}else{
					for(Object o : list){
						message+= o + "\t";
					}
				}
				System.out.print(message + "\n");
			}
		}
	}
	
	synchronized public void error(String component ,String message) {
		Date date = new Date();
		System.err.println(dateFormat.format(date) + " LocalNode: " + localNode.getKey() + " ERROR: (" + component + ") " + message);
	}
	
	synchronized public void exception(String component , Throwable exc) {
		Date date = new Date();
		System.err.println(dateFormat.format(date) + " LocalNode: " + localNode.getKey() + " EXEPTION: (" + component + ") " + exc.toString());
	}
}
