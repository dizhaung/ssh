package host.command;

public interface CollectCommand {
	
	public enum AixCommand  implements CollectCommand{
		HOST_TYPE("uname -M"); 
		
		private final String command;
		private AixCommand(String command){
			this.command = command;
		}
		@Override
		public String toString() {
			// TODO Auto-generated method stub
			return command;
		}
		
	}
	
	public enum LinuxCommand  implements CollectCommand{
		HOST_TYPE("uname -M"); 
		
		private final String command;
		private LinuxCommand(String command){
			this.command = command;
		}
		@Override
		public String toString() {
			// TODO Auto-generated method stub
			return command;
		}
	}
	
	public enum CommonCommand  implements CollectCommand{
		HOST_OS("uname");
		private final String command;
		private CommonCommand(String command){
			this.command = command;
		}
		@Override
		public String toString() {
			// TODO Auto-generated method stub
			return command;
		}
		
	}
}
