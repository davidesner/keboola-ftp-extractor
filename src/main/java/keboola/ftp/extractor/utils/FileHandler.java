/*
 */
package keboola.ftp.extractor.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 *
 * @author David Esner <esnerda at gmail.com>
 * @created 2015
 */
public class FileHandler {

	public static void deleteFile(String filePath) throws IOException {
		File file = new File(filePath);
		// make sure file exists
		if (!file.exists()) {
			throw new IOException("File not exists");
		}
		if (file.isDirectory()) {
			// directory is empty, then delete it
			if (file.list().length == 0) {

				file.delete();
				System.out.println("Directory is deleted : " + file.getAbsolutePath());

			} else {
				// list all the directory contents
				String files[] = file.list();

				for (String temp : files) {
					// construct the file structure
					File fileDelete = new File(file, temp);

					// recursive delete
					deleteFile(fileDelete.getAbsolutePath());
				}

				// check the directory again, if empty then delete it
				if (file.list().length == 0) {
					file.delete();
				}
			}

		} else {
			// if file, then delete it
			file.delete();
		}
	}

	public static void deleteFiles(List<String> filePaths) throws IOException {
		for (String fPath : filePaths) {
			File file = new File(fPath);
			deleteFile(fPath);

		}
	}

	public static void deleteFilesInFolder(Collection<String> fileNames, String folder) throws IOException {
		for (String fname : fileNames) {
			File file = new File(folder + File.separator + fname);
			deleteFile(folder + File.separator + fname);

		}
	}

	public static void convertFileCharset(File source, Charset srcEncoding, Charset tgtEncoding) throws Exception {
		File tmp = new File(source.getParent() + File.separator + "tempRes");
		copyAndConvertToCharset(source, tmp, srcEncoding, tgtEncoding);
		source.delete();
		tmp.renameTo(source);
	}

	public static File copyAndConvertToCharset(File source, File target, Charset srcEncoding, Charset tgtEncoding)	throws Exception {
		try (	
				BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(source), srcEncoding));
				BufferedWriter bw = new BufferedWriter(	new OutputStreamWriter(new FileOutputStream(target), tgtEncoding));
			) {
			char[] buffer = new char[16384];
			int read;
			while ((read = br.read(buffer)) != -1)
				bw.write(buffer, 0, read);
		} catch (Exception e) {
			throw e;
		}
		return target;
	}

	/**
	 * Decompress ZIP files and return list of result file names
	 * @param outFolderPath
	 * @param zipFile
	 * @return
	 * @throws Exception
	 */
	public static List<String> decompressZipFile(String outFolderPath, File zipFile) throws Exception {
		byte[] buffer = new byte[1024];
		List<String> resultFileNames = new ArrayList<>();

		ZipInputStream zis = null;
		try {
			// get the zip file content
			zis = new ZipInputStream(new FileInputStream(zipFile));
			// get the zipped file list entry
			ZipEntry ze = zis.getNextEntry();

			while (ze != null) {

				File newFile = new File(outFolderPath + File.separator + ze.getName());

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

	/**
	 * Decompress GZIP files and return list of result file names
	 * @param outFolderPath
	 * @param zipFile
	 * @return
	 * @throws Exception
	 */
	public static List<String> decompressGZip(String outFolderPath, File zipFile) throws Exception {
		byte[] buffer = new byte[1024];
		List<String> resultFileNames = new ArrayList<>();
		
		GZIPInputStream zis = null;
		try {
			// get the zip file content
			zis = new GZIPInputStream(new FileInputStream(zipFile));
							
				File newFile = new File(outFolderPath + File.separator + zipFile.getName().substring(0, zipFile.getName().length() - 3));
				
				// create all non exists folders else you will hit
				// FileNotFoundException for compressed folder
				new File(newFile.getParent()).mkdirs();
				
				FileOutputStream fos = new FileOutputStream(newFile);
				
				int len;
				while ((len = zis.read(buffer)) > 0) {
					fos.write(buffer, 0, len);
				}
				
				fos.close();
				resultFileNames.add(newFile.getName());
			
			
		} catch (Exception e) {
			throw e;
		} finally {
			try {
				zis.close();
			} catch (IOException e) {
				// nn
			}
			
		}
		return resultFileNames;
	}


}
