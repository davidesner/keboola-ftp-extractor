/*
 */
package keboola.ftp.extractor.state;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.File;
import java.io.IOException;
import java.util.List;
import keboola.ftp.extractor.config.KBCConfig;

/**
 *
 * @author David Esner <esnerda at gmail.com>
 * @created 2016
 */
public class YamlStateWriter {

    public static void writeStateFile(String resultStateFilePath, List<VisitedFolder> visFolders) throws IOException {

        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        File stateFile = new File(resultStateFilePath);
        mapper.writeValue(stateFile, new VisitedFoldersList(visFolders));
    }

}
