package il.technion.ewolf.test;


import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class YouTubeTrace {
	
	public static Map<String,Integer> parseWeek(String Path1,String Path2)

	{
		Map<String,Integer> week1 = parseFile(Path1);
		Map<String,Integer> week2 = parseFile(Path2);
		for (String item : week1.keySet()) {
			
			week2.put(item, week2.get(item)-week1.get(item));
		}
		return week2;
	}
	
	
	
	public static Map<String,Integer> parseFile(String Path)

	{
		Map<String,Integer>	items = new HashMap<String,Integer>(); 
		try{
			// Open the file that is the first 
			// command line parameter
			FileInputStream fstream = new FileInputStream(Path);

			// Get the object of DataInputStream
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			//Read File Line By Line
			String[] parsedLine;
			while ((strLine = br.readLine()) != null)   {
				// Print the content on the console
				parsedLine = strLine.split("\t");
			
				if(parsedLine.length == 1)
				{
					//System.out.println(parsedLine[0]);
					items.put(parsedLine[0], 0);
				}
				else
				{
					//System.out.println(parsedLine[0]+" " + parsedLine[1]);
					items.put(parsedLine[0], Integer.parseInt(parsedLine[1]));
				}
			}




			return items;
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return items;

	}
}
