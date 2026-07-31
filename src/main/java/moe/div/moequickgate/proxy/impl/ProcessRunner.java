package moe.div.moequickgate.proxy.impl;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

/**
 * Phase 4 使用的窄范围进程适配器；Phase 5 将由 CommandUtil 取代。
 * Narrow Phase 4 process adapter; CommandUtil will replace it in Phase 5.
 */
@FunctionalInterface
interface ProcessRunner {
    ProcessResult run(List<String> command, Duration timeout) throws IOException, InterruptedException;
}
