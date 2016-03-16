/*
 */
package keboola.ftp.extractor.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author David Esner <esnerda at gmail.com>
 * @created 2015
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class KBCConfig {

    String validationError;
    @JsonProperty("storage")
    private KBCStorage storage;
    @JsonProperty("parameters")
    private KBCParameters params;

    public KBCConfig() {
        validationError = null;
    }

    public KBCConfig(KBCStorage storage, KBCParameters params) {
        this.storage = storage;
        this.params = params;
    }

    public boolean validate() {
        List<String> misPar = params.getMissingFields();
        if (misPar == null) {
            try {
                if (!params.validateMappings()) {
                    return false;
                }
            } catch (ValidationException ex) {
                this.validationError = ex.getMessage();
                return false;
            }
            return true;
        } else {
            setValidationError(misPar);
            return false;
        }

    }

    public String getValidationError() {
        return validationError;
    }

    private void setValidationError(List<String> missingFields) {
        this.validationError = "Required config fields are missing: ";
        int i = 0;
        for (String fld : missingFields) {
            if (i < missingFields.size()) {
                this.validationError += fld + ", ";
            } else {
                this.validationError += fld;
            }
        }
    }

    public KBCStorage getStorage() {
        return storage;
    }

    public void setStorage(KBCStorage storage) {
        this.storage = storage;
    }

    public KBCParameters getParams() {
        return params;
    }

    public void setParams(KBCParameters params) {
        this.params = params;
    }

    public Map<Integer, KBCOutputMapping> getOutputTables() {
        return this.storage.getOutputTables().getTables();
    }
}
