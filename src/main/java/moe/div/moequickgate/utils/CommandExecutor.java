package moe.div.moequickgate.utils;

import java.time.Duration;
import java.util.List;

/**
 * 可注入的外部命令执行能力。
 * Injectable external command execution capability.
 */
@FunctionalInterface
public interface CommandExecutor {
    CommandResult execute(List<String> command, Duration timeout);
}
