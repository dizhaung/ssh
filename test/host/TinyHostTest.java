package host;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class TinyHostTest {

	private static TinyHost  host = new TinyHost();
	@Before
	public void setUp() throws Exception {
	}

	public static  <E> void testGeneric(List<E> list){
		
	}
	@Test
	public <Hosase> List<? extends HostBase>   testGetHostList(List<? extends Hosase> f) {
		List<TinyHost> list = host.getHostList(FileManager.readFile("/hostConfig.txt"));
		System.out.println(list);
		
		List<? extends HostBase> list2 = new  ArrayList();
		List<TinyHost> list3 = new ArrayList();
		list2.add(new TinyHost());
		list3.add(new TinyHost());
		
		TinyHostTest.testGeneric(Collections.emptyList());
		List<String> list4 = Collections.emptyList();
		return f;
	}

}
