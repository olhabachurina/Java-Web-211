package itstep.learning.services.kdf;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public interface KdfService {
    String dk(String password, String salt);
}