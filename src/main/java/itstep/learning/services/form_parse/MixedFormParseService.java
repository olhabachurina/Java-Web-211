package itstep.learning.services.form_parse;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.fileupload2.core.DiskFileItemFactory;
import org.apache.commons.fileupload2.core.FileItem;
import org.apache.commons.fileupload2.core.FileUploadException;
import org.apache.commons.fileupload2.jakarta.servlet6.JakartaServletFileUpload;

@Singleton
public class MixedFormParseService implements FormParseService {
    private final JakartaServletFileUpload uploader;

    @Inject
    public MixedFormParseService() {
        DiskFileItemFactory factory = DiskFileItemFactory
                .builder()
                .setBufferSize( 1 * 1024 * 1024 )
                .setPath( "C:/tmp" )
                .get();
        uploader = new JakartaServletFileUpload( factory );
        uploader.setSizeMax( 10 * 1024 * 1024 );
    }


    @Override
    public FormParseResult parseRequest( HttpServletRequest req ) throws IOException {
        Map<String, String> fields = new HashMap<>();
        Map<String, FileItem> files = new HashMap<>();

        if( req.getContentType().startsWith( "multipart/form-data" ) ) {
            List<FileItem> fileItems = uploader.parseRequest(req);
            for( FileItem fileItem : fileItems ) {
                if( fileItem.isFormField() ) {
                    fields.put(
                            fileItem.getFieldName(),
                            fileItem.getString( StandardCharsets.UTF_8 )
                    ) ;
                }
                else {
                    files.put(
                            fileItem.getFieldName(),
                            fileItem
                    );
                }
            }
        }
        else {   // не-multipart запит, видобуваємо поля через параметри
            Enumeration<String> names = req.getParameterNames();
            while( names.hasMoreElements() ) {
                String name = names.nextElement() ;
                fields.put( name, req.getParameter( name ) ) ;
            }
        }

        return new FormParseResult() {
            @Override
            public Map<String, String> getFields() {
                return fields;
            }

            @Override
            public Map<String, FileItem> getFiles() {
                return files;
            }
        };
    }

}
/*
Змішана імплементація - якщо запит приходить multipart, то
використовуємо Apache, інакше - Servlet-API
*/