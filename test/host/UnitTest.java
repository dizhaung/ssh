package host;

import static org.junit.Assert.*;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;

public class UnitTest {

	@Before
	public void setUp() throws Exception {
	}

//	@Test
	public void testUnitValue() {
		DecimalFormat df = new DecimalFormat("#.##");
		System.out.println(df.format(122222.222));
	}
	
	@Test
	public void testNoMethodError(){
		System.out.println(String.class);
		System.out.println(Date[].class);
	}
}
