package com.histar.be.discovery.explore.video;

import com.histar.be.common.exception.BusinessRuleException;

/** A contiguous playback frontier, bounded by the server's clock rather than client duration. */
public final class VideoPlaybackRules {
    private VideoPlaybackRules() {}
    public static double advance(double watched, double position, double elapsedSeconds, double duration) {
        if (!Double.isFinite(position) || position < 0 || position > duration + .5 || elapsedSeconds < 0)
            throw new BusinessRuleException("Vị trí video không hợp lệ.");
        double advance = position - watched;
        if (advance > 15 || advance > elapsedSeconds * 1.15 + 1)
            throw new BusinessRuleException("Chỉ thời gian xem liên tục mới được ghi nhận. Vui lòng phát tiếp từ vị trí đã lưu.");
        return Math.min(duration, Math.max(watched, position));
    }
    public static boolean ready(double watched, double duration) { return watched >= duration * .9; }
}
