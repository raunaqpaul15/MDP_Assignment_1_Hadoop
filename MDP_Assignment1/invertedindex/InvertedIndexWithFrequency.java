package invertedindex;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;



public class InvertedIndexWithFrequency extends Configured implements Tool {
	public static void main(String[] args) throws Exception {
		System.out.println(Arrays.toString(args));
		int res = ToolRunner.run(new Configuration(),
				new InvertedIndexWithFrequency(), args);

		System.exit(res);
	}

	@Override
	public int run(String[] args) throws Exception {
		System.out.println(Arrays.toString(args));
		Job job = new Job(getConf(), "InvertedIndexdemo");

		job.setJarByClass(InvertedIndexWithFrequency.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);

		job.setMapperClass(StubMapper.class);
		job.setCombinerClass(StubCombiner.class);
		job.setReducerClass(StubReducer.class);

		job.setInputFormatClass(TextInputFormat.class);
		job.setOutputFormatClass(TextOutputFormat.class);

		job.getConfiguration().set(
				"mapreduce.output.textoutputformat.separator", " -> ");
		job.setNumReduceTasks(1);

		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));

		FileSystem fs = FileSystem.newInstance(getConf());

		if (fs.exists(new Path(args[1]))) {
			fs.delete(new Path(args[1]), true);
		}

		job.waitForCompletion(true);

		return 0;
	}

	public static class StubMapper extends Mapper<Object, Text, Text, Text> {
		private Text word = new Text();
		private Text filename = new Text();

		@Override
		public void map(Object key, Text value, Context context)
				throws IOException, InterruptedException {

			HashSet<String> stopwords = new HashSet<String>();
			BufferedReader Reader = new BufferedReader(
					new FileReader(
							new File(
									"/home/cloudera/workspace/MDP_Assignment1/stopwords.txt")));
			String line;
			while ((line = Reader.readLine()) != null) {
				stopwords.add(line.toString().toLowerCase());
			}
			Reader.close();
			String filenameStr = ((FileSplit) context.getInputSplit())
					.getPath().getName();
			
			filename = new Text(filenameStr);

			StringTokenizer tokenizer = new StringTokenizer(value.toString().replaceAll("[\\p{Punct}\\d ]"," "));
			//System.out.print(tokenizer);
			while (tokenizer.hasMoreTokens()) {
				word.set(tokenizer.nextToken().toString().toLowerCase());	
				context.write(word,filename);
			}
			
		}
	}

	public static class StubCombiner extends Reducer<Text,Text,Text,Text>
	{   private Text combo= new Text();
		public void reduce(Text key, Iterable<Text> values, Context context)
				throws IOException, InterruptedException {
			
			Integer frequency=new Integer(0);
			String fname= new String();
			
			for(Text val:values)
			{
				frequency=frequency+1;
				if(frequency==1)
				{
					fname=val.toString();
					
				}
				
			}
			combo.set(fname+"#"+frequency.toString());
			context.write(key,combo);
		}
		
	}
	public static class StubReducer extends Reducer<Text, Text, Text, Text> {
        private  Text combo=new Text();
		@Override
		public void reduce(Text key, Iterable<Text> values, Context context)
				throws IOException, InterruptedException {
            String result=new String();
			Map<String,Integer> nmap=new HashMap<String,Integer>();
			String Filename=new String();
			Integer count=new Integer(0);
			
  
			for (Text value : values) {
				String[] s=value.toString().split("#");
				Filename=s[0];
				count=new Integer(s[1]);
				if(nmap.containsKey(Filename))
				{
					nmap.put(Filename, nmap.get(Filename)+count);
					
				}
				else
				{
					nmap.put(Filename, count);
				}
			}

			for (String v :nmap.keySet()) 
			{
				
			 result+=","+v+"#"+nmap.get(Filename).toString();
			}
            combo.set(result);
			context.write(key, combo);

		}
	}
}