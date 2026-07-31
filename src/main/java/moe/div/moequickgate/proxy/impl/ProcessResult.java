package moe.div.moequickgate.proxy.impl;

/**
 * 单次外部进程执行结果。
 * Result of one external process invocation.
 */
record ProcessResult(int exitCode, String output, boolean timedOut) {
}
