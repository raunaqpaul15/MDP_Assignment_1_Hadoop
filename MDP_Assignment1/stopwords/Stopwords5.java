package stopwords;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
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



public class Stopwords5 extends Configured implements Tool {

	public static void main(String[] args) throws Exception {
		System.out.println(Arrays.toString(args));
		int res = ToolRunner.run(new Configuration(), new Stopwords5(),args);

		System.exit(res);
	}

	@Override
	public int run(String[] args) throws Exception {
		System.out.println(Arrays.toString(args));
		Job job = new Job(getConf(), "StopWordsJob_50reducers_withCombiners_Compression");

		job.setJarByClass(Stopwords5.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(IntWritable.class);

		job.setMapperClass(StubMapper.class);
		job.setCombinerClass(StubReducer.class);
		job.setReducerClass(StubReducer.class);
		//job.setCombinerClass(StubReducer.class);
        
		job.setInputFormatClass(TextInputFormat.class);
		job.setOutputFormatClass(TextOutputFormat.class);

		job.getConfiguration().set("mapreduce.output.textoutputformat.separator", ",");
		job.setNumReduceTasks(50);
         
		FileOutputFormat.setCompressOutput(job, true);
		FileOutputFormat.setOutputCompressorClass(job,org.apache.hadoop.io.compress.BZip2Codec.class);
		
		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));

		FileSystem fs = FileSystem.newInstance(getConf());

		if (fs.exists(new Path(args[1]))) {
			fs.delete(new Path(args[1]), true);
		}

		job.waitForCompletion(true);

		return 0;
	}

	public static class StubMapper extends Mapper<LongWritable, Text, Text, IntWritable>
	{
                 private final static IntWritable ONE = new IntWritable(1);
                 private Text word = new Text();

             @Override
              public void map(LongWritable key, Text value, Context context)
		          throws IOException, InterruptedException 
             {
	                            for (String token : value.toString().replaceAll("[^a-zA-Z ]", "").split("\\s+")) 
	                            {
		                           word.set(token.toLowerCase());
		                           context.write(word, ONE);
	                            }
             }
        }
	public static class StubReducer extends Reducer<Text, IntWritable, Text, IntWritable>
	{
                 @Override
                    public void reduce(Text key, Iterable<IntWritable> values,Context context) throws IOException, InterruptedException 
                 {
	                                     int sum = 0;
	                                     for (IntWritable val : values) 
	                                             {
		                                           sum += val.get();
	                                             }
	                                     if (sum > 4000) 
	                                      {
		                                     context.write(key, new IntWritable(sum));
	                                      }
                 }

	}
}
