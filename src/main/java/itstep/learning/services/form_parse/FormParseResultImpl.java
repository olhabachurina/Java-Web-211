package itstep.learning.services.form_parse;

import org.apache.commons.fileupload2.core.FileItem;

import java.util.Map;

public class FormParseResultImpl implements FormParseResult {
    private final Map<String, String> fields;
    private final Map<String, FileItem> files;

    public FormParseResultImpl(Map<String, String> fields, Map<String, FileItem> files) {
        this.fields = fields;
        this.files = files;
    }

    @Override
    public Map<String, String> getFields() {
        return fields;
    }

    @Override
    public Map<String, FileItem> getFiles() {
        return files;
    }
}