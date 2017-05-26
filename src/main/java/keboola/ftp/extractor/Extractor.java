/*
 */
package keboola.ftp.extractor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
import keboola.ftp.extractor.utils.CsvFileMerger;
import keboola.ftp.extractor.utils.CsvUtils;
import keboola.ftp.extractor.utils.FileHandler;
import keboola.ftp.extractor.utils.MergeException;

/**
 *
 * @author David Esner <esnerda at gmail.com>
 * @created 2016
 */
public class Extractor {
	final static Logger log = Logger.getLogger(Extractor.class);
	
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

		initFtpClient();

		List<VisitedFolder> visitedFoldersCurrent = new ArrayList<>();
		Map<String, Date> retrievedFiles = null;

		Date lastRun = null;
		Date currDate;
		/* Download all files and generate manifests */
		log.info("Downloading files...");
		int count = 0;
		try {
			for (FtpMapping mapping : mappings) {
				// set download parameters according to previous runs
				lastRun = getLastRun(mapping, visitedFolders);

				retrievedFiles = retrieveFiles(mapping, lastRun);

				count += retrievedFiles.size();

				// create historical state record for current folder
				currDate = new Date();
				VisitedFolder f = new VisitedFolder(mapping.getFtpPath(), retrievedFiles, currDate, mapping);
				visitedFoldersCurrent.add(f);

				if (count == 0) {
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
				log.error(ex);
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
					mapping.getPrefix(), lastRun);
		}
		return ftpClient.downloadAllNewCsvFiles(mapping.getFtpPath(), mapping.getExtension(), outTablesPath, null,
				lastRun);
	}

	private static void proccessArchivedFiles(Collection<String> files, FtpMapping mapping) {
		List<String> resultFileNames = new ArrayList<>();
		try {
			for (String file : files) {
				resultFileNames.addAll(unzip(file, outTablesPath));
			}
			// delete zipfiles
			FileHandler.deleteFiles(files.stream().map(f -> outTablesPath + File.separator + f).collect(Collectors.toList()));
			// process unzipped results
			processNormalFiles(resultFileNames, mapping);
		} catch (Exception e) {
			log.error("Failed to download archived files. " + e.getMessage(), e);
			System.exit(1);
		}
	}

	private static void processNormalFiles(Collection<String> files, FtpMapping mapping) {
		try {
			CsvFileMerger.dataStructureMatch(files, outTablesPath, mapping.getDelimiterChar(), mapping.getEnclosureChar());
		} catch (MergeException ex) {
			log.error("Failed to merge files. " + ex.getMessage(), ex);
			System.exit(1);
		}
		buildManifestFiles(new ArrayList<String>(files), mapping);
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
		List<File> files = fileNames.stream().map(f -> new File(outTablesPath + File.separator)).collect(Collectors.toList());
		// get colums
				String[] headerCols = CsvUtils.readHeader(files.get(0),
						mapping.getDelimiterChar(), mapping.getEnclosureChar(), '\\', false, false);
				// remove headers and create results
				for (String file : fileNames) {
					CsvUtils.removeHeaderFromCsv(new File(outTablesPath + File.separator + file));
				}
				//in case some files did not contain any data
				CsvUtils.deleteEmptyFiles(files);
				return headerCols;
	}


	private static List<String> unzip(String fileName, String outPath) throws Exception {
		byte[] buffer = new byte[1024];
		File zipFile = new File(outPath + File.separator + fileName);
		List<String> resultFileNames = new ArrayList<>();

		ZipInputStream zis = null;
		try {
			// get the zip file content
			zis = new ZipInputStream(new FileInputStream(zipFile));
			// get the zipped file list entry
			ZipEntry ze = zis.getNextEntry();

			while (ze != null) {

				File newFile = new File(outPath + File.separator + ze.getName());

				log.info("file unzip : " + newFile.getAbsoluteFile());

				// create all non exists folders else you will hit
				// FileNotFoundException for compressed folder
				new File(newFile.getParent()).mkdirs();

				FileOutputStream fos = new FileOutputStream(newFile);

				int len;
				while ((len = zis.read(buffer)) > 0) {
					fos.write(buffer, 0, len);
				}

				fos.close();
				ze = zis.getNextEntry();
				resultFileNames.add(newFile.getName());
			}

		} catch (Exception e) {
			throw e;
		} finally {
			try {
				zis.closeEntry();
				zis.close();
			} catch (IOException e) {
				// nn
			}

		}
		return resultFileNames;

	}

	/* -- internal Kbc methods -- */

	private static Date getLastRun(FtpMapping mapping, VisitedFoldersList visitedFolders) {
		if (visitedFolders == null) {
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
			log.error(ex);
			System.err.println("Failed to create FTP client. " + ex.getMessage());
			// ex.printStackTrace();
			System.exit(ex.getSeverity());
		} catch (IOException ex) {
			log.error(ex);
			System.err.println("Failed to create FTP client. " + ex.getMessage());
			// ex.printStackTrace();
			System.exit(1);
		}
	}

}
