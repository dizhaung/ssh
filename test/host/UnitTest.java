package host;

import static org.junit.Assert.*;

import java.text.DecimalFormat;

import org.junit.Before;
import org.junit.Test;

public class UnitTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testUnitValue() {
		DecimalFormat df = new DecimalFormat("#.##");
		System.out.println(df.format(122222.222));
	}

}
