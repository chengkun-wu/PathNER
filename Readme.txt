Introduction:

PathNer is a tool for mining biological pathway mentions from biomedical literature.  It's implemented under the GATE framework, including two major components: rule-based detection and dictionary matching. 

The rules are implemented using JAPE and the dictionary matching is based on a Java toolkit SecondString. It also utilises information from BANNER, a gene/protein name recognition tool.

You can use the code in your own pipeline or you can run the executable jar file 'PathNer.jar' directly through commandline. 

If you use PathNER, please cite the following paper:

Wu C, Schwartz JM, Nenadic G. PathNER: A tool for systematic
identification of biological pathway mentions in the literature. BMC Systems Biology 2013, 7(Suppl 3):S2 (16 October 2013)


If you have any problem with PathNER or you have suggestions and comments, please contact me via chengkun.wu@manchester.ac.uk.

Suggestions and comments are welcome. 

=====Usage: ============================================
You can run the PathNer jar file directly to have a test.

Examples:

1. See the results on a test str (preset)
   java -jar PathNer.jar
2. See the results on gold corpus
   java -jar PathNer.jar --test gold
3. See the results on a test file
   java -jar PathNer.jar --test test
4. See the results on your file
   java -jar PathNer.jar --test YOUR_FILE
5. Save results to an output file:
   java -jar PathNer.jar --test TEST_FILE --output RESULT_FILE
   Note: If an output file is not specified, the results will be written to the "file_test_result.txt" file under the same folder as PathNer.jar.

If you want to process a large file, you might need to add Java Virtual Machine parameters. For instance, 

java -Xmx4G -jar PathNer.jar --test YOUR_FILE
