package com.zifang.z.script.engine.sandbox;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 沙箱执行器 (D03 · FEATURE055)。
 * <p>
 * 把脚本放到独立的守护线程执行，施加墙钟超时；超时后 {@link Future#cancel(boolean)} 中断线程。
 * 配合 Groovy 的 {@code @ThreadInterrupt} AST 变换，可打断脚本里的死循环 / 长时间运算。
 * <p>
 * 线程池有界，避免脚本大量并发拖垮宿主进程；队列为 {@link SynchronousQueue} 直接移交，
 * 超过 {@code maxThreads} 时以 {@link ThreadPoolExecutor.CallerRunsPolicy} 回退到调用线程
 * (此时仍受 Future 超时保护)。
 */
public class SandboxExecutor {

    private final ThreadPoolExecutor pool;

    public SandboxExecutor(int maxThreads) {
        ThreadFactory factory = new ThreadFactory() {
            private final AtomicInteger seq = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "z-script-sandbox-" + seq.incrementAndGet());
                t.setDaemon(true);
                return t;
            }
        };
        this.pool = new ThreadPoolExecutor(
                0, Math.max(1, maxThreads),
                30L, TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                factory,
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    /**
     * 在受控线程内执行任务，超过 {@code timeoutMs} 则中断并抛 {@link ScriptTimeoutException}。
     *
     * @param task      脚本执行体
     * @param timeoutMs 墙钟超时 (毫秒)
     * @return 任务返回值
     */
    public Object execute(Callable<Object> task, long timeoutMs) {
        Future<Object> future = pool.submit(task);
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true); // 中断线程 -> 触发 @ThreadInterrupt 检查点
            throw new ScriptTimeoutException("脚本执行超时(" + timeoutMs + "ms)，已强制中断", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof ScriptSecurityException) {
                throw (ScriptSecurityException) cause;
            }
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new RuntimeException(cause.getMessage(), cause);
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw new RuntimeException("脚本执行被中断", e);
        }
    }

    public void shutdown() {
        pool.shutdownNow();
    }
}
