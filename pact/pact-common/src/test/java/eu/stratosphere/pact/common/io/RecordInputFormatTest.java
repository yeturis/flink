package eu.stratosphere.pact.common.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import eu.stratosphere.nephele.configuration.Configuration;
import eu.stratosphere.nephele.fs.FileInputSplit;
import eu.stratosphere.nephele.fs.Path;
import eu.stratosphere.pact.common.type.PactRecord;
import eu.stratosphere.pact.common.type.base.PactInteger;
import eu.stratosphere.pact.common.type.base.parser.DecimalTextIntParser;

public class RecordInputFormatTest {

	@Mock
	protected Configuration config;
	
	protected File tempFile;
	
	private final RecordInputFormat format = new RecordInputFormat();
	
	// --------------------------------------------------------------------------------------------
	
	@Before
	public void setup() {
		initMocks(this);
	}
	
	@After
	public void setdown() throws Exception {
		if (this.format != null) {
			this.format.close();
		}
		if (this.tempFile != null) {
			this.tempFile.delete();
		}
	}

	@Test
	public void testConfigure() 
	{
		
		Configuration config = new Configuration();
		config.setString(RecordInputFormat.FILE_PARAMETER_KEY, "file:///some/file/that/will/not/be/read");
		
		// check missing number of fields
		boolean validConfig = true;
		try {
			format.configure(config);
		} catch(IllegalArgumentException iae) {
			validConfig = false;
		}
		assertFalse(validConfig);
		
		// check missing file parser
		config.setInteger(RecordInputFormat.NUM_FIELDS_PARAMETER, 2);
		validConfig = true;
		try {
			format.configure(config);
		} catch(IllegalArgumentException iae) {
			validConfig = false;
		}
		assertFalse(validConfig);
		
		// check valid config
		config.setClass(RecordInputFormat.FIELD_PARSER_PARAMETER_PREFIX + 0, DecimalTextIntParser.class);
		config.setClass(RecordInputFormat.FIELD_PARSER_PARAMETER_PREFIX + 1, DecimalTextIntParser.class);
		validConfig = true;
		try {
			format.configure(config);
		} catch(IllegalArgumentException iae) {
			validConfig = false;
		}
		assertTrue(validConfig);
		
		// check invalid file parser config
		config.setInteger(RecordInputFormat.NUM_FIELDS_PARAMETER, 3);
		validConfig = true;
		try {
			format.configure(config);
		} catch(IllegalArgumentException iae) {
			validConfig = false;
		}
		assertFalse(validConfig);
		
		// check invalid field delimiter
		config.setClass(RecordInputFormat.FIELD_PARSER_PARAMETER_PREFIX + 2, DecimalTextIntParser.class);
		config.setString(RecordInputFormat.FIELD_DELIMITER_PARAMETER, "||");
		validConfig = true;
		try {
			format.configure(config);
		} catch(IllegalArgumentException iae) {
			validConfig = false;
		}
		assertFalse(validConfig);
		
		// check valid config
		config.setString(RecordInputFormat.FIELD_DELIMITER_PARAMETER, "|");
		validConfig = true;
		try {
			format.configure(config);
		} catch(IllegalArgumentException iae) {
			validConfig = false;
		}
		assertTrue(validConfig);
		
		// check invalid text pos config
		config.setInteger(RecordInputFormat.TEXT_POSITION_PARAMETER_PREFIX + 1, 0);
		validConfig = true;
		try {
			format.configure(config);
		} catch(IllegalArgumentException iae) {
			validConfig = false;
		}
		assertFalse(validConfig);
		
		// check valid text pos config
		config.setInteger(RecordInputFormat.TEXT_POSITION_PARAMETER_PREFIX + 0, 3);
		config.setInteger(RecordInputFormat.TEXT_POSITION_PARAMETER_PREFIX + 2, 9);
		validConfig = true;
		try {
			format.configure(config);
		} catch(IllegalArgumentException iae) {
			validConfig = false;
		}
		assertTrue(validConfig);
		
		// check invalid record pos config
		config.setInteger(RecordInputFormat.RECORD_POSITION_PARAMETER_PREFIX + 1, 0);
		validConfig = true;
		try {
			format.configure(config);
		} catch(IllegalArgumentException iae) {
			validConfig = false;
		}
		assertFalse(validConfig);
		
		// check valid record pos config
		config.setInteger(RecordInputFormat.RECORD_POSITION_PARAMETER_PREFIX + 0, 3);
		config.setInteger(RecordInputFormat.RECORD_POSITION_PARAMETER_PREFIX + 2, 9);
		validConfig = true;
		try {
			format.configure(config);
		} catch(IllegalArgumentException iae) {
			validConfig = false;
		}
		assertTrue(validConfig);
	}
	
	@Test
	public void testReadNoPosAll() throws IOException
	{
		final String fileContent = "111|222|333|444|555\n666|777|888|999|000|";
		final FileInputSplit split = createTempFile(fileContent);	
	
		final Configuration parameters = new Configuration();
		parameters.setString(RecordInputFormat.FILE_PARAMETER_KEY, "file:///some/file/that/will/not/be/read");
		parameters.setInteger(RecordInputFormat.NUM_FIELDS_PARAMETER, 5);
		parameters.setString(RecordInputFormat.RECORD_DELIMITER_PARAMETER, "\n");
		parameters.setString(RecordInputFormat.FIELD_DELIMITER_PARAMETER, "|");
		parameters.setClass(RecordInputFormat.FIELD_PARSER_PARAMETER_PREFIX + 0, DecimalTextIntParser.class);
		parameters.setClass(RecordInputFormat.FIELD_PARSER_PARAMETER_PREFIX + 1, DecimalTextIntParser.class);
		parameters.setClass(RecordInputFormat.FIELD_PARSER_PARAMETER_PREFIX + 2, DecimalTextIntParser.class);
		parameters.setClass(RecordInputFormat.FIELD_PARSER_PARAMETER_PREFIX + 3, DecimalTextIntParser.class);
		parameters.setClass(RecordInputFormat.FIELD_PARSER_PARAMETER_PREFIX + 4, DecimalTextIntParser.class);
		
		format.configure(parameters);
		format.open(split);
		
		PactRecord record = new PactRecord();
		
		assertTrue(format.nextRecord(record));
		assertEquals(111, record.getField(0, PactInteger.class).getValue());
		assertEquals(222, record.getField(1, PactInteger.class).getValue());
		assertEquals(333, record.getField(2, PactInteger.class).getValue());
		assertEquals(444, record.getField(3, PactInteger.class).getValue());
		assertEquals(555, record.getField(4, PactInteger.class).getValue());
		
		assertTrue(format.nextRecord(record));
		assertEquals(666, record.getField(0, PactInteger.class).getValue());
		assertEquals(777, record.getField(1, PactInteger.class).getValue());
		assertEquals(888, record.getField(2, PactInteger.class).getValue());
		assertEquals(999, record.getField(3, PactInteger.class).getValue());
		assertEquals(000, record.getField(4, PactInteger.class).getValue());
		
		assertFalse(format.nextRecord(record));
		assertTrue(format.reachedEnd());
		
	}
	
	@Test
	public void testReadNoPosFirstN() throws IOException
	{
		final String fileContent = "111|222|333|444|555|\n666|777|888|999|000|";
		final FileInputSplit split = createTempFile(fileContent);	
	
		final Configuration parameters = new Configuration();
		parameters.setString(RecordInputFormat.FILE_PARAMETER_KEY, "file:///some/file/that/will/not/be/read");
		parameters.setInteger(RecordInputFormat.NUM_FIELDS_PARAMETER, 2);
		parameters.setString(RecordInputFormat.RECORD_DELIMITER_PARAMETER, "\n");
		parameters.setString(RecordInputFormat.FIELD_DELIMITER_PARAMETER, "|");
		parameters.setClass(RecordInputFormat.FIELD_PARSER_PARAMETER_PREFIX + 0, DecimalTextIntParser.class);
		parameters.setClass(RecordInputFormat.FIELD_PARSER_PARAMETER_PREFIX + 1, DecimalTextIntParser.class);
		
		format.configure(parameters);
		format.open(split);
		
		PactRecord record = new PactRecord();
		
		assertTrue(format.nextRecord(record));
		assertEquals(111, record.getField(0, PactInteger.class).getValue());
		assertEquals(222, record.getField(1, PactInteger.class).getValue());
		boolean notParsed = false;
		try {
			record.getField(2, PactInteger.class);
		} catch (IndexOutOfBoundsException ioo) {
			notParsed = true;
		}
		assertTrue(notParsed);
		
		assertTrue(format.nextRecord(record));
		assertEquals(666, record.getField(0, PactInteger.class).getValue());
		assertEquals(777, record.getField(1, PactInteger.class).getValue());
		notParsed = false;
		try {
			record.getField(2, PactInteger.class);
		} catch (IndexOutOfBoundsException ioo) {
			notParsed = true;
		}
		assertTrue(notParsed);
		
		assertFalse(format.nextRecord(record));
		assertTrue(format.reachedEnd());
		
	}
	
	@Test
	public void testReadTextPos() throws IOException
	{
		final String fileContent = "111|222|333|444|555|666|777|888|999|000|\n000|999|888|777|666|555|444|333|222|111|";
		final FileInputSplit split = createTempFile(fileContent);	
	
		final Configuration parameters = new Configuration();
		parameters.setString(RecordInputFormat.FILE_PARAMETER_KEY, "file:///some/file/that/will/not/be/read");
		parameters.setInteger(RecordInputFormat.NUM_FIELDS_PARAMETER, 3);
		parameters.setString(RecordInputFormat.RECORD_DELIMITER_PARAMETER, "\n");
		parameters.setString(RecordInputFormat.FIELD_DELIMITER_PARAMETER, "|");
		parameters.setClass(RecordInputFormat.FIELD_PARSER_PARAMETER_PREFIX + 0, DecimalTextIntParser.class);
		parameters.setClass(RecordInputFormat.FIELD_PARSER_PARAMETER_PREFIX + 1, DecimalTextIntParser.class);
		parameters.setClass(RecordInputFormat.FIELD_PARSER_PARAMETER_PREFIX + 2, DecimalTextIntParser.class);
		parameters.setInteger(RecordInputFormat.TEXT_POSITION_PARAMETER_PREFIX + 0, 0);
		parameters.setInteger(RecordInputFormat.TEXT_POSITION_PARAMETER_PREFIX + 1, 3);
		parameters.setInteger(RecordInputFormat.TEXT_POSITION_PARAMETER_PREFIX + 2, 7);
		
		format.configure(parameters);
		format.open(split);
		
		PactRecord record = new PactRecord();
		
		assertTrue(format.nextRecord(record));
		assertEquals(111, record.getField(0, PactInteger.class).getValue());
		assertEquals(444, record.getField(1, PactInteger.class).getValue());
		assertEquals(888, record.getField(2, PactInteger.class).getValue());
		
		assertTrue(format.nextRecord(record));
		assertEquals(000, record.getField(0, PactInteger.class).getValue());
		assertEquals(777, record.getField(1, PactInteger.class).getValue());
		assertEquals(333, record.getField(2, PactInteger.class).getValue());
		
		assertFalse(format.nextRecord(record));
		assertTrue(format.reachedEnd());
		
		// switched order
		
		parameters.setInteger(RecordInputFormat.TEXT_POSITION_PARAMETER_PREFIX + 0, 8);
		parameters.setInteger(RecordInputFormat.TEXT_POSITION_PARAMETER_PREFIX + 1, 1);
		parameters.setInteger(RecordInputFormat.TEXT_POSITION_PARAMETER_PREFIX + 2, 3);
		
		format.configure(parameters);
		format.open(split);
		
		assertTrue(format.nextRecord(record));
		assertEquals(999, record.getField(0, PactInteger.class).getValue());
		assertEquals(222, record.getField(1, PactInteger.class).getValue());
		assertEquals(444, record.getField(2, PactInteger.class).getValue());
		
		assertTrue(format.nextRecord(record));
		assertEquals(222, record.getField(0, PactInteger.class).getValue());
		assertEquals(999, record.getField(1, PactInteger.class).getValue());
		assertEquals(777, record.getField(2, PactInteger.class).getValue());
		
		assertFalse(format.nextRecord(record));
		assertTrue(format.reachedEnd());
		
	}
	
	@Test
	public void testReadRecPos() throws IOException
	{
		final String fileContent = "111|222|333|\n444|555|666|";
		final FileInputSplit split = createTempFile(fileContent);	
	
		final Configuration parameters = new Configuration();
		parameters.setString(RecordInputFormat.FILE_PARAMETER_KEY, "file:///some/file/that/will/not/be/read");
		parameters.setInteger(RecordInputFormat.NUM_FIELDS_PARAMETER, 3);
		parameters.setString(RecordInputFormat.RECORD_DELIMITER_PARAMETER, "\n");
		parameters.setString(RecordInputFormat.FIELD_DELIMITER_PARAMETER, "|");
		parameters.setClass(RecordInputFormat.FIELD_PARSER_PARAMETER_PREFIX + 0, DecimalTextIntParser.class);
		parameters.setClass(RecordInputFormat.FIELD_PARSER_PARAMETER_PREFIX + 1, DecimalTextIntParser.class);
		parameters.setClass(RecordInputFormat.FIELD_PARSER_PARAMETER_PREFIX + 2, DecimalTextIntParser.class);
		parameters.setInteger(RecordInputFormat.RECORD_POSITION_PARAMETER_PREFIX + 0, 1);
		parameters.setInteger(RecordInputFormat.RECORD_POSITION_PARAMETER_PREFIX + 1, 2);
		parameters.setInteger(RecordInputFormat.RECORD_POSITION_PARAMETER_PREFIX + 2, 0);
				
		format.configure(parameters);
		format.open(split);
		
		PactRecord record = new PactRecord();
		
		assertTrue(format.nextRecord(record));
		assertEquals(111, record.getField(1, PactInteger.class).getValue());
		assertEquals(222, record.getField(2, PactInteger.class).getValue());
		assertEquals(333, record.getField(0, PactInteger.class).getValue());
		
		assertTrue(format.nextRecord(record));
		assertEquals(444, record.getField(1, PactInteger.class).getValue());
		assertEquals(555, record.getField(2, PactInteger.class).getValue());
		assertEquals(666, record.getField(0, PactInteger.class).getValue());
		
		assertFalse(format.nextRecord(record));
		assertTrue(format.reachedEnd());
		
		// sparse record
		
		parameters.setInteger(RecordInputFormat.RECORD_POSITION_PARAMETER_PREFIX + 0, 1);
		parameters.setInteger(RecordInputFormat.RECORD_POSITION_PARAMETER_PREFIX + 1, 8);
		parameters.setInteger(RecordInputFormat.RECORD_POSITION_PARAMETER_PREFIX + 2, 3);
				
		format.configure(parameters);
		format.open(split);
		
		assertTrue(format.nextRecord(record));
		assertEquals(111, record.getField(1, PactInteger.class).getValue());
		assertEquals(222, record.getField(8, PactInteger.class).getValue());
		assertEquals(333, record.getField(3, PactInteger.class).getValue());
		
		assertTrue(format.nextRecord(record));
		assertEquals(444, record.getField(1, PactInteger.class).getValue());
		assertEquals(555, record.getField(8, PactInteger.class).getValue());
		assertEquals(666, record.getField(3, PactInteger.class).getValue());
		
		assertFalse(format.nextRecord(record));
		assertTrue(format.reachedEnd());
		
	}
	
	@Test
	public void testReadTextPosRecPos() throws IOException
	{
		final String fileContent = "111|222|333|444|555|666|777|888|999|000|\n000|999|888|777|666|555|444|333|222|111|";
		final FileInputSplit split = createTempFile(fileContent);	
	
		final Configuration parameters = new Configuration();
		parameters.setString(RecordInputFormat.FILE_PARAMETER_KEY, "file:///some/file/that/will/not/be/read");
		parameters.setInteger(RecordInputFormat.NUM_FIELDS_PARAMETER, 5);
		parameters.setString(RecordInputFormat.RECORD_DELIMITER_PARAMETER, "\n");
		parameters.setString(RecordInputFormat.FIELD_DELIMITER_PARAMETER, "|");
		parameters.setClass(RecordInputFormat.FIELD_PARSER_PARAMETER_PREFIX + 0, DecimalTextIntParser.class);
		parameters.setClass(RecordInputFormat.FIELD_PARSER_PARAMETER_PREFIX + 1, DecimalTextIntParser.class);
		parameters.setClass(RecordInputFormat.FIELD_PARSER_PARAMETER_PREFIX + 2, DecimalTextIntParser.class);
		parameters.setClass(RecordInputFormat.FIELD_PARSER_PARAMETER_PREFIX + 3, DecimalTextIntParser.class);
		parameters.setClass(RecordInputFormat.FIELD_PARSER_PARAMETER_PREFIX + 4, DecimalTextIntParser.class);
		parameters.setInteger(RecordInputFormat.TEXT_POSITION_PARAMETER_PREFIX + 0, 3);
		parameters.setInteger(RecordInputFormat.TEXT_POSITION_PARAMETER_PREFIX + 1, 0);
		parameters.setInteger(RecordInputFormat.TEXT_POSITION_PARAMETER_PREFIX + 2, 1);
		parameters.setInteger(RecordInputFormat.TEXT_POSITION_PARAMETER_PREFIX + 3, 8);
		parameters.setInteger(RecordInputFormat.TEXT_POSITION_PARAMETER_PREFIX + 4, 4);
		parameters.setInteger(RecordInputFormat.RECORD_POSITION_PARAMETER_PREFIX + 0, 2);
		parameters.setInteger(RecordInputFormat.RECORD_POSITION_PARAMETER_PREFIX + 1, 0);
		parameters.setInteger(RecordInputFormat.RECORD_POSITION_PARAMETER_PREFIX + 2, 1);
		parameters.setInteger(RecordInputFormat.RECORD_POSITION_PARAMETER_PREFIX + 3, 3);
		parameters.setInteger(RecordInputFormat.RECORD_POSITION_PARAMETER_PREFIX + 4, 6);
		
		
		format.configure(parameters);
		format.open(split);
		
		PactRecord record = new PactRecord();
		
		assertTrue(format.nextRecord(record));
		assertEquals(444, record.getField(2, PactInteger.class).getValue());
		assertEquals(111, record.getField(0, PactInteger.class).getValue());
		assertEquals(222, record.getField(1, PactInteger.class).getValue());
		assertEquals(999, record.getField(3, PactInteger.class).getValue());
		assertEquals(555, record.getField(6, PactInteger.class).getValue());
		
		assertTrue(format.nextRecord(record));
		assertEquals(777, record.getField(2, PactInteger.class).getValue());
		assertEquals(000, record.getField(0, PactInteger.class).getValue());
		assertEquals(999, record.getField(1, PactInteger.class).getValue());
		assertEquals(222, record.getField(3, PactInteger.class).getValue());
		assertEquals(666, record.getField(6, PactInteger.class).getValue());
		
		assertFalse(format.nextRecord(record));
		assertTrue(format.reachedEnd());
		
	}
	
	private FileInputSplit createTempFile(String content) throws IOException
	{
		this.tempFile = File.createTempFile("test_contents", "tmp");
		DataOutputStream dos = new DataOutputStream(new FileOutputStream(tempFile));
		
		dos.writeBytes(content);
		
		dos.close();
			
		return new FileInputSplit(0, new Path("file://" + this.tempFile.getAbsolutePath()), 0, this.tempFile.length(), new String[] {"localhost"});
	}

}
