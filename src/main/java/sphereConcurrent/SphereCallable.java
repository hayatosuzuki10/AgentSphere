package sphereConcurrent;

import java.io.Serializable;
import java.util.concurrent.Callable;

public interface SphereCallable<V> extends Callable<V>,Serializable{
}
