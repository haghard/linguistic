package linguistic.ps.cassandraBPlusTree;

import java.io.*;

public class HeapUtils {

  public static String logNativeMemory() {
    StringBuilder builder = new StringBuilder();
    try {
      Long processId = ProcessHandle.current().pid();

      String jcmdPath = getJcmdPath();
      String jcmdCommand = jcmdPath == null ? "jcmd" : jcmdPath;
      String[] nmCommands = new String[]{jcmdCommand, processId.toString(), "VM.native_memory summary"};

      //String[] histoCommands = new String[]{ jcmdCommand, processId.toString(), "GC.class_histogram" };
      //logProcessOutput(Runtime.getRuntime().exec(histoCommands));

      //logProcessOutput(Runtime.getRuntime().exec(nmCommands));
      Process p = Runtime.getRuntime().exec(nmCommands);
      try (BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
        String line;
        while ((line = input.readLine()) != null) {
          builder.append(line).append('\n');
        }
      }

      return builder.toString();
    } catch (Throwable e) {
      return builder.toString();
    }
  }

  private static String getJcmdPath() {
    String javaHome = System.getenv("JAVA_HOME");
    if (javaHome == null) {
      return null;
    }

    File javaBinDirectory = new File(javaHome, "bin");
    File[] files = javaBinDirectory.listFiles((dir, name) -> name.startsWith("jcmd"));

    return files.length == 0 ? null : files[0].getPath();
  }

  private static void logProcessOutput(Process p) throws IOException {
    try (BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
      StringBuilder builder = new StringBuilder();
      String line;
      while ((line = input.readLine()) != null) {
        builder.append(line).append('\n');
      }
    }
  }
}
