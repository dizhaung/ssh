package ssh;

import static org.junit.Assert.*;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import host.FileManager;
import host.TinyHost;

import org.junit.Before;
import org.junit.Test;

public class SSHClientTest {
	private static TinyHost  host = new TinyHost();
	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testStartBat() {
		System.out.println( Matcher.quoteReplacement("ls|oo $"));
	
		//System.out.println("/$".replaceAll(Pattern.quote("$"), ""));
	}

	/*@Test
	public void testBatHosts() {
		List<TinyHost> list = host.getHostList(FileManager.readFile("/hostConfig.txt"));
		SSHClient.batHosts(list, "ls |grep lib",false);
		System.out.println(list);
	}*/

}
