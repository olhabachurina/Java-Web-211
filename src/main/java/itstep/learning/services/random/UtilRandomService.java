package itstep.learning.services.random;

import com.google.inject.Singleton;
import java.util.Random;

@Singleton
public class UtilRandomService implements RandomService {
    private final Random random = new Random();

    @Override
    public int randomInt() {
        return random.nextInt();
    }
}