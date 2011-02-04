package test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;


public class Blaj {

	@Test
	public void test() {
		
		try {
			List<String> original = toLines(new URI("file://C:/workspace/sandbox/itsm/itsm-generated-ws/itsm-incident-ws/src/main/resources/HPD_IncidentInterface_WS.wsdl"));
			List<String> revised = toLines(new URI("http://se-sto1as33/arsys/WSDL/public/se-sto1ta35/HPD_IncidentInterface_WS"));

			Patch patch = DiffUtils.diff(original, revised);
			if(!patch.getDeltas().isEmpty()) {
				for(Delta delta : patch.getDeltas()) {
					System.out.println(String.format("diff: \n%s\n%s", delta.getOriginal().toString(), delta.getRevised().toString()));
				}
			} else {
				System.out.println("files are identical");
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static List<String> toLines(URI uri) throws Exception {
		List<String> lines = new ArrayList<String>();
		BufferedReader in = null;
		if("file".equals(uri.getScheme())) {
			File f = new File(String.format("%s%s", uri.getAuthority(), uri.getPath()));
			in = new BufferedReader(new FileReader(f));
		} else if("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) {
			in = new BufferedReader(
					new InputStreamReader(
							uri.toURL().openStream()));
		} else {
			throw new IllegalArgumentException("unsupported uri: " + uri.toString());
		}
		
		String line = null;
		while((line = in.readLine()) != null) {
			lines.add(line);
		}
		in.close();
		
		return lines;
	}
}
