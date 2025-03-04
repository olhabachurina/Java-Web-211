package itstep.learning.services.form_parse;

import java.util.Map;
import org.apache.commons.fileupload2.core.FileItem;
public interface FormParseResult {
    Map<String, String> getFields();
    Map<String, FileItem> getFiles();
}
