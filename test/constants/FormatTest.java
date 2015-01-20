package constants;

import static org.junit.Assert.*;

import java.io.File;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.junit.Before;
import org.junit.Test;

public class FormatTest {

	@Before
	public void setUp() throws Exception {
	}

//	@Test
	public void testToString() {
		
	}
	@Test
	public void testDocument(){
		String text = "";
		SAXReader reader = new SAXReader();
		 
		try {
			Document doc = reader.read(new File("server.xml"));
			
		} catch (DocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
