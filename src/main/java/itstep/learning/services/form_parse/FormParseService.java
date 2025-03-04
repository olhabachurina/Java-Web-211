package itstep.learning.services.form_parse;

import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;

public interface FormParseService {
    FormParseResult parseRequest( HttpServletRequest req )throws IOException;
}
/*
Завантаження файлів, розбір даних форм
При передачі файлів форми використовують спец. тип запитів - multipart
При прийомі файлів необхідно забезпечити
а) їх тимчасове зберігання - зазвичай у системних ресурсах
б) постійне зберігання - у спеціально відведеному місці / сервісі.
*/