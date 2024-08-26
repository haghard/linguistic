package linguistic.ps.cassandraBPlusTree;

import java.io.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class HeapUtils {
  static ZoneId defaultTZ = ZoneId.of(java.util.TimeZone.getDefault().getID());
  static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z");

  public static String logNativeMemory() {
    StringBuilder builder = new StringBuilder();
    try {
      Long processId = ProcessHandle.current().pid();

      String jcmdPath = getJcmdPath();
      String jcmdCommand = jcmdPath == null ? "jcmd" : jcmdPath;
      String[] cmd = new String[]{jcmdCommand, processId.toString(), "VM.native_memory summary"};

      builder
        .append(formatter.format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(System.currentTimeMillis()), defaultTZ)))
        .append(':')
        .append('\n')
        .append(cmd[0] + " " + cmd[1] + " " + cmd[2])
        .append('\n')
        .append("PID:");


      //logProcessOutput(Runtime.getRuntime().exec(histoCommands));
      Process p = Runtime.getRuntime().exec(cmd);
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

/*
Java Heap (reserved=50 331 648KB, committed=1 048 576 KB)
          (mmap: reserved=50 331 648 KB, committed=1 048 576 KB)


Java Heap (reserved=37158912KB, committed=774 144 KB)
[info]    (mmap: reserved=37 158 912KB, committed=774 144KB)

*/