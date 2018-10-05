/*
 */
package keboola.ftp.extractor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.log4j.Logger;

import keboola.ftp.extractor.config.FtpMapping;
import keboola.ftp.extractor.config.FtpMapping.Compression;
import keboola.ftp.extractor.config.JsonConfigParser;
import keboola.ftp.extractor.config.KBCConfig;
import keboola.ftp.extractor.config.KBCParameters;
import keboola.ftp.extractor.config.tableconfig.ManifestBuilder;
import keboola.ftp.extractor.config.tableconfig.ManifestFile;
import keboola.ftp.extractor.ftpclient.FTPClientBuilder;
import keboola.ftp.extractor.ftpclient.FtpException;
import keboola.ftp.extractor.ftpclient.IFTPClient;
import keboola.ftp.extractor.state.JsonStateWriter;
import keboola.ftp.extractor.state.VisitedFolder;
import keboola.ftp.extractor.state.VisitedFoldersList;
import keboola.ftp.extractor.utils.CsvUtils;
import keboola.ftp.extractor.utils.FileHandler;
import keboola.ftp.extractor.utils.MergeException;

/**
 *
 * @author David Esner <esnerda at gmail.com>
 * @created 2016
 */
public class Extractor {
	private static final String VERSION = "1.3.2";
	final static Logger log = Logger.getLogger(Extractor.class);
	private static final Charset DEFAUT_CHARSET = StandardCharsets.UTF_8;
	
	private static String outTablesPath;
	private static String dataPath;
	private static KBCConfig config = null;
	private static IFTPClient ftpClient = null;

	public static void run(String[] args) throws Exception {

		initEnvironment(args);
		// retrieve stateFile
		VisitedFoldersList visitedFolders = loadLastState();

		// get user mappings
		KBCParameters confParams = config.getParams();
		List<FtpMapping> mappings = confParams.getMappings();
		printEnvStats();
		initFtpClient();

		List<VisitedFolder> visitedFoldersCurrent = new ArrayList<VisitedFolder>();
		Map<String, Date> retrievedFiles = null;

		Date lastRun = null;
		Date currDate;
		/* Download all files and generate manifests */
		log.info("Running version " + VERSION);
		log.info("Downloading files...");
		int count = 0;
		try {
			for (FtpMapping mapping : mappings) {
				// create historical state record for current folder
				currDate = new Date();
				VisitedFolder f = new VisitedFolder(mapping.getFtpPath(), currDate, mapping);
				visitedFoldersCurrent.add(f);

				// set download parameters according to previous runs
				lastRun = getLastRun(mapping, visitedFolders);

				retrievedFiles = retrieveFiles(mapping, lastRun);

				count += retrievedFiles.size();
				if (count == 0 || retrievedFiles.size() == 0) {
					log.warn("No files found for mapping: " + mapping.toString()); 
					continue;
				}

				

				if (!Compression.NONE.equals(mapping.getCompressionEnum())) {
					proccessArchivedFiles(retrievedFiles.keySet(), mapping);
				} else {
					processNormalFiles(retrievedFiles.keySet(), mapping);
				}

			}
		} catch (FtpException ex) {
			log.error("Failed to download files. " + ex.getMessage(), ex);
			System.exit(ex.getSeverity());
		} finally {
			try {
				ftpClient.disconnect();
			} catch (IOException ex) {
				log.error(ex.getMessage(), ex);
			}
		}

		/* Write state file */
		try {
			JsonStateWriter.writeStateFile(dataPath + File.separator + "out" + File.separator + "state.json",
					visitedFoldersCurrent);
		} catch (IOException ex) {
			log.error(ex);
		}

		log.info(count + " files successfuly downloaded.");

	}

	/* - Ftp processing methods */

	private static void printEnvStats() {
		// Get current size of heap in bytes
		long heapSize = Runtime.getRuntime().totalMemory(); 

		// Get maximum size of heap in bytes. The heap cannot grow beyond this size.// Any attempt will result in an OutOfMemoryException.
		long heapMaxSize = Runtime.getRuntime().maxMemory();

		 // Get amount of free memory within the heap in bytes. This size will increase // after garbage collection and decrease as new objects are created.
		long heapFreeSize = Runtime.getRuntime().freeMemory();
		
		log.info("Initial Heap size (MB): " + heapSize/1000000);
		log.info("Max Heap size (MB): " + heapMaxSize/1000000);
		log.info("Initial free memory (MB): " + heapFreeSize/1000000);		
	}

	private static Map<String, Date> retrieveFiles(FtpMapping mapping, Date lastRun) throws FtpException {
		if (mapping.isFolder()) {
			// download all csv files in case of whole folder
			return retrieveFilesInFolder(mapping, lastRun);
		}
		File f = new File(mapping.getFtpPath());
		return ftpClient.downloadFile(f.getParent(), f.getName(), outTablesPath);
	}

	private static Map<String, Date> retrieveFilesInFolder(FtpMapping mapping, Date lastRun) throws FtpException {
		// download all csv files in case of whole folder
		if (mapping.getPrefix() != null) {// in case of files by prefix
			return ftpClient.downloadAllNewCsvFilesByPrefix(mapping.getFtpPath(), mapping.getExtension(), outTablesPath,
					mapping.getPrefix(), lastRun, mapping.isWildcardSupport());
		}
		return ftpClient.downloadAllNewCsvFiles(mapping.getFtpPath(), mapping.getExtension(), outTablesPath, null,
				lastRun);
	}

	private static void proccessArchivedFiles(Collection<String> files, FtpMapping mapping) {
		List<String> resultFileNames = new ArrayList<>();
		try {
			for (String file : files) {
				resultFileNames.addAll(unzip(mapping.getCompressionEnum(), file, outTablesPath));
			}
			// delete zipfiles
			FileHandler.deleteFiles(getFilePaths(files));
			// process unzipped results
			processNormalFiles(resultFileNames, mapping);
		} catch (Exception e) {
			log.error("Failed to download archived files. " + e.getMessage(), e);
			System.exit(1);
		}
	}


	private static List<String> getFilePaths(Collection<String> fileNames) {
		List<String> res = new ArrayList<String>();
		for (String file : fileNames) {
			res.add(outTablesPath + File.separator + file);
		}
		return res;
	}
	private static void processNormalFiles(Collection<String> files, FtpMapping mapping) {
		if (files == null || files.isEmpty()) {
    		return;
    	}
		try {
			//CsvFileMerger.dataStructureMatch(files, outTablesPath, mapping.getDelimiterChar(), mapping.getEnclosureChar());
			convertCharset(getFilePaths(files),mapping.getSrcCharset());
		} catch (MergeException ex) {
			log.error("Failed to merge files. " + ex.getMessage(), ex);
			System.exit(1);
		} catch (Exception e) {
			log.error("Failed to process files. " + e.getMessage(), e);
			System.exit(2);
		}
		buildManifestFiles(new ArrayList<String>(files), mapping);
	}

	private static void convertCharset(Collection<String> files, Charset srcCharset) throws Exception {
		if (!srcCharset.equals(DEFAUT_CHARSET)) {
			for (String f : files) {
				FileHandler.convertFileCharset(new File(f), srcCharset, DEFAUT_CHARSET);
			}
		}		
	}

	private static void buildManifestFiles(List<String> fileNames, FtpMapping mapping) {
		if (fileNames.isEmpty()) {
			return;
		}
		try {
		// get colums
		String[] headerCols = prepareSlicedTables(fileNames, mapping);
		// build manifest files
			for (String fileName : fileNames) {
				ManifestFile manFile = new ManifestFile.Builder(fileName, mapping.getSapiPath())
						.setIncrementalLoad(mapping.isIncremental()).setPrimaryKey(mapping.getPkey())
						.setDelimiter(mapping.getDelimiter()).setEnclosure(mapping.getEnclosure())
						.setColumns(headerCols).build();

				ManifestBuilder.buildManifestFile(manFile, outTablesPath, fileName);
			}
			} catch (Exception ex) {
				log.error("Failed to build manifest file. " + ex.getMessage(), ex);
				System.exit(2);
			}
		}
	

	private static String[] prepareSlicedTables(List<String> fileNames, FtpMapping mapping) throws Exception {
		if (fileNames.size() < 2) {
			return null;
		}
		List<File> files = new ArrayList<>();
		for (String name : fileNames) {
			files.add(new File(outTablesPath + File.separator + name));
		}
		List<File> resultFiles = new ArrayList<>();
		File slicedFileFolder = new File(outTablesPath + File.separator + mapping.getSapiTableName() + ".csv");
		slicedFileFolder.mkdirs();
		// get colums
				String[] headerCols = CsvUtils.readHeader(files.get(0),
						mapping.getDelimiterChar(), mapping.getEnclosureChar(), '\\', false, false);
				// remove headers and create results
				for (File file : files) {
					CsvUtils.removeHeaderFromCsv(file);
					//move to folder
					resultFiles.add(Files.move(file.toPath(), slicedFileFolder.toPath().resolve(file.getName())).toFile());
					file.delete();
				}
				//in case some files did not contain any data
				CsvUtils.deleteEmptyFiles(resultFiles);
				fileNames.clear();
				fileNames.add(slicedFileFolder.getName());
				return headerCols;
	}


	private static List<String> unzip(Compression compressionType, String fileName, String outPath) throws Exception {
		byte[] buffer = new byte[1024];
		File zipFile = new File(outPath + File.separator + fileName);
		List<String> resultFileNames = new ArrayList<>();
		switch (compressionType) {
		case ZIP:
			resultFileNames = FileHandler.decompressZipFile(outPath, zipFile);
			break;
		case GZIP:
			resultFileNames = FileHandler.decompressGZip(outPath, zipFile);
			break;
		default:
			throw new Exception("Unsupported compression type!");
		}
		return resultFileNames;
	}

	

	/* -- internal Kbc methods -- */

	private static Date getLastRun(FtpMapping mapping, VisitedFoldersList visitedFolders) {
		if (visitedFolders == null || mapping.getSinceLast() == 0) {
			return null;
		}
		VisitedFolder vf = visitedFolders.getFolderByPath(mapping.getFtpPath());
		if (vf != null && mapping.equals(vf.getLastMapping())) {
			return vf.getLastRun();
		}
		return null;

	}

	private static void initEnvironment(String[] args) {
		if (args.length == 0) {
			log.error("No parameters provided.");
			System.exit(1);
		}
		dataPath = args[0];
		outTablesPath = dataPath + File.separator + "out" + File.separator + "tables";
		// parse config
		loadConfig();
	}

	private static void loadConfig() {
		// parse config
		File confFile = new File(dataPath + File.separator + "config.json");
		if (!confFile.exists()) {
			System.err.println("config.json does not exist!");
			System.exit(1);
		}
		// Parse config file
		try {
			if (confFile.exists() && !confFile.isDirectory()) {
				config = JsonConfigParser.parseFile(confFile);
			}
		} catch (Exception ex) {
			log.error("Failed to parse config file");
			System.exit(1);
		}

		if (!config.validate()) {
			log.error(config.getValidationError());
			System.err.println(config.getValidationError());
			System.exit(1);
		}
	}

	private static VisitedFoldersList loadLastState() {
		// retrieve stateFile
		File stateFile = new File(dataPath + File.separator + "in" + File.separator + "state.json");
		VisitedFoldersList visitedFolders = null;
		if (stateFile.exists()) {

			try {
				visitedFolders = (VisitedFoldersList) JsonConfigParser.parseFile(stateFile, VisitedFoldersList.class);
			} catch (IOException ex) {
				log.error(ex);
			}
		} else {
			log.info("State file does not exist. (first run?)");
		}
		return visitedFolders;
	}

	private static void initFtpClient() {
		KBCParameters confParams = config.getParams();
		try {
			ftpClient = FTPClientBuilder.create(confParams.getFtpProtocol(), confParams.getFtpUrl())
					.setPort(confParams.getPort()).setUser(confParams.getUser()).setPass(confParams.getPass())
					.setHostTz(TimeZone.getTimeZone(confParams.getTimezone())).build();
			ftpClient.connect();

		} catch (FtpException ex) {
			log.error(ex.getMessage(), ex);
			System.err.println("Failed to create FTP client. " + ex.getMessage());
			// ex.printStackTrace();
			System.exit(ex.getSeverity());
		} catch (IOException ex) {
			log.error(ex.getMessage(), ex);
			System.err.println("Failed to create FTP client. " + ex.getMessage());
			// ex.printStackTrace();
			System.exit(1);
		}
	}

}
