package recog;

import recog.util.CpURLStreamHandler;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;


public class CpURLStreamHandlerFactory implements URLStreamHandlerFactory {

	@Override
	public URLStreamHandler createURLStreamHandler(String protocol) {
		if ( protocol.equalsIgnoreCase("cp") )
            return new CpURLStreamHandler();
        else
            return null;
	}

}
