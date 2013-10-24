
package edu.isi.bmkeg.utils.parser;

import java.util.*;

public class ParserEvent extends EventObject {

    private boolean success;

    public  ParserEvent(Object source, boolean success) {        
        super(source);
        this.success = success;
    } 

    public boolean wasSuccessful() {        
      return this.success;
    } 

}
