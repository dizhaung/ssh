package ssh;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class ShellTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testShell() {
		 try {
			Shell shell = new Shell("10.204.16.11",22,"root","1234qwer");
			shell.executeCommands(new String[]{"ls"});
		} catch (ShellException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void testExecuteCommands() {
		 
	}
	
//	public static void main(String[] args){
//		 try {
//				Shell shell = new Shell("10.204.4.17",22,"jk","jk123qwe");
//				shell.executeCommands(new String[]{"ls"});
//			} catch (ShellException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//	}
}
