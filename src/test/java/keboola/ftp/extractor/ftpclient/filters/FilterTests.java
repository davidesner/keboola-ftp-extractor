package keboola.ftp.extractor.ftpclient.filters;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;


/**
 * @author David Esner
 */
public class FilterTests {

	  @Test
	  public void testSFTPAsterixMatchingWildcardPrefix() {
		  
		String testingFileName = "my_amazing_file_name";

	    String matchingTestPrefix = "*file_name";
	    assertTrue(SftpFilters.hasPrefix(testingFileName, matchingTestPrefix));

	    matchingTestPrefix = "my_*name";
	    assertTrue(SftpFilters.hasPrefix(testingFileName, matchingTestPrefix));

	    matchingTestPrefix = "my_amaz*";
	    assertTrue(SftpFilters.hasPrefix(testingFileName, matchingTestPrefix));  
	    
	    matchingTestPrefix = "my_*";
	    assertTrue(SftpFilters.hasPrefix(testingFileName, matchingTestPrefix));  
	  }

	  @Test
	  public void testSFTPAsterixNotMatchingWildcardPrefix() {
	    String testingFileName = "my_amazing_file_name";

	    String matchingTestPrefix = "*sfile_name";
	    assertFalse(SftpFilters.hasPrefix(testingFileName, matchingTestPrefix));

	    matchingTestPrefix = "my_*names";
	    assertFalse(SftpFilters.hasPrefix(testingFileName, matchingTestPrefix));

	    matchingTestPrefix = "my_amas*";
	    assertFalse(SftpFilters.hasPrefix(testingFileName, matchingTestPrefix));    
	  }


	  @Test
	  public void testSFTPCombinedMatchingWildcardPrefix() {
	    String testingFileName = "my_amazing_file_name";

	    String matchingTestPrefix = "*f?le_name*";
	    assertTrue(SftpFilters.hasPrefix(testingFileName, matchingTestPrefix));

	    matchingTestPrefix = "?y_am*";
	    assertTrue(SftpFilters.hasPrefix(testingFileName, matchingTestPrefix));

	    matchingTestPrefix = "my?ama*";
	    assertTrue(SftpFilters.hasPrefix(testingFileName, matchingTestPrefix));    
	  }

	  @Test
	  public void testSFTPBackwardCompatibilityMatchingWildcardPrefix() {
	    String testingFileName = "my_amazing_file_name";  

	    String matchingTestPrefixold = "my_";
	    
	    
	    
	    assertTrue(SftpFilters.hasPrefix(testingFileName, matchingTestPrefixold) && testingFileName.startsWith(matchingTestPrefixold)); 
	  }
	  
	  
	  
	  /*
	   * FTP
	   * */
	  
	  @Test
	  public void testFTPAsterixMatchingWildcardPrefix() {
		  
		String testingFileName = "my_amazing_file_name";

	    String matchingTestPrefix = "*file_name";
	    assertTrue(FtpFilters.hasPrefix(testingFileName, matchingTestPrefix));

	    matchingTestPrefix = "my_*name";
	    assertTrue(FtpFilters.hasPrefix(testingFileName, matchingTestPrefix));

	    matchingTestPrefix = "my_amaz*";
	    assertTrue(FtpFilters.hasPrefix(testingFileName, matchingTestPrefix));  
	    
	    matchingTestPrefix = "my_*";
	    assertTrue(FtpFilters.hasPrefix(testingFileName, matchingTestPrefix));  
	  }

	  @Test
	  public void testFTPAsterixNotMatchingWildcardPrefix() {
	    String testingFileName = "my_amazing_file_name";

	    String matchingTestPrefix = "*sfile_name";
	    assertFalse(FtpFilters.hasPrefix(testingFileName, matchingTestPrefix));

	    matchingTestPrefix = "my_*names";
	    assertFalse(FtpFilters.hasPrefix(testingFileName, matchingTestPrefix));

	    matchingTestPrefix = "my_amas*";
	    assertFalse(FtpFilters.hasPrefix(testingFileName, matchingTestPrefix));    
	  }


	  @Test
	  public void testFTPCombinedMatchingWildcardPrefix() {
	    String testingFileName = "my_amazing_file_name";

	    String matchingTestPrefix = "*f?le_name*";
	    assertTrue(FtpFilters.hasPrefix(testingFileName, matchingTestPrefix));

	    matchingTestPrefix = "?y_am*";
	    assertTrue(FtpFilters.hasPrefix(testingFileName, matchingTestPrefix));

	    matchingTestPrefix = "my?ama*";
	    assertTrue(FtpFilters.hasPrefix(testingFileName, matchingTestPrefix));    
	  }

	  @Test
	  public void testFTPBackwardCompatibilityMatchingWildcardPrefix() {
	    String testingFileName = "my_amazing_file_name";  

	    String matchingTestPrefixold = "my_";
	    
	    
	    
	    assertTrue(FtpFilters.hasPrefix(testingFileName, matchingTestPrefixold) && testingFileName.startsWith(matchingTestPrefixold)); 
	  }
}
