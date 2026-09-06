package decok.dfcdvadstf.catframe.ui.extended.notifications;

/**
 * <p>
 * Tracks the animation state of a {@link Notification} currently on screen.<br>
 * Each notification transitions through four phases:
 * </p>
 * <ol>
 *   <li>{@link Phase#SLIDE_IN}  — slides in from the right over {@value #SLIDE_MS} ms.</li>
 *   <li>{@link Phase#VISIBLE}   — fully visible for the notification's duration.</li>
 *   <li>{@link Phase#SLIDE_OUT} — slides back out to the right over {@value #SLIDE_MS} ms.</li>
 *   <li>{@link Phase#DONE}      — animation complete; the entry is removed from the queue.</li>
 * </ol>
 * <p>
 * 跟踪当前在屏幕上显示的 {@link Notification} 的动画状态。<br>
 * 每条通知依次经历四个阶段：
 * </p>
 * <ol>
 *   <li>{@link Phase#SLIDE_IN}  —— 在 {@value #SLIDE_MS} 毫秒内从右侧滑入。</li>
 *   <li>{@link Phase#VISIBLE}   —— 完全可见，持续通知的设定时长。</li>
 *   <li>{@link Phase#SLIDE_OUT} —— 在 {@value #SLIDE_MS} 毫秒内向右侧滑出。</li>
 *   <li>{@link Phase#DONE}      —— 动画完成；该条目从队列中移除。</li>
 * </ol>
 */
public class ActiveNotification {

    /** Slide animation duration in milliseconds. / 滑动动画持续时间（毫秒）。 */
    public static final long SLIDE_MS = 250;

    /**
     * Animation phase of a notification on screen. / 通知在屏幕上的动画阶段。
     */
    public enum Phase {
        /** Sliding in from the right / 从右侧滑入 */
        SLIDE_IN,
        /** Fully visible, countdown running / 完全可见，倒计时进行中 */
        VISIBLE,
        /** Sliding out to the right / 向右侧滑出 */
        SLIDE_OUT,
        /** Animation complete, ready for removal / 动画完成，可移除 */
        DONE
    }

    private final Notification notification;
    private final long startMs;
    private final long visibleMs;
    private Phase phase = Phase.SLIDE_IN;

    /**
     * Wrap a notification for on-screen animation.
     * <p>将一条通知包装为屏幕动画。</p>
     *
     * @param notification the notification data / 通知数据
     */
    public ActiveNotification(Notification notification) {
        this.notification = notification;
        this.startMs = System.currentTimeMillis();
        this.visibleMs = (notification.getDuration() * 1000L) / 20L;
    }

    /** @return the underlying notification data / 底层通知数据 */
    public Notification getNotification() {
        return notification;
    }

    /** @return the current animation phase / 当前动画阶段 */
    public Phase getPhase() {
        return phase;
    }

    /** @return {@code true} when the animation is complete and this entry can be discarded
     *  <p>{@code true} 表示动画已完成，此条目可丢弃。</p> */
    public boolean isDone() {
        return phase == Phase.DONE;
    }

    /**
     * Advance the animation phase based on elapsed time. Called every client tick.
     * <p>根据已流逝的时间推进动画阶段。每客户端 tick 调用。</p>
     */
    public void update() {
        long elapsed = elapsed();
        if (elapsed < SLIDE_MS) {
            phase = Phase.SLIDE_IN;
        } else if (elapsed < SLIDE_MS + visibleMs) {
            phase = Phase.VISIBLE;
        } else if (elapsed < SLIDE_MS * 2 + visibleMs) {
            phase = Phase.SLIDE_OUT;
        } else {
            phase = Phase.DONE;
        }
    }

    /**
     * Returns a {@code [0, 1]} progress value for the slide animation.
     * {@code 1.0} means fully on screen; {@code 0.0} means fully off screen.
     * <p>返回滑动动画的 {@code [0, 1]} 进度值。{@code 1.0} 表示完全在屏幕上；
     * {@code 0.0} 表示完全在屏幕外。</p>
     */
    public float getSlideProgress() {
        long e = elapsed();
        switch (phase) {
            case SLIDE_IN:
                return clamp01((float) e / SLIDE_MS);
            case VISIBLE:
                return 1.0F;
            case SLIDE_OUT:
                return clamp01(1.0F - (float) (e - SLIDE_MS - visibleMs) / SLIDE_MS);
            case DONE:
                return 0.0F;
            default:
                return 0.0F;
        }
    }

    /**
     * Returns a {@code [0, 1]} progress value representing remaining visible time.
     * Used to drive the progress bar ({@code 1.0} = full, {@code 0.0} = expired).
     * <p>返回表示剩余可见时间的 {@code [0, 1]} 进度值。用于驱动进度条
     * （{@code 1.0} = 满，{@code 0.0} = 已过期）。</p>
     */
    public float getVisibleProgress() {
        long e = elapsed();
        if (e < SLIDE_MS) {
            return 1.0F;
        }
        if (e > SLIDE_MS + visibleMs) {
            return 0.0F;
        }
        return clamp01(1.0F - (float) (e - SLIDE_MS) / visibleMs);
    }

    /** @return milliseconds since this notification was created / 自创建以来经过的毫秒数 */
    private long elapsed() {
        return System.currentTimeMillis() - startMs;
    }

    /** Clamp a float to [0, 1]. / 将浮点值钳位到 [0, 1]。 */
    private static float clamp01(float v) {
        return Math.max(0.0F, Math.min(1.0F, v));
    }
}
