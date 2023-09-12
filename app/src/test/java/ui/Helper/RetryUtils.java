package ui.Helper;

import java.util.concurrent.Callable;
import lombok.extern.slf4j.Slf4j;
//Source: https://kymr.github.io/2017/01/09/RetryUtils-with-java8/
@Slf4j
public class RetryUtils {
  private static final int RETRY = 3;
  private static final long DELAY = 6000L;

  @FunctionalInterface
  public interface RunnableWithException {
    void run() throws Exception;
  }

  public static <V> V retry(Callable<V> callable, Throwable throwable, String message) {
    return retryLogics(callable, throwable, message);
  }

  public static void retry(RunnableWithException runnable, Throwable throwable, String message) {
    retryLogics(() -> {
      runnable.run();
      return null;
    }, throwable, message);
  }

  private static <T> T retryLogics(Callable<T> callable, Throwable throwable, String message) {
    int counter = 0;

    while (counter < RETRY) {
      try {
        return callable.call();
      } catch (Exception e) {
        counter++;
        log.error("retry {} / {}, {}", counter, RETRY, message, e);

        try {
          Thread.sleep(DELAY);
        } catch (InterruptedException e1) {
          e1.printStackTrace();
        }
      }
    }

    throw new RuntimeException(throwable);
  }
}