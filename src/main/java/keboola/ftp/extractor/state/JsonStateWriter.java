/*
 */
package keboola.ftp.extractor.state;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author David Esner <esnerda at gmail.com>
 * @created 2016
 */
public class JsonStateWriter {

    public static void writeStateFile(String resultStateFilePath, List<VisitedFolder> visFolders) throws IOException {

        final ObjectMapper mapper = new ObjectMapper(new JsonFactory());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        File stateFile = new File(resultStateFilePath);
        mapper.writeValue(stateFile, new VisitedFoldersList(visFolders));
    }

}
