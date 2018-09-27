package test;

import java.io.PrintWriter;
import java.net.Socket;

public class JavaAutomationClient {

	public static void main(String[] args) throws Exception {

		// inputs: port number and path to instruction set
		int portNumber = 1789;
		String inputPath = "/Users/aurora/EclipseWorkspace/Equinox/docs/plugin_api/NewFile.xml";

		// create network socket and send input file path to server
		try (Socket s = new Socket("localhost", portNumber); PrintWriter out = new PrintWriter(s.getOutputStream(), true)) {
			out.println(inputPath);
		}
	}
}