package moe.div.moequickgate.proxy.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 使用参数数组执行进程，不经过 Shell。
 * Executes argument-array processes without invoking a shell.
 */
final class DefaultProcessRunner implements ProcessRunner {
    @Override
    public ProcessResult run(List<String> command, Duration timeout)
            throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        CompletableFuture<String> output = CompletableFuture.supplyAsync(() -> {
            try {
                return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException exception) {
                return "无法读取命令输出：" + exception.getMessage();
            }
        });

        boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            process.waitFor();
        }
        return new ProcessResult(finished ? process.exitValue() : -1, output.join(), !finished);
    }
}
