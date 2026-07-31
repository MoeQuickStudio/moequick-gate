package moe.div.moequickgate.utils;

import java.nio.file.Path;
import java.util.Arrays;

/** Test-only child process used by CommandUtilTest. */
public final class CommandTestHelper {
    private CommandTestHelper() {
    }

    public static void main(String[] args) throws Exception {
        switch (args[0]) {
            case "streams" -> {
                System.out.print("标准输出✓");
                System.err.print("错误输出✓");
            }
            case "exit" -> System.exit(7);
            case "sleep" -> Thread.sleep(30_000);
            case "large" -> {
                byte[] stdout = new byte[CommandUtil.MAX_OUTPUT_BYTES + 8192];
                byte[] stderr = new byte[CommandUtil.MAX_OUTPUT_BYTES + 4096];
                Arrays.fill(stdout, (byte) 'x');
                Arrays.fill(stderr, (byte) 'y');
                System.out.write(stdout);
                System.err.write(stderr);
            }
            case "spawn-child" -> {
                Process child = new ProcessBuilder(
                        javaExecutable(),
                        "-cp",
                        System.getProperty("java.class.path"),
                        CommandTestHelper.class.getName(),
                        "sleep")
                        .start();
                System.out.println("childPid=" + child.pid());
                System.out.flush();
                Thread.sleep(30_000);
            }
            case "echo-args" -> System.out.print(String.join("|", Arrays.copyOfRange(args, 1, args.length)));
            default -> throw new IllegalArgumentException("unknown mode");
        }
    }

    private static String javaExecutable() {
        return Path.of(System.getProperty("java.home"), "bin", "java").toString();
    }
}
