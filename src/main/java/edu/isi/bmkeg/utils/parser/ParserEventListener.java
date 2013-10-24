package edu.isi.bmkeg.utils.parser;

import java.util.*;

public interface ParserEventListener extends EventListener {
    public void parseCompleted(ParserEvent evt) throws Exception;
}


