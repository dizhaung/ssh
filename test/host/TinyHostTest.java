package host;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class TinyHostTest {

	private static TinyHost  host = new TinyHost();
	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testGetHostList() {
		List<TinyHost> list = host.getHostList(FileManager.readFile("/hostConfig.txt"));
		System.out.println(list);
	}

}
